package com.github.grishberg.cad3d.keyboard

import com.github.grishberg.cad3d.keyboard.Utils.cube
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import eu.printingin3d.javascad.basic.Radius
import eu.printingin3d.javascad.models.Abstract3dModel
import eu.printingin3d.javascad.models.Cylinder

object KeyPlaceholder {

    const val CORNER_OFFSET = 7.85
    const val OFFSET = 6.85
    private const val EDGE_HEIGHT = 1.2
    private const val OUTER_WIDTH = 17.3
    private const val OUTER_HEIGHT = 17.3
    private const val KEY_HOLE_OUTER_WIDTH = 14.7
    private const val KEY_HOLE_INNER_WIDTH = 14.0
    private const val KEY_HOLE_HEIGHT = 14.0
    private const val WALL_THICKNESS = 1.5
    private const val HORIZONTAL_WALL_HEIGHT = 4.0
    private const val VERTICAL_WALL_HEIGHT = 3.2
    private const val TOP_THICKNESS = 4.0
    private const val CORNER_PLACEHOLDER_TOP_OFFSET = 2.0
    private const val BASE_TOP_OFFSET = 1.5 + 0.5
    private const val VERTICAL_TOP_OFFSET = BASE_TOP_OFFSET - VERTICAL_WALL_HEIGHT / 2 + 2.6 / 2
    private const val HORIZONTAL_TOP_OFFSET = BASE_TOP_OFFSET - HORIZONTAL_WALL_HEIGHT / 2 + 2.6 / 2

    fun placeHolder(cfg: KeyboardConfig): Abstract3dModel {
        return if (cfg.isLowProfile) lowProfilePlaceholder() else standardProfilePlaceholder()
    }

    private fun lowProfilePlaceholder(): Abstract3dModel {
        val cornerCubeHeight = 2.0
        val cornerObject = Cylinder(cornerCubeHeight, Radius.fromDiameter(2.0))
        val cornerCubes = cornerObject.move(-KEY_HOLE_INNER_WIDTH/2, -KEY_HOLE_INNER_WIDTH/2, cornerCubeHeight/2)
            .addModel(
                cornerObject.move(KEY_HOLE_INNER_WIDTH/2, -KEY_HOLE_INNER_WIDTH/2, cornerCubeHeight/2)
            )
            .addModel(
                cornerObject.move(KEY_HOLE_INNER_WIDTH/2, KEY_HOLE_INNER_WIDTH/2, cornerCubeHeight/2)
            )
            .addModel(
                cornerObject.move(-KEY_HOLE_INNER_WIDTH/2, KEY_HOLE_INNER_WIDTH/2, cornerCubeHeight/2)
            )
        return cube(OUTER_WIDTH, OUTER_HEIGHT, TOP_THICKNESS).move(0.0, 0.0, BASE_TOP_OFFSET)
            .subtractModel(cube(KEY_HOLE_INNER_WIDTH, KEY_HOLE_HEIGHT, 10.0)).subtractModel(
                cube(KEY_HOLE_OUTER_WIDTH, KEY_HOLE_HEIGHT, TOP_THICKNESS).move(
                    0.0, 0.0, VERTICAL_TOP_OFFSET - EDGE_HEIGHT
                )
            ).subtractModel(
                cube(KEY_HOLE_INNER_WIDTH, KEY_HOLE_HEIGHT, 2.0).move(
                    0.0, CORNER_OFFSET, VERTICAL_TOP_OFFSET - 2
                )
            ).subtractModel(
                cube(5.0, 15.0, 1.0).moveZ(0.7 + 1.8 - 1.3 + EDGE_HEIGHT)
            ).addModel(cornerCubes)
    }

    private fun standardProfilePlaceholder(): Abstract3dModel =
        cube(OUTER_WIDTH, OUTER_HEIGHT, TOP_THICKNESS).move(0.0, 0.0, BASE_TOP_OFFSET)
            .subtractModel(cube(KEY_HOLE_INNER_WIDTH, KEY_HOLE_HEIGHT, 10.0)).subtractModel(
                cube(KEY_HOLE_OUTER_WIDTH, KEY_HOLE_HEIGHT, TOP_THICKNESS).move(
                    0.0, 0.0, VERTICAL_TOP_OFFSET - EDGE_HEIGHT
                )
            ).subtractModel(
                cube(KEY_HOLE_INNER_WIDTH, KEY_HOLE_HEIGHT, 2.0).move(
                    0.0, CORNER_OFFSET, VERTICAL_TOP_OFFSET - 2
                )
            ).subtractModel(
                cube(5.0, 15.0, 1.0).moveZ(0.7 + 1.8 - 1.3 + EDGE_HEIGHT)
            )

