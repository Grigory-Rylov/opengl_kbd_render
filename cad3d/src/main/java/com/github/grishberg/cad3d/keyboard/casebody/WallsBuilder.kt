package com.github.grishberg.cad3d.keyboard.casebody

import com.github.grishberg.javascad.models.Model

interface WallsBuilder {

    fun backWall(
        onlyBorder: Boolean = false, keyPlace: (Model) -> Model
    ): Model

    fun backMidWall(
        onlyBorder: Boolean = false,
        leftOffset: Double = 0.0,
        rightOffset: Double = 0.0,
        leftPlace: (Model) -> Model,
        rightPlace: (Model) -> Model,
    ): Model

    fun leftWall(
        topOffset: Double = 0.0, bottomOffset: Double = 0.0, keyPlace: (Model) -> Model
    ): List<Model>

    fun leftMidWall(
        leftPlace: (Model) -> Model,
        rightPlace: (Model) -> Model,
    ): Model

    fun frontWall(
        leftOffset: Double = 0.0,
        rightOffset: Double = 0.0,
        onlyBorder: Boolean = false,
        keyPlace: (Model) -> Model,
    ): Model

    fun frontMidWall(
        leftOffset: Double = 0.0,
        rightOffset: Double = 0.0,
        leftPlace: (Model) -> Model,
        rightPlace: (Model) -> Model,
    ): Model

    fun rightWall(
        topOffset: Double = 0.0, bottomOffset: Double = 0.0, keyPlace: (Model) -> Model
    ): Model

    fun rightMidWall(
        backPlace: (Model) -> Model,
        frontPlace: (Model) -> Model,
    ): Model

    fun midEdge(
        midPlace: (Model) -> Model,
        leftPlace: (Model) -> Model,
        rightPlace: (Model) -> Model,
    ): Model

    fun rightDiagonal(
        backKeyPlace: (Model) -> Model,
        frontKeyPlace: (Model) -> Model,
    ): Model

}
