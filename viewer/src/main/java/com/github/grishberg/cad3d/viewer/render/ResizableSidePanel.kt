package com.github.grishberg.cad3d.viewer.render

import java.awt.Component
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JSplitPane
import javax.swing.SwingUtilities

/**
 * Горизонтальный [JSplitPane]: слева основной компонент, справа боковая панель
 * с изменяемой мышкой шириной. Ширина читается/сохраняется через [widthProvider]/[onWidthChanged].
 */
class ResizableSidePanel(
    left: Component,
    right: Component,
    private val widthProvider: () -> Int,
    private val onWidthChanged: (Int) -> Unit,
    minLeftWidth: Int = 100,
    minRightWidth: Int = 150,
) : JSplitPane(HORIZONTAL_SPLIT, left, right) {

    private val minRight = minRightWidth

    init {
        isContinuousLayout = true
        resizeWeight = 1.0 // при ресайзе окна растёт левая часть, правая держит ширину
        dividerSize = 8
        left.minimumSize = Dimension(minLeftWidth, 100)
        right.minimumSize = Dimension(minRightWidth, 100)

        // Первичная установка позиции разделителя после появления layout
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                applyWidth()
                removeComponentListener(this)
            }
        })
        addPropertyChangeListener(DIVIDER_LOCATION_PROPERTY) {
            val total = width
            if (total > 0) {
                val rightWidth = total - dividerLocation - dividerSize
                if (rightWidth > 0) {
                    onWidthChanged(rightWidth)
                }
            }
        }
        SwingUtilities.invokeLater { applyWidth() }
    }

    private fun applyWidth() {
        val total = width
        if (total <= 0) return
        val target = widthProvider().coerceIn(minRight, (total - 100).coerceAtLeast(minRight))
        dividerLocation = total - target - dividerSize
    }
}
