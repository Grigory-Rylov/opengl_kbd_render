package com.github.grishberg.cad3d.trackball

import com.github.grishberg.cad3d.keyboard.ModelHolder
import com.github.grishberg.cad3d.keyboard.Utils
import com.github.grishberg.cad3d.keyboard.casebody.controllers.Controller
import com.github.grishberg.cad3d.keyboard.casebody.controllers.ControllerFactory
import com.github.grishberg.cad3d.keyboard.casebody.controllers.ControllerPlace
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.keyboard.screws.ScrewBase
import com.github.grishberg.cad3d.keyboard.screws.ScrewWallPlaces
import com.github.grishberg.openscad.models.Abstract3dModel
import com.github.grishberg.openscad.utils.Color

class TrackballCase(
    private val cfg: KeyboardConfig,
    private val screwWallPlaces: ScrewWallPlaces,
    private val controllerPlace: ControllerPlace,
    private val controller: Controller,
    private val controllerFactory: ControllerFactory,
) {

    private val width = 50
    private val depth = 60
    private val height = 14
    private val offsetX = -28

    fun create(): ModelHolder {
        val usbPortHole =
            controllerPlace.place(controller.placeUsbPort(controllerFactory.createUsbPortHole())).moveY(-2.0)
        val usbPortHoleCase =
            controllerPlace.place(controller.placeUsbPort(controllerFactory.createUsbPortCase())).moveY(-1.0)

        val holes = createHoles().move(Utils.v3d(offsetX, 7.0, 14.0))
        val case = createShape().subtractModel(createShape(2.0)).move(-52.0, -15.0, 0.0).subtractModel(usbPortHole)
            .subtractModel(usbPortHoleCase).subtractModel(Utils.cylinder(4.0, 10.0).move(-28.0, 35.0, 14.0))
            .subtractModel(holes)

        val screwBase = ScrewBase(cfg)
        val screwHolder = screwBase.screwHolder(height = 12.0).moveZ(cfg.keyPlaceConfig.plateThickness - 0.5)
        val screwsHolder = screwWallPlaces.placeControllerScrews(
            screwHolder, ScrewWallPlaces.HeightMode.ControllerHolder, ScrewWallPlaces.ControllerMode.Trackball
        )
        return ModelHolder(cfg, case.addModel(screwsHolder), case, screwsHolder, createHolder())
    }

    fun createPlate(): ModelHolder {
        val screwBase = ScrewBase(cfg)
        val holes = screwWallPlaces.placeControllerScrews(
            screwBase.plateScrewHolder(), ScrewWallPlaces.HeightMode.Plate, ScrewWallPlaces.ControllerMode.Trackball
        )
        val plate = createShape(withTop = false).move(-52.0, -15.0, 0.0).subtractModel(holes)
        return ModelHolder(cfg, plate.withColor(Color.LIGHT_SKY_BLUE).moveZ(-2.0))
    }

    fun createHolder(): Abstract3dModel {
        val trackBallHolder = Trackball(cfg).trackBallCaseHolder().addModel(Utils.cube(28.0, 10.0, 1.5).move(0.0, 0.0, -5.0))
            .withColor(Color.ORANGE)
        val trackHolderOffset = Utils.v3d(offsetX, 7.0, height + 10.9)
        return trackBallHolder.subtractModel(createHoles(rad = 3.5 / 2.0).move(0.0, 0.0, -5.0)).move(trackHolderOffset)
    }

    private fun createHoles(rad: Double = 0.7): Abstract3dModel {
        val holeCylinder = Utils.cylinder(rad, 10.0)
        val delta = 10.0
        return Utils.union(holeCylinder.moveX(-delta), holeCylinder.moveX(delta))
    }

    private fun createShape(offset: Double = 0.0, withTop: Boolean = true): Abstract3dModel {
        val bottomZ = 0.0
        val topZ = height - offset
        val leftBottom = Utils.v3d(offset, offset, bottomZ)
        val rightBottom = Utils.v3d(width - offset, offset, bottomZ)
        val rightFrontBottom = Utils.v3d(width - offset, depth - offset, bottomZ)
        val leftFrontBottom = Utils.v3d(offset, depth - offset, bottomZ)

        val leftTop = Utils.v3d(offset, offset, topZ)
        val rightTop = Utils.v3d(-offset + width, offset, topZ)
        val rightFrontTop = Utils.v3d(-offset + width, 0 + depth - offset, topZ)
        val leftFrontTop = Utils.v3d(offset, 0 + depth - offset, topZ)
        val bottomObject = Utils.cylinder(5, 2)

        val topObject = Utils.sphere(5)
        if (!withTop) {
            return Utils.hull(
                bottomObject.move(leftBottom),
                bottomObject.move(rightBottom),
                bottomObject.move(leftFrontBottom),
                bottomObject.move(rightFrontBottom),
            )
        }
        return Utils.hull(
            bottomObject.move(leftBottom),
            bottomObject.move(rightBottom),
            bottomObject.move(leftFrontBottom),
            bottomObject.move(rightFrontBottom),

            topObject.move(leftTop),
            topObject.move(rightTop),
            topObject.move(leftFrontTop),
            topObject.move(rightFrontTop),
        )
    }
}
