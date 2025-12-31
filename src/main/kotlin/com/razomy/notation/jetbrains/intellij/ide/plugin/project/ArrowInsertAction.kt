package com.razomy.notation.jetbrains.intellij.ide.plugin.project

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor

abstract class BaseSymbolAction(private val symbol: String, name: String?) : AnAction(name) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getProject()
        val editor = e.getData<Editor?>(CommonDataKeys.EDITOR)
        if (project == null || editor == null) return

        WriteCommandAction.runWriteCommandAction(project, Runnable {
            val offset = editor.getCaretModel().getOffset()
            editor.getDocument().insertString(offset, symbol)
            editor.getCaretModel().moveToOffset(offset + 1)
        })
    }
}

// 2. The Specific Actions (Register these classes in XML)
class ArrowInsertAction {
    class Delta : BaseSymbolAction("Δ", "Insert Delta")
    class Left : BaseSymbolAction("←", "Insert Left Arrow")
    class Up : BaseSymbolAction("↑", "Insert Up Arrow")
    class Right : BaseSymbolAction("→", "Insert Right Arrow")
    class Down : BaseSymbolAction("↓", "Insert Down Arrow")

    class Horizontal : BaseSymbolAction("↔", "Insert Horizontal Arrow")
    class Vertical : BaseSymbolAction("↕", "Insert Vertical Arrow")

    class NorthWest : BaseSymbolAction("↖", "Insert NW Arrow")
    class NorthEast : BaseSymbolAction("↗", "Insert NE Arrow")
    class SouthEast : BaseSymbolAction("↘", "Insert SE Arrow")
    class SouthWest : BaseSymbolAction("↙", "Insert SW Arrow")
}