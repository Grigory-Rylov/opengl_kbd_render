package com.github.grishberg.cad3d.keyboard.plate

import com.github.grishberg.cad3d.keyboard.ModelHolder
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.keyboard.screws.ScrewBase
import com.github.grishberg.cad3d.keyboard.screws.ScrewWallPlaces
import com.github.grishberg.cad3d.util.fromModel
import eu.printingin3d.javascad.basic.Radius
import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.models.Abstract3dModel
import eu.printingin3d.javascad.models.Cube
import eu.printingin3d.javascad.models.Cylinder
import eu.printingin3d.javascad.models.Hull
import eu.printingin3d.javascad.tranzitions.Union
import eu.printingin3d.javascad.utils.Color

class TwoRows5ThumbsPlate(
    private val cfg: KeyboardConfig,
    private val bottomPoints: BottomPoints,
    private val screwWallPlaces: ScrewWallPlaces,
    private val screwBase: ScrewBase,
): Plate {

    override fun create(): ModelHolder {
        val models = mutableListOf<Abstract3dModel>()

        val screw = screwBase.plateScrewHolder()
        val screws = screwWallPlaces.place(screw, ScrewWallPlaces.HeightMode.Plate)

        //main part
        val part0 = Hull(
            platePoint(bottomPoints.BCL),
            platePoint(bottomPoints.LB),
            platePoint(bottomPoints.BCML),
        ).subtractModel(screws)
        models.add(part0)

        val part1 = Hull(
            platePoint(bottomPoints.BCML),
            platePoint(bottomPoints.BCML),
            platePoint(bottomPoints.BCM),
            platePoint(bottomPoints.BCR),

            platePoint(bottomPoints.LB),
            platePoint(bottomPoints.L0),
            platePoint(bottomPoints.L1),
            platePoint(bottomPoints.L2),
            platePoint(bottomPoints.L3),
            platePoint(bottomPoints.L4),
            platePoint(bottomPoints.L5),

//            platePoint(bottomPoints.T0),
            platePoint(bottomPoints.T2),

            platePoint(bottomPoints.B7),
            platePoint(bottomPoints.B8),
            platePoint(bottomPoints.B6),
            platePoint(bottomPoints.B5),
            platePoint(bottomPoints.B4),
            platePoint(bottomPoints.B3),
            platePoint(bottomPoints.B2),
            platePoint(bottomPoints.B1),
            platePoint(bottomPoints.R0),
            platePoint(bottomPoints.R1),
            platePoint(bottomPoints.R2),
            platePoint(bottomPoints.R3),
            platePoint(bottomPoints.R4),
        ).subtractModel(screws)
        models.add(part1)

        val partBetweenMainAndThumb = Hull(
            platePoint(bottomPoints.L5),

            platePoint(bottomPoints.T0),
            platePoint(bottomPoints.T2),

        ).subtractModel(screws)
        models.add(partBetweenMainAndThumb)
        //thumb1
        val thumbPart1 = Hull(
            platePoint(bottomPoints.T0),
            platePoint(bottomPoints.T1),
            platePoint(bottomPoints.T4),
            platePoint(bottomPoints.T5),
            platePoint(bottomPoints.T7),
            platePoint(bottomPoints.T6),
            platePoint(bottomPoints.TR2L1!!),
            platePoint(bottomPoints.TR2L2!!),
            platePoint(bottomPoints.TR2L3!!),
            platePoint(bottomPoints.F0),

            ).subtractModel(screws)
        models.add(thumbPart1)

        //thumb1
        val thumbPart2 = Hull(
            platePoint(bottomPoints.TR2R2!!),
            platePoint(bottomPoints.TR2R1!!),
            platePoint(bottomPoints.TR2L3!!),
            platePoint(bottomPoints.T4),
        ).subtractModel(screws)
        models.add(thumbPart2)
        //thumb2
        val part3 = Hull(
            platePoint(bottomPoints.T0),
            platePoint(bottomPoints.T6),
            platePoint(bottomPoints.T9),
        ).subtractModel(screws)
        models.add(part3)

        val part4 = Hull(
            platePoint(bottomPoints.T0),
            platePoint(bottomPoints.T9),
            platePoint(bottomPoints.T8),
        ).subtractModel(screws)
        models.add(part4)


        val part6 = Hull(
            platePoint(bottomPoints.F0),
            platePoint(bottomPoints.F1),
            platePoint(bottomPoints.R4),
        ).subtractModel(screws)
        models.add(part6)

        val part7 = Hull(
            platePoint(bottomPoints.R4),
            platePoint(bottomPoints.F1),
            platePoint(bottomPoints.F2),
            platePoint(bottomPoints.F3),
            platePoint(bottomPoints.R5),
        ).subtractModel(screws)
        models.add(part7)

        val offsetZ = -cfg.keyPlaceConfig.plateThickness / 2

        val debugObject = Cube(3.0)
        return ModelHolder(
            Union(models),
            fromModel(part0.moveZ(offsetZ), Color.ORANGE, cfg.fn),
            fromModel(part1.moveZ(offsetZ), Color.ORANGE, cfg.fn),
            fromModel(partBetweenMainAndThumb.moveZ(offsetZ), Color.ORANGE, cfg.fn),
            fromModel(thumbPart1.moveZ(offsetZ), Color.ORANGE, cfg.fn),
            fromModel(thumbPart2.moveZ(offsetZ), Color.ORANGE, cfg.fn),
            fromModel(part3.moveZ(offsetZ), Color.ORANGE, cfg.fn),
            fromModel(part4.moveZ(offsetZ), Color.ORANGE, cfg.fn),
            fromModel(part6.moveZ(offsetZ), Color.ORANGE, cfg.fn),
            fromModel(part7.moveZ(offsetZ), Color.ORANGE, cfg.fn),
/*
            fromModel(debugObject.move(bottomPoints.L0).moveZ(offsetZ), Color.AZURE, cfg.fn),
            fromModel(debugObject.move(bottomPoints.L1).moveZ(offsetZ), Color.BLUE, cfg.fn),
            fromModel(debugObject.move(bottomPoints.L2).moveZ(offsetZ), Color.BLUE_VIOLET, cfg.fn),
            fromModel(debugObject.move(bottomPoints.L3).moveZ(offsetZ), Color.DARK_GOLDEN_ROD, cfg.fn),
            fromModel(debugObject.move(bottomPoints.L4).moveZ(offsetZ), Color.VIOLET, cfg.fn),
            fromModel(debugObject.move(bottomPoints.L5).moveZ(offsetZ), Color.GREEN, cfg.fn),
            fromModel(debugObject.move(bottomPoints.LB).moveZ(offsetZ), Color.RED, cfg.fn),

            fromModel(debugObject.move(bottomPoints.TR2L1!!).moveZ(offsetZ), Color.BEIGE, cfg.fn),
            fromModel(debugObject.move(bottomPoints.TR2L2!!).moveZ(offsetZ), Color.BLACK, cfg.fn),
            fromModel(debugObject.move(bottomPoints.TR2R1!!).moveZ(offsetZ), Color.LIGHT_BLUE, cfg.fn),
            fromModel(debugObject.move(bottomPoints.TR2R2!!).moveZ(offsetZ), Color.LIGHT_SKY_BLUE, cfg.fn),
            fromModel(debugObject.move(bottomPoints.TR2L3!!).moveZ(offsetZ), Color.RED, cfg.fn),
 */
        )
    }

    private fun platePoint(point: V3d): Abstract3dModel {
        return Cylinder(cfg.keyPlaceConfig.plateThickness, Radius.fromDiameter(3.0)).move(point)
    }
}
