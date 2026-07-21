// case.kt — корпус для matrix_right, портирован из cad3d (Walls/OuterWallsBuilder)
// Запуск: ./gradlew :scripting:run --args="examples/case.kt /tmp/case.stl"

// === SHARED CONFIG ===
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

// === KEY CORNER HELPERS (from KeyPlaceholder) ===
val KPH_CORNER_OFFSET = 18.5 / 2
val KPH_WT = 1.5
val KPH_CPTO = 2.0

fun phBL(): Model = cube(KPH_WT, KPH_WT, KPH_WT).move(-KPH_CORNER_OFFSET - KPH_WT, KPH_CORNER_OFFSET + KPH_WT, KPH_CPTO)
fun phBR(): Model = cube(KPH_WT, KPH_WT, KPH_WT).move(KPH_CORNER_OFFSET + KPH_WT, KPH_CORNER_OFFSET + KPH_WT, KPH_CPTO)
fun phFL(): Model = cube(KPH_WT, KPH_WT, KPH_WT).move(-KPH_CORNER_OFFSET - KPH_WT, -KPH_CORNER_OFFSET - KPH_WT, KPH_CPTO)
fun phFR(): Model = cube(KPH_WT, KPH_WT, KPH_WT).move(KPH_CORNER_OFFSET + KPH_WT, -KPH_CORNER_OFFSET - KPH_WT, KPH_CPTO)
fun phBack(): Model = cube(KPH_WT, KPH_WT, KPH_WT).move(0.0, KPH_CORNER_OFFSET + KPH_WT, KPH_CPTO)
fun phFront(): Model = cube(KPH_WT, KPH_WT, KPH_WT).move(0.0, -KPH_CORNER_OFFSET - KPH_WT, KPH_CPTO)
fun phLeft(): Model = cube(KPH_WT, KPH_WT, KPH_WT).move(-KPH_CORNER_OFFSET - KPH_WT, 0.0, KPH_CPTO)
fun phRight(): Model = cube(KPH_WT, KPH_WT, KPH_WT).move(KPH_CORNER_OFFSET + KPH_WT, 0.0, KPH_CPTO)

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

// === CASE HELPERS ===
fun List<Model>.merge(): Model {
    if (isEmpty()) return emptyModel()
    var r = this[0]
    for (i in 1 until size) r = r.addModel(this[i])
    return r
}

fun sphereBorder(): Model = sphere(borderThickness / 2.0)
fun borderObj(): Model = cylinder(borderThickness, borderHeight)
fun bottomCyl(): Model = cylinder(borderThickness, bottomBorderHeight)

fun topBorder(obj: Model): Model = sphereBorder().move(obj.move)
fun vertCube(obj: Model): Model = borderObj().moveZ(topEdgeOffsetZ).move(obj.move)

// Default bottom edge patcher: projects point to Z=bottomBorderHeight/2
fun bottomPoint(obj: Model): Model {
    val p = obj.move
    return bottomCyl().move(V3d(p.x, p.y, bottomBorderHeight / 2))
}

// === WALL BUILDERS (from cad3d OuterWallsBuilder) ===

// Back wall for a single column (row 0)
fun wallBack(col: Int): Model {
    val left = kpPlace(col, 0, phBL().move(0.0, outerVerticalOffset, outerBorderZOffset))
    val right = kpPlace(col, 0, phBR().move(0.0, outerVerticalOffset, outerBorderZOffset))

    val border = hull(
        topBorder(left), topBorder(right),
        vertCube(kpPlace(col, 0, phBL().move(0.0, verticalOffset, borderZOffset))),
        vertCube(kpPlace(col, 0, phBR().move(0.0, verticalOffset, borderZOffset)))
    )
    val wall = hull(
        topBorder(left), topBorder(right),
        bottomPoint(left), bottomPoint(right)
    )
    return border.addModel(wall)
}

// Left wall for a single row (col 0)
fun wallLeft(row: Int): Model {
    val back = kpPlace(0, row, phBL().move(-outerHorizontalOffset, 0.0, outerBorderZOffset))
    val front = kpPlace(0, row, phFL().move(-outerHorizontalOffset, 0.0, outerBorderZOffset))

    val border = hull(
        topBorder(back), topBorder(front),
        vertCube(kpPlace(0, row, phBL().move(-leftOffset, 0.0, borderZOffset))),
        vertCube(kpPlace(0, row, phFL().move(-leftOffset, 0.0, borderZOffset)))
    )
    val wall = hull(
        topBorder(back), topBorder(front),
        bottomPoint(back), bottomPoint(front)
    )
    return border.addModel(wall)
}

