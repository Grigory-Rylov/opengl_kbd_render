package com.github.grishberg.javascad.optimizator

import com.github.grishberg.openscad.coords.V3d
import com.github.grishberg.openscad.utils.Const
import java.util.Objects
import kotlin.math.abs
import kotlin.math.sqrt

class LineKey private constructor(// Геттеры (опционально, если нужно получить данные из ключа)
    val pointOnLine: V3d, // Возвращаем копию, если V3d не иммутабельный
    val directionUnitVector: V3d
) {// Возвращаем копию, если V3d не иммутабельный

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }
        val lineKey = o as LineKey
        return pointOnLine == lineKey.pointOnLine && directionUnitVector == lineKey.directionUnitVector
    }

    override fun hashCode(): Int {
        // Используем Objects.hash для комбинирования хэш-кодов
        return Objects.hash(pointOnLine, directionUnitVector)
    }

    override fun toString(): String {
        return "LineKey{" + "pointOnLine=" + pointOnLine + ", directionUnitVector=" + directionUnitVector + '}'
    }

    companion object {

        /**
         * Создает LineKey из двух точек, определяющих отрезок.
         *
         * @param p0 Первая точка отрезка.
         * @param p1 Вторая точка отрезка.
         * @return LineKey, представляющий прямую, проходящую через p0 и p1.
         * @throws IllegalArgumentException если p0 и p1 совпадают.
         */
        @JvmStatic
        fun fromSegment(p0: V3d, p1: V3d): LineKey? {
            if (p0 == p1) {
                return null
            }

            // Вычисляем направляющий вектор
            val directionVector = V3d(p1.x - p0.x, p1.y - p0.y, p1.z - p0.z)

            // Нормализуем направляющий вектор
            val length =
                sqrt(directionVector.x * directionVector.x + directionVector.y * directionVector.y + directionVector.z * directionVector.z)

            //require(!(length < Const.EPSILON)) {
            //    "Точки p0 и p1 совпадают (длина вектора близка к нулю)."
           // }

            var unitDirection = V3d(
                directionVector.x / length, directionVector.y / length, directionVector.z / length
            )

            // Вычисляем точку на прямой, ближайшую к началу координат (точка перпендикуляра из
            // (0,0,0) на прямую)
            // Параметр t для точки на прямой P(t) = p0 + t * (p1 - p0) ближайшей к (0,0,0):
            // t = - (p0 . (p1 - p0)) / |p1 - p0|^2
            val dotProduct = p0.x * directionVector.x + p0.y * directionVector.y + p0.z * directionVector.z
            val t = -dotProduct / (length * length)

            val closestPoint = V3d(
                p0.x + t * directionVector.x, p0.y + t * directionVector.y, p0.z + t * directionVector.z
            )

            // Для обеспечения канонического представления, убедимся, что направляющий вектор
            // имеет положительную компоненту X (или Y, если X=0, или Z, если X=Y=0)
            // Это предотвратит ситуацию, когда (p0,p1) и (p1,p0) дадут разные ключи.
            if (needsToFlipDirection(unitDirection)) {
                unitDirection = V3d(-unitDirection.x, -unitDirection.y, -unitDirection.z)
                // Точка closestPoint остается той же, так как она определяет положение прямой
            }

            return LineKey(closestPoint, unitDirection)
        }

        /**
         * Определяет, нужно ли инвертировать направляющий вектор для канонического представления.
         *
         * @param unitDirection Единичный направляющий вектор.
         * @return true, если вектор нужно инвертировать.
         */
        private fun needsToFlipDirection(unitDirection: V3d): Boolean {
            // Стандартный способ: выбрать направление, при котором первая ненулевая компонента
            // положительна
            if (abs(unitDirection.x) > Const.EPSILON) {
                return unitDirection.x < 0
            }
            if (abs(unitDirection.y) > Const.EPSILON) {
                return unitDirection.y < 0
            }
            if (abs(unitDirection.z) > Const.EPSILON) {
                return unitDirection.z < 0
            }
            // Все компоненты нулевые (не должно происходить для единичного вектора)
            return false
        }
    }
}
