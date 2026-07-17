package com.github.grishberg.scripting

import eu.printingin3d.javascad.manifold.Manifold3dEngine
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Cad3dIntegrationTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun initManifold() {
            Manifold3dEngine.initialize()
        }
    }

    private fun getCad3dEvaluator(): ScriptEvaluator {
        val jars = mutableListOf<String>()

        val javascadJar = File("../javascad/build/libs/javascad-1.0.jar")
        if (javascadJar.exists()) jars.add(javascadJar.absolutePath)

        val cad3dJar = File("../cad3d/build/libs/cad3d-1.0.1.jar")
        if (cad3dJar.exists()) jars.add(cad3dJar.absolutePath)

        val kbdCoreJar = File("../kbd_core/build/libs/kbd_core-1.0.1.jar")
        if (kbdCoreJar.exists()) jars.add(kbdCoreJar.absolutePath)

        val pluginJar = File("../plugin/build/libs/plugin-1.0.jar")
        if (pluginJar.exists()) jars.add(pluginJar.absolutePath)

        val scriptingJar = File("build/libs/scripting-1.0.0.jar")
        if (scriptingJar.exists()) jars.add(scriptingJar.absolutePath)

        val manifoldJar = File("../libs/manifold3d-0.1.4.jar")
        if (manifoldJar.exists()) jars.add(manifoldJar.absolutePath)

        val kotlinStdlib = File("../libs/kotlin-stdlib-1.8.0.jar")
        if (kotlinStdlib.exists()) jars.add(kotlinStdlib.absolutePath)

        val coroutinesJar = File("../libs/kotlinx-coroutines-core-1.8.0.jar")
        if (coroutinesJar.exists()) jars.add(coroutinesJar.absolutePath)

        return ScriptEvaluator(jars)
    }

    @Test
    fun `cad3d imports available in script`() {
        val evaluator = getCad3dEvaluator()
        // Проверяем, что импорты cad3d доступны и скрипт компилируется
        val result = evaluator.evaluate(
            """
            val kp = com.github.grishberg.cad3d.plugin.cfg.KeyboardPart.KeyMatrix
            bindings.cube(10.0)
            """.trimIndent()
        )
        assertTrue(result.error.isNullOrEmpty(), "cad3d imports should compile: ${result.error}")
        assertNotNull(result.model, "cad3d imports script should produce a model")
    }

    @Test
    fun `cad3d multi-file script compiles`() {
        val evaluator = getCad3dEvaluator()
        // Проверяем, что мульти-файл с импортами cad3d компилируется
        val dir = File(System.getProperty("java.io.tmpdir"), "cad3d-test-${System.nanoTime()}")
        dir.mkdirs()
        try {
            File(dir, "Config.kt").writeText("""
                import com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode
                
                object Cfg {
                    val thumbMode = ThumbClusterMode.SingleColumn3Buttons
                    val columns = 5
                }
            """.trimIndent())
            File(dir, "Main.kt").writeText("""
                fun scriptMain(): Abstract3dModel {
                    return bindings.cube(Cfg.columns * 19.0).moveZ(3.0)
                }
            """.trimIndent())

            val result = evaluator.evaluateScriptDir(dir.absolutePath)
            assertTrue(result.error.isNullOrEmpty(), "cad3d multi-file should compile: ${result.error}")
            assertNotNull(result.model, "cad3d multi-file should produce a model")
        } finally {
            dir.deleteRecursively()
        }
    }
}
