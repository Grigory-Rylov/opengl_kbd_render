package com.github.grishberg.cad3d.viewer.dialog

import org.fife.ui.rsyntaxtextarea.Token
import org.fife.ui.rsyntaxtextarea.TokenMaker
import org.fife.ui.rsyntaxtextarea.TokenMakerBase
import org.fife.ui.rsyntaxtextarea.TokenTypes
import javax.swing.text.Segment

/**
 * Self-contained Kotlin token maker. Highlights:
 *  - class / data class / enum class / sealed class names -> DATA_TYPE (blue)
 *  - interface names -> RESERVED_WORD_2 (light green)
 *  - function names (after `fun`) -> FUNCTION (orange)
 * Plus standard comment / string / number / keyword coloring.
 */
class KotlinHighlightTokenMaker : TokenMakerBase(), TokenMaker {

    private val keywords = setOf(
        "val", "var", "fun", "class", "interface", "object", "data", "enum",
        "sealed", "abstract", "final", "open", "override", "private", "public",
        "protected", "internal", "return", "if", "else", "when", "for", "while",
        "do", "try", "catch", "finally", "throw", "import", "package", "this",
        "super", "null", "true", "false", "is", "in", "out", "where", "init",
        "constructor", "by", "get", "set", "companion", "suspend", "inline",
        "extension", "as", "break", "continue", "typeof"
    )

    // Shape-builder functions -> blue (DATA_TYPE)
    private val shapeFns = setOf(
        "cube", "sphere", "cylinder", "prism", "hull", "union", "emptyModel",
        "v3", "angles", "deg", "repeat", "buildMatrixRight"
    )

    // Transform / modifier functions -> light green (RESERVED_WORD_2)
    private val transformFns = setOf(
        "move", "rotate", "withColor", "scale", "rotateX", "rotateY", "rotateZ",
        "mirror", "translate"
    )

    override fun getTokenList(text: Segment, initialTokenType: Int, startOffset: Int): Token {
        try {
        resetTokenList()

        val array = text.array
        val offset = text.offset
        val count = text.count
        val end = offset + count

        var i = offset
        var pending = -1

        while (i < end) {
            val c = array[i]

            // whitespace (does NOT reset pending: class <ws> Name)
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                var j = i + 1
                while (j < end && (array[j] == ' ' || array[j] == '\t' || array[j] == '\n' || array[j] == '\r')) j++
                addToken(array, i, j - 1, TokenTypes.WHITESPACE, startOffset + i - offset)
                i = j
                continue
            }

            // line comment (does NOT reset pending)
            if (c == '/' && i + 1 < end && array[i + 1] == '/') {
                var j = i + 2
                while (j < end && array[j] != '\n' && array[j] != '\r') j++
                addToken(array, i, j - 1, TokenTypes.COMMENT_EOL, startOffset + i - offset)
                i = j
                continue
            }

            // block comment (does NOT reset pending)
            if (c == '/' && i + 1 < end && array[i + 1] == '*') {
                var j = i + 2
                while (j + 1 < end && !(array[j] == '*' && array[j + 1] == '/')) j++
                if (j + 1 < end) j += 2
                addToken(array, i, j - 1, TokenTypes.COMMENT_MULTILINE, startOffset + i - offset)
                i = j
                continue
            }

            // triple-quoted string
            if (c == '"' && i + 2 < end && array[i + 1] == '"' && array[i + 2] == '"') {
                var j = i + 3
                while (j + 2 < end && !(array[j] == '"' && array[j + 1] == '"' && array[j + 2] == '"')) j++
                if (j + 2 < end) j += 3
                addToken(array, i, j - 1, TokenTypes.LITERAL_STRING_DOUBLE_QUOTE, startOffset + i - offset)
                pending = -1
                i = j
                continue
            }

            // string / char
            if (c == '"' || c == '\'') {
                val quote = c
                var j = i + 1
                while (j < end) {
                    if (array[j] == '\\' && j + 1 < end) { j += 2; continue }
                    if (array[j] == quote) { j++; break }
                    j++
                }
                val type = if (quote == '"') TokenTypes.LITERAL_STRING_DOUBLE_QUOTE else TokenTypes.LITERAL_CHAR
                addToken(array, i, j - 1, type, startOffset + i - offset)
                pending = -1
                i = j
                continue
            }

            // annotation
            if (c == '@') {
                var j = i + 1
                while (j < end && isPart(array[j])) j++
                addToken(array, i, j - 1, TokenTypes.ANNOTATION, startOffset + i - offset)
                pending = -1
                i = j
                continue
            }

            // number
            if (c.isDigit()) {
                var j = i + 1
                while (j < end && (array[j].isDigit() || array[j] == '.' || array[j].lowercaseChar() == 'e'
                            || array[j] == 'f' || array[j] == 'L' || array[j] == '_' || array[j] == 'x')) j++
                addToken(array, i, j - 1, TokenTypes.LITERAL_NUMBER_DECIMAL_INT, startOffset + i - offset)
                pending = -1
                i = j
                continue
            }

            // identifier / keyword
            if (isStart(c)) {
                var j = i + 1
                while (j < end && isPart(array[j])) j++
                val word = String(array, i, j - i)
                val type = when {
                    pending != -1 -> {
                        val t = pending
                        pending = -1
                        t
                    }
                    keywords.contains(word) -> {
                        pending = when (word) {
                            "class", "data", "enum", "sealed" -> TokenTypes.DATA_TYPE
                            "interface" -> TokenTypes.RESERVED_WORD_2
                            "fun" -> TokenTypes.FUNCTION
                            else -> -1
                        }
                        TokenTypes.RESERVED_WORD
                    }
                    shapeFns.contains(word) -> TokenTypes.DATA_TYPE
                    transformFns.contains(word) -> TokenTypes.RESERVED_WORD_2
                    else -> TokenTypes.IDENTIFIER
                }
                addToken(array, i, j - 1, type, startOffset + i - offset)
                i = j
                continue
            }

            // operator / separator (single char)
            addToken(array, i, i, TokenTypes.OPERATOR, startOffset + i - offset)
            pending = -1
            i++
        }

        addNullToken()
        return firstToken
        } catch (t: Throwable) {
            t.printStackTrace()
            // Fallback: single identifier token so the text is still shown.
            resetTokenList()
            addToken(text.array, text.offset, text.offset + text.count - 1, TokenTypes.IDENTIFIER, startOffset)
            addNullToken()
            return firstToken
        }
    }

    private fun isStart(c: Char): Boolean = c == '_' || c.isLetter()
    private fun isPart(c: Char): Boolean = c == '_' || c.isLetterOrDigit()
}
