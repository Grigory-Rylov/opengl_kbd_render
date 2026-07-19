package com.github.grishberg.scripting.matrix

import com.github.grishberg.javascad.models.*
import com.github.grishberg.javascad.tranzitions.*

fun createPlaceholder(): Abstract3dModel {
    return Cube(PH.OUTER_W, PH.OUTER_H, PH.KEY_PLACE_TOP_THICKNESS)
        .move(0.0, 0.0, PH.BASE_TOP_OFFSET)
        .subtractModel(Cube(PH.KEY_HOLE_INNER_W, PH.KEY_HOLE_H, 10.0))
        .subtractModel(
            Cube(PH.KEY_HOLE_OUTER_W, PH.KEY_HOLE_H, PH.KEY_PLACE_TOP_THICKNESS)
                .move(0.0, 0.0, PH.VERTICAL_TOP_OFFSET - PH.EDGE_HEIGHT)
        )
        .subtractModel(
            Cube(PH.KEY_HOLE_INNER_W, PH.KEY_HOLE_H, 2.0)
                .move(0.0, PH.CORNER_OFFSET, PH.VERTICAL_TOP_OFFSET - 2.0 - PH.delta)
        )
        .subtractModel(
            Cube(5.0, 15.0, 1.0)
                .moveZ(0.7 + 1.8 - 1.3 + PH.EDGE_HEIGHT - PH.delta)
        )
        .moveZ(PH.delta)
}

// Boundary blocks для hull-соединений
fun placeHolderFront(): Abstract3dModel =
    Cube(PH.OUTER_W, 1.5, PH.TOP_THICKNESS).move(0.0, -PH.CORNER_OFFSET - 1.5, PH.BASE_TOP_OFFSET)

fun placeHolderBack(): Abstract3dModel =
    Cube(PH.OUTER_W, 1.5, PH.TOP_THICKNESS).move(0.0, PH.CORNER_OFFSET + 1.5, PH.BASE_TOP_OFFSET)

fun placeHolderLeft(): Abstract3dModel =
    Cube(1.5, PH.OUTER_H, PH.TOP_THICKNESS).move(-PH.CORNER_OFFSET - 1.5, 0.0, PH.BASE_TOP_OFFSET)

fun placeHolderRight(): Abstract3dModel =
    Cube(1.5, PH.OUTER_H, PH.TOP_THICKNESS).move(PH.CORNER_OFFSET + 1.5, 0.0, PH.BASE_TOP_OFFSET)

fun placeHolderFrontLeft(): Abstract3dModel =
    Cube(1.5, 1.5, PH.TOP_THICKNESS).move(-PH.CORNER_OFFSET - 1.5, -PH.CORNER_OFFSET - 1.5, PH.BASE_TOP_OFFSET)

fun placeHolderFrontRight(): Abstract3dModel =
    Cube(1.5, 1.5, PH.TOP_THICKNESS).move(PH.CORNER_OFFSET + 1.5, -PH.CORNER_OFFSET - 1.5, PH.BASE_TOP_OFFSET)

fun placeHolderBackLeft(): Abstract3dModel =
    Cube(1.5, 1.5, PH.TOP_THICKNESS).move(-PH.CORNER_OFFSET - 1.5, PH.CORNER_OFFSET + 1.5, PH.BASE_TOP_OFFSET)

fun placeHolderBackRight(): Abstract3dModel =
    Cube(1.5, 1.5, PH.TOP_THICKNESS).move(PH.CORNER_OFFSET + 1.5, PH.CORNER_OFFSET + 1.5, PH.BASE_TOP_OFFSET)

fun placeHolderTopLeft(): Abstract3dModel =
    Cube(1.5, 1.5, PH.TOP_THICKNESS).move(-PH.CORNER_OFFSET - 1.5, -PH.CORNER_OFFSET - 1.5, PH.BASE_TOP_OFFSET + PH.TOP_THICKNESS)

fun placeHolderTopRight(): Abstract3dModel =
    Cube(1.5, 1.5, PH.TOP_THICKNESS).move(PH.CORNER_OFFSET + 1.5, -PH.CORNER_OFFSET - 1.5, PH.BASE_TOP_OFFSET + PH.TOP_THICKNESS)

fun placeHolderBottomLeft(): Abstract3dModel =
    Cube(1.5, 1.5, PH.TOP_THICKNESS).move(-PH.CORNER_OFFSET - 1.5, PH.CORNER_OFFSET + 1.5, PH.BASE_TOP_OFFSET + PH.TOP_THICKNESS)

fun placeHolderBottomRight(): Abstract3dModel =
    Cube(1.5, 1.5, PH.TOP_THICKNESS).move(PH.CORNER_OFFSET + 1.5, PH.CORNER_OFFSET + 1.5, PH.BASE_TOP_OFFSET + PH.TOP_THICKNESS)
