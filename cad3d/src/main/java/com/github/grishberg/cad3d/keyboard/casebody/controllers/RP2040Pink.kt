package com.github.grishberg.cad3d.keyboard.casebody.controllers

import com.github.grishberg.cad3d.keyboard.ModelHolder
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.util.fromModel
import com.github.grishberg.javascad.basic.Radius
import com.github.grishberg.javascad.coords.Angles3d
import com.github.grishberg.javascad.models.Abstract3dModel
import com.github.grishberg.javascad.models.Cube
import com.github.grishberg.javascad.models.Cylinder
import com.github.grishberg.javascad.models.Hull
import com.github.grishberg.javascad.utils.Color

class RP2040Pink(
    private val cfg: KeyboardConfig,
) : Controller {

    override val width = 20.73
    override val depth = 53.7
    override val height = 1.6

    override val isWireless: Boolean = false

    private val topOffset = 1.5

    override fun create(controllerPlace: ControllerPlace): ModelHolder {
        val usbPort = placeUsbPort(createUsb())
        val model = Cube(width, depth, height).moveZ(height / 2)
        val resetButton = Cube(3.3, 4.4, 2.0).move(2, 14.6, -(height / 2))

        return ModelHolder(
            model.addModel(usbPort),
            fromModel(createBody(controllerPlace), cfg.fn),
            fromModel(place(controllerPlace, usbPort), Color.YELLOW, cfg.fn),
            fromModel(place(controllerPlace, resetButton), Color.RED, cfg.fn),
        )
    }

    override fun createBody(controllerPlace: ControllerPlace): Abstract3dModel {
        return place(controllerPlace, Cube(width, depth, height).moveZ(height / 2)).withColor(Color.CYAN)
    }

    override fun createResetButton(controllerPlace: ControllerPlace): Abstract3dModel? {
        val resetButton = Cube(3.3, 4.4, 2.0).move(2, 14.6, -(height / 2))
        return place(controllerPlace, resetButton).withColor(Color.RED)
    }

    override fun placeUsbPort(obj: Abstract3dModel): Abstract3dModel {
        return obj.move(0.0, depth / 2 - 1.0, -(height / 2 + 2.4))
    }

    private fun createUsb(): Abstract3dModel {
        val diameter = 3.2
        val width = 8.34

        val cylinder = Cylinder(7.5, Radius.fromDiameter(diameter)).rotate(Angles3d.xOnly(90.0))
        return Hull(
            cylinder.moveX(-width / 2 + diameter / 2), cylinder.moveX(width / 2 - diameter / 2)
        ).moveZ(diameter / 2).moveY(-2.6)
    }

    private fun place(controllerPlace: ControllerPlace, o: Abstract3dModel): Abstract3dModel {
        return controllerPlace.place(o).moveZ(topOffset)
    }
}
