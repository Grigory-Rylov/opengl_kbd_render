package com.github.grishberg.cad3d

import com.github.grishberg.cad3d.keyboard.KeyPlace
import com.github.grishberg.cad3d.keyboard.ThumbKeyPlace
import com.github.grishberg.cad3d.keyboard.casebody.DefaultBottomEdgePatcher
import com.github.grishberg.cad3d.keyboard.casebody.Walls
import com.github.grishberg.cad3d.keyboard.casebody.controllers.Controller
import com.github.grishberg.cad3d.keyboard.casebody.controllers.ControllerFactory
import com.github.grishberg.cad3d.keyboard.casebody.controllers.ControllerHolderDimensions
import com.github.grishberg.cad3d.keyboard.casebody.wall.ControllerHolderWall
import com.github.grishberg.cad3d.keyboard.casebody.controllers.ControllerPlace
import com.github.grishberg.cad3d.keyboard.casebody.controllers.SwitcherPlace
import com.github.grishberg.cad3d.keyboard.casebody.controllers.switcher.SwitcherFactory
import com.github.grishberg.cad3d.keyboard.casebody.thumb.SingleColumn3ButtonsThumbsBordersBuilder
import com.github.grishberg.cad3d.keyboard.casebody.thumb.SingleColumn3ButtonsThumbWalls
import com.github.grishberg.cad3d.keyboard.casebody.thumb.ThumbBorders
import com.github.grishberg.cad3d.keyboard.casebody.thumb.ThumbPoints
import com.github.grishberg.cad3d.keyboard.casebody.thumb.ThumbWalls
import com.github.grishberg.cad3d.keyboard.casebody.thumb.TwoRows5ButtonsMatrixThumbsBordersBuilder
import com.github.grishberg.cad3d.keyboard.casebody.thumb.TwoRows5ButtonsThumbWalls
import com.github.grishberg.cad3d.keyboard.casebody.wall.FrontRightToMatrixWallBuilder
import com.github.grishberg.cad3d.keyboard.casebody.wall.SingleRow3ButtonsFrontRightToMatrixWallBuilder
import com.github.grishberg.cad3d.keyboard.casebody.wall.TwoRowsButtonsFrontRightToMatrixWallBuilder
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.keyboard.cfg.WallsSettings
import com.github.grishberg.cad3d.keyboard.screws.ScrewWallPlaces
import com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode
import com.github.grishberg.cad3d.trackball.Trackball

