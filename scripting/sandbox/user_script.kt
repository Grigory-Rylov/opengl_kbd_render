// matrix_right — полный порт из cad3d, только примитивы
// Все классы cad3d перенесены в скрипт, используются только cube/cylinder/hull/union/subtractModel

fun List<Model>.merge(): Model {
    if (isEmpty()) return emptyModel()
    var r = this[0]
    for (i in 1 until size) r = r.addModel(this[i])
    return r
}

fun Iterable<Model>.merge(): Model {
    var r = emptyModel()
    for (m in this) r = r.addModel(m)
    return r
}

fun hull(vararg models: Model): Model {
    return com.github.grishberg.javascad.tranzitions.Hull(models.toList())
}

// === CONFIG ===
val plateZOffset = 8.0
val rowCurvature = 20.1
val tentingAngle = 8.0
val columnCurvature = 12.1
val keyswitchHeight = 18.0
val keyswitchWidth = 18.0
val extraWidth = 2.5
val extraHeight = 1.0
val keyPlaceHolderWidth = 15.7
val keyPlaceHolderDepth = 15.7
val keyPlaceHolderHeight = 4.0
val horizontalExtraSpace = 1.0
val verticalExtraSpace = 1.0
val plateThickness = 2.0
val saProfileKeyHeight = 4.5
val columnsCount = 6
val rowsCount = 3
val centerCol = 2
val centerRow = 1
val isLowProfile = true
val thumbXOffset = 0.0
val thumbYOffset = -50.0
val thumbZOffset = 37.0
val thumbRotateY = -45.0
val thumbRotateZ = 18.0
val thumbArcRadiusZ = 0.0
val thumbArcRadiusY = -80.0
val spaceBetweenKey = 6.5
val thumbButtonsCount = 3
val screwNutHoleDiameter = 4.0
val screwHolderWallhickness = 1.6
val capTopHeight = plateThickness + saProfileKeyHeight
val mountWidth = keyswitchWidth + horizontalExtraSpace
val mountHeight = keyswitchWidth + verticalExtraSpace
val kpColumnRadius = ((mountWidth + extraHeight) / 2.0) / sin(Math.toRadians(columnCurvature) / 2.0) + capTopHeight
val kpRowRadius = ((mountHeight + extraWidth) / 2.0) / sin(Math.toRadians(rowCurvature) / 2.0) + capTopHeight

// === KEY PLACE ===
fun kpCalcYAngle(col: Int) = columnCurvature * (centerCol - col)
fun kpCalcXAngle(row: Int) = rowCurvature * (centerRow - row)

fun kpColumnOffset(col: Int): V3d {
    return when (col) {
        0 -> V3d(-6.0, -7.8, 3.0)
        1 -> V3d(-2.0, -5.8, 3.0)
        2 -> V3d(1.5, 2.82, -3.5)
        3 -> V3d(6.0, -2.0, 0.0)
        4 -> V3d(9.5, -15.0, 5.64)
        5 -> V3d(14.0, -20.0, 5.64)
        else -> V3d(0.0, -2.0, 0.0)
    }
}

fun kpZAngle(col: Int): Double {
    return when (col) {
        0 -> 4.0
        1 -> 2.0
        3 -> -7.0
        4 -> -13.0
        5 -> -15.0
        else -> 0.0
    }
}

fun kpPlace(col: Int, row: Int, obj: Model): Model {
    val co = kpColumnOffset(col)
    return obj.move(0, 0, -kpRowRadius).rotate(Angles3d.xOnly(kpCalcXAngle(row))).move(0, 0, kpRowRadius).move(0, 0, -kpColumnRadius).rotate(Angles3d.yOnly(kpCalcYAngle(col))).move(0, 0, kpColumnRadius).move(co.x, co.y, co.z).rotate(Angles3d.zOnly(kpZAngle(col))).rotate(Angles3d.yOnly(tentingAngle)).move(0, 0, plateZOffset)
}

fun kpCoords(col: Int, row: Int, init: V3d = V3d(0.0, 0.0, 0.0)): V3d {
    var x = init.x; var y = init.y; var z = init.z
    val xRad = Math.toRadians(kpCalcXAngle(row))
    val yRad = Math.toRadians(kpCalcYAngle(col))
    val tRad = Math.toRadians(tentingAngle)
    z -= kpRowRadius
    y = y * cos(xRad) - z * sin(xRad); z = y * sin(xRad) + z * cos(xRad)
    z += kpRowRadius; z -= kpColumnRadius
    x = x * cos(yRad) + z * sin(yRad); z = -x * sin(yRad) + z * cos(yRad)
    z += kpColumnRadius
    x = x * cos(tRad) + z * sin(tRad); z = -x * sin(tRad) + z * cos(tRad)
    z += plateZOffset
    return V3d(x, y, z)
}

