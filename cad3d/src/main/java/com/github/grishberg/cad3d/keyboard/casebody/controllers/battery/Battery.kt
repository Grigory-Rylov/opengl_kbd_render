package com.github.grishberg.cad3d.keyboard.casebody.controllers.battery

import com.github.grishberg.javascad.models.Abstract3dModel

interface Battery {

    val width: Double
    val depth: Double
    val height: Double

    fun create(): Abstract3dModel

    fun createBatteryPreview(): Abstract3dModel
}
