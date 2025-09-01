package com.github.grishberg.cad3d.keyboard

import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode
import com.github.grishberg.cad3d.plugin.cfg.ThumbClusterSettings
import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.models.Abstract3dModel

class ThumbKeyPlace(private val cfg: KeyboardConfig) {

    private val thumbConfig: ThumbClusterSettings = cfg.thumbClusterSettings
    private val radiusZ: Double = thumbConfig.arcRadiusZ
    private val radiusY: Double = thumbConfig.arcRadiusY
    private val thumbCoordinates = mutableListOf<V3d>()

    init {
        val count = when (thumbConfig.type) {
            ThumbClusterMode.SingleColumn4Buttons -> 4
            else -> 3
        }
        var offset = 0.0
        for (thumb in 0 until count) {
            thumbCoordinates.add(0, V3d(offset, 0.0, 0.0))
            offset -= (thumbConfig.spaceBetweenKey + cfg.keyPlaceHolderWidth)
        }
    }

    fun thumbPlace(obj: Abstract3dModel): Abstract3dModel {
        return when (thumbConfig.type) {
            ThumbClusterMode.SingleColumn4Buttons -> placeR(obj).addModel(placeM(obj)).addModel(placeL(obj))
                .addModel(placeLM(obj))

            else -> placeR(obj).addModel(placeM(obj)).addModel(placeL(obj))
        }
    }

    private fun convertToArc(point: V3d): ArcResult {
        if (radiusZ == 0.0 && radiusY == 0.0) {
            return ArcResult(0.0, 0.0, point)
        }

        val x = point.x

        // Рассчитываем угол поворота (в радианах)
        val thetaRadiansZ = if (radiusZ == 0.0) 0.0 else x / radiusZ
        val thetaRadiansY = if (radiusY == 0.0) 0.0 else x / radiusY

        // Вычисляем новые координаты
        val newXZ =
            if (radiusZ == 0.0) x else radiusZ * Math.sin(thetaRadiansZ) //+ radiusY * Math.sin(thetaRadiansY)) // TODO: Calculate with Y
        val newXY =
            if (radiusY == 0.0) x else radiusY * Math.sin(thetaRadiansY) //+ radiusY * Math.sin(thetaRadiansY)) // TODO: Calculate with Y
        val newY = if (radiusZ == 0.0) point.y else radiusZ - radiusZ * Math.cos(thetaRadiansZ)
        val newZ = if (radiusY == 0.0) point.z else radiusY * Math.cos(thetaRadiansY) - radiusY

        // Угол поворота объекта (касательная к дуге) в градусах

        // Угол поворота объекта (касательная к дуге) в градусах
        val rotationAngleDegreesZ = Math.toDegrees(thetaRadiansZ)
        val rotationAngleDegreesY = Math.toDegrees(thetaRadiansY)

        // Сохраняем исходную Z-координату
        return ArcResult(rotationAngleDegreesZ, rotationAngleDegreesY, V3d((x + newXY + newXZ) / 3, newY, newZ))
    }

    private fun convertToArc0(point: V3d): ArcResult {
        if (radiusZ == 0.0 && radiusY == 0.0) {
            return ArcResult(0.0, 0.0, point)
        }

        // Применяем кривизну по оси X
        val thetaX: Double = if (radiusZ == 0.0) 0.0 else point.x / radiusZ
        var x: Double = radiusZ * Math.sin(thetaX)
        if (radiusZ == 0.0) {
            x = point.x
        }

        // Применяем кривизну по оси Y

        // Применяем кривизну по оси Y
        val thetaY = if (radiusY == 0.0) 0.0 else point.y / radiusY
        val y = radiusY * Math.sin(thetaY)
        val zY = radiusY * (1 - Math.cos(thetaY))

        // Комбинируем координаты

        // Комбинируем координаты
        val newPos = V3d(
            x, y, point.z + zY
        )

        // Рассчитываем углы поворота

        // Рассчитываем углы поворота
        val pitch = Math.toDegrees(thetaY)
        val yaw = Math.toDegrees(thetaX)

        return ArcResult(yaw, pitch, newPos)

    }

    fun placeR(obj: Abstract3dModel): Abstract3dModel {
        val offset = when (thumbConfig.type) {
            ThumbClusterMode.SingleColumn4Buttons -> thumbCoordinates[3]
            else -> thumbCoordinates[2]
        }
        val arcResult = convertToArc(offset)
        return place(obj, 0.0, arcResult.angleY, arcResult.angleZ, arcResult.offset)

    }

    fun placeM(obj: Abstract3dModel): Abstract3dModel {
        val offset = when (thumbConfig.type) {
            ThumbClusterMode.SingleColumn4Buttons -> thumbCoordinates[2]
            else -> thumbCoordinates[1]
        }
        val arcResult = convertToArc(offset)
        return place(
            obj, 0.0, arcResult.angleY, arcResult.angleZ, arcResult.offset
        )
    }

    fun placeL(obj: Abstract3dModel): Abstract3dModel {
        val offset = thumbCoordinates[0]
        val arcResult = convertToArc(offset)
        return place(
            obj, 0.0, arcResult.angleY, arcResult.angleZ, arcResult.offset
        )
    }

    fun placeLM(obj: Abstract3dModel): Abstract3dModel {
        val offset = thumbCoordinates[1]
        val arcResult = convertToArc(offset)
        return place(
            obj, 0.0, arcResult.angleY, arcResult.angleZ, arcResult.offset
        )
    }

    private fun place(
        obj: Abstract3dModel, xAngle: Number, yAngle: Number, zAngle: Number, offset: V3d
    ): Abstract3dModel {
        return obj.rotate(xAngle.toDouble(), yAngle.toDouble(), zAngle.toDouble()).move(offset)
            .rotate(0.0, thumbConfig.rotateY, thumbConfig.rotateZ)
            .move(thumbConfig.xOffset, thumbConfig.yOffset, thumbConfig.zOffset + cfg.plateZOffset)
    }

    private data class ArcResult(val angleZ: Double, val angleY: Double, val offset: V3d)
}
