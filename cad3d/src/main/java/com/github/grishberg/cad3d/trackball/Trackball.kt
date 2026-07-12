package com.github.grishberg.cad3d.trackball

import com.github.grishberg.cad3d.keyboard.KeyPlace
import com.github.grishberg.cad3d.keyboard.ModelHolder
import com.github.grishberg.cad3d.keyboard.Utils.hull
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.plugin.VertexHolder
import com.github.grishberg.cad3d.util.fromModel
import com.github.grishberg.openscad.basic.Radius
import com.github.grishberg.openscad.coords.Angles3d
import com.github.grishberg.openscad.utils.Dims3d
import com.github.grishberg.openscad.coords.V3d
import com.github.grishberg.openscad.models.Abstract3dModel
import com.github.grishberg.openscad.models.Cube
import com.github.grishberg.openscad.models.Cylinder
import com.github.grishberg.openscad.models.Hull
import com.github.grishberg.openscad.models.Sphere
import com.github.grishberg.openscad.tranzitions.Union
import com.github.grishberg.openscad.utils.Color

class Trackball(private val cfg: KeyboardConfig) {

    private val lensHeight = 3.5
    private val lensWidth = 8.15
    private val lensDepth = 17.0
    private val distanceToLens = 2.4
    private val legHeight = 14.0
    private val holeDiameter = cfg.trackball.ballDiameter + cfg.trackball.bearingDiameter
    private val wallSize = 2.0
    private val outerDiameter = holeDiameter + wallSize * 2
    private val holderHolesDistance = 27.0
    private val controllerDiameter = 32.0
    private val wallWidth = 1.5
    private val sensorOuterDiameter = controllerDiameter + 1 + wallWidth * 2
    private val bottomHoleDiameter = controllerDiameter + 1
    private val sensorCaseHeight = 10.0
    private val caseHeight = legHeight + 5
    private val legsBaseOffset = legHeight / 2 - cfg.trackball.ballDiameter / 2
    private val legOffset = legsBaseOffset - distanceToLens - lensHeight

    fun create(keyPlace: KeyPlace): ModelHolder {

        val model = createTrackballModel()

        val vertex = mutableListOf(fromModel(placeTrackball(model, keyPlace), Color.LIGHT_GRAY, cfg.fn))

        return ModelHolder(
            model, vertex
        )
    }

    fun createTrackBall(keyPlace: KeyPlace): VertexHolder {
        return fromModel(
            placeTrackball(Sphere(Radius.fromDiameter(cfg.trackball.ballDiameter).value), keyPlace), Color.ORANGE, cfg.fn
        )
    }

    fun createTrackballWireHole(keyPlace: KeyPlace): Abstract3dModel {
        return keyPlace.place(
            1, 0, Cylinder(12.0, Radius.fromDiameter(5.0).value).rotate(Angles3d.xOnly(-75.0)), V3d(0.0, 20.0, -7.0)
        ).moveY(6.0)
    }

    /**
     * Держалка корзины трекбола
     */
    fun trackBallCaseHolderOrigin(): Abstract3dModel {
        val legsBaseOffset = legHeight / 2 - cfg.trackball.ballDiameter / 2

        val legOffset = legsBaseOffset - distanceToLens - lensHeight

        val caseHeight = legHeight + 5
        val diameter = outerDiameter

        val holeDiameter = 3.2
        val wall = 1.5
        val height = holeDiameter + wall * 2
        val midHoleWidth = 7.1
        val width = 14.0
        val holder = Cube(width, 5.0 + 2, height).moveY(-3.5 - height).addModel(
            Cylinder(width, Radius.fromDiameter(height).value).rotate(Angles3d.yOnly(90.0)).moveY(-height)
        ).subtractModel(
            Cylinder(width, Radius.fromDiameter(holeDiameter).value).rotate(Angles3d.yOnly(90.0)).moveY(-height)
        ).subtractModel(Cube(midHoleWidth, 20.0, 20.0))

        val sensorCase = holder.move(0.0, -diameter / 2 + 3, 14.5).moveZ(-caseHeight / 2 - sensorCaseHeight / 2)
            .moveZ(legOffset + (caseHeight - legHeight) / 2)

        return sensorCase.withColor(Color.YELLOW_GREEN)
    }

