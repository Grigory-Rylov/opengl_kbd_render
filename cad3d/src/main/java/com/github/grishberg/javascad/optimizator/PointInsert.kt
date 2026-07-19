package com.github.grishberg.javascad.optimizator

import com.github.grishberg.javascad.coords.V3d
import java.util.Objects

class PointInsert(val point: V3d, val position: Int) : Comparable<PointInsert> {

    override fun equals(o: Any?): Boolean {
        if (o == null || javaClass != o.javaClass) {
            return false
        }
        val that = o as PointInsert
        return position == that.position && point == that.point
    }

    override fun hashCode(): Int {
        return Objects.hash(point, position)
    }

    // Реализация метода compareTo для сортировки по position по УБЫВАНИЮ
    override fun compareTo(other: PointInsert): Int {
        return other.position.compareTo(this.position)
    }
}
