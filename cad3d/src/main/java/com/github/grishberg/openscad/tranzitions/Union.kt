package com.github.grishberg.openscad.tranzitions

import com.github.grishberg.openscad.models.Abstract3dModel
import com.github.grishberg.openscad.models.IModel
import com.github.grishberg.openscad.utils.Color
import com.github.grishberg.csg.geom.PolySet3
import com.github.grishberg.csg.model.Model as CsgModel

class Union : Abstract3dModel {
    constructor() : super(CsgModel(PolySet3.EMPTY))
    constructor(vararg models: IModel) : super(CsgModel(PolySet3.EMPTY)) { require(models.isNotEmpty()) }
    constructor(models: Collection<IModel>) : super(CsgModel(PolySet3.EMPTY)) { require(models.isNotEmpty()) }
    constructor(color: Color, models: Collection<IModel>) : super(CsgModel(PolySet3.EMPTY))
    constructor(models: List<Abstract3dModel>) : super(CsgModel(PolySet3.EMPTY)) {
        require(models.isNotEmpty())
    }
    override fun cloneModel(): Union = Union()
}

class Difference : Abstract3dModel {
    constructor() : super(CsgModel(PolySet3.EMPTY))
    constructor(model1: IModel, model2: IModel) : super(CsgModel(PolySet3.EMPTY))
    constructor(models: List<IModel>) : super(CsgModel(PolySet3.EMPTY)) { require(models.isNotEmpty()) }
    override fun cloneModel(): Difference = Difference()
}
