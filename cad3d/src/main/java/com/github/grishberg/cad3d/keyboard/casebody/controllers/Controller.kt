package com.github.grishberg.cad3d.keyboard.casebody.controllers

import com.github.grishberg.cad3d.keyboard.ModelHolder
import com.github.grishberg.javascad.models.Abstract3dModel

interface Controller {

    val width: Double
    val depth: Double
    val height: Double

    val isWireless: Boolean

    fun create(controllerPlace: ControllerPlace): ModelHolder
    fun createBody(controllerPlace: ControllerPlace): Abstract3dModel
    fun createResetButton(controllerPlace: ControllerPlace): Abstract3dModel? = null

    fun placeUsbPort(obj: Abstract3dModel): Abstract3dModel
}
