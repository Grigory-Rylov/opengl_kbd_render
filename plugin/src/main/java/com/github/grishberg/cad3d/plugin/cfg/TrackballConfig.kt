package com.github.grishberg.cad3d.plugin.cfg

import kotlinx.serialization.Serializable


@Serializable
data class TrackballConfig(
    val mode: TrackballMode,
    val ballDiameter: Double,
    val bearingDiameter: Double,
)
