package com.razomy.notation.jetbrains.intellij.ide.plugin.ai

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.JLabel
import javax.swing.JPanel

class InlineAiAction : AnAction() {

    private val templates = listOf(
        "Refactor this code",
        "Find bugs and fix them",
        "Add doc comments",
        "Optimize performance",
        "Explain this code"
    )

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val selectedText = editor.selectionModel.selectedText ?: return

        showInputPopup(project, editor, selectedText)
    }

    private fun showInputPopup(project: Project, editor: Editor, selectedCode: String) {
        // Жестко собираем панель
        val mainPanel = JPanel(BorderLayout(0, 5))
        mainPanel.border = JBUI.Borders.empty(5)
        mainPanel.preferredSize = Dimension(400, 200)

        // Верх: Label + TextField
        val topPanel = JPanel(BorderLayout(5, 0))
        val promptLabel = JLabel("AI Prompt:")
        val textField = JBTextField()
        topPanel.add(promptLabel, BorderLayout.WEST)
        topPanel.add(textField, BorderLayout.CENTER)

        // Низ: Список шаблонов
        val listModel = DefaultListModel<String>().apply { addAll(templates) }
        val templateList = JBList(listModel)
        val scrollPane = JBScrollPane(templateList)

        mainPanel.add(topPanel, BorderLayout.NORTH)
        mainPanel.add(scrollPane, BorderLayout.CENTER)

        // Создаем попап, явно указывая, что textField должен получить фокус
        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(mainPanel, textField)
            .setTitle("Ask Local AI")
            .setRequestFocus(true)
            .setMovable(true)
            .setCancelOnClickOutside(true)
            .createPopup()

        fun submitPrompt(prompt: String) {
            if (prompt.isNotBlank()) {
                popup.cancel()
                processAiRequest(project, editor, selectedCode, prompt)
            }
        }

        textField.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    val text = textField.text
                    if (text.isNotBlank()) submitPrompt(text)
                    else templateList.selectedValue?.let { submitPrompt(it) }
                } else if (e.keyCode == KeyEvent.VK_DOWN) {
                    e.consume()
                    if (templateList.selectedIndex < listModel.size() - 1) {
                        templateList.selectedIndex++
                        templateList.ensureIndexIsVisible(templateList.selectedIndex)
                    }
                } else if (e.keyCode == KeyEvent.VK_UP) {
                    e.consume()
                    if (templateList.selectedIndex > 0) {
                        templateList.selectedIndex--
                        templateList.ensureIndexIsVisible(templateList.selectedIndex)
                    }
                }
            }
        })

        templateList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 1 || e.clickCount == 2) {
                    templateList.selectedValue?.let { submitPrompt(it) }
                }
            }
        })

        // Показываем под кареткой
        popup.showInBestPositionFor(editor)
    }
    override fun update(e: AnActionEvent) {
        // Действие активно только если открыт редактор кода
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = editor != null
    }
    private fun processAiRequest(project: Project, editor: Editor, oldCode: String, prompt: String) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val responseMessages = AiApiClient.sendRequest(prompt, oldCode)
            val lastMessage = responseMessages.lastOrNull() ?: return@executeOnPooledThread

            val regex = Regex("<newcode>(.*?)</newcode>", RegexOption.DOT_MATCHES_ALL)
            val matchResult = regex.find(lastMessage)

            if (matchResult != null) {
                val newCode = matchResult.groupValues[1].trim()
                ApplicationManager.getApplication().invokeLater {
                    WriteCommandAction.runWriteCommandAction(project) {
                        val document = editor.document
                        val selectionModel = editor.selectionModel
                        document.replaceString(selectionModel.selectionStart, selectionModel.selectionEnd, newCode)
                        selectionModel.removeSelection()
                    }
                }
            }
        }
    }
}