package com.github.grishberg.cad3d.keyboard.casebody

import eu.printingin3d.javascad.models.Abstract3dModel

interface WallsBuilder {

    fun backWall(
        onlyBorder: Boolean = false,
        leftVerticalOffset: Double? = null,
        rightVerticalOffset: Double? = null,
        keyPlace:(Abstract3dModel) -> Abstract3dModel): Abstract3dModel

    fun backMidWall(
        onlyBorder: Boolean = false,
        leftVerticalOffset: Double? = null,
        rightVerticalOffset: Double? = null,
        leftPlace: (Abstract3dModel) -> Abstract3dModel,
        rightPlace: (Abstract3dModel) -> Abstract3dModel,
    ): Abstract3dModel

    fun leftWall(
        topOffset: Double = 0.0,
        bottomOffset: Double = 0.0,
        keyPlace:(Abstract3dModel) -> Abstract3dModel): Abstract3dModel
    fun leftMidWall(
        leftPlace: (Abstract3dModel) -> Abstract3dModel,
        rightPlace: (Abstract3dModel) -> Abstract3dModel,
    ): Abstract3dModel
    fun frontWall(
        leftOffset: Double = 0.0,
        rightOffset: Double = 0.0,
        onlyBorder: Boolean = false,
        keyPlace:(Abstract3dModel) -> Abstract3dModel,
        ): Abstract3dModel
    fun frontMidWall(
        leftOffset: Double = 0.0,
        rightOffset: Double = 0.0,
        leftPlace: (Abstract3dModel) -> Abstract3dModel,
        rightPlace: (Abstract3dModel) -> Abstract3dModel,
    ): Abstract3dModel
    fun rightWall(
        topOffset: Double = 0.0,
        bottomOffset: Double = 0.0,
        keyPlace:(Abstract3dModel) -> Abstract3dModel): Abstract3dModel
    fun rightMidWall(
        backPlace: (Abstract3dModel) -> Abstract3dModel,
        frontPlace: (Abstract3dModel) -> Abstract3dModel,
    ): Abstract3dModel

    fun midEdge(
        midPlace: (Abstract3dModel) -> Abstract3dModel,
        leftPlace: (Abstract3dModel) -> Abstract3dModel,
        rightPlace: (Abstract3dModel) -> Abstract3dModel,
    ): Abstract3dModel
}
