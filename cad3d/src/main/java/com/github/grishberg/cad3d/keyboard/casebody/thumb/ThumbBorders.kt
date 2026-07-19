package com.github.grishberg.cad3d.keyboard.casebody.thumb

import com.github.grishberg.cad3d.keyboard.casebody.CornerWallBuilder
import com.github.grishberg.cad3d.keyboard.casebody.WallsBuilder
import com.github.grishberg.javascad.models.Abstract3dModel

interface ThumbBorders {

    fun create(
        wallsBuilder: WallsBuilder,
        cornerWallBuilder: CornerWallBuilder,
        isWallMode: Boolean = false,
    ): List<Abstract3dModel>
}
