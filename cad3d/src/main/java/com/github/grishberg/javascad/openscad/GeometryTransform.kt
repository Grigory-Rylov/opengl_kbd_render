package com.github.grishberg.javascad.openscad

import eu.printingin3d.javascad.coords.V3d

data class GeometryTransform(
    val m00: Double, val m01: Double, val m02: Double, val m03: Double,
    val m10: Double, val m11: Double, val m12: Double, val m13: Double,
    val m20: Double, val m21: Double, val m22: Double, val m23: Double
) {
    companion object {
        val IDENTITY = GeometryTransform(
            1.0, 0.0, 0.0, 0.0,
            0.0, 1.0, 0.0, 0.0,
            0.0, 0.0, 1.0, 0.0
        )

        fun translation(x: Double, y: Double, z: Double) = GeometryTransform(
            1.0, 0.0, 0.0, x,
            0.0, 1.0, 0.0, y,
            0.0, 0.0, 1.0, z
        )

        fun rotationX(angleDeg: Double): GeometryTransform {
            val rad = Math.toRadians(angleDeg)
            val c = Math.cos(rad)
            val s = Math.sin(rad)
            return GeometryTransform(
                1.0, 0.0, 0.0, 0.0,
                0.0, c, -s, 0.0,
                0.0, s, c, 0.0
            )
        }

        fun rotationY(angleDeg: Double): GeometryTransform {
            val rad = Math.toRadians(angleDeg)
            val c = Math.cos(rad)
            val s = Math.sin(rad)
            return GeometryTransform(
                c, 0.0, s, 0.0,
                0.0, 1.0, 0.0, 0.0,
                -s, 0.0, c, 0.0
            )
        }

        fun rotationZ(angleDeg: Double): GeometryTransform {
            val rad = Math.toRadians(angleDeg)
            val c = Math.cos(rad)
            val s = Math.sin(rad)
            return GeometryTransform(
                c, -s, 0.0, 0.0,
                s, c, 0.0, 0.0,
                0.0, 0.0, 1.0, 0.0
            )
        }
    }

    fun transform(v: V3d): V3d {
        val x = m00 * v.x + m01 * v.y + m02 * v.z + m03
        val y = m10 * v.x + m11 * v.y + m12 * v.z + m13
        val z = m20 * v.x + m21 * v.y + m22 * v.z + m23
        return V3d(x, y, z)
    }

    fun isMirror(): Boolean {
        val det = m00 * (m11 * m22 - m12 * m21) -
            m01 * (m10 * m22 - m12 * m20) +
            m02 * (m10 * m21 - m11 * m20)
        return det < 0
    }

    fun times(other: GeometryTransform): GeometryTransform {
        return GeometryTransform(
            m00 * other.m00 + m01 * other.m10 + m02 * other.m20,
            m00 * other.m01 + m01 * other.m11 + m02 * other.m21,
            m00 * other.m02 + m01 * other.m12 + m02 * other.m22,
            m00 * other.m03 + m01 * other.m13 + m02 * other.m23 + m03,
            m10 * other.m00 + m11 * other.m10 + m12 * other.m20,
            m10 * other.m01 + m11 * other.m11 + m12 * other.m21,
            m10 * other.m02 + m11 * other.m12 + m12 * other.m22,
            m10 * other.m03 + m11 * other.m13 + m12 * other.m23 + m13,
            m20 * other.m00 + m21 * other.m10 + m22 * other.m20,
            m20 * other.m01 + m21 * other.m11 + m22 * other.m21,
            m20 * other.m02 + m21 * other.m12 + m22 * other.m22,
            m20 * other.m03 + m21 * other.m13 + m22 * other.m23 + m23
        )
    }
}
