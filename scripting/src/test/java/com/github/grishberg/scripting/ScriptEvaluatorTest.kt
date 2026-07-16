package com.github.grishberg.scripting

import eu.printingin3d.javascad.manifold.Manifold3dEngine
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ScriptEvaluatorTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun initManifold() {
            Manifold3dEngine.initialize()
        }
    }

    private fun getEvaluator(): ScriptEvaluator {
        val jars = mutableListOf<String>()

        val javascadJar = File("../javascad/build/libs/javascad-1.0.jar")
        if (javascadJar.exists()) jars.add(javascadJar.absolutePath)

        val scriptingJar = File("build/libs/scripting-1.0.0.jar")
        if (scriptingJar.exists()) jars.add(scriptingJar.absolutePath)

        val manifoldJar = File("../libs/manifold3d-0.1.4.jar")
        if (manifoldJar.exists()) jars.add(manifoldJar.absolutePath)

        val kotlinStdlib = File("../libs/kotlin-stdlib-1.8.0.jar")
        if (kotlinStdlib.exists()) jars.add(kotlinStdlib.absolutePath)

        return ScriptEvaluator(jars)
    }

    @Test
    fun `simple cube script`() {
        val evaluator = getEvaluator()
        val result = evaluator.evaluate("bindings.cube(10.0)")

        assertNull(result.error)
        assertNotNull(result.model)
    }

    @Test
    fun `csg subtract script`() {
        val evaluator = getEvaluator()
        val result = evaluator.evaluate("""
            val box = bindings.cube(100.0, 60.0, 15.0)
            val hole = bindings.cylinder(20.0, 3.0).move(50.0, 30.0, 0.0)
            box.subtractModel(hole)
        """.trimIndent())

        assertNull(result.error)
        assertNotNull(result.model)
    }

    @Test
    fun `infix operators script`() {
        val evaluator = getEvaluator()
        val result = evaluator.evaluate("""
            val base = bindings.cube(10.0)
            val sphere = bindings.sphere(3.0).move(5.0, 0.0, 0.0)
            base union sphere
        """.trimIndent())

        assertNull(result.error)
        assertNotNull(result.model)
    }

    @Test
    fun `repeat helper script`() {
        val evaluator = getEvaluator()
        val result = evaluator.evaluate("""
            bindings.repeat(3) { i ->
                bindings.cube(5.0).moveX(i * 10.0)
            }
        """.trimIndent())

        assertNull(result.error)
        assertNotNull(result.model)
    }

    @Test
    fun `syntax error returns error`() {
        val evaluator = getEvaluator()
        val result = evaluator.evaluate("bindings.cube(10.0 invalid syntax")

        assertNotNull(result.error)
        assertNull(result.model)
    }
}
