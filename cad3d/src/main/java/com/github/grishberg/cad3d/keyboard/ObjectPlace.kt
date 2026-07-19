package com.github.grishberg.cad3d.keyboard

import com.github.grishberg.javascad.models.Abstract3dModel

interface ObjectPlace {

    fun place(obj: Abstract3dModel): Abstract3dModel
}
