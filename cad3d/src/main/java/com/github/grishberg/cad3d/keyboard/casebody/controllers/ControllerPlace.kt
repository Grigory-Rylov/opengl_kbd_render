package com.github.grishberg.cad3d.keyboard.casebody.controllers

import com.github.grishberg.cad3d.keyboard.KeyPlace
import com.github.grishberg.cad3d.keyboard.KeyPlaceholder
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.openscad.coords.V3d
import com.github.grishberg.openscad.models.Abstract3dModel
import kotlin.math.max

/**
 * Places controller and controller holder.
 */
class ControllerPlace(
    private val cfg: KeyboardConfig,
    private val keyPlace: KeyPlace,
    private val controller: Controller,
) {

    fun place(
        obj: Abstract3dModel,
    ): Abstract3dModel {
        val wallsSettings = cfg.wallsSettings
        val left = keyPlace.place(
            2,
            0,
            KeyPlaceholder.placeHolderBackLeft()
                .move(0.0, wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
        )

        val right = keyPlace.place(
            2,
            0,
            KeyPlaceholder.placeHolderBackRight()
                .move(0.0, wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
        )
        val leftPatch = left.move
        val rightPatch = right.move
        val backEdgePoint = max(leftPatch.y, rightPatch.y)

        val p = keyPlace.calculateCoordinates(0, 0)
        val targetPoint = V3d(p.x, backEdgePoint, 0.0)
        return obj.move(targetPoint.add(V3d(
            15.0,
            -controller.depth/2 - wallsSettings.borderThickness - 2.1,
            4.5)))
    }
}
