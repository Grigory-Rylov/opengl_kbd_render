package com.github.grishberg.scripting.matrix

import com.github.grishberg.javascad.tranzitions.Union

fun scriptMain(): Model {
    val placeholder = createPlaceholder()

    val placeholderModels = mutableListOf<Model>()
    for (col in 0 until KeyCfg.columnsCount) {
        for (row in 0 until KeyCfg.rowsCount) {
            placeholderModels.add(placeKey(placeholder, col, row))
        }
    }
    placeholderModels.add(placeThumbL(placeholder))
    placeholderModels.add(placeThumbM(placeholder))
    placeholderModels.add(placeThumbR(placeholder))

    val connections = buildConnections()
    val borders = buildBorders()

    return Union(placeholderModels).addModel(connections).addModel(borders)
}
