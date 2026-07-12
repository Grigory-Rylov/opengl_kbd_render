package com.github.grishberg.cad3d.keyboard

import com.github.grishberg.cad3d.kbd.core.cfg.KeyPlaceConfig
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.openscad.coords.Angles3d
import com.github.grishberg.openscad.coords.V3d
import com.github.grishberg.openscad.models.Abstract3dModel
import kotlin.math.cos
import kotlin.math.sin

class KeyPlace(private val cfg: KeyPlaceConfig) {

    private val capTopHeight: Double = cfg.plateThickness + cfg.saProfileKeyHeight
    private val mountWidth: Double = cfg.keyswitchWidth + cfg.horizontalExtraSpace
    private val mountHeight: Double = cfg.keyswitchWidth + cfg.verticalExtraSpace

    private val columnRadius: Double =
        ((mountWidth + cfg.extraHeight) / 2.0) / sin(Math.toRadians(cfg.columnCurvature) / 2.0) + capTopHeight
    private val rowRadius: Double =
        ((mountHeight + cfg.extraWidth) / 2.0) / sin(Math.toRadians(cfg.rowCurvature) / 2.0) + capTopHeight

    @JvmOverloads
    fun place(column: Int, row: Int, obj: Abstract3dModel, offset: V3d = V3d(0.0, 0.0, 0.0)): Abstract3dModel {
        val keyOffset = cfg.columnOffsetProvider.getOffset(column)

        return obj.move(offset).move(0.0, 0.0, -rowRadius).rotate(Angles3d.xOnly(calculateXAngle(row)))
            .move(0.0, 0.0, rowRadius).move(0.0, 0.0, -columnRadius).rotate(Angles3d.yOnly(calculateYAngle(column)))
            .move(0.0, 0.0, columnRadius).move(keyOffset.x, keyOffset.y, keyOffset.z)
            .rotate(Angles3d.zOnly(cfg.zAngleProvider.getZAngle(column))).rotate(Angles3d.yOnly(cfg.tentingAngle))
            .move(0.0, 0.0, cfg.plateZOffset)
    }

    @JvmOverloads
    fun calculateCoordinates(
        column: Int, row: Int, initialPoint: V3d = V3d(0.0, 0.0, 0.0)
    ): V3d {
        val zOffset = cfg.plateZOffset
        val zAngle = cfg.zAngleProvider.getZAngle(column)
        val offset = cfg.columnOffsetProvider.getOffset(column)
        val dx = offset.getX()
        val dy = offset.getY()
        val dz = offset.getZ()
        val zRad1 = columnRadius
        val zRad2 = rowRadius
        var x = initialPoint.getX()
        var y = initialPoint.getY()
        var z = initialPoint.getZ()

        // Convert angles from degrees to radians because Java trigonometric functions use radians
        val zAngleRad = Math.toRadians(zAngle)
        val yAngleRad = Math.toRadians(calculateYAngle(column))
        val xAngleRad = Math.toRadians(calculateXAngle(row))
        val tentingAngleRad = Math.toRadians(cfg.tentingAngle)

        // 1) move(0,0,-rowRadius)
        z -= zRad2

        // 2) rotate X by +xAngle
        val yAfterX = y * cos(xAngleRad) - z * sin(xAngleRad)
        val zAfterX = y * sin(xAngleRad) + z * cos(xAngleRad)
        y = yAfterX
        z = zAfterX

        // 3) move back by rowRadius
        z += zRad2

        // 4) move(0,0,-columnRadius)
        z -= zRad1

        // 5) rotate Y by +yAngle
        val xAfterY = x * cos(yAngleRad) + z * sin(yAngleRad)
        val zAfterY = -x * sin(yAngleRad) + z * cos(yAngleRad)
        x = xAfterY
        z = zAfterY

        // 6) move back by columnRadius
        z += zRad1

        // 7) move by key offset
        x += dx
        y += dy
        z += dz

        // 8) rotate Z by zAngle
        val xAfterZ = x * cos(zAngleRad) - y * sin(zAngleRad)
        val yAfterZ = x * sin(zAngleRad) + y * cos(zAngleRad)
        x = xAfterZ
        y = yAfterZ

        // 9) rotate Y by tentingAngle
        val xAfterTenting = x * cos(tentingAngleRad) + z * sin(tentingAngleRad)
        val zAfterTenting = -x * sin(tentingAngleRad) + z * cos(tentingAngleRad)
        x = xAfterTenting
        z = zAfterTenting

        // 10) move by plateZOffset
        z += zOffset

        return V3d(x, y, z)
    }

    private fun calculateYAngle(column: Int): Double {
        return cfg.columnCurvature * (cfg.centerCol - column)
    }

    private fun calculateXAngle(row: Int): Double {
        return cfg.rowCurvature * (cfg.centerRow - row)
    }

    @JvmOverloads
    fun calculatePlacePoint(
        column: Int,
        row: Int,
        pointType: PlacePointType,
        xOffset: Double = 0.0,
        yOffset: Double = 0.0,
        zOffset: Double = 0.0
    ): V3d {
        var x = 0.0
        var y = 0.0
        var z = 0.0

        when (pointType) {
            PlacePointType.BackLeftTop -> {
                x = cfg.keyPlaceHolderWidth / -2.0 + xOffset
                y = cfg.keyPlaceHolderDepth / 2.0 + yOffset
                z = cfg.keyPlaceHolderHeight + zOffset
            }

            PlacePointType.BackRightTop -> {
                x = cfg.keyPlaceHolderWidth / 2.0 + xOffset
                y = cfg.keyPlaceHolderDepth / 2.0 + yOffset
                z = cfg.keyPlaceHolderHeight + zOffset
            }

            PlacePointType.FrontLeftTop -> {
                x = cfg.keyPlaceHolderWidth / -2.0 + xOffset
                y = cfg.keyPlaceHolderDepth / -2.0 + yOffset
                z = cfg.keyPlaceHolderHeight + zOffset
            }

            PlacePointType.FrontRightTop -> {
                x = cfg.keyPlaceHolderWidth / 2.0 + xOffset
                y = cfg.keyPlaceHolderDepth / -2.0 + yOffset
                z = cfg.keyPlaceHolderHeight + zOffset
            }

            PlacePointType.BackLeftBottom -> {
                x = cfg.keyPlaceHolderWidth / -2.0 + xOffset
                y = cfg.keyPlaceHolderDepth / 2.0 + yOffset
                z = zOffset
            }

            PlacePointType.BackRightBottom -> {
                x = cfg.keyPlaceHolderWidth / 2.0 + xOffset
                y = cfg.keyPlaceHolderDepth / 2.0 + yOffset
                z = zOffset
            }

            PlacePointType.FrontLeftBottom -> {
                x = cfg.keyPlaceHolderWidth / -2.0 + xOffset
                y = cfg.keyPlaceHolderDepth / -2.0 + yOffset
                z = zOffset
            }

            PlacePointType.FrontRightBottom -> {
                x = cfg.keyPlaceHolderWidth / 2.0 + xOffset
                y = cfg.keyPlaceHolderDepth / -2.0 + yOffset
                z = zOffset
            }
        }

        val initialPoint3d = V3d(x, y, z)
        return calculateCoordinates(column, row, initialPoint3d)
    }
}
