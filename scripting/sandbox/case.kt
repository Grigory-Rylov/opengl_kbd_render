// case.kt — функция для генерации корпуса клавиатуры
fun case(): Model {
    val width = 200.0
    val depth = 150.0
    val wallHeight = 20.0
    val wallThick = 3.0
    val baseThick = 2.0

    val base = cube(width, depth, baseThick)
        .move(0.0, 0.0, baseThick / 2.0)

    val top = cube(width, depth, baseThick)
        .move(0.0, 0.0, wallHeight - baseThick / 2.0)

    val front = cube(width - wallThick * 2, wallThick, wallHeight - baseThick * 2)
        .move(0.0, -depth / 2.0 + wallThick / 2.0, wallHeight / 2.0)

    val back = cube(width - wallThick * 2, wallThick, wallHeight - baseThick * 2)
        .move(0.0, depth / 2.0 - wallThick / 2.0, wallHeight / 2.0)

    val left = cube(wallThick, depth - wallThick * 2, wallHeight - baseThick * 2)
        .move(-width / 2.0 + wallThick / 2.0, 0.0, wallHeight / 2.0)

    val right = cube(wallThick, depth - wallThick * 2, wallHeight - baseThick * 2)
        .move(width / 2.0 - wallThick / 2.0, 0.0, wallHeight / 2.0)

    return union(
        base, top, front, back, left, right
    )
}
