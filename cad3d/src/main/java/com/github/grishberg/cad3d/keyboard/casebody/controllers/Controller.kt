package com.github.grishberg.cad3d.keyboard.casebody.controllers

import com.github.grishberg.cad3d.keyboard.ModelHolder
import com.github.grishberg.javascad.models.Model

interface Controller {

    val width: Double
    val depth: Double
    val height: Double

    val isWireless: Boolean

    fun create(controllerPlace: ControllerPlace): ModelHolder
    fun createBody(controllerPlace: ControllerPlace): Model
    fun createResetButton(controllerPlace: ControllerPlace): Model? = null

    fun placeUsbPort(obj: Model): Model
}
