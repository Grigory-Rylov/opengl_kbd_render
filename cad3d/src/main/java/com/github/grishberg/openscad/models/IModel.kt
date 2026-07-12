package com.github.grishberg.openscad.models

import com.github.grishberg.openscad.vrl.CSG
import com.github.grishberg.openscad.vrl.FacetGenerationContext

interface IModel {
    fun toCSG(context: FacetGenerationContext): CSG
    fun cloneModel(): IModel
}
