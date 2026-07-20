package com.github.grishberg.cad3d.keyboard.casebody

import com.github.grishberg.javascad.coords.V3d
import com.github.grishberg.javascad.models.Model

interface WallBottomEdgePatcher {
    fun backPoint(o: Model): Model
    fun leftPoint(o: Model): Model = o
    fun rightPoint(o: Model): Model = o
    fun frontPoint(o: Model): Model = o
    fun projection(o: Model): Model

    fun verticalPoint(src: V3d): V3d
    fun horizontalPoint(src: V3d): V3d
}
