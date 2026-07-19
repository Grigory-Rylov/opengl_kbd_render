package com.github.grishberg.cad3d.keyboard.casebody

import com.github.grishberg.javascad.coords.V3d
import com.github.grishberg.javascad.models.Abstract3dModel

interface WallBottomEdgePatcher {
    fun backPoint(o: Abstract3dModel): Abstract3dModel
    fun leftPoint(o: Abstract3dModel): Abstract3dModel = o
    fun rightPoint(o: Abstract3dModel): Abstract3dModel = o
    fun frontPoint(o: Abstract3dModel): Abstract3dModel = o
    fun projection(o: Abstract3dModel): Abstract3dModel

    fun verticalPoint(src: V3d): V3d
    fun horizontalPoint(src: V3d): V3d
}