// Right wall for a single row (last col)
fun wallRight(row: Int): Model {
    val back = kpPlace(columnsCount - 1, row, phBR().move(outerHorizontalOffset, 0.0, outerBorderZOffset))
    val front = kpPlace(columnsCount - 1, row, phFR().move(outerHorizontalOffset, 0.0, outerBorderZOffset))

    val border = hull(
        topBorder(back), topBorder(front),
        vertCube(kpPlace(columnsCount - 1, row, phBR().move(rightOffset, 0.0, borderZOffset))),
        vertCube(kpPlace(columnsCount - 1, row, phFR().move(rightOffset, 0.0, borderZOffset)))
    )
    val wall = hull(
        topBorder(back), topBorder(front),
        bottomPoint(back), bottomPoint(front)
    )
    return border.addModel(wall)
}

// Front wall for a single column (last row)
fun wallFront(col: Int): Model {
    val left = kpPlace(col, rowsCount - 1, phFL().move(0.0, -outerVerticalOffset, outerBorderZOffset))
    val right = kpPlace(col, rowsCount - 1, phFR().move(0.0, -outerVerticalOffset, outerBorderZOffset))

    val border = hull(
        topBorder(left), topBorder(right),
        vertCube(kpPlace(col, rowsCount - 1, phFL().move(0.0, -verticalOffset, borderZOffset))),
        vertCube(kpPlace(col, rowsCount - 1, phFR().move(0.0, -verticalOffset, borderZOffset)))
    )
    val wall = hull(
        topBorder(left), topBorder(right),
        bottomPoint(left), bottomPoint(right)
    )
    return border.addModel(wall)
}

// Mid wall between two keys (back edge)
fun wallBackMid(leftCol: Int, rightCol: Int): Model {
    val left = kpPlace(leftCol, 0, phBR().move(0.0, outerVerticalOffset, outerBorderZOffset))
    val right = kpPlace(rightCol, 0, phBL().move(0.0, outerVerticalOffset, outerBorderZOffset))

    val border = hull(
        topBorder(left), topBorder(right),
        vertCube(kpPlace(leftCol, 0, phBR().move(0.0, verticalOffset, borderZOffset))),
        vertCube(kpPlace(rightCol, 0, phBL().move(0.0, verticalOffset, borderZOffset)))
    )
    val wall = hull(
        topBorder(left), topBorder(right),
        bottomPoint(left), bottomPoint(right)
    )
    return border.addModel(wall)
}

// Mid wall between two keys (left edge, vertical)
fun wallLeftMid(topRow: Int, bottomRow: Int): Model {
    val top = kpPlace(0, topRow, phFL().move(-outerHorizontalOffset, 0.0, outerBorderZOffset))
    val bottom = kpPlace(0, bottomRow, phBL().move(-outerHorizontalOffset, 0.0, outerBorderZOffset))

    val border = hull(
        topBorder(top), topBorder(bottom),
        vertCube(kpPlace(0, topRow, phFL().move(-leftOffset, 0.0, borderZOffset))),
        vertCube(kpPlace(0, bottomRow, phBL().move(-leftOffset, 0.0, borderZOffset)))
    )
    val wall = hull(
        topBorder(top), topBorder(bottom),
        bottomPoint(top), bottomPoint(bottom)
    )
    return border.addModel(wall)
}

// Mid wall between two keys (right edge, vertical)
fun wallRightMid(topRow: Int, bottomRow: Int): Model {
    val top = kpPlace(columnsCount - 1, topRow, phFR().move(outerHorizontalOffset, 0.0, outerBorderZOffset))
    val bottom = kpPlace(columnsCount - 1, bottomRow, phBR().move(outerHorizontalOffset, 0.0, outerBorderZOffset))

    val border = hull(
        topBorder(top), topBorder(bottom),
        vertCube(kpPlace(columnsCount - 1, topRow, phFR().move(rightOffset, 0.0, borderZOffset))),
        vertCube(kpPlace(columnsCount - 1, bottomRow, phBR().move(rightOffset, 0.0, borderZOffset)))
    )
    val wall = hull(
        topBorder(top), topBorder(bottom),
        bottomPoint(top), bottomPoint(bottom)
    )
    return border.addModel(wall)
}