// === KEY PLACEHOLDER ===
val KPH_CORNER_OFFSET = 18.5 / 2
val KPH_OW = 18.0 + 2; val KPH_OH = 18.0 + 2
val KPH_KH_OW = 14.7; val KPH_KH_IW = 14.0; val KPH_KH_H = 14.0
val KPH_WT = 1.5; val KPH_TT = 4.0; val KPH_KPTT = 3.0
val KPH_CPTO = 2.0; val KPH_BTO = 1.5 + 0.5; val KPH_EH = 1.2
val KPH_VTO = KPH_BTO - 3.2 / 2 + 2.6 / 2

fun kph(): Model { return cube(KPH_OW, KPH_OH, KPH_KPTT).move(0.0, 0.0, KPH_BTO).subtractModel(cube(KPH_KH_IW, KPH_KH_H, 10.0)).subtractModel(cube(KPH_KH_OW, KPH_KH_H, KPH_KPTT).move(0.0, 0.0, KPH_VTO - KPH_EH)).subtractModel(cube(KPH_KH_IW, KPH_KH_H, 2.0).move(0.0, KPH_CORNER_OFFSET, KPH_VTO - 2)).subtractModel(cube(5.0, 15.0, 1.0).moveZ(0.7 + 1.8 - 1.3 + KPH_EH)) }

fun kphBack(t: Double = KPH_WT): Model { return cube(KPH_OW, t, KPH_TT).move(0.0, KPH_CORNER_OFFSET + t, KPH_CPTO) }
fun kphLeft(): Model { return cube(KPH_WT, KPH_OH, KPH_TT).move(-KPH_CORNER_OFFSET - KPH_WT, 0.0, KPH_CPTO) }
fun kphRight(): Model { return cube(KPH_WT, KPH_OH, KPH_TT).move(KPH_CORNER_OFFSET + KPH_WT, 0.0, KPH_CPTO) }
fun kphFront(): Model { return cube(KPH_OW, KPH_WT, KPH_TT).move(0.0, -KPH_CORNER_OFFSET - KPH_WT, KPH_CPTO) }
fun kphBL(): Model { return cube(KPH_WT, KPH_WT, KPH_TT).move(-KPH_CORNER_OFFSET - KPH_WT, KPH_CORNER_OFFSET + KPH_WT, KPH_CPTO) }
fun kphBR(): Model { return cube(KPH_WT, KPH_WT, KPH_TT).move(KPH_CORNER_OFFSET + KPH_WT, KPH_CORNER_OFFSET + KPH_WT, KPH_CPTO) }
fun kphFL(): Model { return cube(KPH_WT, KPH_WT, KPH_TT).move(-KPH_CORNER_OFFSET - KPH_WT, -KPH_CORNER_OFFSET - KPH_WT, KPH_CPTO) }
fun kphFR(): Model { return cube(KPH_WT, KPH_WT, KPH_TT).move(KPH_CORNER_OFFSET + KPH_WT, -KPH_CORNER_OFFSET - KPH_WT, KPH_CPTO) }

// === THUMB KEY PLACE ===
val thumbCoords = run {
    val l = mutableListOf<V3d>()
    var o = 0.0
    for (i in 0 until thumbButtonsCount) { l.add(0, V3d(o, 0.0, 0.0)); o -= (spaceBetweenKey + keyPlaceHolderWidth) }
    l
}
class TArc(val az: Double, val ay: Double, val o: V3d)
fun tArc(p: V3d, ry: Double = thumbArcRadiusY, rz: Double = thumbArcRadiusZ): TArc {
    if (rz == 0.0 && ry == 0.0) return TArc(0.0, 0.0, p)
    val x = p.x
    val tz = if (rz == 0.0) 0.0 else x / rz
    val ty = if (ry == 0.0) 0.0 else x / ry
    val nxz = if (rz == 0.0) x else rz * Math.sin(tz)
    val nxy = if (ry == 0.0) x else ry * Math.sin(ty)
    val ny = if (rz == 0.0) p.y else rz - rz * Math.cos(tz)
    val nz = if (ry == 0.0) p.z else ry * Math.cos(ty) - ry
    return TArc(Math.toDegrees(tz), Math.toDegrees(ty), V3d((x + nxy + nxz) / 3, ny, nz))
}
fun tPlace(obj: Model): Model = tR(obj).addModel(tM(obj)).addModel(tL(obj))
fun tR(obj: Model): Model { val a = tArc(thumbCoords[2]); return tSingle(obj, a) }
fun tM(obj: Model): Model { val a = tArc(thumbCoords[1]); return tSingle(obj, a) }
fun tL(obj: Model): Model { val a = tArc(thumbCoords[0]); return tSingle(obj, a) }
fun tSingle(obj: Model, a: TArc): Model =
    obj.rotate(0.0, a.ay, a.az).move(a.o).rotate(0.0, thumbRotateY, thumbRotateZ).move(thumbXOffset, thumbYOffset, thumbZOffset + plateZOffset)

