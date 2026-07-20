package com.github.grishberg.cad3d.keyboard.casebody

import com.github.grishberg.javascad.models.Model

interface CornerWallBuilder {

    fun backLeft(keyPlace: (Model) -> Model): Model
    fun backRight(keyPlace: (Model) -> Model): List<Model>
    fun frontLeft(keyPlace: (Model) -> Model): Model
    fun frontRight(keyPlace: (Model) -> Model): Model
}