    /**
     * Держалка корзины трекбола в нормальной ориентации
     */
    fun trackBallCaseHolder(): Abstract3dModel {
        val holeDiameter = 3.2
        val wall = 1.5
        val height = holeDiameter + wall * 2
        val midHoleWidth = 7.1
        val width = 14.0
        val holder = Cube(width, height, 2.0).moveZ(-holeDiameter - wall)
        val tube = Cylinder(width, Radius.fromDiameter(height).value).rotate(Angles3d.yOnly(90.0))
        val base = hull(tube, holder)
        .subtractModel(Cylinder(width, Radius.fromDiameter(holeDiameter).value).rotate(Angles3d.yOnly(90.0))).subtractModel(Cube(midHoleWidth, 20.0, 20.0))

        return Union(holder, base).withColor(Color.YELLOW_GREEN)
    }

    fun placeTrackball(model: Abstract3dModel, keyPlace: KeyPlace): Abstract3dModel {
        return keyPlace.place(
            1, 0, model.rotate(Angles3d.xOnly(60.0)), V3d(0.0, 26.5, 24.0)
        )
    }

    private fun createTrackballModel(): Abstract3dModel {

        val legs = createSensorHolderLeg(legHeight).moveZ(legOffset)

        val case = case(caseHeight).moveZ(legOffset + (caseHeight - legHeight) / 2)
        val innerHole = Sphere(Radius.fromDiameter(holeDiameter).value)
        val holeCube = Cube(Dims3d(100.0, 100.0, outerDiameter)).rotate(Angles3d.xOnly(-30.0)).moveZ(8.0)
        val lensHoleCube = Cube(Dims3d(lensWidth + 1, lensDepth + 1, outerDiameter))
        val outerSphere =
            Sphere(Radius.fromDiameter(outerDiameter).value).addModel(legs).subtractModel(innerHole).addModel(case)
                .subtractModel(holeCube.moveZ(outerDiameter / 2))
                .subtractModel(lensHoleCube.moveZ(-outerDiameter / 2 + cfg.trackball.bearingDiameter / 2))
                .addModel(bearingsHoles())

        return outerSphere.subtractModel(sensorCapLegs())
    }

    private fun case(caseHeight: Double): Abstract3dModel {
        val holeDiameter = outerDiameter - wallWidth * 2

        val diameter = outerDiameter
        val case =
            Cylinder(caseHeight, Radius.fromDiameter(sensorOuterDiameter).value, Radius.fromDiameter(diameter).value).subtractModel(
                Cylinder(
                    caseHeight, Radius.fromDiameter(bottomHoleDiameter).value, Radius.fromDiameter(holeDiameter).value
                )
            )

        val sensorCase = Cylinder(sensorCaseHeight, Radius.fromDiameter(sensorOuterDiameter).value).addModel(
            trackballHolder().move(
                0.0, -diameter / 2 + 3, 15.0
            )
        ).subtractModel(Cylinder(sensorCaseHeight, Radius.fromDiameter(bottomHoleDiameter).value))
            .moveZ(-caseHeight / 2 - sensorCaseHeight / 2)

        return case.addModel(sensorCase).subtractModel(cylinderHoles().moveZ(-5.5))
    }

    private fun cylinderHoles(): Abstract3dModel {
        val diameter = 14.0
        val cylinder = Cylinder(50.0, Radius.fromDiameter(diameter).value).rotate(Angles3d.yOnly(90.0))
        val hole = Hull(cylinder, cylinder.moveZ(2.0))
        return hole.addModel(hole.rotate(Angles3d.zOnly(60.0))).addModel(hole.rotate(Angles3d.zOnly(-60.0)))
    }

    private fun trackballHolder(): Abstract3dModel {
        val holeDiameter = 3.2
        val wall = 1.5
        val height = holeDiameter + wall * 2
        val width = 7.0
        return Cube(width, 5.0, height).moveY(-2.5).addModel(
            Cylinder(width, Radius.fromDiameter(height).value).rotate(Angles3d.yOnly(90.0)).moveY(-height)
        ).subtractModel(
            Cylinder(width, Radius.fromDiameter(holeDiameter).value).rotate(Angles3d.yOnly(90.0)).moveY(-height)
        )
    }

