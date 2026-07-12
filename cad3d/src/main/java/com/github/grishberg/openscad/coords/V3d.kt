package com.github.grishberg.openscad.coords

class V3d(
    @JvmField val x: Double = 0.0,
    @JvmField val y: Double = 0.0,
    @JvmField val z: Double = 0.0
) {
    companion object {
        @JvmField val ZERO = V3d(0.0, 0.0, 0.0)
        @JvmField val X = V3d(1.0, 0.0, 0.0)
        @JvmField val Y = V3d(0.0, 1.0, 0.0)
        @JvmField val Z = V3d(0.0, 0.0, 1.0)

        @JvmStatic fun xOnly(x: Double): V3d = V3d(x, 0.0, 0.0)
        @JvmStatic fun yOnly(y: Double): V3d = V3d(0.0, y, 0.0)
        @JvmStatic fun zOnly(z: Double): V3d = V3d(0.0, 0.0, z)
        @JvmStatic fun midPoint(a: V3d, b: V3d): V3d = V3d((a.x + b.x) / 2, (a.y + b.y) / 2, (a.z + b.z) / 2)
    }

    fun add(other: V3d) = V3d(x + other.x, y + other.y, z + other.z)
    operator fun plus(other: V3d) = add(other)
    operator fun minus(other: V3d) = V3d(x - other.x, y - other.y, z - other.z)
    operator fun unaryMinus() = V3d(-x, -y, -z)
    operator fun div(s: Double) = V3d(x / s, y / s, z / s)
    fun mul(s: Double) = V3d(x * s, y * s, z * s)
    fun scale(s: Double) = mul(s)
    fun inverse() = unaryMinus()
    fun subtract(other: V3d) = minus(other)

    fun getX(): Double = x
    fun getY(): Double = y
    fun getZ(): Double = z

    fun dot(other: V3d): Double = x * other.x + y * other.y + z * other.z
    fun cross(other: V3d): V3d = V3d(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )
    fun magnitude(): Double = Math.sqrt(x * x + y * y + z * z)
    fun normalized(): V3d {
        val m = magnitude()
        return if (m > 1e-15) this / m else V3d(0.0, 0.0, 1.0)
    }
    fun distanceTo(other: V3d): Double = (this - other).magnitude()
    fun distance(other: V3d): Double = distanceTo(other)
    fun unit(): V3d = normalized()
    fun angleTo(other: V3d): Double = Math.acos(dot(other) / (magnitude() * other.magnitude()))
    fun reflect(normal: V3d): V3d {
        val n = normal.normalized()
        val d = 2 * dot(n)
        return V3d(x - d * n.x, y - d * n.y, z - d * n.z)
    }
    fun lerp(other: V3d, t: Double): V3d = V3d(x + (other.x - x) * t, y + (other.y - y) * t, z + (other.z - z) * t)
    fun clone(): V3d = V3d(x, y, z)
    fun isZero(): Boolean = x == 0.0 && y == 0.0 && z == 0.0
    fun toArray(): DoubleArray = doubleArrayOf(x, y, z)
    fun projectionZ(z: Double): V3d = V3d(this.x, this.y, z)
    fun projectionY(y: Double): V3d = V3d(this.x, y, this.z)
    fun projectionX(x: Double): V3d = V3d(x, this.y, this.z)
    override fun toString(): String = "V3d($x, $y, $z)"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is V3d) return false
        return x == other.x && y == other.y && z == other.z
    }
    override fun hashCode(): Int = 31 * (31 * x.hashCode() + y.hashCode()) + z.hashCode()
}

class Angles3d(
    @JvmField val x: Double = 0.0,
    @JvmField val y: Double = 0.0,
    @JvmField val z: Double = 0.0
) {
    companion object {
        @JvmField val ZERO = Angles3d(0.0, 0.0, 0.0)
        @JvmStatic fun xOnly(x: Double): Angles3d = Angles3d(x, 0.0, 0.0)
        @JvmStatic fun yOnly(y: Double): Angles3d = Angles3d(0.0, y, 0.0)
        @JvmStatic fun zOnly(z: Double): Angles3d = Angles3d(0.0, 0.0, z)
    }

    fun isZero(): Boolean = x == 0.0 && y == 0.0 && z == 0.0
    override fun toString(): String = "Angles3d($x, $y, $z)"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Angles3d) return false
        return x == other.x && y == other.y && z == other.z
    }
    override fun hashCode(): Int = 31 * (31 * x.hashCode() + y.hashCode()) + z.hashCode()
}

class Dims3d(val x: Double, val y: Double, val z: Double) {
    fun clone(): Dims3d = Dims3d(x, y, z)
}

// Extension functions for backward compatibility
fun V3d.withZ(z: Double): V3d = V3d(this.x, this.y, z)
fun V3d.withY(y: Double): V3d = V3d(this.x, y, this.z)
fun V3d.withX(x: Double): V3d = V3d(x, this.y, this.z)