// Mid wall between two keys (front edge)
fun wallFrontMid(leftCol: Int, rightCol: Int): Model {
    val left = kpPlace(leftCol, rowsCount - 1, phFR().move(0.0, -outerVerticalOffset, outerBorderZOffset))
    val right = kpPlace(rightCol, rowsCount - 1, phFL().move(0.0, -outerVerticalOffset, outerBorderZOffset))

    val border = hull(
        topBorder(left), topBorder(right),
        vertCube(kpPlace(leftCol, rowsCount - 1, phFR().move(0.0, -verticalOffset, borderZOffset))),
        vertCube(kpPlace(rightCol, rowsCount - 1, phFL().move(0.0, -verticalOffset, borderZOffset)))
    )
    val wall = hull(
        topBorder(left), topBorder(right),
        bottomPoint(left), bottomPoint(right)
    )
    return border.addModel(wall)
}

// === THUMB WALLS (simplified) ===
fun wallThumbBack(side: Int): Model {
    val ph = phBack()
    val topM = when (side) { 0 -> tL(ph.move(0.0, outerVerticalOffset, outerBorderZOffset)) 1 -> tM(ph.move(0.0, outerVerticalOffset, outerBorderZOffset)) else -> tR(ph.move(0.0, outerVerticalOffset, outerBorderZOffset)) }
    val vertM = when (side) { 0 -> tL(ph.move(0.0, verticalOffset, borderZOffset)) 1 -> tM(ph.move(0.0, verticalOffset, borderZOffset)) else -> tR(ph.move(0.0, verticalOffset, borderZOffset)) }
    return hull(topBorder(topM), vertCube(vertM), bottomPoint(topM))
}

fun wallThumbLeft(): Model {
    val ph = phLeft()
    val top = tL(ph.move(-outerHorizontalOffset, 0.0, outerBorderZOffset))
    return hull(topBorder(top), vertCube(tL(ph.move(-leftOffset, 0.0, borderZOffset))), bottomPoint(top))
}

fun wallThumbRight(): Model {
    val ph = phRight()
    val top = tR(ph.move(outerHorizontalOffset, 0.0, outerBorderZOffset))
    return hull(topBorder(top), vertCube(tR(ph.move(rightOffset, 0.0, borderZOffset))), bottomPoint(top))
}

fun wallThumbFront(side: Int): Model {
    val ph = phFront()
    val topM = when (side) { 0 -> tL(ph.move(0.0, -outerVerticalOffset, outerBorderZOffset)) 1 -> tM(ph.move(0.0, -outerVerticalOffset, outerBorderZOffset)) else -> tR(ph.move(0.0, -outerVerticalOffset, outerBorderZOffset)) }
    val vertM = when (side) { 0 -> tL(ph.move(0.0, -verticalOffset, borderZOffset)) 1 -> tM(ph.move(0.0, -verticalOffset, borderZOffset)) else -> tR(ph.move(0.0, -verticalOffset, borderZOffset)) }
    return hull(topBorder(topM), vertCube(vertM), bottomPoint(topM))
}

// === ASSEMBLY ===
fun caseFull(): Model {
    val m = mutableListOf<Model>()

    // Back walls
    for (c in 0 until columnsCount) m.add(wallBack(c))

    // Left walls
    for (r in 0 until rowsCount) m.add(wallLeft(r))

    // Right walls
    for (r in 0 until rowsCount) m.add(wallRight(r))

    // Front walls (cols 3+)
    for (c in 3 until columnsCount) m.add(wallFront(c))

    // Back mid walls
    for (c in 0 until columnsCount - 1) m.add(wallBackMid(c, c + 1))

    // Left mid walls
    for (r in 0 until rowsCount - 1) m.add(wallLeftMid(r, r + 1))

    // Right mid walls
    for (r in 0 until rowsCount - 1) m.add(wallRightMid(r, r + 1))

    // Front mid walls
    for (c in 3 until columnsCount - 1) m.add(wallFrontMid(c, c + 1))

    // Thumb walls
    for (s in 0 until thumbButtonsCount) m.add(wallThumbBack(s))
    for (s in 0 until thumbButtonsCount) m.add(wallThumbFront(s))
    m.add(wallThumbLeft())
    m.add(wallThumbRight())

    return m.merge()
}

caseFull()