    /**
     * Держатель сенсора (ножки)
     */
    private fun createSensorHolderLeg(height: Double): Abstract3dModel {
        val holesDiameter = cfg.trackball.controllerScrewDiameter
        val legDiameter = 5.5

        val holeHeight = 4.0
        val leg = Cylinder(height, Radius.fromDiameter(legDiameter).value).subtractModel(
            Cylinder(
                holeHeight, Radius.fromDiameter(holesDiameter).value
            ).moveZ(-height / 2 + holeHeight / 2)
        )

        return Union(
            leg.moveY(-holderHolesDistance / 2.0), leg.moveY(holderHolesDistance / 2.0)
        )
    }

    private fun bearingsHoles(): Abstract3dModel {
        val bearingPlaceRadius = cfg.trackball.ballDiameter / 2 + cfg.trackball.bearingDiameter / 2

        return Union(
            bearingPlace(bearingPlaceRadius, 30.0, 30.0 - 180),
            bearingPlace(bearingPlaceRadius, 30.0, 160.0 - 180),
            bearingPlace(bearingPlaceRadius, 30.0, -90.0 - 180),

            bearingPlace(bearingPlaceRadius, -20.0, 160.0 - 180),
            bearingPlace(bearingPlaceRadius, -20.0, 30.0 - 180),

            )
    }

    private fun bearingPlace(radius: Double, yAngle: Double, zAngle: Double): Abstract3dModel {
        val bearing = Sphere(Radius.fromDiameter(cfg.trackball.bearingDiameter).value)
        return bearing.moveX(radius).rotate(Angles3d(0.0, yAngle, zAngle))
    }

    fun createTrackballSensor(keyPlace: KeyPlace): ModelHolder {
        val plateHeight = 1.65
        val legsBaseOffset = -cfg.trackball.ballDiameter / 2
        val legOffset = legsBaseOffset - distanceToLens - lensHeight

        val holeCylinder = Cylinder(4.0, Radius.fromDiameter(2.0).value)
        val holes = holeCylinder.moveY(holderHolesDistance / 2).addModel(holeCylinder.moveY(-holderHolesDistance / 2))

        val trackballSensor =
            Cylinder(plateHeight, Radius.fromDiameter(controllerDiameter).value).subtractModel(holes).addModel(
                Cube(
                    8.15, 16.71, lensHeight
                ).moveZ(lensHeight / 2 + plateHeight / 2)
            ).moveZ(-plateHeight / 2).moveZ(legOffset)

        return ModelHolder(
            placeTrackball(trackballSensor, keyPlace),
            fromModel(placeTrackball(trackballSensor, keyPlace), Color.ORANGE, 20),
        )
    }

    fun createSensorCap(keyPlace: KeyPlace): ModelHolder {
        val model = sensorCapModel()

        return ModelHolder(
            placeTrackball(model, keyPlace),
            fromModel(placeTrackball(model, keyPlace), Color.BLUE, cfg.fn),
        )
    }

    private fun sensorCapModel(): Abstract3dModel {
        val height = 1.5
        val offset = legOffset - legHeight - (sensorCaseHeight - 6)
        val case = Cylinder(
            height, Radius.fromDiameter(sensorOuterDiameter).value
        ).moveZ(offset).addModel(sensorCapLegs())
        return case
    }

    private fun sensorCapLegs(): Abstract3dModel {
        val height = 2.0
        val offset = legOffset - legHeight - (sensorCaseHeight - 6)
        val leg = Cylinder(3.0, Radius.fromRadius(1.0).value).rotate(Angles3d.yOnly(90.0)).moveZ(2.0)
            .addModel(Cube(3.0, 4.0, height).move(0.0, -1.5, height / 2))

        val diameter = controllerDiameter - 0.5
        val twoLegs = leg.moveY(diameter / 2).addModel(leg.rotate(Angles3d.zOnly(180.0)).moveY(-diameter / 2))
        return twoLegs.addModel(twoLegs.rotate(Angles3d.zOnly(90.0))).moveZ(offset)
    }
}
