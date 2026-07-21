// case.kt — функция для генерации корпуса клавиатуры

fun case(): Model {
    val base = cube(180.0, 140.0, 10.0)
        .move(0.0, 0.0, 5.0)
        .withColor(Color.GRAY)

    val lip = cube(180.0, 140.0, 6.0)
        .move(0.0, 0.0, 4.0)
        .withColor(Color.GRAY)

    val backWall = cube(180.0, 12.0, 24.0)
        .move(0.0, -64.0, 12.0)
        .withColor(Color.GRAY)

    val corners = listOf(
        cube(8.0, 8.0, 20.0).move(-86.0, -66.0, 10.0),
        cube(8.0, 8.0, 20.0).move(86.0, -66.0, 10.0),
        cube(8.0, 8.0, 20.0).move(-86.0, 66.0, 10.0),
        cube(8.0, 8.0, 20.0).move(86.0, 66.0, 10.0)
    ).map { it.withColor(Color.GRAY) }

    return union(listOf(base, lip, backWall) + corners)
}
