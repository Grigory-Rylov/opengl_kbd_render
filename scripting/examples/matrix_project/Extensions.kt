// Extensions.kt — полезные расширения для работы с моделями

fun Model.at(x: Double, y: Double, z: Double = 0.0): Model =
    this.move(x, y, z)

fun Model.atXY(x: Double, y: Double): Model =
    this.move(x, y, 0.0)

fun Model.scaled(s: Double): Model =
    Scale(this, V3d(s, s, s))

fun Model.scaled(x: Double, y: Double, z: Double): Model =
    Scale(this, V3d(x, y, z))

fun Model.rotated(yDeg: Double): Model =
    this.rotate(Angles3d(0.0, bindings.deg(yDeg), 0.0))

fun Model.rotatedY(yDeg: Double): Model =
    this.rotate(Angles3d(0.0, bindings.deg(yDeg), 0.0))

fun Model.rotatedZ(zDeg: Double): Model =
    this.rotate(Angles3d(0.0, 0.0, bindings.deg(zDeg)))

fun Model.beveled(): Model = this

fun List<Model>.merge(): Model {
    var result = bindings.emptyModel()
    for (m in this) result = result.addModel(m)
    return result
}

fun Iterable<Model>.merge(): Model {
    var result = bindings.emptyModel()
    for (m in this) result = result.addModel(m)
    return result
}
