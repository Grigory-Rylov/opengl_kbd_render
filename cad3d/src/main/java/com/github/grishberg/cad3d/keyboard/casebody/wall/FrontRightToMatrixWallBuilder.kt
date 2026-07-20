package com.github.grishberg.cad3d.keyboard.casebody.wall

import com.github.grishberg.javascad.models.Model

interface FrontRightToMatrixWallBuilder {

    fun create(
        keyPlace: (Model) -> Model,
        matrixOuterPlace: (Model) -> Model,
        matrixInnerPlace: (Model) -> Model,
    ): List<Model>
}
