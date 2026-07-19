package com.github.grishberg.scripting

import com.github.grishberg.javascad.manifold.Manifold3dEngine
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ScriptAdvancedTest {

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

    // ===== CLASS =====

    @Test
    fun `class with properties and constructor`() {
        val result = getEvaluator().evaluate("""
            class Box(val w: Double, val h: Double, val d: Double) {
                fun toModel() = bindings.cube(w, h, d)
            }
            Box(20.0, 10.0, 5.0).toModel()
        """.trimIndent())
        assertNull(result.error)
        assertNotNull(result.model)
    }

    @Test
    fun `class with secondary constructor`() {
        val result = getEvaluator().evaluate("""
            class Box(val w: Double, val h: Double, val d: Double) {
                constructor(size: Double) : this(size, size, size)
                fun toModel() = bindings.cube(w, h, d)
            }
            Box(10.0).toModel()
        """.trimIndent())
        assertNull(result.error)
        assertNotNull(result.model)
    }

    // ===== INTERFACE =====

    @Test
    fun `interface with implementation`() {
        val result = getEvaluator().evaluate("""
            interface Shape {
                fun build(): Abstract3dModel
            }
            class CylinderShape(val length: Double, val radius: Double) : Shape {
                override fun build() = bindings.cylinder(length, radius)
            }
            CylinderShape(10.0, 3.0).build()
        """.trimIndent())
        assertNull(result.error)
        assertNotNull(result.model)
    }

    @Test
    fun `interface with default implementation`() {
        val result = getEvaluator().evaluate("""
            interface Shape {
                fun build(): Abstract3dModel = bindings.sphere(1.0)
            }
            class DefaultShape : Shape
            DefaultShape().build()
        """.trimIndent())
        assertNull(result.error)
        assertNotNull(result.model)
    }

    // ===== EXTENSION FUNCTIONS =====

    @Test
    fun `extension function on Abstract3dModel`() {
        val result = getEvaluator().evaluate("""
            fun Abstract3dModel.moveToCenter(cx: Double, cy: Double, cz: Double) =
                this.move(cx, cy, cz)

            bindings.cube(5.0).moveToCenter(10.0, 0.0, 0.0)
        """.trimIndent())
        assertNull(result.error)
        assertNotNull(result.model)
    }

    @Test
    fun `multiple chained extensions`() {
        val result = getEvaluator().evaluate("""
            fun Abstract3dModel.movedY(y: Double) =
                this.moveY(y)

            fun Abstract3dModel.movedZ(z: Double) =
                this.moveZ(z)

            bindings.cube(5.0).movedY(10.0).movedZ(5.0)
        """.trimIndent())
        assertNull(result.error)
        assertNotNull(result.model)
    }

    // ===== LAMBDAS =====

    @Test
    fun `lambda with parameters`() {
        val result = getEvaluator().evaluate("""
            val makeCube: (Double) -> Abstract3dModel = { s -> bindings.cube(s) }
            makeCube(10.0)
        """.trimIndent())
        assertNull(result.error)
        assertNotNull(result.model)
    }

    @Test
    fun `lambda used in collection operations`() {
        val result = getEvaluator().evaluate("""
            val sizes = listOf(5.0, 10.0, 15.0)
            var combined: Abstract3dModel = bindings.emptyModel()
            sizes.forEach { s ->
                combined = combined.addModel(bindings.cube(s).moveX(s))
            }
            combined
        """.trimIndent())
        assertNull(result.error)
        assertNotNull(result.model)
    }

    @Test
    fun `map and reduce in lambda`() {
        val result = getEvaluator().evaluate("""
            val sizes = listOf(5.0, 10.0, 15.0)
            val cubes = sizes.map { s -> bindings.cube(s).moveX(s * 2.0) }
            cubes.reduce { a, b -> a.addModel(b) }
        """.trimIndent())
        assertNull(result.error)
        assertNotNull(result.model)
    }

    @Test
    fun `inline lambda with receiver`() {
        val result = getEvaluator().evaluate("""
            bindings.repeat(4) { i ->
                bindings.cube(5.0).moveX(i * 10.0).moveZ(i * 2.0)
            }
        """.trimIndent())
        assertNull(result.error)
        assertNotNull(result.model)
    }

    // ===== DATA CLASS =====

    @Test
    fun `data class with copy`() {
        val result = getEvaluator().evaluate("""
            data class Size(val w: Double, val h: Double, val d: Double) {
                fun toModel() = bindings.cube(w, h, d)
            }
            val s1 = Size(10.0, 5.0, 5.0)
            val s2 = s1.copy(w = 20.0)
            s1.toModel().addModel(s2.toModel().moveX(30.0))
        """.trimIndent())
        assertNull(result.error)
        assertNotNull(result.model)
    }

    // ===== ENUM =====

    @Test
    fun `enum with properties`() {
        val result = getEvaluator().evaluate("""
            enum class Preset(val w: Double, val h: Double, val d: Double) {
                SMALL(5.0, 5.0, 5.0),
                MEDIUM(10.0, 10.0, 10.0),
                LARGE(20.0, 20.0, 20.0)
            }
            bindings.cube(Preset.MEDIUM.w, Preset.MEDIUM.h, Preset.MEDIUM.d)
        """.trimIndent())
        assertNull(result.error)
        assertNotNull(result.model)
    }

    // ===== WHEN expression =====

    @Test
    fun `when expression with types`() {
        val result = getEvaluator().evaluate("""
            fun makeShape(type: String) = when (type) {
                "box" -> bindings.cube(10.0)
                "ball" -> bindings.sphere(5.0)
                else -> bindings.cylinder(10.0, 3.0)
            }
            makeShape("ball")
        """.trimIndent())
        assertNull(result.error)
        assertNotNull(result.model)
    }

    // ===== SEaled class =====

    @Test
    fun `sealed class with when`() {
        val result = getEvaluator().evaluate("""
            sealed class Shape {
                abstract fun build(): Abstract3dModel
            }
            class BoxShape(val size: Double) : Shape() {
                override fun build() = bindings.cube(size)
            }
            class BallShape(val radius: Double) : Shape() {
                override fun build() = bindings.sphere(radius)
            }
            val s: Shape = BallShape(5.0)
            when (s) {
                is BoxShape -> s.build()
                is BallShape -> s.build()
            }
        """.trimIndent())
        assertNull(result.error)
        assertNotNull(result.model)
    }

    // ===== COMPLEX: class + interface + extension + lambda =====

    @Test
    fun `complex class implementing interface with extension and lambda`() {
        val result = getEvaluator().evaluate("""
            interface Component {
                fun render(): Abstract3dModel
            }

            class BoxComponent(
                val size: Double,
                val transform: (Abstract3dModel) -> Abstract3dModel
            ) : Component {
                override fun render() = transform(bindings.cube(size))
            }

            fun Abstract3dModel.mirrorX(): Abstract3dModel =
                this.addModel(Mirror.mirrorX(this))

            val right = BoxComponent(5.0) { it.moveX(10.0) }
            val left = BoxComponent(5.0) { it.moveX(-10.0) }
            right.render().addModel(left.render())
        """.trimIndent())
        assertNull(result.error)
        assertNotNull(result.model)
    }

    // ===== FOR LOOP + COLLECTION =====

    @Test
    fun `for loop with index and collection`() {
        val result = getEvaluator().evaluate("""
            val positions = listOf(
                V3d(0.0, 0.0, 0.0),
                V3d(10.0, 0.0, 0.0),
                V3d(0.0, 10.0, 0.0)
            )
            var result: Abstract3dModel = bindings.emptyModel()
            for ((i, pos) in positions.withIndex()) {
                result = result.addModel(
                    bindings.sphere(2.0).move(pos.x, pos.y, pos.z)
                )
            }
            result
        """.trimIndent())
        assertNull(result.error)
        assertNotNull(result.model)
    }

    // ===== NESTED CLASS =====

    @Test
    fun `nested class`() {
        val result = getEvaluator().evaluate("""
            class Factory {
                class Builder {
                    private var size = 10.0
                    fun setSize(s: Double) = apply { size = s }
                    fun build() = bindings.cube(size)
                }
                companion object {
                    fun builder() = Builder()
                }
            }
            Factory.builder().setSize(15.0).build()
        """.trimIndent())
        assertNull(result.error)
        assertNotNull(result.model)
    }

    // ===== COMPANION OBJECT + FACTORY =====

    @Test
    fun `companion object factory`() {
        val result = getEvaluator().evaluate("""
            class Part(val model: Abstract3dModel) {
                companion object {
                    fun box(s: Double) = Part(bindings.cube(s))
                    fun ball(r: Double) = Part(bindings.sphere(r))
                }
            }
            Part.ball(5.0).model
        """.trimIndent())
        assertNull(result.error)
        assertNotNull(result.model)
    }

    // ===== GENERIC =====

    @Test
    fun `generic class`() {
        val result = getEvaluator().evaluate("""
            class Holder<T>(val value: T)
            val h = Holder(bindings.cube(10.0))
            h.value
        """.trimIndent())
        assertNull(result.error)
        assertNotNull(result.model)
    }

    // ===== NAMED AND DEFAULT ARGUMENTS =====

    @Test
    fun `named and default arguments`() {
        val result = getEvaluator().evaluate("""
            fun makeBox(w: Double = 10.0, h: Double = 10.0, d: Double = 10.0) =
                bindings.cube(w, h, d)

            makeBox(d = 20.0)
        """.trimIndent())
        assertNull(result.error)
        assertNotNull(result.model)
    }

    // ===== PROPERTY DELEGATE =====

    @Test
    fun `by lazy delegate`() {
        val result = getEvaluator().evaluate("""
            val cached by lazy {
                bindings.cube(100.0, 60.0, 15.0).subtractModel(bindings.cylinder(20.0, 3.0))
            }
            cached
        """.trimIndent())
        assertNull(result.error)
        assertNotNull(result.model)
    }

    // ===== MULTI-FILE =====

    @Test
    fun `multi file class in one file entry point in another`() {
        val dir = createScriptDir(
            "Shapes.kt" to """
                class Box(val size: Double) {
                    fun build() = bindings.cube(size)
                }
                fun Abstract3dModel.doubled() = this.addModel(this)
            """.trimIndent(),
            "Main.kt" to """
                fun scriptMain(): Abstract3dModel {
                    return Box(10.0).build().doubled()
                }
            """.trimIndent()
        )
        val result = getEvaluator().evaluateScriptDir(dir.absolutePath)
        assertNull(result.error)
        assertNotNull(result.model)
    }

    @Test
    fun `multi file interface implementation across files`() {
        val dir = createScriptDir(
            "Component.kt" to """
                interface Component {
                    fun render(): Abstract3dModel
                }
                class SphereComponent(val radius: Double) : Component {
                    override fun render() = bindings.sphere(radius)
                }
            """.trimIndent(),
            "Main.kt" to """
                fun scriptMain(): Abstract3dModel {
                    val c1: Component = SphereComponent(5.0)
                    val c2: Component = SphereComponent(3.0)
                    return c1.render().addModel(c2.render().moveX(10.0))
                }
            """.trimIndent()
        )
        val result = getEvaluator().evaluateScriptDir(dir.absolutePath)
        assertNull(result.error)
        assertNotNull(result.model)
    }

    @Test
    fun `multi file sealed class with multiple implementations`() {
        val dir = createScriptDir(
            "Shape.kt" to """
                sealed class Shape {
                    abstract fun build(): Abstract3dModel
                }
                class CubeShape(val size: Double) : Shape() {
                    override fun build() = bindings.cube(size)
                }
                class SphereShape(val radius: Double) : Shape() {
                    override fun build() = bindings.sphere(radius)
                }
            """.trimIndent(),
            "Renderer.kt" to """
                fun renderAll(shapes: List<Shape>): Abstract3dModel {
                    var result = bindings.emptyModel()
                    for ((i, s) in shapes.withIndex()) {
                        result = result.addModel(s.build().moveX(i * 20.0))
                    }
                    return result
                }
            """.trimIndent(),
            "Main.kt" to """
                fun scriptMain(): Abstract3dModel {
                    val shapes = listOf<Shape>(
                        CubeShape(10.0),
                        SphereShape(5.0),
                        CubeShape(8.0)
                    )
                    return renderAll(shapes)
                }
            """.trimIndent()
        )
        val result = getEvaluator().evaluateScriptDir(dir.absolutePath)
        assertNull(result.error)
        assertNotNull(result.model)
    }

    @Test
    fun `multi file extension function defined in separate file`() {
        val dir = createScriptDir(
            "Extensions.kt" to """
                fun Abstract3dModel.movedY(y: Double) = this.moveY(y)
                fun Abstract3dModel.movedZ(z: Double) = this.moveZ(z)
                infix fun Abstract3dModel.with(other: Abstract3dModel) = this.addModel(other)
            """.trimIndent(),
            "Main.kt" to """
                fun scriptMain(): Abstract3dModel {
                    val base = bindings.cube(10.0).movedY(5.0)
                    val top = bindings.sphere(3.0).movedZ(10.0)
                    return base with top
                }
            """.trimIndent()
        )
        val result = getEvaluator().evaluateScriptDir(dir.absolutePath)
        assertNull(result.error)
        assertNotNull(result.model)
    }

    @Test
    fun `multi file lambda and collection helpers in separate file`() {
        val dir = createScriptDir(
            "Helpers.kt" to """
                fun grid(size: Int, spacing: Double, factory: (Int, Int) -> Abstract3dModel): Abstract3dModel {
                    var result = bindings.emptyModel()
                    for (x in 0 until size) {
                        for (y in 0 until size) {
                            result = result.addModel(factory(x, y).moveX(x * spacing).moveY(y * spacing))
                        }
                    }
                    return result
                }
            """.trimIndent(),
            "Main.kt" to """
                fun scriptMain(): Abstract3dModel {
                    return grid(3, 12.0) { x, y ->
                        if ((x + y) % 2 == 0) bindings.cube(8.0) else bindings.sphere(4.0)
                    }
                }
            """.trimIndent()
        )
        val result = getEvaluator().evaluateScriptDir(dir.absolutePath)
        assertNull(result.error)
        assertNotNull(result.model)
    }

    @Test
    fun `multi file data class in one file used in entry point`() {
        val dir = createScriptDir(
            "Config.kt" to """
                data class BoxConfig(val w: Double, val h: Double, val d: Double) {
                    fun toModel() = bindings.cube(w, h, d)
                }
                data class HoleConfig(val radius: Double, val length: Double) {
                    fun toModel() = bindings.cylinder(length, radius)
                }
            """.trimIndent(),
            "Main.kt" to """
                fun scriptMain(): Abstract3dModel {
                    val base = BoxConfig(100.0, 60.0, 15.0).toModel()
                    val hole = HoleConfig(3.0, 20.0).toModel()
                    return base.subtractModel(hole)
                }
            """.trimIndent()
        )
        val result = getEvaluator().evaluateScriptDir(dir.absolutePath)
        assertNull(result.error)
        assertNotNull(result.model)
    }

    private fun createScriptDir(vararg files: Pair<String, String>): File {
        val dir = File(tempDir, "dsl-script-test-${System.nanoTime()}").apply { mkdirs() }
        for ((name, content) in files) {
            File(dir, name).writeText(content)
        }
        return dir
    }

    private val tempDir = File(System.getProperty("java.io.tmpdir")).also { it.mkdirs() }
}
