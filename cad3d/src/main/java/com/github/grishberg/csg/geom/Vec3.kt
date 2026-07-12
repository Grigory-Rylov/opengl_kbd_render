package com.github.grishberg.csg.geom

/**
 * Immutable 3D vector (double precision).
 */
class Vec3(
    x: Double = 0.0,
    y: Double = 0.0,
    z: Double = 0.0
) {
    public val x: Double = x
    public val y: Double = y
    public val z: Double = z

    companion object {
        val ZERO = Vec3(0.0, 0.0, 0.0)
    }

    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(s: Double) = Vec3(x * s, y * s, z * s)
    operator fun div(s: Double) = Vec3(x / s, y / s, z / s)
    operator fun unaryMinus() = Vec3(-x, -y, -z)

    fun dot(other: Vec3): Double = x * other.x + y * other.y + z * other.z
    fun cross(other: Vec3): Vec3 = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun length(): Double = Math.sqrt(x * x + y * y + z * z)
    fun normalized(): Vec3 = this / length()
    fun squaredLength(): Double = x * x + y * y + z * z

    fun snap(epsilon: Double = 1e-10): Vec3 = Vec3(
        if (Math.abs(x) < epsilon) 0.0 else x,
        if (Math.abs(y) < epsilon) 0.0 else y,
        if (Math.abs(z) < epsilon) 0.0 else z
    )
}
