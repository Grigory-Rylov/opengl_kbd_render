package com.github.grishberg.cad3d.keyboard.casebody.controllers.battery

import com.github.grishberg.javascad.models.Model

object NoBattery : Battery {

    override val width: Double = 0.0
    override val depth: Double = 0.0
    override val height: Double = 0.0

    override fun create(): Model {
        throw IllegalStateException()
    }

    override fun createBatteryPreview(): Model {
        throw IllegalStateException()
    }
}
