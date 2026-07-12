package com.github.grishberg.cad3d.keyboard

import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode
import com.github.grishberg.cad3d.plugin.cfg.ThumbClusterSettings
import com.github.grishberg.openscad.coords.V3d
import com.github.grishberg.openscad.models.Abstract3dModel

class ThumbKeyPlace(private val cfg: KeyboardConfig) {

    private val thumbConfig: ThumbClusterSettings = cfg.thumbClusterSettings
    private val radiusZ: Double = thumbConfig.arcRadiusZ
    private val radiusY: Double = thumbConfig.arcRadiusY

    private val row2radiusZ: Double = thumbConfig.arcRadiusZ
    private val row2radiusY: Double = thumbConfig.arcRadiusY
    private val thumbCoordinates = mutableListOf<V3d>()
    private val thumbCoordinatesRow2 = mutableListOf<V3d>() // Координаты для второго ряда
    private val secondRowOffset = V3d(11.5, -18.5, 0.0)

    init {
        val count = when (thumbConfig.type) {
            ThumbClusterMode.SingleColumn4Buttons -> 4
            ThumbClusterMode.SingleColumn3Buttons -> 3
            ThumbClusterMode.TwoRows5Buttons -> 3
        }
        var offset = 0.0
        for (thumb in 0 until count) {
            thumbCoordinates.add(0, V3d(offset, 0.0, 0.0))
            offset -= (thumbConfig.spaceBetweenKey + cfg.keyPlaceConfig.keyPlaceHolderWidth)
        }

        if (thumbConfig.type == ThumbClusterMode.TwoRows5Buttons) {
            // Добавляем второй ряд со смещением по Y ближе
            offset = 0.0

            for (thumb in 0 until 2) {
                offset -= (thumbConfig.spaceBetweenKey + cfg.keyPlaceConfig.keyPlaceHolderWidth)
                thumbCoordinatesRow2.add(0, V3d(offset, 0.0, 0.0))

            }
        }
    }

    fun thumbPlace(obj: Abstract3dModel): Abstract3dModel {
        return when (thumbConfig.type) {
            ThumbClusterMode.SingleColumn4Buttons -> placeR(obj).addModel(placeM(obj)).addModel(placeL(obj))
                .addModel(placeLM(obj))

            ThumbClusterMode.TwoRows5Buttons -> placeR(obj).addModel(placeM(obj)).addModel(placeL(obj))
                .addModel(placeR2(obj)).addModel(placeL2(obj)) // Добавляем кнопки второго ряда

            else -> placeR(obj).addModel(placeM(obj)).addModel(placeL(obj))
        }
    }

    private fun convertToArc(
        point: V3d, radiusY: Double = this.radiusY, radiusZ: Double = this.radiusZ, offset: V3d = V3d(0.0, 0.0, 0.0)
    ): ArcResult {
        if (radiusZ == 0.0 && radiusY == 0.0) {
            return ArcResult(0.0, 0.0, point)
        }

        val x = point.x

        // Рассчитываем угол поворота (в радианах)
        val thetaRadiansZ = if (radiusZ == 0.0) 0.0 else x / radiusZ
        val thetaRadiansY = if (radiusY == 0.0) 0.0 else x / radiusY

        // Вычисляем новые координаты
        val newXZ = if (radiusZ == 0.0) x else radiusZ * Math.sin(thetaRadiansZ)
        val newXY = if (radiusY == 0.0) x else radiusY * Math.sin(thetaRadiansY)
        val newY = if (radiusZ == 0.0) point.y else radiusZ - radiusZ * Math.cos(thetaRadiansZ)
        val newZ = if (radiusY == 0.0) point.z else radiusY * Math.cos(thetaRadiansY) - radiusY

        // Угол поворота объекта (касательная к дуге) в градусах
        val rotationAngleDegreesZ = Math.toDegrees(thetaRadiansZ)
        val rotationAngleDegreesY = Math.toDegrees(thetaRadiansY)

        // Сохраняем исходную Z-координату
        return ArcResult(
            rotationAngleDegreesZ, rotationAngleDegreesY, V3d((x + newXY + newXZ) / 3, newY, newZ).add(offset)
        )
    }

    fun placeR(obj: Abstract3dModel): Abstract3dModel {
        val offset = when (thumbConfig.type) {
            ThumbClusterMode.SingleColumn4Buttons -> thumbCoordinates[3]
            ThumbClusterMode.TwoRows5Buttons -> thumbCoordinates[2]
            else -> thumbCoordinates[2]
        }
        val arcResult = convertToArc(offset)
        return place(obj, 0.0, arcResult.angleY, arcResult.angleZ, arcResult.offset)
    }

    fun placeM(obj: Abstract3dModel): Abstract3dModel {
        val offset = when (thumbConfig.type) {
            ThumbClusterMode.SingleColumn4Buttons -> thumbCoordinates[2]
            ThumbClusterMode.TwoRows5Buttons -> thumbCoordinates[1]
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
        val arcResult = convertToArc(point = offset)
        return place(
            obj, 0.0, arcResult.angleY, arcResult.angleZ, arcResult.offset
        )
    }

    // Методы для второго ряда
    fun placeR2(obj: Abstract3dModel): Abstract3dModel {
        val offset = thumbCoordinatesRow2[1] // Правая кнопка второго ряда
        val arcResult = convertToArc(point = offset, radiusY = row2radiusY, radiusZ = row2radiusZ, secondRowOffset)
        return place(
            obj, 0.0, arcResult.angleY, arcResult.angleZ - 7.0, arcResult.offset.add(V3d(1.0, -1.0, 0.0))
        )
    }

    fun placeL2(obj: Abstract3dModel): Abstract3dModel {
        val offset = thumbCoordinatesRow2[0] // Левая кнопка второго ряда
        val arcResult = convertToArc(point = offset, radiusY = row2radiusY, radiusZ = row2radiusZ, secondRowOffset)
        return place(
            obj, 0.0, arcResult.angleY, arcResult.angleZ, arcResult.offset
        )
    }

    private fun place(
        obj: Abstract3dModel, xAngle: Number, yAngle: Number, zAngle: Number, offset: V3d
    ): Abstract3dModel {
        return obj.rotate(xAngle.toDouble(), yAngle.toDouble(), zAngle.toDouble()).move(offset)
            .rotate(0.0, thumbConfig.rotateY, thumbConfig.rotateZ)
            .move(thumbConfig.xOffset, thumbConfig.yOffset, thumbConfig.zOffset + cfg.keyPlaceConfig.plateZOffset)
    }

    private data class ArcResult(val angleZ: Double, val angleY: Double, val offset: V3d)
}
