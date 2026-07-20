package com.github.grishberg.cad3d.keyboard.casebody.thumb

import com.github.grishberg.cad3d.keyboard.KeyPlace
import com.github.grishberg.cad3d.keyboard.KeyPlaceholder
import com.github.grishberg.cad3d.keyboard.ThumbKeyPlace
import com.github.grishberg.cad3d.keyboard.Utils
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.javascad.models.Model

/**
 * FR - Front Right Corner
 * FL - Front Left Corner
 * BR - Back Right Corner
 * BL - Back Left Corner
 */
class ThumbPoints(
    private val cfg: KeyboardConfig,
    keyPlace: KeyPlace,
    thumbKeyPlace: ThumbKeyPlace,
) {

    val row2RightFRRightInner = thumbKeyPlace.placeR2(
        KeyPlaceholder.placeHolderFrontRight()
            .move(cfg.wallsSettings.verticalOffset + 0.0, 0.0, cfg.wallsSettings.borderZOffset)
    )

    val row2RightFRRightOuter = topBorderObj(
        thumbKeyPlace.placeR2(
            KeyPlaceholder.placeHolderFrontRight()
                .move(0.0, -cfg.wallsSettings.outerVerticalOffset, cfg.wallsSettings.outerBorderZOffset)
        )
    )

    val row1RightFRInner = thumbKeyPlace.placeR(
        KeyPlaceholder.placeHolderFrontRight().move(2.0, 0.0, cfg.wallsSettings.borderZOffset)
    )
    val row1RightBRInner = thumbKeyPlace.placeR(
        KeyPlaceholder.placeHolderBackRight().move(2.2, -3.0, cfg.wallsSettings.borderZOffset)
    )
    val col4LFOut = keyPlace.place(
        4, cfg.lastRow, KeyPlaceholder.placeHolderFrontLeft().move(
            0.0, -cfg.wallsSettings.outerVerticalOffset, cfg.wallsSettings.outerBorderZOffset
        )
    )

    private fun topBorderObj(obj: Model): Model {
        return Utils.sphere(cfg.wallsSettings.borderThickness / 2.0).move(obj.move)
    }

}
