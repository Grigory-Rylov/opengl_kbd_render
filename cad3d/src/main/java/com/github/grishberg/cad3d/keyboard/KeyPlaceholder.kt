package com.github.grishberg.cad3d.keyboard

import com.github.grishberg.cad3d.keyboard.Utils.cube
import com.github.grishberg.cad3d.plugin.cfg.KeyPlaceholderType
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import eu.printingin3d.javascad.basic.Radius
import eu.printingin3d.javascad.models.Abstract3dModel
import eu.printingin3d.javascad.models.Cylinder

object KeyPlaceholder {

    const val CORNER_OFFSET = 18.5/2
    const val OFFSET = 6.85
    private const val EDGE_HEIGHT = 1.2
    private const val OUTER_WIDTH = 18.0 + 2
    private const val OUTER_HEIGHT = 18.0 + 2
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
        return if (cfg.isLowProfile) lowProfilePlaceholder(cfg) else standardProfilePlaceholder()
    }

    private fun lowProfilePlaceholder(cfg: KeyboardConfig): Abstract3dModel {
        val cornerCubeHeight = 2.0
        val cornerObject = Cylinder(cornerCubeHeight, Radius.fromDiameter(4.0))
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
        val placeholder = cube(OUTER_WIDTH, OUTER_HEIGHT, TOP_THICKNESS).move(0.0, 0.0, BASE_TOP_OFFSET)

            .subtractModel(cube(KEY_HOLE_INNER_WIDTH, KEY_HOLE_HEIGHT, 10.0))
            .subtractModel(cube(KEY_HOLE_OUTER_WIDTH, KEY_HOLE_HEIGHT, TOP_THICKNESS).move(
                    0.0, 0.0, VERTICAL_TOP_OFFSET - EDGE_HEIGHT
                )
            )
            .subtractModel(cube(KEY_HOLE_INNER_WIDTH, KEY_HOLE_HEIGHT, 2.0).move(
                    0.0, CORNER_OFFSET, VERTICAL_TOP_OFFSET - 2
                )
            )
            .subtractModel(
                cube(5.0, 15.0, 1.0).moveZ(0.7 + 1.8 - 1.3 + EDGE_HEIGHT)
            )
            return if (cfg.keyPlaceholderType == KeyPlaceholderType.AmoebaSu120){
                placeholder.addModel(cornerCubes)
            } else {
                placeholder
            }

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
    fun placeHolderBack(thickness: Double = WALL_THICKNESS): Abstract3dModel {
        return cube(OUTER_WIDTH, thickness, TOP_THICKNESS).move(0.0, CORNER_OFFSET + thickness, CORNER_PLACEHOLDER_TOP_OFFSET)
    }

    @JvmStatic
    fun placeHolderLeft(obj: Abstract3dModel = cube(WALL_THICKNESS, OUTER_HEIGHT, TOP_THICKNESS)): Abstract3dModel {
        return obj.move(
            -CORNER_OFFSET - WALL_THICKNESS, 0.0, CORNER_PLACEHOLDER_TOP_OFFSET
        )
    }

    @JvmStatic
    fun placeHolderRight(obj: Abstract3dModel = cube(WALL_THICKNESS, OUTER_HEIGHT, TOP_THICKNESS)): Abstract3dModel {
        return obj.move(CORNER_OFFSET + WALL_THICKNESS, 0.0, CORNER_PLACEHOLDER_TOP_OFFSET)
    }

    @JvmStatic
    fun placeHolderFront(): Abstract3dModel {
        return cube(OUTER_WIDTH, WALL_THICKNESS, TOP_THICKNESS).move(
            0.0, -CORNER_OFFSET - WALL_THICKNESS, CORNER_PLACEHOLDER_TOP_OFFSET
        )
    }

    @JvmStatic
    fun placeHolderBackLeft(): Abstract3dModel {
        return cube(WALL_THICKNESS, WALL_THICKNESS, TOP_THICKNESS).move(
            -CORNER_OFFSET - WALL_THICKNESS, CORNER_OFFSET + WALL_THICKNESS, CORNER_PLACEHOLDER_TOP_OFFSET
        )
    }

    @JvmStatic
    fun placeHolderBackRight(): Abstract3dModel {
        return cube(WALL_THICKNESS, WALL_THICKNESS, TOP_THICKNESS).move(
            CORNER_OFFSET + WALL_THICKNESS, CORNER_OFFSET + WALL_THICKNESS, CORNER_PLACEHOLDER_TOP_OFFSET
        )
    }

    @JvmStatic
    fun placeHolderBackRight(height: Int): Abstract3dModel {
        return cube(WALL_THICKNESS, WALL_THICKNESS, height.toDouble()).move(
            CORNER_OFFSET + WALL_THICKNESS, CORNER_OFFSET + WALL_THICKNESS, CORNER_PLACEHOLDER_TOP_OFFSET
        )
    }

    @JvmStatic
    fun placeHolderFrontLeft(): Abstract3dModel {
        return cube(WALL_THICKNESS, WALL_THICKNESS, TOP_THICKNESS).move(
            -CORNER_OFFSET - WALL_THICKNESS, -CORNER_OFFSET - WALL_THICKNESS, CORNER_PLACEHOLDER_TOP_OFFSET
        )
    }

    @JvmStatic
    fun placeHolderFrontLeft(height: Int): Abstract3dModel {
        return cube(WALL_THICKNESS, WALL_THICKNESS, height.toDouble()).move(
            -CORNER_OFFSET - WALL_THICKNESS, -CORNER_OFFSET - WALL_THICKNESS, CORNER_PLACEHOLDER_TOP_OFFSET
        )
    }

    @JvmStatic
    fun placeHolderFrontRight(): Abstract3dModel {
        return cube(WALL_THICKNESS, WALL_THICKNESS, TOP_THICKNESS).move(
            CORNER_OFFSET + WALL_THICKNESS, -CORNER_OFFSET - WALL_THICKNESS, CORNER_PLACEHOLDER_TOP_OFFSET
        )
    }

    @JvmStatic
    fun placeHolderFrontRight(height: Int): Abstract3dModel {
        return cube(WALL_THICKNESS, WALL_THICKNESS, height.toDouble()).move(
            CORNER_OFFSET + WALL_THICKNESS, -CORNER_OFFSET - WALL_THICKNESS, CORNER_PLACEHOLDER_TOP_OFFSET
        )
    }
}
