package com.github.grishberg.cad3d.keyboard.casebody.wall

import com.github.grishberg.cad3d.keyboard.KeyPlace
import com.github.grishberg.cad3d.keyboard.KeyPlaceholder
import com.github.grishberg.cad3d.keyboard.cfg.WallsSettings
import com.github.grishberg.javascad.coords.V3d
import kotlin.math.max

class ControllerHolderWall(
    private val cfg: WallsSettings,
    private val keyPlace: KeyPlace,
) {

    private val backEdgePoint: Double
    private val backEdgePointZ: Double

    init {
        val left = keyPlace.place(
            2,
            0,
            KeyPlaceholder.placeHolderBackLeft().move(0.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )

        val right = keyPlace.place(
            2, 0, KeyPlaceholder.placeHolderBackRight().move(0.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )

        val leftPatch = left.move
        val rightPatch = right.move
        backEdgePoint = max(leftPatch.y, rightPatch.y)
        backEdgePointZ = max(leftPatch.z, rightPatch.z)

    }

    fun getWallPoint(inPoint: V3d): V3d {
        return V3d(inPoint.x, backEdgePoint, backEdgePointZ)
    }
}
