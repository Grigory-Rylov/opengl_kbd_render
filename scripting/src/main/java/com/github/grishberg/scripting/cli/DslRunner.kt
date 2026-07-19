package com.github.grishberg.scripting.cli

import com.github.grishberg.scripting.ScriptEvaluator
import com.github.grishberg.scripting.ScriptResult
import com.github.grishberg.javascad.manifold.Manifold3dEngine
import com.github.grishberg.javascad.vrl.FacetGenerationContext
import java.io.File

fun main(args: Array<String>) {
    Manifold3dEngine.initialize()

    val classPaths = System.getProperty("java.class.path")
        .split(File.pathSeparator)
        .map(::File)
        .filter { it.exists() }
        .map { it.absolutePath }

    val evaluator = ScriptEvaluator(classPaths)

    when {
        args.isNotEmpty() -> {
            val input = File(args[0])
            if (!input.exists()) {
                println("[ERROR] Not found: ${input.absolutePath}")
                return
            }

            val result = if (input.isDirectory) {
                // Multi-file: all .kt files in directory, entry point = scriptMain()
                println("[DSL] Multi-file project: ${input.absolutePath}")
                evaluator.evaluateScriptDir(input.absolutePath)
            } else {
                // Single file script
                println("[DSL] Evaluating: ${input.absolutePath}")
                evaluator.evaluateFile(input.absolutePath)
            }

            printResult(result)

            val outArgIdx = if (input.isDirectory) 1 else 1
            val mdl = result.model
            if (mdl != null && args.size > outArgIdx) {
                val outputFile = File(args[outArgIdx])
                val context = FacetGenerationContext.DEFAULT.apply { setFn(60) }
                val mesh = mdl.toNativeMesh(context)
                Manifold3dEngine.exportStl(mesh, outputFile)
                Manifold3dEngine.delete(mesh)
                println("[DSL] Exported STL: ${outputFile.absolutePath}")
            }
        }
        else -> {
            println("[DSL] Kotlin 3D Scripting REPL")
            println("[DSL] Type Kotlin expressions using `bindings` for primitives.")
            println("[DSL] Examples:")
            println("  bindings.cube(10.0)")
            println("  bindings.cube(100.0, 60.0, 15.0).subtractModel(bindings.cylinder(20.0, 3.0))")
            println("[DSL] Type 'exit' or 'quit' to leave.")
            println()

            val reader = java.io.BufferedReader(java.io.InputStreamReader(System.`in`))
            var line: String?
            while (true) {
                print("> ")
                line = reader.readLine() ?: break
                if (line.isBlank()) continue
                if (line.lowercase() in listOf("exit", "quit")) break

                val result = evaluator.evaluate(line)
                printResult(result)
            }
        }
    }

    evaluator.clearCache()
}

private fun printResult(result: ScriptResult) {
    if (result.error != null) {
        println("[ERROR] ${result.error.take(4000)}")
    } else if (result.model != null) {
        println("[OK] Model created (${result.compilationTimeMs}ms)")
    } else {
        println("[WARN] Script executed but returned null model (${result.compilationTimeMs}ms)")
    }
}
