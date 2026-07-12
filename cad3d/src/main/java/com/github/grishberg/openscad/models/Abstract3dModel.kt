package com.github.grishberg.openscad.models

import com.github.grishberg.openscad.coords.Angles3d
import com.github.grishberg.openscad.coords.V3d
import com.github.grishberg.openscad.utils.Color
import com.github.grishberg.openscad.enums.Side
import com.github.grishberg.openscad.vrl.CSG
import com.github.grishberg.openscad.vrl.FacetGenerationContext
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
        return CSG()
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

    fun addModel(other: IModel): Abstract3dModel = applyClone()
    fun subtractModel(other: IModel): Abstract3dModel = applyClone()
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
        return cloneModel() as Abstract3dModel
    }
}
