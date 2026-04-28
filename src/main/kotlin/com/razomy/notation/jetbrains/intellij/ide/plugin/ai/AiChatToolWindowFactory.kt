package com.razomy.notation.jetbrains.intellij.ide.plugin.ai

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.ColorUtil
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*

class AiChatToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val mainPanel = JPanel(BorderLayout())
        val toolbarPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5))

        val chatSelector = ComboBox<String>()
        val cardLayout = CardLayout()
        val cardsPanel = JPanel(cardLayout)

        var chatCounter = 0

        fun createNewChat() {
            chatCounter++
            val chatName = "Chat $chatCounter"
            chatSelector.addItem(chatName)

            val newChatPanel = AiSingleChatPanel()
            cardsPanel.add(newChatPanel, chatName)

            chatSelector.selectedItem = chatName
            cardLayout.show(cardsPanel, chatName)
        }

        val addBtn = JButton(AllIcons.General.Add).apply {
            toolTipText = "New Chat"
            addActionListener { createNewChat() }
        }

        val removeBtn = JButton(AllIcons.General.Remove).apply {
            toolTipText = "Delete Current Chat"
            addActionListener {
                val selected = chatSelector.selectedItem as? String
                if (selected != null && chatSelector.itemCount > 1) {
                    chatSelector.removeItem(selected)
                }
            }
        }

        chatSelector.addActionListener {
            val selected = chatSelector.selectedItem as? String
            if (selected != null) cardLayout.show(cardsPanel, selected)
        }

        toolbarPanel.add(chatSelector)
        toolbarPanel.add(addBtn)
        toolbarPanel.add(removeBtn)

        createNewChat()

        mainPanel.add(toolbarPanel, BorderLayout.NORTH)
        mainPanel.add(cardsPanel, BorderLayout.CENTER)

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(mainPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class AiSingleChatPanel : JPanel(BorderLayout()) {

    // Вместо JList используем вертикальный BoxLayout для сообщений
    private val messagesPanel = JPanel()
    private val chatScrollPane: JBScrollPane
    private val inputArea = JBTextArea()

    // Парсер Markdown
    private val mdParser = Parser.builder().build()
    private val htmlRenderer = HtmlRenderer.builder().build()

    private var messageCount = 0

    init {
        messagesPanel.layout = BoxLayout(messagesPanel, BoxLayout.Y_AXIS)
        messagesPanel.border = JBUI.Borders.empty(5)

        chatScrollPane = JBScrollPane(messagesPanel)
        chatScrollPane.border = JBUI.Borders.empty()
        // Ускоряем прокрутку
        chatScrollPane.verticalScrollBar.unitIncrement = 16

        setupUI()
        loadInitialHistory()
    }

    private fun loadInitialHistory() {
        addMessage("System", "Loading history...", isAi = true)
        ApplicationManager.getApplication().executeOnPooledThread {
            val history = AiApiClient.fetchInitialChat()
            ApplicationManager.getApplication().invokeLater {
                messagesPanel.removeAll()
                if (history.isEmpty()) {
                    addMessage("System", "Chat is empty. Start typing!", isAi = true)
                } else {
                    history.forEach { addMessage("AI", it, isAi = true) }
                }
            }
        }
    }

    private fun setupUI() {
        add(chatScrollPane, BorderLayout.CENTER)

        val inputContainer = JPanel(BorderLayout(5, 5))
        inputContainer.border = JBUI.Borders.empty(5)

        inputArea.rows = 3
        inputArea.lineWrap = true
        inputArea.wrapStyleWord = true
        val inputScrollPane = JBScrollPane(inputArea)

        val sendBtn = JButton("Send")
        sendBtn.preferredSize = Dimension(80, 50)

        fun triggerSend() {
            val text = inputArea.text
            if (text.isNotBlank()) sendMessage(text)
        }

        sendBtn.addActionListener { triggerSend() }

        inputArea.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER && (e.isMetaDown || e.isControlDown)) {
                    e.consume()
                    triggerSend()
                }
            }
        })

        val rightPanel = JPanel(BorderLayout())
        rightPanel.add(sendBtn, BorderLayout.CENTER)
        rightPanel.add(JLabel("<html><small><b>Cmd+Enter</b></small></html>"), BorderLayout.SOUTH)

        inputContainer.add(inputScrollPane, BorderLayout.CENTER)
        inputContainer.add(rightPanel, BorderLayout.EAST)

        add(inputContainer, BorderLayout.SOUTH)
    }

    private fun sendMessage(prompt: String) {
        inputArea.text = ""
        addMessage("You", prompt, isAi = false)

        ApplicationManager.getApplication().executeOnPooledThread {
            val responseArray = AiApiClient.sendRequest(prompt)
            ApplicationManager.getApplication().invokeLater {
                // Если API возвращает только последнее сообщение - используйте его.
                // Если массив всей истории - можно очистить messagesPanel.removeAll() и отрендерить всё заново
                responseArray.forEach { addMessage("AI", it, isAi = true) }
            }
        }
    }

    /**
     * Конвертирует Markdown в HTML и добавляет в список сообщений
     */
    private fun addMessage(author: String, mdText: String, isAi: Boolean) {
        messageCount++

        // 1. Конвертация Markdown -> HTML
        val document = mdParser.parse(mdText)
        val htmlContent = htmlRenderer.render(document)

        // 2. Получаем цвета текущей темы (Темная/Светлая)
        val fgColor = ColorUtil.toHex(UIUtil.getLabelForeground())
        val codeBgColor = ColorUtil.toHex(if (UIUtil.isUnderDarcula()) Color(60, 63, 65) else Color(235, 235, 235))

        // 3. Формируем красивый HTML с инлайн-стилями
        val styledHtml = """
            <html>
            <head>
                <style>
                    body {
                        font-family: ${UIUtil.getLabelFont().family};
                        font-size: ${UIUtil.getLabelFont().size}pt;
                        color: #$fgColor;
                        margin: 0;
                        padding: 5px;
                    }
                    pre {
                        background-color: #$codeBgColor;
                        padding: 8px;
                        border-radius: 4px;
                    }
                    code {
                        background-color: #$codeBgColor;
                        padding: 2px 4px;
                        font-family: monospace;
                    }
                    p { margin-top: 5px; margin-bottom: 5px; }
                </style>
            </head>
            <body>
                <b>$author:</b>
                $htmlContent
            </body>
            </html>
        """.trimIndent()

        // 4. Создаем компонент JEditorPane для рендера HTML
        val textPane = JEditorPane().apply {
            contentType = "text/html"
            text = styledHtml
            isEditable = false
            isOpaque = true
            // Чередуем фон сообщений для строгости
            background = if (messageCount % 2 == 0) JBUI.CurrentTheme.ToolWindow.background() else JBUI.CurrentTheme.EditorTabs.background()
            border = JBUI.Borders.empty(8)
            putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
        }

        // 5. Упаковываем в контейнер, чтобы он не растягивался на весь экран
        val wrapper = JPanel(BorderLayout())
        wrapper.add(textPane, BorderLayout.CENTER)
        wrapper.maximumSize = Dimension(Int.MAX_VALUE, textPane.preferredSize.height)

        messagesPanel.add(wrapper)
        messagesPanel.revalidate()
        messagesPanel.repaint()

        // Скроллим вниз
        SwingUtilities.invokeLater {
            val vertical = chatScrollPane.verticalScrollBar
            vertical.value = vertical.maximum
        }
    }
}