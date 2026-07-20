package com.github.grishberg.cad3d.keyboard.casebody

import com.github.grishberg.cad3d.keyboard.Utils
import com.github.grishberg.javascad.coords.V3d
import com.github.grishberg.javascad.models.Model

class DefaultBottomEdgePatcher(
    private val thickness: Double,
    private val objectHeight: Double,
) : WallBottomEdgePatcher {

    override fun backPoint(o: Model): Model {
        return projection(o)
    }

    override fun projection(obj: Model): Model {
        val point = obj.move
        return borderObject(thickness, objectHeight).move(V3d(point.x, point.y, objectHeight / 2))
    }

    override fun leftPoint(o: Model): Model = projection(o)

    override fun rightPoint(o: Model): Model = projection(o)

    override fun frontPoint(o: Model): Model = projection(o)

    override fun verticalPoint(src: V3d): V3d {
        return V3d(src.x, src.y, 0.0)
    }

    override fun horizontalPoint(src: V3d): V3d {
        return V3d(src.x, src.y, 0.0)
    }

    private fun borderObject(thickness: Double, height: Double): Model {
        return Utils.cylinder(thickness, height)
    }
}
