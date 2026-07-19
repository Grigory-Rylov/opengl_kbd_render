package com.github.grishberg.cad3d.keyboard.casebody.controllers.battery

import com.github.grishberg.javascad.models.Abstract3dModel

object NoBattery : Battery {

    override val width: Double = 0.0
    override val depth: Double = 0.0
    override val height: Double = 0.0

    override fun create(): Abstract3dModel {
        throw IllegalStateException()
    }

    override fun createBatteryPreview(): Abstract3dModel {
        throw IllegalStateException()
    }
}
