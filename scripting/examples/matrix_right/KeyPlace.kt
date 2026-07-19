package com.github.grishberg.scripting.matrix

import com.github.grishberg.javascad.models.Abstract3dModel

fun placeKey(model: Abstract3dModel, column: Int, row: Int): Abstract3dModel {
    val kfc = KeyCfg
    val ko = keyOffsets[column]
    val zAngle = zAngles[column]

    var result = model
    result = result.move(0.0, 0.0, -kfc.rowRadius)
    result = result.rotate(kfc.rowCurvature * (kfc.centerRow - row), 0.0, 0.0)
    result = result.move(0.0, 0.0, kfc.rowRadius)
    result = result.move(0.0, 0.0, -kfc.columnRadius)
    result = result.rotate(0.0, kfc.columnCurvature * (kfc.centerCol - column), 0.0)
    result = result.move(0.0, 0.0, kfc.columnRadius)
    result = result.move(ko.x, ko.y, ko.z)
    result = result.rotate(0.0, 0.0, zAngle)
    result = result.rotate(0.0, kfc.tentingAngle, 0.0)
    result = result.move(0.0, 0.0, kfc.plateZOffset)

    val colSpacing = kfc.mountWidth
    val rowSpacing = kfc.mountHeight

    result = result.move(column * colSpacing, row * rowSpacing, 0.0)

    return result
}
