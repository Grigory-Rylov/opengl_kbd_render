package com.github.grishberg.cad3d.viewer.dialog

import com.github.grishberg.cad3d.plugin.StlExportListener
import java.awt.BorderLayout
import javax.swing.DefaultListModel
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingUtilities

class StlExportDialog(owner: JFrame) : JDialog(owner, "Экспорт STL", true), StlExportListener {

    private val model = DefaultListModel<String>()
    private val list = JList(model)
    private val statusLabel = JLabel("Ожидание...")
    private val itemIndexByName = HashMap<String, Int>()

    init {
        layout = BorderLayout()
        val panel = JPanel(BorderLayout())
        panel.add(statusLabel, BorderLayout.NORTH)
        panel.add(JScrollPane(list), BorderLayout.CENTER)
        contentPane.add(panel, BorderLayout.CENTER)
        setSize(500, 300)
        setLocationRelativeTo(owner)
    }

    override fun onExportPlan(fileNames: List<String>) {
        SwingUtilities.invokeLater {
            model.clear()
            itemIndexByName.clear()
            for (absPath in fileNames) {
                model.addElement("$fileNames — Ожидание")
                itemIndexByName[absPath] = model.size() - 1
            }
            statusLabel.text = "Запланировано: ${fileNames.size}"
        }
    }

    override fun onExportStart(fileName: String) {
        SwingUtilities.invokeLater {
            val text = "$fileName — Рендеринг"
            val idx = itemIndexByName[fileName]
            if (idx == null) {
                model.addElement(text)
                itemIndexByName[fileName] = model.size() - 1
            } else {
                model.set(idx, text)
            }
            statusLabel.text = "Экспорт: $fileName"
        }
    }

    override fun onExportProgress(fileName: String, stage: String) {
        SwingUtilities.invokeLater {
            val text = "$fileName — $stage"
            val idx = itemIndexByName[fileName]
            if (idx != null) {
                model.set(idx, text)
            }
            statusLabel.text = text
        }
    }

    override fun onExportFinish(fileName: String, success: Boolean, errorMessage: String?) {
        SwingUtilities.invokeLater {
            val status = if (success) "Готово" else "Ошибка"
            val text = buildString {
                append(fileName).append(" — ").append(status)
                if (!success && errorMessage != null) append(" (").append(errorMessage).append(")")
            }
            val idx = itemIndexByName[fileName]
            if (idx == null) {
                model.addElement(text)
                itemIndexByName[fileName] = model.size() - 1
            } else {
                model.set(idx, text)
            }
            statusLabel.text = text
        }
    }

    override fun onAllFinished() {
        SwingUtilities.invokeLater {
            statusLabel.text = "Экспорт завершён"
        }
    }
}


