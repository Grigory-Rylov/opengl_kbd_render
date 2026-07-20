package com.github.grishberg.scripting.matrix

import com.github.grishberg.javascad.models.Model

val thumbSpacing = ThumbCfg.spaceBetweenKey + KeyCfg.keyPlaceHolderWidth

fun placeThumbL(model: Model): Model = placeThumb(0.0, model)
fun placeThumbM(model: Model): Model = placeThumb(-thumbSpacing, model)
fun placeThumbR(model: Model): Model = placeThumb(-thumbSpacing * 2, model)

fun placeThumb(localX: Double, model: Model): Model {
    var result = model
    result = result.move(localX, 0.0, 0.0)
    result = result.rotate(0.0, ThumbCfg.rotateY, ThumbCfg.rotateZ)
    result = result.move(ThumbCfg.xOffset, ThumbCfg.yOffset, ThumbCfg.zOffset + KeyCfg.plateZOffset)
    return result
}
