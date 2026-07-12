package com.github.grishberg.cad3d.keyboard.casebody.wall

/** EdgeType: типы кромок. */
enum class EdgeType {
    ROUNDED,
    CHAMFERED,
    STRAIGHT
}

/** SmoothSurface: гладкие поверхности. */
interface SmoothSurface {
    fun getS12x3(): SmoothSurface = this
}

/** VoronoiSurface: поверхность Вороного. */
class VoronoiSurface(
    val cellSize: Double
)

object surfaces {
    val smooth: SmoothSurface = object : SmoothSurface {}
}