// === CONNECTIONS ===
fun matrixConnections(): Model {
    val m = mutableListOf<Model>()
    for (c in 0 until columnsCount - 1) for (r in 0 until rowsCount) m.add(hull(kpPlace(c, r, kphRight()), kpPlace(c + 1, r, kphLeft())))
    for (c in 0 until columnsCount) for (r in 0 until rowsCount - 1) m.add(hull(kpPlace(c, r, kphFront()), kpPlace(c, r + 1, kphBack())))
    for (c in 0 until columnsCount - 1) for (r in 0 until rowsCount - 1) m.add(hull(kpPlace(c, r, kphFR()), kpPlace(c + 1, r, kphFL()), kpPlace(c + 1, r + 1, kphBL()), kpPlace(c, r + 1, kphBR())))
    return m.merge()
}
fun thumbConnections(): Model {
    return listOf(hull(tR(kphLeft()), tM(kphRight())), hull(tM(kphLeft()), tL(kphRight()))).merge()
}

// === SCREWS ===
fun screwHole(): Model { return cylinder(10.0, 3.1 / 2.0).moveZ(-10.0 / 2 + 2.0 / 2).addModel(cylinder(2.0, 5.0 / 2.0).moveZ(2.0 / 2)) }
fun matrixScrews(): Model {
    val shOff = (screwHolderWallhickness * 2 + screwNutHoleDiameter) / 2
    val slOff = -8.0 + shOff - 1; val srOff = 8.0 - shOff + 1
    val h = screwHole()
    val m = mutableListOf<Model>()
    m.add(kpPlace(0, 0, kphLeft().moveX(slOff)))
    m.add(kpPlace(0, rowsCount - 1, kphLeft().moveX(slOff)))
    m.add(kpPlace(columnsCount - 1, 0, kphRight().rotate(Angles3d.zOnly(180.0)).moveX(srOff)))
    m.add(kpPlace(columnsCount - 1, rowsCount - 1, kphRight().rotate(Angles3d.zOnly(180.0)).moveX(srOff)))
    m.add(tL(kphLeft().moveX(slOff)))
    return m.merge()
}

// === BORDERS ===
fun matrixBorders(bt: Double = 1.5, bh: Double = 2.5): Model {
    val m = mutableListOf<Model>()
    for (c in 0 until columnsCount) m.add(kpPlace(c, 0, kphBack(bt)).moveZ(bh))
    for (c in 3 until columnsCount) m.add(kpPlace(c, rowsCount - 1, kphFront()).moveZ(bh))
    for (r in 0 until rowsCount) { m.add(kpPlace(0, r, kphLeft()).moveZ(bh)); m.add(kpPlace(columnsCount - 1, r, kphRight()).moveZ(bh)) }
    return m.merge()
}
fun thumbBorders(bt: Double = 1.5, bh: Double = 2.5): Model {
    val m = mutableListOf<Model>()
    m.add(tL(kphBack(bt)).moveZ(bh)); m.add(tR(kphBack(bt)).moveZ(bh)); m.add(tM(kphBack(bt)).moveZ(bh))
    m.add(tR(kphFront()).moveZ(bh)); m.add(tM(kphFront()).moveZ(bh)); m.add(tL(kphFront()).moveZ(bh))
    m.add(tL(kphLeft()).moveZ(bh)); m.add(tR(kphRight()).moveZ(bh))
    return m.merge()
}

// === THUMB-WALL CONNECTOR ===
fun thumbMatrixWall(): Model { return hull(tR(kphBR().move(0.0, 2.0, -2.0)), kpPlace(3, rowsCount - 1, kphFL())).addModel(hull(tR(kphBR().move(0.0, 2.0, -2.0)), kpPlace(4, rowsCount - 1, kphFL()))) }

// === ASSEMBLY ===
val placeholders = (0 until columnsCount).flatMap { c -> (0 until rowsCount).map { r -> kpPlace(c, r, kph()) } }.merge().addModel(tPlace(kph()))
val connections = matrixConnections().addModel(thumbConnections())
val borders = matrixBorders().addModel(thumbBorders())
val thumbWalls = thumbMatrixWall()
val screws = screwHole()

placeholders.addModel(connections).addModel(borders).addModel(thumbWalls).subtractModel(matrixScrews())
