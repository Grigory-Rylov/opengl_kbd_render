package com.github.grishberg.scripting.matrix

import com.github.grishberg.javascad.models.Abstract3dModel
import com.github.grishberg.javascad.models.Cube
import com.github.grishberg.javascad.models.Cylinder
import com.github.grishberg.javascad.tranzitions.Hull
import com.github.grishberg.javascad.tranzitions.Union

val borderThickness = 1.5
val borderHeight = 2.5
val borderVerticalOffset = 4.0
val borderLeftOffset = -8.0
val borderRightOffset = 8.0
val borderZOffset = -2.0

fun borderObject(): Abstract3dModel =
    Cylinder(borderThickness, borderHeight).moveZ(borderHeight / 2)

// Matrix borders
fun matrixBorders(): List<Abstract3dModel> {
    val kfc = KeyCfg
    val models = mutableListOf<Abstract3dModel>()

    // Back wall (top row)
    for (col in 0 until kfc.columnsCount) {
        models.add(backWall { obj -> placeKey(obj, col, 0) })
    }
    // Front wall (bottom row, right side only: columns 3-5)
    for (col in 3 until kfc.columnsCount) {
        models.add(frontWall { obj -> placeKey(obj, col, kfc.lastRow) })
    }
    // Back diagonal walls between columns
    for (col in 0 until kfc.columnsCount - 1) {
        if (col == 1) continue
        models.add(backMidWall(
            { obj -> placeKey(obj, col, 0) },
            { obj -> placeKey(obj, col + 1, 0) }
        ))
    }
    // Front diagonal walls (columns 3-5)
    for (col in 3 until kfc.columnsCount - 1) {
        models.add(frontMidWall(
            { obj -> placeKey(obj, col, kfc.lastRow) },
            { obj -> placeKey(obj, col + 1, kfc.lastRow) }
        ))
    }
    // Left wall
    for (row in 0 until kfc.rowsCount) {
        models.addAll(leftWall { obj -> placeKey(obj, 0, row) })
    }
    // Right wall
    for (row in 0 until kfc.rowsCount) {
        models.add(rightWall { obj -> placeKey(obj, kfc.lastCol, row) })
    }
    // Left mid walls
    for (row in 0 until kfc.rowsCount - 1) {
        models.add(leftMidWall(
            { obj -> placeKey(obj, 0, row) },
            { obj -> placeKey(obj, 0, row + 1) }
        ))
    }
    // Right mid walls
    for (row in 0 until kfc.rowsCount - 1) {
        models.add(rightMidWall(
            { obj -> placeKey(obj, kfc.lastCol, row) },
            { obj -> placeKey(obj, kfc.lastCol, row + 1) }
        ))
    }

    return models
}

fun verticalCube(keyModel: Abstract3dModel): Abstract3dModel =
    Hull(keyModel, Cube(borderThickness * 2, borderThickness * 2, borderHeight).moveZ(borderHeight / 2))

fun backWall(keyPlace: (Abstract3dModel) -> Abstract3dModel): Abstract3dModel =
    Hull(
        keyPlace(placeHolderBack()),
        verticalCube(keyPlace(placeHolderBackLeft().move(0.0, borderVerticalOffset, borderZOffset))),
        verticalCube(keyPlace(placeHolderBackRight().move(0.0, borderVerticalOffset, borderZOffset)))
    )

fun backMidWall(leftPlace: (Abstract3dModel) -> Abstract3dModel, rightPlace: (Abstract3dModel) -> Abstract3dModel): Abstract3dModel =
    Hull(
        leftPlace(placeHolderBackRight()),
        rightPlace(placeHolderBackLeft()),
        verticalCube(leftPlace(placeHolderBackRight().move(borderLeftOffset, borderVerticalOffset, borderZOffset))),
        verticalCube(rightPlace(placeHolderBackLeft().move(borderRightOffset, borderVerticalOffset, borderZOffset)))
    )

fun frontWall(keyPlace: (Abstract3dModel) -> Abstract3dModel): Abstract3dModel =
    Hull(
        keyPlace(placeHolderFront()),
        verticalCube(keyPlace(placeHolderFrontLeft().move(0.0, -borderVerticalOffset, borderZOffset))),
        verticalCube(keyPlace(placeHolderFrontRight().move(0.0, -borderVerticalOffset, borderZOffset)))
    )

fun frontMidWall(leftPlace: (Abstract3dModel) -> Abstract3dModel, rightPlace: (Abstract3dModel) -> Abstract3dModel): Abstract3dModel =
    Hull(
        leftPlace(placeHolderFrontRight()),
        rightPlace(placeHolderFrontLeft()),
        verticalCube(leftPlace(placeHolderFrontRight().move(borderLeftOffset, -borderVerticalOffset, borderZOffset))),
        verticalCube(rightPlace(placeHolderFrontLeft().move(borderRightOffset, -borderVerticalOffset, borderZOffset)))
    )

fun leftWall(keyPlace: (Abstract3dModel) -> Abstract3dModel): List<Abstract3dModel> =
    listOf(
        Hull(
            keyPlace(placeHolderLeft()),
            verticalCube(keyPlace(placeHolderBackLeft().move(borderLeftOffset, 0.0, borderZOffset))),
            verticalCube(keyPlace(placeHolderFrontLeft().move(borderLeftOffset, 0.0, borderZOffset)))
        )
    )

fun leftMidWall(leftPlace: (Abstract3dModel) -> Abstract3dModel, rightPlace: (Abstract3dModel) -> Abstract3dModel): Abstract3dModel =
    Hull(
        leftPlace(placeHolderLeft()),
        rightPlace(placeHolderLeft()),
        verticalCube(leftPlace(placeHolderBackLeft().move(borderLeftOffset, 0.0, borderZOffset))),
        verticalCube(rightPlace(placeHolderFrontLeft().move(borderLeftOffset, 0.0, borderZOffset)))
    )

