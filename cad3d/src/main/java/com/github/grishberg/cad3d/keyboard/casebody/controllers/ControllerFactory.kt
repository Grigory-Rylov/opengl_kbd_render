package com.github.grishberg.cad3d.keyboard.casebody.controllers

import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.plugin.cfg.ControllerType
import com.github.grishberg.javascad.basic.Radius
import com.github.grishberg.javascad.coords.Angles3d
import com.github.grishberg.javascad.models.Model
import com.github.grishberg.javascad.models.Cylinder
import com.github.grishberg.javascad.models.Hull

class ControllerFactory(private val cfg: KeyboardConfig) {

    private val usbHoleWidth = 13.0
    private val usbHoleHeight = 9.0
    private val usbPortHeight = 3.2
    private val usbHoleDepthPortHeight = 6.0
    private val usbHolderWallWidth = 1.0

    fun createController(): Controller {
        return when (cfg.controllerType){
            ControllerType.SuperMiniNRF52840 -> SuperMiniNRF52840(cfg)
            ControllerType.Rp2040Pink -> RP2040Pink(cfg)
            ControllerType.Rp2040Mini-> RP2040Mini(cfg)
        }
    }

    fun createUsbPortHole(): Model {
        return place(usbHoleObject())
    }

    fun createUsbPortCase(): Model {
        return place(
            usbHoleCaseObject().subtractModel(usbHoleObject().moveY(0.5)).subtractModel(createUsb()),
        )
    }

    private fun usbHoleObject(): Model {
        val diameter = usbHoleHeight
        val width = usbHoleWidth

        val cylinder = Cylinder(usbHoleDepthPortHeight, Radius.fromDiameter(diameter)).rotate(Angles3d.xOnly(90.0))
        return Hull(
            cylinder.moveX(-width / 2 + diameter / 2), cylinder.moveX(width / 2 - diameter / 2)
        ).moveY(6.5)
            .moveZ(( usbPortHeight) / 2)
    }

    private fun usbHoleCaseObject(): Model {
        val diameter = usbHoleHeight + 2 * usbHolderWallWidth
        val width = usbHoleWidth + 2 * usbHolderWallWidth

        val cylinder = Cylinder(cfg.wallsSettings.borderThickness *2, Radius.fromDiameter(diameter)).rotate(Angles3d.xOnly(90.0))
        return Hull(
            cylinder.moveX(-width / 2 + diameter / 2), cylinder.moveX(width / 2 - diameter / 2)
        ).moveY(4.5).moveZ((usbPortHeight) / 2)
    }

    private fun createUsb(): Model {
        val diameter = 3.2
        val width = 8.34

        val cylinder = Cylinder(5.0, Radius.fromDiameter(diameter)).rotate(Angles3d.xOnly(90.0))
        return Hull(
            cylinder.moveX(-width / 2 + diameter / 2), cylinder.moveX(width / 2 - diameter / 2)

        ).moveZ(diameter / 2).moveY(2.0)
    }

    private fun place(o: Model): Model {
        return o.moveZ(1.5)
    }
}
