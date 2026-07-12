package com.github.grishberg.cad3d.keyboard.casebody.wall

import com.github.grishberg.openscad.models.Abstract3dModel

interface FrontRightToMatrixWallBuilder {

    fun create(
        keyPlace: (Abstract3dModel) -> Abstract3dModel,
        matrixOuterPlace: (Abstract3dModel) -> Abstract3dModel,
        matrixInnerPlace: (Abstract3dModel) -> Abstract3dModel,
    ): List<Abstract3dModel>
}
