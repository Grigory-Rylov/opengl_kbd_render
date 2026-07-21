// case.kt — корпус для matrix_right, портирован из cad3d (Walls/OuterWallsBuilder)
// Запуск: ./gradlew :scripting:run --args="examples/case.kt /tmp/case.stl"

// === SHARED CONFIG (copy from sandbox/user_script.kt) ===
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

// === KEY PLACEHOLDER CORNERS ===
val KPH_CORNER_OFFSET = 18.5 / 2
val KPH_OW = 18.0 + 2
val KPH_OH = 18.0 + 2
val KPH_WT = 1.5
val KPH_TT = 4.0
val KPH_KPTT = 3.0
val KPH_CPTO = 2.0
val KPH_BTO = 1.5 + 0.5

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

// === CASE WALLS SETTINGS (from cad3d WallsSettings) ===
val bottomBorderHeight = 1.0
val outerVerticalOffset = 10.0
val outerHorizontalOffset = 15.0
val outerBorderZOffset = -6.0
val borderThickness = 1.5
val borderHeight = 4.0
val verticalOffset = 5.0
val leftOffset = 10.0
val rightOffset = 10.0
val borderZOffset = -2.0
val outerLeftOffset = 15.0
val outerRightOffset = 15.0
val topEdgeOffsetZ = 0.0
val isSkeletonMode = false

// === CASE HELPERS ===
fun borderObj(): Model = cylinder(borderThickness, borderHeight)
fun sphereBorder(): Model = sphere(borderThickness / 2.0)
fun bottomCylinder(): Model = cylinder(borderThickness, bottomBorderHeight)
fun List<Model>.merge(): Model {
    if (isEmpty()) return emptyModel()
    var r = this[0]
    for (i in 1 until size) r = r.addModel(this[i])
    return r
}

fun topBorderObj(obj: Model): Model = sphereBorder().move(obj.move)
fun topBorderObj(point: V3d): Model = sphereBorder().move(point)

fun verticalCube(obj: Model): Model = borderObj().moveZ(topEdgeOffsetZ).move(obj.move)

fun bottomPoint(obj: Model): Model = bottomCylinder().moveZ(bottomBorderHeight / 2).move(obj.move)

// === CASE WALLS (simplified from OuterWallsBuilder) ===
fun caseBackWall(col: Int): Model {
    val left = kpPlace(col, 0, cube(borderThickness, borderThickness, borderThickness).move(0.0, outerVerticalOffset, outerBorderZOffset))
    val right = kpPlace(col, 0, cube(borderThickness, borderThickness, borderThickness).move(0.0, outerVerticalOffset, outerBorderZOffset))
    return hull(
        topBorderObj(left), topBorderObj(right),
        verticalCube(kpPlace(col, 0, cube(borderThickness, borderThickness, borderThickness).move(0.0, verticalOffset, borderZOffset))),
        verticalCube(kpPlace(col, 0, cube(borderThickness, borderThickness, borderThickness).move(0.0, verticalOffset, borderZOffset))),
        bottomPoint(left), bottomPoint(right)
    )
}

fun caseLeftWall(row: Int): Model {
    val top = kpPlace(0, row, cube(borderThickness, borderThickness, borderThickness).move(-outerHorizontalOffset, 0.0, outerBorderZOffset))
    val bottom = kpPlace(0, row, cube(borderThickness, borderThickness, borderThickness).move(-outerHorizontalOffset, 0.0, outerBorderZOffset))
    return hull(
        topBorderObj(top), topBorderObj(bottom),
        bottomPoint(top), bottomPoint(bottom)
    )
}

fun caseRightWall(row: Int): Model {
    val top = kpPlace(columnsCount - 1, row, cube(borderThickness, borderThickness, borderThickness).move(outerHorizontalOffset, 0.0, outerBorderZOffset))
    val bottom = kpPlace(columnsCount - 1, row, cube(borderThickness, borderThickness, borderThickness).move(outerHorizontalOffset, 0.0, outerBorderZOffset))
    return hull(
        topBorderObj(top), topBorderObj(bottom),
        bottomPoint(top), bottomPoint(bottom)
    )
}

fun caseFrontWall(col: Int): Model {
    val left = kpPlace(col, rowsCount - 1, cube(borderThickness, borderThickness, borderThickness).move(0.0, -outerVerticalOffset, outerBorderZOffset))
    val right = kpPlace(col, rowsCount - 1, cube(borderThickness, borderThickness, borderThickness).move(0.0, -outerVerticalOffset, outerBorderZOffset))
    return hull(
        topBorderObj(left), topBorderObj(right),
        bottomPoint(left), bottomPoint(right)
    )
}

// === ASSEMBLY ===
fun caseWalls(): Model {
    val m = mutableListOf<Model>()
    
    // Back walls (row 0)
    for (c in 0 until columnsCount) {
        m.add(caseBackWall(c))
    }
    
    // Left wall (col 0)
    for (r in 0 until rowsCount) {
        m.add(caseLeftWall(r))
    }
    
    // Right wall (last col)
    for (r in 0 until rowsCount) {
        m.add(caseRightWall(r))
    }
    
    // Front walls (cols 3..last, last row)
    for (c in 3 until columnsCount) {
        m.add(caseFrontWall(c))
    }
    
    return m.merge()
}

caseWalls()
