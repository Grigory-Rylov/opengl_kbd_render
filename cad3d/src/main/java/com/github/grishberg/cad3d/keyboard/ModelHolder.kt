package com.github.grishberg.cad3d.keyboard

import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.plugin.VertexHolder
import com.github.grishberg.cad3d.util.fromModel
import com.github.grishberg.javascad.models.Model

data class ModelHolder(
    /**
     * Model for printing
     */
    val model: Model,
    /**
     * Models for rendering in viewer
     */
    val vertexHolders: List<VertexHolder>
) {

    constructor(
        model: Model, vararg vertexHolders: VertexHolder
    ) : this(model, vertexHolders.asList())

    constructor(
        model: Model, vertexHoldersList: List<VertexHolder>, vararg vertexHolders: VertexHolder
    ) : this(model, vertexHoldersList + vertexHolders.asList())

    constructor(
        cfg: KeyboardConfig,
        model: Model,
        partialModelsList: List<Model>,
        vararg partialModels: Model,
    ) : this(model, (partialModelsList + partialModels.asList()).map { fromModel(it, cfg.fn) })

    constructor(
        cfg: KeyboardConfig, model: Model, vararg partialModels: Model,
    ) : this(model, createVertexHolders(cfg, model, partialModels.asList()))

    private companion object {

        fun createVertexHolders(
            cfg: KeyboardConfig,
            model: Model,
            partialModels: List<Model>,
        ): List<VertexHolder> {
            val vertexHolders = partialModels.map { fromModel(it, cfg.fn) }
            return vertexHolders.ifEmpty { listOf(fromModel(model, cfg.fn)) }
        }
    }
}
