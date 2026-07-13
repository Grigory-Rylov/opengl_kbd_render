package com.github.grishberg.openscad.tranzitions

import com.github.grishberg.openscad.models.Abstract3dModel
import com.github.grishberg.openscad.models.IModel
import com.github.grishberg.openscad.utils.Color
import com.github.grishberg.openscad.vrl.CSG
import com.github.grishberg.openscad.vrl.FacetGenerationContext
import com.github.grishberg.csg.geom.PolySet3
import com.github.grishberg.csg.model.Model as CsgModel

class Union(vararg models: IModel) : Abstract3dModel(CsgModel(PolySet3.EMPTY)) {
    constructor(models: Collection<IModel>) : this(*models.filterNotNull().toTypedArray())
    constructor(color: Color, models: Collection<IModel>) : this(*models.filterNotNull().toTypedArray())
    constructor(models: List<Abstract3dModel>) : this(*models.toTypedArray())

    private val children: List<IModel> = models.filterNotNull()

    override fun toCSG(context: FacetGenerationContext): CSG {
        if (children.isEmpty()) return super.toCSG(context)
        if (children.size == 1) return children[0].toCSG(context)

        // Конкатенация полигонов (как в оригинальном JSCAD Union)
        val result = CSG()
        for (child in children) {
            result.polygons.addAll(child.toCSG(context).polygons)
        }
        return result
    }

    override fun cloneModel(): Union = Union(*children.toTypedArray())
}

class Difference(val model1: IModel, val model2: IModel) : Abstract3dModel(CsgModel(PolySet3.EMPTY)) {
    constructor(models: List<IModel>) : this(
        models[0],
        models.getOrElse(1) { EmptyModel }
    )

    override fun toCSG(context: FacetGenerationContext): CSG {
        return model1.toCSG(context).difference(model2.toCSG(context))
    }

    override fun cloneModel(): Difference = Difference(model1, model2)

    companion object {
        private val EmptyModel = object : IModel {
            override fun toCSG(context: FacetGenerationContext): CSG = CSG()
            override fun cloneModel(): IModel = this
        }
    }
}
