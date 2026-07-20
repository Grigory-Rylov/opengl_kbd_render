package com.github.grishberg.cad3d.keyboard.casebody.controllers

import com.github.grishberg.cad3d.keyboard.ModelHolder
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.util.fromModel
import com.github.grishberg.javascad.basic.Radius
import com.github.grishberg.javascad.coords.Angles3d
import com.github.grishberg.javascad.models.Model
import com.github.grishberg.javascad.models.Cube
import com.github.grishberg.javascad.models.Cylinder
import com.github.grishberg.javascad.models.Hull
import com.github.grishberg.javascad.utils.Color

class RP2040Mini(
    private val cfg: KeyboardConfig,
) : Controller {

    override val width = 17.97
    override val depth = 23.22
    override val height = 1.0

    override val isWireless: Boolean = false

    override fun create(controllerPlace: ControllerPlace): ModelHolder {
        val usbPort = placeUsbPort(createUsb())
        val model = Cube(width, depth, height).moveZ(height / 2)

        return ModelHolder(
            model.addModel(usbPort),
            fromModel(place(controllerPlace, model), Color.CYAN, cfg.fn),
            fromModel(place(controllerPlace, usbPort), Color.YELLOW, cfg.fn),
        )
    }

    override fun createBody(controllerPlace: ControllerPlace): Model {
        val model = Cube(width, depth, height).moveZ(height / 2)
        return place(controllerPlace, model).withColor(Color.CYAN)
    }

    override fun createResetButton(controllerPlace: ControllerPlace): Model? {
        return null
    }

    override fun placeUsbPort(obj: Model): Model {
        return obj.move(0.0, depth / 2 - 1.0, height / 2)
    }

    private fun createUsb(): Model {
        val diameter = 3.2
        val width = 8.34

        val cylinder = Cylinder(5.0, Radius.fromDiameter(diameter)).rotate(Angles3d.xOnly(90.0))
        return Hull(
            cylinder.moveX(-width / 2 + diameter / 2), cylinder.moveX(width / 2 - diameter / 2)
        ).moveZ(diameter / 2)
    }

    private fun place(controllerPlace: ControllerPlace, o: Model): Model {
        return controllerPlace.place(o).moveZ(1.5)
    }
}
