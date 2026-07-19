package com.github.grishberg.cad3d.keyboard.casebody

import com.github.grishberg.javascad.models.Abstract3dModel

interface CornerWallBuilder {

    fun backLeft(keyPlace: (Abstract3dModel) -> Abstract3dModel): Abstract3dModel
    fun backRight(keyPlace: (Abstract3dModel) -> Abstract3dModel): List<Abstract3dModel>
    fun frontLeft(keyPlace: (Abstract3dModel) -> Abstract3dModel): Abstract3dModel
    fun frontRight(keyPlace: (Abstract3dModel) -> Abstract3dModel): Abstract3dModel
}
