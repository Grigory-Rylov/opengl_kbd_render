package com.github.grishberg.cad3d.keyboard.casebody.controllers

import com.github.grishberg.javascad.models.Model

class SwitcherPlace(
    private val controller: Controller,
    private val controllerPlace: ControllerPlace,
) {

    fun place(model: Model): Model {
        return controllerPlace.place(model).move(-18.0, controller.depth / 2.0 +0.5, 2.5)
    }
}
