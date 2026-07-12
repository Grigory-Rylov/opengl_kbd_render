package com.github.grishberg.openscad.basic

@JvmInline value class Radius(val value: Double) {
    companion object {
        @JvmStatic fun fromRadius(r: Double): Radius = Radius(r)
        @JvmStatic fun fromDiameter(d: Double): Radius = Radius(d / 2)
        @JvmStatic fun fromRadius(r: Int): Radius = Radius(r.toDouble())
        @JvmStatic fun fromDiameter(d: Int): Radius = Radius(d.toDouble() / 2)
    }
}

@JvmInline value class Angle(val value: Double) {
    companion object {
        @JvmStatic fun fromAngle(d: Double): Angle = Angle(d)
        @JvmStatic fun fromAngle(d: Int): Angle = Angle(d.toDouble())
    }
}
