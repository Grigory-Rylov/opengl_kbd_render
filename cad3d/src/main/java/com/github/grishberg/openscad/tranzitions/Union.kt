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

        // Collect CSG from all children and merge polygons
        var combined = children[0].toCSG(context)
        for (i in 1 until children.size) {
            val otherCsg = children[i].toCSG(context)
            combined.polygons.addAll(otherCsg.polygons)
        }
        return combined
    }

    override fun cloneModel(): Union = Union(*children.toTypedArray())
}

class Difference(val model1: IModel, val model2: IModel) : Abstract3dModel(CsgModel(PolySet3.EMPTY)) {
    constructor(models: List<IModel>) : this(
        models[0],
        models.getOrElse(1) { CubePlaceholder() }
    )

    override fun toCSG(context: FacetGenerationContext): CSG {
        // For now: just return model1's CSG (difference not implemented yet)
        return model1.toCSG(context)
    }

    override fun cloneModel(): Difference = Difference(model1, model2)

    private class CubePlaceholder : IModel {
        override fun toCSG(context: FacetGenerationContext): CSG = CSG()
        override fun cloneModel(): IModel = CubePlaceholder()
    }
}
