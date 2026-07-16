package com.github.grishberg.scripting

import eu.printingin3d.javascad.models.Abstract3dModel
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URL
import java.net.URLClassLoader

/**
 * Результат выполнения скрипта.
 */
data class ScriptResult(
    val model: Abstract3dModel? = null,
    val error: String? = null,
    val stdout: String = "",
    val compilationTimeMs: Long = 0,
)

/**
 * Выполняет DSL-скрипты для 3D-моделирования.
 * Компилирует через встроенный Kotlin Compiler API.
 */
class ScriptEvaluator(
    classPaths: List<String>,
) {
    private val bindings = ScriptBindings()
    private val jarFiles: List<File> = classPaths.map(::File).filter { it.exists() }
    private val tempDir = File(System.getProperty("java.io.tmpdir"), "dsl-script-cache").apply { mkdirs() }

    private val extraClasspath: String = jarFiles.joinToString(File.pathSeparator) { it.absolutePath }

    fun evaluate(scriptSource: String): ScriptResult {
        val start = System.currentTimeMillis()
        return try {
            val wrapped = wrapScript(scriptSource)
            val outputDir = compileScript(wrapped)
            val model = executeCompiled(outputDir)
            ScriptResult(
                model = model,
                compilationTimeMs = System.currentTimeMillis() - start
            )
        } catch (e: Exception) {
            ScriptResult(
                error = buildErrorString(e),
                compilationTimeMs = System.currentTimeMillis() - start
            )
        }
    }

    fun evaluateFile(filePath: String): ScriptResult {
        return evaluate(File(filePath).readText())
    }

    private val fileImports = """
import eu.printingin3d.javascad.models.*
import eu.printingin3d.javascad.coords.*
import eu.printingin3d.javascad.tranzitions.*
import eu.printingin3d.javascad.utils.*
import com.github.grishberg.scripting.ScriptBindings

"""

    private val globalDecls = """
infix fun Abstract3dModel.union(other: Abstract3dModel) = this.addModel(other)
infix fun Abstract3dModel.minus(other: Abstract3dModel) = this.subtractModel(other)

var bindings = ScriptBindings()

"""

    fun evaluateScriptDir(scriptDir: String): ScriptResult {
        val start = System.currentTimeMillis()
        return try {
            val dir = File(scriptDir)
            if (!dir.isDirectory) {
                return ScriptResult(error = "Not a directory: $scriptDir", compilationTimeMs = System.currentTimeMillis() - start)
            }

            val ktFiles = dir.listFiles { _, name -> name.endsWith(".kt") }?.sortedBy { it.name }
                    ?: emptyList()
            if (ktFiles.isEmpty()) {
                return ScriptResult(error = "No .kt files in: $scriptDir", compilationTimeMs = System.currentTimeMillis() - start)
            }

            val outputDir = compileMultiFile(ktFiles)
            val model = executeMultiFile(outputDir, ktFiles)
            ScriptResult(model = model, compilationTimeMs = System.currentTimeMillis() - start)
        } catch (e: Exception) {
            ScriptResult(error = buildErrorString(e), compilationTimeMs = System.currentTimeMillis() - start)
        }
    }

    private fun compileMultiFile(ktFiles: List<File>): File {
        val outputDir = File(tempDir, "classes").apply {
            deleteRecursively()
            mkdirs()
        }

        val prefixedDir = File(tempDir, "prefixed").apply {
            deleteRecursively()
            mkdirs()
        }

        // Преамбл добавляем только в первый файл
        val prefixedFiles = ktFiles.mapIndexed { idx, orig ->
            val content = fileImports + (if (idx == 0) globalDecls else "") + orig.readText()
            val target = File(prefixedDir, orig.name)
            target.writeText(content)
            target
        }

        val args = arrayOf(
            "-no-stdlib",
            "-no-reflect",
            "-cp", extraClasspath,
            "-d", outputDir.absolutePath,
            "-jvm-target", "17"
        ) + prefixedFiles.map { it.absolutePath }

        val baos = ByteArrayOutputStream()
        val compiler = K2JVMCompiler()
        val exitCode = compiler.exec(PrintStream(baos, true, "UTF-8"), *args)
        val compilerOutput = String(baos.toByteArray())

        if (exitCode.getCode() != 0) {
            throw ScriptCompilationException("Compilation failed with exit code ${exitCode.getCode()}\n${compilerOutput}")
        }

        return outputDir
    }

    private fun executeMultiFile(classDir: File, ktFiles: List<File>): Abstract3dModel? {
        val urls = (jarFiles.map { it.toURI().toURL() } + classDir.toURI().toURL()).toTypedArray()
        val loader = URLClassLoader(urls, ScriptEvaluator::class.java.classLoader)

        for (file in ktFiles) {
            // Kotlin generates <filename>Kt.class for files with top-level functions
            val className = file.nameWithoutExtension + "Kt"
            try {
                val clazz = loader.loadClass(className)
                val mainMethod = clazz.getMethod("scriptMain")
                return mainMethod.invoke(null) as? Abstract3dModel
            } catch (e: NoSuchMethodException) {
                // not the entry point, try next file
            } catch (e: ClassNotFoundException) {
                // class not found, try next
            }
        }

        return null
    }

    private fun wrapScript(source: String): String {
        val trimmed = source.trimMargin().trim()
        val lines = trimmed.lines()
        // Разделяем на объявления (class/interface/enum/sealed/fun) и код, учитывая вложенность
        val declarations = mutableListOf<String>()
        val expressions = mutableListOf<String>()
        var braceDepth = 0
        var inDeclaration = false
        var firstDeclLine = true
        var expressionBody = false

        for (line in lines) {
            val l = line.trim()
            val isDeclStart = !inDeclaration && (
                l.startsWith("class ") || l.startsWith("data class ") ||
                l.startsWith("interface ") || l.startsWith("enum ") ||
                l.startsWith("sealed ") || l.startsWith("sealed class ") ||
                l.startsWith("fun ") || l.startsWith("typealias ")
            )

            if (isDeclStart) {
                inDeclaration = true
                firstDeclLine = true
                expressionBody = false
                braceDepth = 0
                declarations.add(l)
                braceDepth += countBraces(l)
                if (braceDepth == 0) {
                    // Находим "=" после закрывающей скобки параметров (для fun) или вообще "=" (для typealias)
                    val parensClose = l.lastIndexOf(')')
                    val equalsSearchStart = if (parensClose >= 0) parensClose else 0
                    val equalsIdx = l.indexOf('=', equalsSearchStart)
                    val hasEquals = equalsIdx >= 0
                    val afterEquals = if (hasEquals) l.substring(equalsIdx + 1).trim() else ""
                    when {
                        !hasEquals -> inDeclaration = false
                        afterEquals.isEmpty() -> { expressionBody = true }
                        else -> inDeclaration = false
                    }
                }
            } else if (inDeclaration) {
                declarations.add(line)
                braceDepth += countBraces(l)
                if (expressionBody && !firstDeclLine) {
                    // Expression body: next line after empty = is the body
                    // Check if body is complete (no trailing operator)
                    inDeclaration = false
                } else if (!firstDeclLine && braceDepth <= 0) {
                    inDeclaration = false
                    braceDepth = 0
                }
                firstDeclLine = false
            } else {
                expressions.add(line)
            }
        }

        val declBlock = if (declarations.isNotEmpty()) "\n    ${declarations.joinToString("\n    ")}" else ""
        val scriptBody = expressions.map { "        $it" }.joinToString("\n")

        return """$fileImports
infix fun Abstract3dModel.union(other: Abstract3dModel) = this.addModel(other)
infix fun Abstract3dModel.minus(other: Abstract3dModel) = this.subtractModel(other)

var bindings = ScriptBindings()

class DslScript {$declBlock

    fun execute(): Abstract3dModel = scriptRun {
$scriptBody
    }
    
    private fun scriptRun(block: DslScript.() -> Abstract3dModel): Abstract3dModel = block()
}
"""
    }

    private fun countBraces(s: String): Int {
        var count = 0
        for (c in s) {
            when (c) {
                '{', '(' -> count++
                '}', ')' -> count--
            }
        }
        return count
    }

    private fun compileScript(source: String): File {
        val srcFile = File(tempDir, "DslScript.kt").apply { writeText(source) }

        val outputDir = File(tempDir, "classes").apply {
            deleteRecursively()
            mkdirs()
        }

        val args = arrayOf(
            "-no-stdlib",
            "-no-reflect",
            "-cp", extraClasspath,
            "-d", outputDir.absolutePath,
            "-jvm-target", "17",
            srcFile.absolutePath
        )

        val baos = ByteArrayOutputStream()
        val compiler = K2JVMCompiler()
        val exitCode = compiler.exec(PrintStream(baos, true, "UTF-8"), *args)
        val compilerOutput = String(baos.toByteArray())

        if (exitCode.getCode() != 0) {
            throw ScriptCompilationException("Compilation failed with exit code ${exitCode.getCode()}\n${compilerOutput}")
        }

        return outputDir
    }

    private fun executeCompiled(classDir: File): Abstract3dModel? {
        val urls = (jarFiles.map { it.toURI().toURL() } + classDir.toURI().toURL()).toTypedArray()
        val loader = URLClassLoader(urls, ScriptEvaluator::class.java.classLoader)

        val clazz = loader.loadClass("DslScript")
        val instance = clazz.getDeclaredConstructor().newInstance()

        // Java reflection для вызова run()
        val runMethod = clazz.getMethod("execute")
        return runMethod.invoke(instance) as? Abstract3dModel
    }

    private fun buildErrorString(e: Exception): String {
        return buildString {
            appendLine("${e.javaClass.simpleName}: ${e.message?.take(500)}")
            e.stackTrace.take(8).forEach { appendLine("\tat $it") }
        }
    }

    fun clearCache() {
        tempDir.deleteRecursively()
    }
}

class ScriptCompilationException(message: String) : RuntimeException(message)
