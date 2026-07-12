package com.github.grishberg.csg.model

import com.github.grishberg.csg.bsp.CsgOp
import com.github.grishberg.csg.bsp.csgOperation
import com.github.grishberg.csg.geom.Matrix4
import com.github.grishberg.csg.geom.PolySet3
import com.github.grishberg.csg.geom.Vec3

/**
 * Базовый класс 3D-модели. Immutable — каждая операция возвращает новый объект.
 * Дроп-ин замена JSCAD Abstract3dModel.
 */
open class Model(
    val mesh: PolySet3,
    var color: Color = Color.GRAY,
    var tag: Int = 0
) {
    // ---- Transformations (immutable chain) ----

    fun move(dx: Double, dy: Double, dz: Double): Model {
        return Model(mesh.transform(Matrix4.translation(dx, dy, dz)), color, tag)
    }

    fun move(v: Vec3): Model = move(v.x, v.y, v.z)

    fun moveX(x: Double): Model = move(x, 0.0, 0.0)
    fun moveY(y: Double): Model = move(0.0, y, 0.0)
    fun moveZ(z: Double): Model = move(0.0, 0.0, z)

    fun rotateX(deg: Double): Model {
        return Model(mesh.transform(Matrix4.rotationX(deg)), color, tag)
    }
    fun rotateY(deg: Double): Model {
        return Model(mesh.transform(Matrix4.rotationY(deg)), color, tag)
    }
    fun rotateZ(deg: Double): Model {
        return Model(mesh.transform(Matrix4.rotationZ(deg)), color, tag)
    }

    fun rotate(rx: Double, ry: Double, rz: Double): Model {
        val m = Matrix4.rotationX(rx) * Matrix4.rotationY(ry) * Matrix4.rotationZ(rz)
        return Model(mesh.transform(m), color, tag)
    }

    fun rotate(axis: Vec3, deg: Double): Model {
        return Model(mesh.transform(Matrix4.rotationAxis(axis, deg)), color, tag)
    }

    fun scale(sx: Double, sy: Double, sz: Double): Model {
        return Model(mesh.transform(Matrix4.scale(sx, sy, sz)), color, tag)
    }
    fun scale(s: Double): Model = scale(s, s, s)

    /** Mirror through XZ plane (flip Y). */
    fun mirrorY(): Model = scale(-1.0, 1.0, 1.0)
    /** Mirror through YZ plane (flip X). */
    fun mirrorX(): Model = scale(1.0, -1.0, 1.0)
    /** Mirror through XY plane (flip Z). */
    fun mirrorZ(): Model = scale(1.0, 1.0, -1.0)

    // ---- CSG Operations (deferred via builder) ----

    /**
     * Создает Builder для CSG-операций.
     * union(b) = add(b), difference(b) = subtract(b)
     */
    fun addModel(other: Model?): Model? {
        if (other == null) return this
        return builder().add(other).evaluate()
    }

    fun subtractModel(other: Model?): Model? {
        if (other == null) return this
        return builder().subtract(other).evaluate()
    }

    fun intersectModel(other: Model?): Model? {
        if (other == null) return this
        return builder().intersect(other).evaluate()
    }

    /**
     * Создаёт CSG Builder, инициализированный текущей моделью.
     * Позволяет делать последовательные union/diff/intersect.
     */
    fun builder(): CsgBuilder = CsgBuilder(this)

    // ---- Utilities ----

    fun isEmpty(): Boolean = mesh.isEmpty()
    fun bounds() = mesh.boundingBox()

    /** Get raw mesh (for export/viewer). */
    fun polySet3(): PolySet3 = mesh
}

/**
 * Builder для последовательных CSG-операций.
 * union(A, B, C) = A + B + C
 * difference(A, B) = A - B
 */
class CsgBuilder(initial: Model) {
    private var result: Model = initial
    private val operations = mutableListOf<Pair<Model, CsgOp>>()

    fun add(model: Model): CsgBuilder {
        operations.add(model to CsgOp.UNION)
        return this
    }
    fun subtract(model: Model): CsgBuilder {
        operations.add(model to CsgOp.DIFFERENCE)
        return this
    }
    fun intersect(model: Model): CsgBuilder {
        operations.add(model to CsgOp.INTERSECTION)
        return this
    }

    fun evaluate(): Model {
        var current = result
        for ((other, op) in operations) {
            current = Model(
                csgOperation(current.mesh, other.mesh, op),
                result.color, result.tag
            )
        }
        return current
    }
}

/** Создать объединение моделей. */
fun union(vararg models: Model): Model {
    if (models.isEmpty()) return Model(PolySet3.EMPTY)
    var result = models[0]
    for (i in 1 until models.size) {
        result = Model(csgOperation(result.mesh, models[i].mesh, CsgOp.UNION))
    }
    return result
}

/** Создать разность моделей. */
fun difference(base: Model, vararg subtractors: Model): Model {
    return subtractors.fold(base, { acc, m ->
        Model(csgOperation(acc.mesh, m.mesh, CsgOp.DIFFERENCE), base.color, base.tag)
    })
}

/** Цвет модели. */
data class Color(val r: Int, val g: Int, val b: Int) {
    companion object {
        val GRAY = Color(128, 128, 128)
        val RED = Color(255, 0, 0)
        val GREEN = Color(0, 255, 0)
        val BLUE = Color(0, 0, 255)
    }
}
