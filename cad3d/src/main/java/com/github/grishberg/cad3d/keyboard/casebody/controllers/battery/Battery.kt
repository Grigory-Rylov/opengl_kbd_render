package com.github.grishberg.cad3d.keyboard.casebody.controllers.battery

import com.github.grishberg.javascad.models.Model

interface Battery {

    val width: Double
    val depth: Double
    val height: Double

    fun create(): Model

    fun createBatteryPreview(): Model
}
