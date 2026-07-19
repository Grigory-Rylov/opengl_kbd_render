package com.github.grishberg.scripting.matrix

import com.github.grishberg.javascad.models.Abstract3dModel

val thumbSpacing = ThumbCfg.spaceBetweenKey + KeyCfg.keyPlaceHolderWidth

fun placeThumbL(model: Abstract3dModel): Abstract3dModel = placeThumb(0.0, model)
fun placeThumbM(model: Abstract3dModel): Abstract3dModel = placeThumb(-thumbSpacing, model)
fun placeThumbR(model: Abstract3dModel): Abstract3dModel = placeThumb(-thumbSpacing * 2, model)

fun placeThumb(localX: Double, model: Abstract3dModel): Abstract3dModel {
    var result = model
    result = result.move(localX, 0.0, 0.0)
    result = result.rotate(0.0, ThumbCfg.rotateY, ThumbCfg.rotateZ)
    result = result.move(ThumbCfg.xOffset, ThumbCfg.yOffset, ThumbCfg.zOffset + KeyCfg.plateZOffset)
    return result
}
