package com.github.grishberg.cad3d.keyboard.casebody

import com.github.grishberg.cad3d.keyboard.Utils
import com.github.grishberg.javascad.coords.V3d
import com.github.grishberg.javascad.models.Abstract3dModel

class DefaultBottomEdgePatcher(
    private val thickness: Double,
    private val objectHeight: Double,
) : WallBottomEdgePatcher {

    override fun backPoint(o: Abstract3dModel): Abstract3dModel {
        return projection(o)
    }

    override fun projection(obj: Abstract3dModel): Abstract3dModel {
        val point = obj.move
        return borderObject(thickness, objectHeight).move(V3d(point.x, point.y, objectHeight / 2))
    }

    override fun leftPoint(o: Abstract3dModel): Abstract3dModel = projection(o)

    override fun rightPoint(o: Abstract3dModel): Abstract3dModel = projection(o)

    override fun frontPoint(o: Abstract3dModel): Abstract3dModel = projection(o)

    override fun verticalPoint(src: V3d): V3d {
        return V3d(src.x, src.y, 0.0)
    }

    override fun horizontalPoint(src: V3d): V3d {
        return V3d(src.x, src.y, 0.0)
    }

    private fun borderObject(thickness: Double, height: Double): Abstract3dModel {
        return Utils.cylinder(thickness, height)
    }
}
