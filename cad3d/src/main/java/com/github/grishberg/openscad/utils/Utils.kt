package com.github.grishberg.openscad.utils

class Color @JvmOverloads constructor(
    val r: Int,
    val g: Int,
    val b: Int,
    val a: Int = 255
) {
    companion object {
        @JvmField val GRAY = Color(128, 128, 128)
        @JvmField val RED = Color(255, 0, 0)
        @JvmField val GREEN = Color(0, 255, 0)
        @JvmField val BLUE = Color(0, 0, 255)
        @JvmField val BLACK = Color(0, 0, 0)
        @JvmField val WHITE = Color(255, 255, 255)
        @JvmField val PINK = Color(255, 192, 203)
        @JvmField val CYAN = Color(0, 255, 255)
        @JvmField val ORANGE = Color(255, 165, 0)
        @JvmField val YELLOW = Color(255, 255, 0)
        @JvmField val MAGENTA = Color(255, 0, 255)
        @JvmField val BROWN = Color(165, 42, 42)
        @JvmField val DARKGREEN = Color(0, 128, 0)
        @JvmField val DARKBLUE = Color(0, 0, 128)
        @JvmField val LIGHTGRAY = Color(211, 211, 211)
        @JvmField val DARKGRAY = Color(64, 64, 64)
        @JvmField val TRANSPARENT = Color(0, 0, 0, 0)
        @JvmField val ALICE_BLUE = Color(240, 248, 255)
        @JvmField val AQUA = Color(0, 255, 255)
        @JvmField val AQUA_MARINE = Color(127, 255, 212)
        @JvmField val BISQUE = Color(255, 228, 196)
        @JvmField val BURLY_WOOD = Color(222, 184, 135)
        @JvmField val CORN_SILK = Color(255, 248, 220)
        @JvmField val DARK_RED = Color(139, 0, 0)
        @JvmField val LIGHT_BLUE = Color(173, 216, 230)
        @JvmField val LIGHT_GRAY = Color(211, 211, 211)
        @JvmField val LIGHT_SKY_BLUE = Color(135, 206, 250)
        @JvmField val YELLOW_GREEN = Color(154, 205, 50)
        @JvmField val green = GREEN
        @JvmField val yellow = YELLOW
        @JvmField val red = RED
        @JvmField val blue = BLUE
        @JvmField val black = BLACK
        @JvmField val white = WHITE
        @JvmField val gray = GRAY
        @JvmField val lightGray = LIGHTGRAY
    }

    fun getRed(): Int = r
    fun getGreen(): Int = g
    fun getBlue(): Int = b
    fun getAlpha(): Int = a
}

class Dims3d(val x: Double, val y: Double, val z: Double)

object Const {
    const val EPSILON: Double = 0.001
    const val TAU: Double = 2.0 * Math.PI
}

object DoubleUtils {
    @JvmStatic
    fun clamp(value: Double, min: Double, max: Double): Double {
        return if (value < min) min else if (value > max) max else value
    }
    @JvmStatic
    fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t
}

object AssertValue {
    @JvmStatic
    fun notNull(value: Any?, name: String) {
        if (value == null) throw IllegalArgumentException("$name must not be null")
    }
}
