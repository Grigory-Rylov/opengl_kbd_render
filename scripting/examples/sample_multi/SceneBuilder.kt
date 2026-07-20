// SceneBuilder.kt
// Функции createBase и createSphere видны из Colors.kt без импортов

fun buildScene(): Model {
    val base = createBase()
    val sphere = createSphere().moveZ(10.0)
    return base.addModel(sphere)
}
