// Colors.kt — Пример класса-хелпера и функции
// Функции и классы из соседних файлов видны без импортов

fun createBase(): Model = bindings.cube(30.0, 30.0, 5.0).moveZ(2.5)
fun createSphere(): Model = bindings.sphere(10.0)

class ColorHelper {
    fun applyColor(model: Model, colorName: String): Model = model.color(colorName)
}
