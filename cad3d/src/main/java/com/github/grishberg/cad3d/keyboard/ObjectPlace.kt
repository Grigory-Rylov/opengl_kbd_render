package com.github.grishberg.cad3d.keyboard

import com.github.grishberg.javascad.models.Model

interface ObjectPlace {

    fun place(obj: Model): Model
}
