package com.github.grishberg.scripting

import eu.printingin3d.javascad.coords.Angles3d
import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.models.Abstract3dModel
import eu.printingin3d.javascad.models.Cube
import eu.printingin3d.javascad.models.Cylinder
import eu.printingin3d.javascad.models.Empty3dModel
import eu.printingin3d.javascad.models.Prism
import eu.printingin3d.javascad.models.Sphere
import eu.printingin3d.javascad.utils.Color

/**
 * Контекст для DSL-скриптов.
 * Предоставляет примитивы, координаты и CSG-операторы.
 */
class ScriptBindings {

    // Примитивы
    fun cube(size: Double): Abstract3dModel = Cube(size)
    fun cube(x: Double, y: Double, z: Double): Abstract3dModel = Cube(x, y, z)
    fun cylinder(length: Double, radius: Double): Abstract3dModel = Cylinder(length, radius)
    fun cylinder(length: Double, bottomR: Double, topR: Double): Abstract3dModel =
        Cylinder(length, bottomR, topR)
    fun sphere(radius: Double): Abstract3dModel = Sphere(radius)
    fun prism(length: Double, radius: Double, sides: Int): Abstract3dModel =
        Prism(length, radius, sides)
    fun prism(length: Double, r1: Double, r2: Double, sides: Int): Abstract3dModel =
        Prism(length, r1, r2, sides)
    fun emptyModel(): Abstract3dModel = Empty3dModel()

    // Координаты и углы
    fun v3(x: Double, y: Double, z: Double): V3d = V3d(x, y, z)
    fun v3(x: Double, y: Double): V3d = V3d(x, y, 0.0)
    fun angles(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0): Angles3d =
        Angles3d(x, y, z)

    // CSG-операторы как infix-функции
    infix fun Abstract3dModel.union(other: Abstract3dModel): Abstract3dModel =
        this.addModel(other)
    infix fun Abstract3dModel.minus(other: Abstract3dModel): Abstract3dModel =
        this.subtractModel(other)

    // Цвета
    fun Abstract3dModel.color(r: Int, g: Int, b: Int): Abstract3dModel =
        this.withColor(Color(r, g, b))
    fun Abstract3dModel.color(name: String): Abstract3dModel =
        when (name.lowercase()) {
            "red" -> this.withColor(Color.RED)
            "green" -> this.withColor(Color.GREEN)
            "blue" -> this.withColor(Color.BLUE)
            "gray" -> this.withColor(Color.GRAY)
            "white" -> this.withColor(Color.WHITE)
            "black" -> this.withColor(Color.BLACK)
            else -> this.withColor(Color.GRAY)
        }

    // Повторение
    fun repeat(count: Int, block: (Int) -> Abstract3dModel): Abstract3dModel {
        var result: Abstract3dModel = emptyModel()
        for (i in 0 until count) {
            result = result.addModel(block(i))
        }
        return result
    }

    fun Abstract3dModel.along(axis: Axis, step: Double, count: Int, block: (Int) -> Abstract3dModel): Abstract3dModel {
        var result: Abstract3dModel = this
        for (i in 0 until count) {
            val m = block(i)
            result = result.addModel(
                when (axis) {
                    Axis.X -> m.moveX(i * step)
                    Axis.Y -> m.moveY(i * step)
                    Axis.Z -> m.moveZ(i * step)
                }
            )
        }
        return result
    }

    enum class Axis { X, Y, Z }

    // Математика
    val PI: Double = Math.PI
    val DEG_TO_RAD: Double = Math.PI / 180.0
    fun deg(degrees: Double): Double = degrees * Math.PI / 180.0
}
