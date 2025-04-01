package com.github.grishberg.cad3d.keyboard.casebody

import eu.printingin3d.javascad.models.Abstract3dModel

interface CornerWallBuilder {

    fun backLeft(keyPlace: (Abstract3dModel) -> Abstract3dModel): Abstract3dModel
    fun backRight(offset: Offset? = null, keyPlace: (Abstract3dModel) -> Abstract3dModel): Abstract3dModel
    fun frontLeft(keyPlace: (Abstract3dModel) -> Abstract3dModel): Abstract3dModel
    fun frontRight(keyPlace: (Abstract3dModel) -> Abstract3dModel): Abstract3dModel

    fun frontRightToMatrix(
        keyPlace: (Abstract3dModel) -> Abstract3dModel,
        matrixOuterPlace: (Abstract3dModel) -> Abstract3dModel,
        matrixInnerPlace: (Abstract3dModel) -> Abstract3dModel,
    ): Abstract3dModel {
        return frontRight(keyPlace)
    }
}
