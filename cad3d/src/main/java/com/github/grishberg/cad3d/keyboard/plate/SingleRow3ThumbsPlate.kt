package com.github.grishberg.cad3d.keyboard.plate

import com.github.grishberg.cad3d.keyboard.ModelHolder
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.keyboard.screws.ScrewBase
import com.github.grishberg.cad3d.keyboard.screws.ScrewWallPlaces
import com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode
import com.github.grishberg.cad3d.util.fromModel
import com.github.grishberg.javascad.basic.Radius
import com.github.grishberg.javascad.coords.V3d
import com.github.grishberg.javascad.models.Model
import com.github.grishberg.javascad.models.Cylinder
import com.github.grishberg.javascad.models.Hull
import com.github.grishberg.javascad.tranzitions.Union
import com.github.grishberg.javascad.utils.Color

class SingleRow3ThumbsPlate(
    private val cfg: KeyboardConfig,
    private val bottomPoints: BottomPoints,
    private val screwWallPlaces: ScrewWallPlaces,
    private val screwBase: ScrewBase,
): Plate {

    override fun create(): ModelHolder {
        val models = mutableListOf<Model>()

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
            platePoint(bottomPoints.T0),
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

        //thumb1
        val part2 = Hull(
            platePoint(bottomPoints.T0),
            platePoint(bottomPoints.T1),
            platePoint(bottomPoints.T4),
            platePoint(bottomPoints.T5),
            platePoint(bottomPoints.T7),
            platePoint(bottomPoints.T6),
        ).subtractModel(screws)
        models.add(part2)
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

        val part5 = Hull(
            platePoint(bottomPoints.T0),
            platePoint(bottomPoints.T2),
            platePoint(bottomPoints.T8),
        ).subtractModel(screws)
        models.add(part5)

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

        return ModelHolder(
            Union(models),
            fromModel(part0.moveZ(offsetZ), Color.ORANGE, cfg.fn),
            fromModel(part1.moveZ(offsetZ), Color.ORANGE, cfg.fn),
            fromModel(part2.moveZ(offsetZ), Color.ORANGE, cfg.fn),
            fromModel(part3.moveZ(offsetZ), Color.ORANGE, cfg.fn),
            fromModel(part4.moveZ(offsetZ), Color.ORANGE, cfg.fn),
            fromModel(part5.moveZ(offsetZ), Color.ORANGE, cfg.fn),
            fromModel(part6.moveZ(offsetZ), Color.ORANGE, cfg.fn),
            fromModel(part7.moveZ(offsetZ), Color.ORANGE, cfg.fn),
        )
    }

    private fun platePoint(point: V3d): Model {
        return Cylinder(cfg.keyPlaceConfig.plateThickness, Radius.fromDiameter(3.0)).move(point)
    }
}