data class BuildContext(
    val keyPlace: KeyPlace,
    val thumbKeyPlace: ThumbKeyPlace,
    val trackball: Trackball,
    val controllerFactory: ControllerFactory,
    val controller: Controller,
    val controllerPlace: ControllerPlace,
    val switcherPlace: SwitcherPlace,
    val switcherFactory: SwitcherFactory,
    val controllerHolderDimensions: ControllerHolderDimensions,
    val wallsSettings: WallsSettings,
    val controllerHolderWall: ControllerHolderWall,
    val screwWallPlaces: ScrewWallPlaces,
    val topEdgeOffsetZ: Double,
    val thumbPoints: ThumbPoints?,
    val frontRightToMatrixWallBuilder: FrontRightToMatrixWallBuilder,
    val thumbBorders: ThumbBorders,
    val thumbWalls: ThumbWalls,
    val walls: Walls,
) {
    companion object {
        fun create(cfg: KeyboardConfig, bottomBorderHeight: Double): BuildContext {
            val keyPlace = KeyPlace(cfg.keyPlaceConfig)
            val thumbKeyPlace = ThumbKeyPlace(cfg)
            val trackball = Trackball(cfg)

            val controllerFactory = ControllerFactory(cfg)
            val controller = controllerFactory.createController()

            val controllerPlace = ControllerPlace(cfg, keyPlace, controller)
            val switcherPlace = SwitcherPlace(controller, controllerPlace)
            val switcherFactory = SwitcherFactory(cfg)
            val controllerHolderDimensions = ControllerHolderDimensions()

            val wallsSettings = WallsSettings(bottomBorderHeight = bottomBorderHeight)
            val controllerHolderWall = ControllerHolderWall(wallsSettings, keyPlace)
            val screwWallPlaces = ScrewWallPlaces(
                cfg, wallsSettings, keyPlace, thumbKeyPlace, controllerHolderWall, controllerHolderDimensions
            )

            val topEdgeOffsetZ = -2.0

            val thumbPoints = if (cfg.thumbClusterSettings.type == ThumbClusterMode.TwoRows5Buttons) {
                ThumbPoints(cfg, keyPlace, thumbKeyPlace)
            } else null
            val bottomEdgePatcher = DefaultBottomEdgePatcher(
                wallsSettings.borderThickness, wallsSettings.bottomBorderHeight
            )
            val frontRightToMatrixWallBuilder: FrontRightToMatrixWallBuilder = when (cfg.thumbClusterSettings.type) {
                ThumbClusterMode.SingleColumn3Buttons -> SingleRow3ButtonsFrontRightToMatrixWallBuilder(
                    cfg, bottomEdgePatcher, topEdgeOffsetZ
                )

                ThumbClusterMode.SingleColumn4Buttons -> SingleRow3ButtonsFrontRightToMatrixWallBuilder(
                    cfg, bottomEdgePatcher, topEdgeOffsetZ
                )

                ThumbClusterMode.TwoRows5Buttons -> TwoRowsButtonsFrontRightToMatrixWallBuilder(
                    cfg, bottomEdgePatcher, topEdgeOffsetZ, thumbPoints!!
                )
            }

            val thumbBorders = when (cfg.thumbClusterSettings.type) {
                ThumbClusterMode.SingleColumn3Buttons -> SingleColumn3ButtonsThumbsBordersBuilder(thumbKeyPlace)
                ThumbClusterMode.SingleColumn4Buttons -> SingleColumn3ButtonsThumbsBordersBuilder(thumbKeyPlace)
                ThumbClusterMode.TwoRows5Buttons -> TwoRows5ButtonsMatrixThumbsBordersBuilder(thumbKeyPlace)
            }

            val thumbWalls: ThumbWalls = when (cfg.thumbClusterSettings.type) {
                ThumbClusterMode.SingleColumn3Buttons -> SingleColumn3ButtonsThumbWalls(
                    cfg, keyPlace, thumbKeyPlace, frontRightToMatrixWallBuilder
                )

                ThumbClusterMode.SingleColumn4Buttons -> SingleColumn3ButtonsThumbWalls(
                    cfg, keyPlace, thumbKeyPlace, frontRightToMatrixWallBuilder
                )

                ThumbClusterMode.TwoRows5Buttons -> TwoRows5ButtonsThumbWalls(
                    cfg, keyPlace, thumbKeyPlace, thumbPoints!!, frontRightToMatrixWallBuilder
                )
            }
            val walls = Walls(
                cfg, wallsSettings, keyPlace, thumbKeyPlace, topEdgeOffsetZ = topEdgeOffsetZ,
                thumbBorders = thumbBorders,
                thumbWalls = thumbWalls,
            )
            return BuildContext(
                keyPlace = keyPlace,
                thumbKeyPlace = thumbKeyPlace,
                trackball = trackball,
                controllerFactory = controllerFactory,
                controller = controller,
                controllerPlace = controllerPlace,
                switcherPlace = switcherPlace,
                switcherFactory = switcherFactory,
                controllerHolderDimensions = controllerHolderDimensions,
                wallsSettings = wallsSettings,
                controllerHolderWall = controllerHolderWall,
                screwWallPlaces = screwWallPlaces,
                topEdgeOffsetZ = topEdgeOffsetZ,
                thumbPoints = thumbPoints,
                frontRightToMatrixWallBuilder = frontRightToMatrixWallBuilder,
                thumbBorders = thumbBorders,
                thumbWalls = thumbWalls,
                walls = walls,
            )
        }
    }
}
