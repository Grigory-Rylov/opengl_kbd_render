package com.github.grishberg.cad3d.keyboard.casebody.thumb

import com.github.grishberg.cad3d.keyboard.casebody.CornerWallBuilder
import com.github.grishberg.cad3d.keyboard.casebody.WallBottomEdgePatcher
import com.github.grishberg.cad3d.keyboard.casebody.WallsBuilder
import com.github.grishberg.javascad.models.Model

interface ThumbWalls {

    fun createThumbWalls(
        wallsBuilder: WallsBuilder,
        cornerWallBuilder: CornerWallBuilder,
        verticalOffset: Double,
        leftOffset: Double,
        bottomEdgePatcher: WallBottomEdgePatcher,
        topEdgeOffsetZ: Double,
    ): List<Model>
}