    @JvmStatic
    @JvmOverloads
    fun placeHolderTop(thickness: Double = WALL_THICKNESS): Abstract3dModel {
        return cube(17.2, thickness, TOP_THICKNESS).move(0.0, CORNER_OFFSET + thickness, CORNER_PLACEHOLDER_TOP_OFFSET)
    }

    @JvmStatic
    fun placeHolderLeft(obj: Abstract3dModel = cube(WALL_THICKNESS, 17.2, TOP_THICKNESS)): Abstract3dModel {
        return obj.move(
            -CORNER_OFFSET - WALL_THICKNESS, 0.0, CORNER_PLACEHOLDER_TOP_OFFSET
        )
    }

    @JvmStatic
    fun placeHolderRight(obj: Abstract3dModel = cube(WALL_THICKNESS, 17.2, TOP_THICKNESS)): Abstract3dModel {
        return obj.move(CORNER_OFFSET + WALL_THICKNESS, 0.0, CORNER_PLACEHOLDER_TOP_OFFSET)
    }

    @JvmStatic
    fun placeHolderBottom(): Abstract3dModel {
        return cube(17.2, WALL_THICKNESS, TOP_THICKNESS).move(
            0.0, -CORNER_OFFSET - WALL_THICKNESS, CORNER_PLACEHOLDER_TOP_OFFSET
        )
    }

    @JvmStatic
    fun placeHolderTopLeft(): Abstract3dModel {
        return cube(WALL_THICKNESS, WALL_THICKNESS, TOP_THICKNESS).move(
            -CORNER_OFFSET - WALL_THICKNESS, CORNER_OFFSET + WALL_THICKNESS, CORNER_PLACEHOLDER_TOP_OFFSET
        )
    }

    @JvmStatic
    fun placeHolderTopLeft(height: Int): Abstract3dModel {
        return cube(WALL_THICKNESS, WALL_THICKNESS, height.toDouble()).move(
            -CORNER_OFFSET - WALL_THICKNESS, CORNER_OFFSET + WALL_THICKNESS, CORNER_PLACEHOLDER_TOP_OFFSET
        )
    }

    @JvmStatic
    fun placeHolderTopRight(): Abstract3dModel {
        return cube(WALL_THICKNESS, WALL_THICKNESS, TOP_THICKNESS).move(
            CORNER_OFFSET + WALL_THICKNESS, CORNER_OFFSET + WALL_THICKNESS, CORNER_PLACEHOLDER_TOP_OFFSET
        )
    }

    @JvmStatic
    fun placeHolderTopRight(height: Int): Abstract3dModel {
        return cube(WALL_THICKNESS, WALL_THICKNESS, height.toDouble()).move(
            CORNER_OFFSET + WALL_THICKNESS, CORNER_OFFSET + WALL_THICKNESS, CORNER_PLACEHOLDER_TOP_OFFSET
        )
    }

    @JvmStatic
    fun placeHolderBottomLeft(): Abstract3dModel {
        return cube(WALL_THICKNESS, WALL_THICKNESS, TOP_THICKNESS).move(
            -CORNER_OFFSET - WALL_THICKNESS, -CORNER_OFFSET - WALL_THICKNESS, CORNER_PLACEHOLDER_TOP_OFFSET
        )
    }

    @JvmStatic
    fun placeHolderBottomLeft(height: Int): Abstract3dModel {
        return cube(WALL_THICKNESS, WALL_THICKNESS, height.toDouble()).move(
            -CORNER_OFFSET - WALL_THICKNESS, -CORNER_OFFSET - WALL_THICKNESS, CORNER_PLACEHOLDER_TOP_OFFSET
        )
    }

    @JvmStatic
    fun placeHolderBottomRight(): Abstract3dModel {
        return cube(WALL_THICKNESS, WALL_THICKNESS, TOP_THICKNESS).move(
            CORNER_OFFSET + WALL_THICKNESS, -CORNER_OFFSET - WALL_THICKNESS, CORNER_PLACEHOLDER_TOP_OFFSET
        )
    }

    @JvmStatic
    fun placeHolderBottomRight(height: Int): Abstract3dModel {
        return cube(WALL_THICKNESS, WALL_THICKNESS, height.toDouble()).move(
            CORNER_OFFSET + WALL_THICKNESS, -CORNER_OFFSET - WALL_THICKNESS, CORNER_PLACEHOLDER_TOP_OFFSET
        )
    }
}
