package com.github.grishberg.openscad.models

import com.github.grishberg.openscad.coords.Angles3d
import com.github.grishberg.openscad.coords.V3d
import com.github.grishberg.openscad.utils.Color
import com.github.grishberg.openscad.enums.Side
import com.github.grishberg.openscad.tranzitions.Union
import com.github.grishberg.openscad.tranzitions.Difference
import com.github.grishberg.openscad.vrl.CSG
import com.github.grishberg.openscad.vrl.FacetGenerationContext
import com.github.grishberg.openscad.vrl.Polygon
import com.github.grishberg.csg.bsp.csgOperation
import com.github.grishberg.csg.bsp.CsgOp
import com.github.grishberg.csg.geom.Matrix4
import com.github.grishberg.csg.geom.PolySet3
import com.github.grishberg.csg.model.Model as CsgModel

abstract class Abstract3dModel(
    private val csgModel: CsgModel
) : IModel {
    var move: V3d = V3d.ZERO
    var rotate: Angles3d = Angles3d.ZERO
    var modelScale: V3d = V3d(1.0, 1.0, 1.0)
    var color: Color = Color.GRAY

    override fun toCSG(context: FacetGenerationContext): CSG {
        var mesh = csgModel.mesh

        // Apply rotation
        if (!rotate.isZero()) {
            val m = Matrix4.rotationX(rotate.x) * Matrix4.rotationY(rotate.y) * Matrix4.rotationZ(rotate.z)
            mesh = mesh.transform(m)
        }

        // Apply scale
        if (modelScale != V3d(1.0, 1.0, 1.0)) {
            mesh = mesh.transform(Matrix4.scale(modelScale.x, modelScale.y, modelScale.z))
        }

        // Apply translation
        if (!move.isZero()) {
            mesh = mesh.transform(Matrix4.translation(move.x, move.y, move.z))
        }

        return polySet3ToCSG(mesh)
    }

    private fun polySet3ToCSG(ps: PolySet3): CSG {
        val result = CSG()
        val verts = ps.vertices
        for (idx in ps.indices.indices) {
            val tri = ps.indices[idx]
            val v0 = V3d(verts[tri.a].x, verts[tri.a].y, verts[tri.a].z)
            val v1 = V3d(verts[tri.b].x, verts[tri.b].y, verts[tri.b].z)
            val v2 = V3d(verts[tri.c].x, verts[tri.c].y, verts[tri.c].z)
            val a = v1 - v0
            val b = v2 - v0
            val cr = a.cross(b)
            val mag = cr.magnitude()
            val normal = if (mag > 1e-15) cr / mag else V3d(0.0, 0.0, 1.0)
            result.polygons.add(Polygon(listOf(v0, v1, v2), normal, color))
        }
        return result
    }

    fun move(x: Double, y: Double, z: Double): Abstract3dModel = applyClone(move + V3d(x, y, z))
    fun move(v: V3d): Abstract3dModel = applyClone(move + v)
    fun moveX(x: Double): Abstract3dModel = applyClone(V3d(move.x + x, move.y, move.z))
    fun moveY(y: Double): Abstract3dModel = applyClone(V3d(move.x, move.y + y, move.z))
    fun moveZ(z: Double): Abstract3dModel = applyClone(V3d(move.x, move.y, move.z + z))

    fun rotate(angle: Double, axis: V3d): Abstract3dModel = applyClone()
    fun rotate(x: Double, y: Double, z: Double): Abstract3dModel = applyClone(rotate = Angles3d(rotate.x + x, rotate.y + y, rotate.z + z))
    fun rotateX(angle: Double): Abstract3dModel = applyClone(rotate = Angles3d(rotate.x + angle, rotate.y, rotate.z))
    fun rotateY(angle: Double): Abstract3dModel = applyClone(rotate = Angles3d(rotate.x, rotate.y + angle, rotate.z))
    fun rotateZ(angle: Double): Abstract3dModel = applyClone(rotate = Angles3d(rotate.x, rotate.y, rotate.z + angle))
    fun rotate(angles: Angles3d): Abstract3dModel = applyClone(rotate = Angles3d(rotate.x + angles.x, rotate.y + angles.y, rotate.z + angles.z))

    fun scale(sx: Double, sy: Double, sz: Double): Abstract3dModel = applyClone(modelScale = V3d(sx, sy, sz))
    fun scale(s: Double): Abstract3dModel = applyClone(modelScale = V3d(s, s, s))

    fun mirror(plane: V3d, offset: Double = 0.0): Abstract3dModel = applyClone(
        modelScale = V3d(
            if (plane.x != 0.0) -modelScale.x else modelScale.x,
            if (plane.y != 0.0) -modelScale.y else modelScale.y,
            if (plane.z != 0.0) -modelScale.z else modelScale.z
        )
    )

    fun addModel(other: IModel?): Abstract3dModel {
        if (other == null) return this
        return Union(this, other).withColor(color)
    }

    fun subtractModel(other: IModel?): Abstract3dModel {
        if (other == null) return this
        return Difference(this, other)
    }

    fun intersectModel(other: IModel): Abstract3dModel = applyClone()

    fun align(side: Side, value: Double): Abstract3dModel = applyClone()
    fun align(side: Side, other: IModel): Abstract3dModel = applyClone()
    fun align(side1: Side, side2: Side): Abstract3dModel = applyClone()
    fun align(side1: Side, side2: Side, other: IModel): Abstract3dModel = applyClone()
    fun withColor(c: Color): Abstract3dModel = applyClone(color = c)

    private fun applyClone(
        move: V3d = this.move,
        rotate: Angles3d = this.rotate,
        modelScale: V3d = this.modelScale,
        color: Color = this.color
    ): Abstract3dModel {
        val cloned = cloneModel() as Abstract3dModel
        cloned.move = move
        cloned.rotate = rotate
        cloned.modelScale = modelScale
        cloned.color = color
        return cloned
    }
}
