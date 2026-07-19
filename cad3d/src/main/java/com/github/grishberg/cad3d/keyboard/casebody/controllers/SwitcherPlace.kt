package com.github.grishberg.cad3d.keyboard.casebody.controllers

import com.github.grishberg.javascad.models.Abstract3dModel

class SwitcherPlace(
    private val controller: Controller,
    private val controllerPlace: ControllerPlace,
) {

    fun place(model: Abstract3dModel): Abstract3dModel {
        return controllerPlace.place(model).move(-18.0, controller.depth / 2.0 +0.5, 2.5)
    }
}
