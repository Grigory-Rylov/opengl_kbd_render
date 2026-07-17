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

    private val baseImports = """
import eu.printingin3d.javascad.models.*
import eu.printingin3d.javascad.coords.*
import eu.printingin3d.javascad.tranzitions.*
import eu.printingin3d.javascad.utils.*
import eu.printingin3d.javascad.manifold.*
import eu.printingin3d.javascad.vrl.*
import com.github.grishberg.scripting.ScriptBindings

"""

    private val cad3dImports = """
import com.github.grishberg.cad3d.*
import com.github.grishberg.cad3d.keyboard.*
import com.github.grishberg.cad3d.keyboard.cfg.*
import com.github.grishberg.cad3d.keyboard.matrix.*
import com.github.grishberg.cad3d.keyboard.casebody.*
import com.github.grishberg.cad3d.keyboard.casebody.controllers.*
import com.github.grishberg.cad3d.keyboard.casebody.thumb.*
import com.github.grishberg.cad3d.keyboard.casebody.wall.*
import com.github.grishberg.cad3d.keyboard.plate.*
import com.github.grishberg.cad3d.keyboard.screws.*
import com.github.grishberg.cad3d.keyboard.wristrest.*
import com.github.grishberg.cad3d.keyboard.amoeba.*
import com.github.grishberg.cad3d.trackball.*
import com.github.grishberg.cad3d.util.*
import com.github.grishberg.cad3d.plugin.*
import com.github.grishberg.cad3d.plugin.cfg.*
import com.github.grishberg.cad3d.kbd.core.cfg.*
import com.github.grishberg.javascad.*

"""

    private val hasCad3d: Boolean by lazy {
        jarFiles.any { f ->
            f.name.contains("cad3d", ignoreCase = true) ||
                f.name.contains("kbd_core", ignoreCase = true) ||
                f.name.contains("plugin", ignoreCase = true)
        }
    }

    private val fileImports: String
        get() = baseImports + (if (hasCad3d) cad3dImports else "")

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
            val originalContent = orig.readText()
            val (pkgLine, fileImportsPart, restContent) = extractPackageAndImports(originalContent)
            val content = pkgLine + fileImports + fileImportsPart + (if (idx == 0) globalDecls else "") + restContent
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

    private fun extractPackageAndImports(content: String): Triple<String, String, String> {
        val lines = content.lines()
        var pkgLine = ""
        val importLines = mutableListOf<String>()
        var restStart = 0
        var foundPkg = false
        var inHeader = true

        for ((i, line) in lines.withIndex()) {
            val trimmed = line.trim()
            if (inHeader) {
                if (trimmed.startsWith("package ")) {
                    pkgLine = line
                    restStart = i + 1
                    foundPkg = true
                } else if (trimmed.startsWith("import ")) {
                    importLines.add(line)
                    restStart = i + 1
                } else if (trimmed.isEmpty() && !foundPkg && i < lines.size - 1) {
                    // Skip blank lines before package/import
                    continue
                } else if (trimmed.isEmpty() && foundPkg && i < lines.size - 1 && lines[i + 1].trim().startsWith("import ")) {
                    continue
                } else {
                    inHeader = false
                }
            }
        }

        val pkgStr = if (pkgLine.isNotEmpty()) pkgLine + "\n" else ""
        val importsStr = importLines.joinToString("\n").takeIf { it.isNotEmpty() }?.let { it + "\n" } ?: ""
        val rest = lines.drop(restStart).joinToString("\n")

        return Triple(pkgStr, importsStr, rest)
    }

    private fun executeMultiFile(classDir: File, ktFiles: List<File>): Abstract3dModel? {
        // ktFiles unused — we scan classDir for scriptMain
        val urls = (jarFiles.map { it.toURI().toURL() } + classDir.toURI().toURL()).toTypedArray()
        val loader = URLClassLoader(urls, ScriptEvaluator::class.java.classLoader)

        // Collect all class files from output directory
        val classFiles = classDir.walkTopDown().filter { it.name.endsWith(".class") }.toList()

        for (classFile in classFiles) {
            // Convert path to class name: package/name/NameKt.class -> package.name.NameKt
            val relPath = classDir.toURI().relativize(classFile.toURI()).path.trimStart('/')
            val className = relPath.replace(File.separatorChar, '.').removeSuffix(".class")
            try {
                val clazz = loader.loadClass(className)
                val mainMethod = clazz.getMethod("scriptMain")
                return mainMethod.invoke(null) as? Abstract3dModel
            } catch (e: NoSuchMethodException) {
                // not the entry point
            } catch (e: ClassNotFoundException) {
                // class not found
            } catch (e: java.lang.reflect.InvocationTargetException) {
                throw RuntimeException("Error in scriptMain: ${e.cause?.message}", e.cause)
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
            var current: Throwable? = e
            while (current != null) {
                appendLine("${current.javaClass.simpleName}: ${current.message?.take(500)}")
                current.stackTrace.take(4).forEach { appendLine("\tat $it") }
                val cause = current.cause
                if (cause != null) {
                    appendLine("  Caused by: ${cause.javaClass.simpleName}: ${cause.message?.take(500)}")
                }
                current = cause
            }
        }
    }

    fun clearCache() {
        tempDir.deleteRecursively()
    }
}

class ScriptCompilationException(message: String) : RuntimeException(message)
