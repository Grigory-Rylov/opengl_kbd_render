// Extensions.kt — полезные расширения для работы с моделями

fun Abstract3dModel.at(x: Double, y: Double, z: Double = 0.0): Abstract3dModel =
    this.move(x, y, z)

fun Abstract3dModel.atXY(x: Double, y: Double): Abstract3dModel =
    this.move(x, y, 0.0)

fun Abstract3dModel.scaled(s: Double): Abstract3dModel =
    Scale(this, V3d(s, s, s))

fun Abstract3dModel.scaled(x: Double, y: Double, z: Double): Abstract3dModel =
    Scale(this, V3d(x, y, z))

fun Abstract3dModel.rotated(yDeg: Double): Abstract3dModel =
    this.rotate(Angles3d(0.0, bindings.deg(yDeg), 0.0))

fun Abstract3dModel.rotatedY(yDeg: Double): Abstract3dModel =
    this.rotate(Angles3d(0.0, bindings.deg(yDeg), 0.0))

fun Abstract3dModel.rotatedZ(zDeg: Double): Abstract3dModel =
    this.rotate(Angles3d(0.0, 0.0, bindings.deg(zDeg)))

fun Abstract3dModel.beveled(): Abstract3dModel = this

fun List<Abstract3dModel>.merge(): Abstract3dModel {
    var result = bindings.emptyModel()
    for (m in this) result = result.addModel(m)
    return result
}

fun Iterable<Abstract3dModel>.merge(): Abstract3dModel {
    var result = bindings.emptyModel()
    for (m in this) result = result.addModel(m)
    return result
}
