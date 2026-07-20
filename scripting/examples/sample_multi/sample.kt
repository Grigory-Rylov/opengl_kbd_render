// sample.kt — Точка входа проекта

fun scriptMain(): Model {
    val scene = buildScene()
    // color — top-level extension, доступна через ScriptBindings.*
    return scene.color("blue")
}
