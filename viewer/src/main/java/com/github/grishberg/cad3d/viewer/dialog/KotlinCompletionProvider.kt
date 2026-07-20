package com.github.grishberg.cad3d.viewer.dialog

import org.fife.ui.autocomplete.BasicCompletion
import org.fife.ui.autocomplete.Completion
import org.fife.ui.autocomplete.DefaultCompletionProvider
import org.fife.ui.autocomplete.ShorthandCompletion
import javax.swing.text.JTextComponent

/**
 * Context-aware completion provider for the script editor.
 *
 * Heuristics (no real type inference):
 *  - "Color."                -> color constants
 *  - "." (after identifier)  -> transform/method names (move, rotate, withColor, ...)
 *  - inside a `fun ...(): Model` / after `return `/`= `
 *                             -> shape builders + transforms (return Model)
 *  - otherwise               -> everything (shapes, keywords, snippets)
 */
class KotlinCompletionProvider : DefaultCompletionProvider() {

    private val shapes = mutableListOf<Completion>()
    private val transforms = mutableListOf<Completion>()
    private val colors = mutableListOf<Completion>()
    private val keywords = mutableListOf<Completion>()
    private val snippets = mutableListOf<Completion>()

    init {
        // DSL shape builders (return Model)
        shapes += BasicCompletion(this, "cube", "cube(size: Number): Model")
        shapes += BasicCompletion(this, "sphere", "sphere(radius: Number): Model")
        shapes += BasicCompletion(this, "cylinder", "cylinder(length, radius): Model")
        shapes += BasicCompletion(this, "prism", "prism(length, radius, sides): Model")
        shapes += BasicCompletion(this, "hull", "hull(vararg models): Model")
        shapes += BasicCompletion(this, "union", "union(vararg models): Model")
        shapes += BasicCompletion(this, "emptyModel", "emptyModel(): Model")
        shapes += BasicCompletion(this, "importStl", "importStl(path: String, color: String? = null): Model")
        shapes += BasicCompletion(this, "v3", "v3(x, y, z): V3d")
        shapes += BasicCompletion(this, "angles", "angles(x, y, z): V3d")
        shapes += BasicCompletion(this, "deg", "deg(degrees: Number): Double")
        shapes += BasicCompletion(this, "repeat", "repeat(count) { i -> Model }")

        // DSL transforms / modifiers (member-like)
        transforms += BasicCompletion(this, "move", "move(x, y, z): Model")
        transforms += BasicCompletion(this, "moveX", "moveX(x): Model")
        transforms += BasicCompletion(this, "moveY", "moveY(y): Model")
        transforms += BasicCompletion(this, "moveZ", "moveZ(z): Model")
        transforms += BasicCompletion(this, "rotate", "rotate(x, y, z): Model")
        transforms += BasicCompletion(this, "rotateX", "rotateX(deg): Model")
        transforms += BasicCompletion(this, "rotateY", "rotateY(deg): Model")
        transforms += BasicCompletion(this, "rotateZ", "rotateZ(deg): Model")
        transforms += BasicCompletion(this, "scale", "scale(x, y, z): Model")
        transforms += BasicCompletion(this, "mirror", "mirror(axis): Model")
        transforms += BasicCompletion(this, "withColor", "withColor(color: Color): Model")

        // Color constants
        colors += BasicCompletion(this, "Color", "java.awt.Color")
        colors += BasicCompletion(this, "Color.RED", "Color.RED")
        colors += BasicCompletion(this, "Color.GREEN", "Color.GREEN")
        colors += BasicCompletion(this, "Color.BLUE", "Color.BLUE")
        colors += BasicCompletion(this, "Color.YELLOW", "Color.YELLOW")
        colors += BasicCompletion(this, "Color.WHITE", "Color.WHITE")
        colors += BasicCompletion(this, "Color.BLACK", "Color.BLACK")
        colors += BasicCompletion(this, "Color.CYAN", "Color.CYAN")
        colors += BasicCompletion(this, "Color.MAGENTA", "Color.MAGENTA")
        colors += BasicCompletion(this, "Color.ORANGE", "Color.ORANGE")
        colors += BasicCompletion(this, "Color.PINK", "Color.PINK")

        // Kotlin keywords
        val kw = listOf(
            "fun", "val", "var", "class", "interface", "object", "data", "enum",
            "sealed", "abstract", "open", "override", "private", "public",
            "protected", "internal", "return", "if", "else", "when", "for", "while",
            "do", "try", "catch", "finally", "throw", "import", "package", "this",
            "super", "null", "true", "false", "is", "in", "out", "where", "init",
            "constructor", "by", "get", "set", "companion", "suspend", "inline",
            "as", "break", "continue"
        )
        kw.forEach { keywords += BasicCompletion(this, it) }

        snippets += ShorthandCompletion(this, "fun", "fun ${"$"}{name}() {\n\t${"$"}{cursor}\n}", "fun template")
    }

    override fun getCompletions(textComponent: JTextComponent): List<Completion> {
        val text = textComponent.text
        val caret = textComponent.caretPosition
        val before = if (caret <= text.length) text.substring(0, caret) else text

        // Context just before the caret (current line up to caret).
        val lineStart = before.lastIndexOf('\n') + 1
        val lineBefore = before.substring(lineStart)

        // "Color." -> colors
        if (Regex("""\bColor\.\w*$""").find(before) != null) {
            return filterByPrefix(colors, lineBefore)
        }

        // ".something" -> transforms (member-like methods)
        if (Regex("""\.\s*\w*$""").find(before) != null) {
            return filterByPrefix(transforms, lineBefore)
        }

        // Inside a model-returning function body (after return /= /fun ...(): Model)
        val insideModelFn = Regex("""fun\s+[\w<>():,\s]*Model\s*\{[^}]*$""").find(before) != null
        val afterReturnOrAssign = Regex("""(return|=)\s*[^=]*$""").find(before) != null
        if (insideModelFn || afterReturnOrAssign) {
            val base = shapes + transforms
            return filterByPrefix(base, lineBefore)
        }

        // Default: everything
        val all = shapes + transforms + colors + keywords + snippets
        return filterByPrefix(all, lineBefore)
    }

    private fun filterByPrefix(items: List<Completion>, lineBefore: String): List<Completion> {
        val m = Regex("""(\w*)$""").find(lineBefore)
        val prefix = m?.groupValues?.get(1) ?: ""
        if (prefix.isEmpty()) return items
        return items.filter { it.getInputText().startsWith(prefix, ignoreCase = true) }
    }
}