fun rightWall(keyPlace: (Abstract3dModel) -> Abstract3dModel): Abstract3dModel =
    Hull(
        keyPlace(placeHolderRight()),
        verticalCube(keyPlace(placeHolderBackRight().move(borderRightOffset, 0.0, borderZOffset))),
        verticalCube(keyPlace(placeHolderFrontRight().move(borderRightOffset, 0.0, borderZOffset)))
    )

fun rightMidWall(backPlace: (Abstract3dModel) -> Abstract3dModel, frontPlace: (Abstract3dModel) -> Abstract3dModel): Abstract3dModel =
    Hull(
        backPlace(placeHolderRight()),
        frontPlace(placeHolderRight()),
        verticalCube(backPlace(placeHolderBackRight().move(borderRightOffset, 0.0, borderZOffset))),
        verticalCube(frontPlace(placeHolderFrontRight().move(borderRightOffset, 0.0, borderZOffset)))
    )

// Thumb borders
fun thumbBorders(): List<Abstract3dModel> {
    val models = mutableListOf<Abstract3dModel>()
    val borderOffset = 2.0

    // Back wall of thumb (top-most button = R)
    models.add(Hull(
        placeThumbR(placeHolderBack()),
        verticalCube(placeThumbR(placeHolderBackLeft().move(0.0, borderOffset, borderZOffset))),
        verticalCube(placeThumbR(placeHolderBackRight().move(0.0, borderOffset, borderZOffset))))
    )
    // Front wall of thumb (bottom-most button = L)
    models.add(Hull(
        placeThumbL(placeHolderFront()),
        verticalCube(placeThumbL(placeHolderFrontLeft().move(0.0, -borderOffset, borderZOffset))),
        verticalCube(placeThumbL(placeHolderFrontRight().move(0.0, -borderOffset, borderZOffset))))
    )
    // Left wall of thumb
    models.add(Hull(
        placeThumbL(placeHolderLeft()),
        verticalCube(placeThumbL(placeHolderBackLeft().move(-borderOffset, 0.0, borderZOffset))),
        verticalCube(placeThumbL(placeHolderFrontLeft().move(-borderOffset, 0.0, borderZOffset))))
    )
    // Right wall of thumb
    models.add(Hull(
        placeThumbR(placeHolderRight()),
        verticalCube(placeThumbR(placeHolderBackRight().move(borderOffset, 0.0, borderZOffset))),
        verticalCube(placeThumbR(placeHolderFrontRight().move(borderOffset, 0.0, borderZOffset))))
    )
    // Mid walls between thumb buttons
    models.add(Hull(
        placeThumbR(placeHolderLeft()),
        placeThumbM(placeHolderRight()),
        verticalCube(placeThumbR(placeHolderBackLeft().move(-borderOffset, 0.0, borderZOffset))),
        verticalCube(placeThumbM(placeHolderBackRight().move(borderOffset, 0.0, borderZOffset)))
    ))
    models.add(Hull(
        placeThumbM(placeHolderLeft()),
        placeThumbL(placeHolderRight()),
        verticalCube(placeThumbM(placeHolderBackLeft().move(-borderOffset, 0.0, borderZOffset))),
        verticalCube(placeThumbL(placeHolderBackRight().move(borderOffset, 0.0, borderZOffset)))
    ))

    return models
}

// Between thumb and matrix borders
fun betweenThumbAndMatrixBorders(): List<Abstract3dModel> {
    val models = mutableListOf<Abstract3dModel>()
    models.add(Hull(
        placeKey(placeHolderBackRight(), 2, 0),
        placeThumbR(placeHolderBackLeft()),
        verticalCube(placeKey(placeHolderBackRight().move(borderRightOffset, borderVerticalOffset, borderZOffset), 2, 0)),
        verticalCube(placeThumbR(placeHolderBackLeft().move(0.0, borderVerticalOffset, borderZOffset)))
    ))
    return models
}

// Screw holes
fun screwHole(): Abstract3dModel {
    return Cylinder(5.0, 2.0).moveZ(1.0).addModel(
        Cylinder(3.1, 10.0).moveZ(-5.0 + 1.0)
    )
}

fun buildScrews(): Abstract3dModel {
    val screwHorizontalOffset = (1.6 * 2 + 4.0) / 2
    val screwLeftOffset = -10.0 + screwHorizontalOffset + 1
    val screwRightOffset = 10.0 - screwHorizontalOffset - 1

    val models = mutableListOf<Abstract3dModel>()
    models.add(placeKey(screwHole().moveX(screwLeftOffset), 0, 0))
    models.add(placeKey(screwHole().moveX(screwLeftOffset), 0, KeyCfg.lastRow))
    models.add(placeKey(screwHole().moveX(screwRightOffset), KeyCfg.lastCol, 0))
    models.add(placeKey(screwHole().moveX(screwRightOffset), KeyCfg.lastCol, KeyCfg.lastRow))
    models.add(placeThumbL(screwHole().moveX(screwLeftOffset)))

    return Union(models)
}

// Final borders: union all borders, subtract screws
fun buildBorders(): Abstract3dModel {
    val allBorders = mutableListOf<Abstract3dModel>()
    allBorders.addAll(matrixBorders())
    allBorders.addAll(thumbBorders())
    allBorders.addAll(betweenThumbAndMatrixBorders())
    
    val bordersUnion = Union(allBorders)
    return bordersUnion.subtractModel(buildScrews())
}
