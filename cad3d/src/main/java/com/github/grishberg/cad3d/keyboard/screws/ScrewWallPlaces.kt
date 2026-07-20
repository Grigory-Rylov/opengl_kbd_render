package com.github.grishberg.cad3d.keyboard.screws

import com.github.grishberg.cad3d.keyboard.KeyPlace
import com.github.grishberg.cad3d.keyboard.PlacePointType
import com.github.grishberg.cad3d.keyboard.ThumbKeyPlace
import com.github.grishberg.cad3d.keyboard.casebody.controllers.ControllerHolderDimensions
import com.github.grishberg.cad3d.keyboard.casebody.wall.ControllerHolderWall
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.keyboard.cfg.WallsSettings
import com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode
import com.github.grishberg.javascad.coords.V3d
import com.github.grishberg.javascad.models.Model
import com.github.grishberg.javascad.models.Cube
import com.github.grishberg.javascad.tranzitions.Union

/**
 * Установка
 */
class ScrewWallPlaces(
    private val cfg: KeyboardConfig,
    private val wallsSettings: WallsSettings,
    private val keyPlace: KeyPlace,
    private val thumbKeyPlace: ThumbKeyPlace,
    private val controllerHolderWall: ControllerHolderWall,
    private val controllerHolderDimensions: ControllerHolderDimensions,
) {

    fun place(o: Model, heightMode: HeightMode): Model {
        val models = mutableListOf<Model>()
        val cube = Cube(1.0)

        models.add(placeControllerScrews(o, heightMode, ControllerMode.All))

        models.add(placeRightBack(o, keyPlace.place(cfg.lastCol, 0, cube), offsetY = 10.0))
        models.add(placeRightFront(o, keyPlace.place(cfg.lastCol, cfg.lastRow, cube), offsetY = -4.0))

        val offsetY = if (cfg.thumbClusterSettings.type == ThumbClusterMode.TwoRows5Buttons) -23.0 else -23.5
        val offsetX = if (cfg.thumbClusterSettings.type == ThumbClusterMode.TwoRows5Buttons) 6.0 else 0.0

        models.add(placeFrontCenter(o, keyPlace.place(3, cfg.lastRow, cube), offsetX = offsetX, offsetY = offsetY))

        if (!cfg.isSkeletonMode) {
            models.add(placeTop(o, keyPlace.place(3, 0, cube), offsetY = 12.0))
        }

        if (cfg.thumbClusterSettings.type == ThumbClusterMode.TwoRows5Buttons) {
            models.add(placeFrontCenter(o, thumbKeyPlace.placeL2(cube), offsetX = -0.5, offsetY = -10.0))
        } else {
            models.add(placeFrontCenter(o, thumbKeyPlace.placeL(cube), offsetX = -0.5, offsetY = -10.0))
        }

        return Union(models)
    }

    fun placeControllerScrews(
        o: Model,
        heightMode: HeightMode,
        mode: ControllerMode,
    ): Model {
        val models = mutableListOf<Model>()
        val horizontalOffset = controllerHolderDimensions.distanceBetweenControllerHolderMountX
        val verticalOffset = controllerHolderDimensions.distanceBetweenControllerHolderMountY

        val controllerOffsetZ = when (heightMode) {
            HeightMode.Walls -> cfg.controllerPlateHeight
            else -> 0.0
        }

        val screwOffset = wallsSettings.borderThickness + cfg.screwHolderWallhickness + cfg.screwNutHoleDiameter / 2
        val calculatePlacePoint = keyPlace.calculatePlacePoint(
            0, 0, PlacePointType.BackLeftBottom, -wallsSettings.outerLeftOffset, wallsSettings.outerBorderZOffset
        )
        val wallLeftOffset = calculatePlacePoint.x
        val backWallPoint = V3d(
            wallLeftOffset + screwOffset - 3.0, controllerHolderWall.getWallPoint(
                keyPlace.calculatePlacePoint(
                    0, 0, PlacePointType.BackLeftBottom
                )
            ).y - screwOffset, controllerOffsetZ
        )

        //left back corner
        if (mode == ControllerMode.All || mode == ControllerMode.Back || mode == ControllerMode.Side || mode == ControllerMode.Trackball) {
            models.add(o.move(backWallPoint))
        }

        //  right back
        if (mode == ControllerMode.All || mode == ControllerMode.Back || mode == ControllerMode.Trackball) {
            models.add(
                o.move(
                    V3d(backWallPoint.x + horizontalOffset, backWallPoint.y, controllerOffsetZ)
                )
            )
        }

        // left front
        if (mode == ControllerMode.All || mode == ControllerMode.Side || mode == ControllerMode.Trackball) {
            models.add(
                o.move(
                    V3d(backWallPoint.x, backWallPoint.y - verticalOffset, controllerOffsetZ)
                )
            )
        }

        if (mode == ControllerMode.Trackball) {
            val offsetZ = when (heightMode) {
                HeightMode.Walls -> -2.5
                else -> 0.0
            }

            models.add(
                o.move(
                    V3d(backWallPoint.x, backWallPoint.y - verticalOffset - 35, offsetZ)
                )
            )

            models.add(
                o.move(
                    V3d(backWallPoint.x + horizontalOffset, backWallPoint.y - verticalOffset - 35, offsetZ)
                )
            )
        }

        return Union(models)
    }

    private fun placeRight(
        obj: Model,
        place: Model,
        offsetX: Double = 0.0,
        offsetY: Double = 0.0,
        offsetZ: Double = 0.0
    ): Model {
        val p = place.move(wallsSettings.outerHorizontalOffset, 0.0, 0.0).move
        val targetPoint = V3d(p.x, p.y, 0.0)
        return obj.move(targetPoint.add(V3d(offsetX, offsetY, offsetZ)))
    }

    private fun placeRightBack(
        obj: Model,
        place: Model,
        offsetX: Double = 0.0,
        offsetY: Double = 0.0,
        offsetZ: Double = 0.0
    ): Model {
        val p = place.move(wallsSettings.outerHorizontalOffset, wallsSettings.outerVerticalOffset, 0.0).move
        val targetPoint = V3d(p.x, p.y + 5.0, 0.0)
        return obj.move(targetPoint.add(V3d(offsetX, offsetY, offsetZ)))
    }

    private fun placeRightFront(
        obj: Model,
        place: Model,
        offsetX: Double = 0.0,
        offsetY: Double = 0.0,
        offsetZ: Double = 0.0
    ): Model {
        val p = place.move(wallsSettings.outerHorizontalOffset, -wallsSettings.outerVerticalOffset, 0.0).move
        val targetPoint = V3d(p.x, p.y, 0.0)
        return obj.move(targetPoint.add(V3d(offsetX, offsetY - 1.5, offsetZ)))
    }

    private fun placeTop(
        obj: Model,
        place: Model,
        offsetX: Double = 0.0,
        offsetY: Double = 0.0,
        offsetZ: Double = 0.0
    ): Model {
        val p = place.move(0.0, wallsSettings.outerVerticalOffset, 0.0).move
        val targetPoint = V3d(p.x, p.y, 0.0)
        return obj.move(targetPoint.add(V3d(offsetX, offsetY, offsetZ)))

    }

    private fun placeFrontCenter(
        obj: Model,
        place: Model,
        offsetX: Double = 0.0,
        offsetY: Double = 0.0,
        offsetZ: Double = 0.0
    ): Model {
        val p = place.move(0.0, -wallsSettings.outerVerticalOffset - 1.5, 0.0).move
        val targetPoint = V3d(p.x, p.y, 0.0)
        return obj.move(targetPoint.add(V3d(offsetX, offsetY, offsetZ)))
    }

    enum class HeightMode { Walls, Plate, ControllerHolder,
    }

    enum class ControllerMode { All, Back, Side, Trackball }
}
