package org.razomy.language

import com.intellij.execution.Executor
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.*
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.filters.RegexpFilter
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.icons.AllIcons
import com.intellij.lang.BracePair
import com.intellij.lang.Commenter
import com.intellij.lang.Language
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.*
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.customization.*
import com.intellij.psi.PsiElement
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.server.OSProcessStreamConnectionProvider
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider
import org.jdom.Element
import java.io.File
import javax.swing.Icon
import javax.swing.JComponent
// Добавь эти импорты в начало Kotlin-файла:
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.LexerBase
import com.intellij.psi.FileViewProvider
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.psi.PsiDirectory
// =============================================================================
// 1. ЯЗЫК, ИКОНКИ И ТИП ФАЙЛОВ RAZOMY (.rl)
// =============================================================================


// Дополнительные импорты (добавьте в шапку файла при необходимости):
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VfsUtil

// =============================================================================
// 8. ЭКСПОРТ ОБЪЕКТА В ОТДЕЛЬНЫЙ ФАЙЛ ПО КЛИКУ НА ИМЯ (`<Name>.rl`)
// =============================================================================

object RazomyObjectExtractor {
    data class ExtractedObject(
        val name: String,
        val startOffset: Int,
        val endOffset: Int,
        val fullText: String
    )

    /**
     * Находит объект (Форму, Функцию, Тип или Привязку) под курсором и вычисляет
     * точные границы его определения (включая сбалансированные блоки `{}`, `[]`, `()`).
     */
    fun findObjectAtCaret(editor: Editor, file: PsiFile): ExtractedObject? {
        if (file.virtualFile?.extension != "rl") return null
        val doc = editor.document
        val text = doc.charsSequence
        if (text.isEmpty()) return null

        val offset = editor.caretModel.offset.coerceIn(0, text.length - 1)

        // 1. Выделяем идентификатор под курсором
        var idStart = offset
        while (idStart > 0 && (text[idStart - 1].isLetterOrDigit() || text[idStart - 1] == '_')) {
            idStart--
        }
        var idEnd = offset
        while (idEnd < text.length && (text[idEnd].isLetterOrDigit() || text[idEnd] == '_')) {
            idEnd++
        }
        if (idStart >= idEnd) return null

        val name = text.subSequence(idStart, idEnd).toString()
        if (name.first().isDigit()) return null

        // Проверяем, что идентификатор стоит в начале строки/предложения (определение объекта)
        val lineNum = doc.getLineNumber(idStart)
        val lineStart = doc.getLineStartOffset(lineNum)
        val prefixOnLine = text.subSequence(lineStart, idStart).toString().trim()
        if (prefixOnLine.isNotEmpty() && prefixOnLine != "Public" && prefixOnLine != "Private" && prefixOnLine != "Bypass") {
            return null
        }

        val declStart = if (prefixOnLine.isNotEmpty()) {
            lineStart + text.subSequence(lineStart, idStart).indexOfFirst { !it.isWhitespace() }
        } else {
            idStart
        }

        // 2. Ищем конец всего объекта (с учетом многострочных скобок {}, [], () и пайпов)
        var pos = idEnd
        var braceDepth = 0
        var bracketDepth = 0
        var parenDepth = 0
        var sawOpeningBlock = false
        var inString: Char? = null

        while (pos < text.length) {
            val ch = text[pos]

            if (inString != null) {
                if (ch == '\\') {
                    pos += 2
                    continue
                }
                if (ch == inString) inString = null
                pos++
                continue
            }

            // Пропускаем строчный комментарий до конца строки
            if (ch == '/' && pos + 1 < text.length && text[pos + 1] == '/') {
                while (pos < text.length && text[pos] != '\n') pos++
                continue
            }

            when (ch) {
                '"', '\'' -> inString = ch
                '{' -> { braceDepth++; sawOpeningBlock = true }
                '}' -> {
                    braceDepth = (braceDepth - 1).coerceAtLeast(0)
                    if (sawOpeningBlock && braceDepth == 0 && bracketDepth == 0 && parenDepth == 0) {
                        // Проверяем, нет ли после `}` хвоста или `[` (например, у функции `fn(a) [ ... ]`)
                        var lookahead = pos + 1
                        while (lookahead < text.length && (text[lookahead] == ' ' || text[lookahead] == '\t')) {
                            lookahead++
                        }
                        if (lookahead >= text.length || text[lookahead] == '\n' || text[lookahead] == '\r') {
                            pos++
                            break
                        }
                    }
                }
                '[' -> { bracketDepth++; sawOpeningBlock = true }
                ']' -> {
                    bracketDepth = (bracketDepth - 1).coerceAtLeast(0)
                    if (sawOpeningBlock && braceDepth == 0 && bracketDepth == 0 && parenDepth == 0) {
                        pos++
                        break
                    }
                }
                '(' -> parenDepth++
                ')' -> parenDepth = (parenDepth - 1).coerceAtLeast(0)
                '\n' -> {
                    if (braceDepth == 0 && bracketDepth == 0 && parenDepth == 0) {
                        // Если строка заканчивается на `|` или `&` (многострочный Union), продолжаем на следующую строку
                        val lineBeforeNewline = text.subSequence(declStart, pos).toString().trimEnd()
                        if (!lineBeforeNewline.endsWith("|") && !lineBeforeNewline.endsWith("&")) {
                            break
                        }
                    }
                }
            }
            pos++
        }

        val fullText = text.subSequence(declStart, pos).toString().trim()
        if (fullText.isEmpty()) return null

        return ExtractedObject(name, declStart, pos, fullText)
    }

    fun exportToNewFile(project: Project, editor: Editor, file: PsiFile, extracted: ExtractedObject) {
        val currentVFile = file.virtualFile ?: return
        val parentDir = currentVFile.parent ?: return
        val targetFileName = "${extracted.name}.rl"

        val existingFile = parentDir.findChild(targetFileName)
        if (existingFile != null) {
            val overwrite = Messages.showYesNoDialog(
                project,
                "Файл '$targetFileName' уже существует. Перезаписать его?",
                "Export Object to File",
                Messages.getQuestionIcon()
            )
            if (overwrite != Messages.YES) return
        }

        var createdFile: VirtualFile? = null

        WriteCommandAction.runWriteCommandAction(project, "Export '${extracted.name}' to $targetFileName", null, {
            val newVFile = parentDir.findOrCreateChildData(this, targetFileName)
            VfsUtil.saveText(newVFile, extracted.fullText + "\n")
            createdFile = newVFile

            // Заменяем тело объекта в исходном файле на импорт `_ <ИмяОбъекта>`
            editor.document.replaceString(
                extracted.startOffset,
                extracted.endOffset,
                "_ ${extracted.name}"
            )
        })

        // Открываем только что созданный файл в редакторе
        createdFile?.let {
            FileEditorManager.getInstance(project).openFile(it, true)
        }
    }
}

/**
 * 1) Intention Action (появляется по Alt+Enter при клике на имя объекта)
 */
class ExportRazomyObjectIntention : IntentionAction {
    override fun getText(): String = "Export Razomy object to separate '.rl' file"
    override fun getFamilyName(): String = "Razomy Refactoring"
    override fun startInWriteAction(): Boolean = false

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (editor == null || file == null) return false
        val extracted = RazomyObjectExtractor.findObjectAtCaret(editor, file) ?: return false
        return extracted.name.isNotEmpty()
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val extracted = RazomyObjectExtractor.findObjectAtCaret(editor, file) ?: return
        RazomyObjectExtractor.exportToNewFile(project, editor, file, extracted)
    }
}

/**
 * 2) Пункт контекстного меню правой кнопки мыши (Right Click -> Export '<Name>' to '<Name>.rl')
 */
class ExportRazomyObjectAction : AnAction(
    "Export Object to Separate .rl File",
    "Extract selected Razomy Form/Function into <Name>.rl",
    RazomyIcons.FILE
), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val file = e.getData(CommonDataKeys.PSI_FILE)
        val extracted = if (editor != null && file != null) {
            RazomyObjectExtractor.findObjectAtCaret(editor, file)
        } else null

        e.presentation.isEnabledAndVisible = extracted != null
        if (extracted != null) {
            e.presentation.text = "Export '${extracted.name}' to '${extracted.name}.rl'"
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val extracted = RazomyObjectExtractor.findObjectAtCaret(editor, file) ?: return
        RazomyObjectExtractor.exportToNewFile(project, editor, file, extracted)
    }
}

object RazomyIcons {
    @JvmField
    val FILE: Icon = IconLoader.getIcon("/icons/rl.svg", RazomyIcons::class.java)
}

class RazomyLanguage private constructor() : Language("RL") {
    override fun getDisplayName(): String = "Razomy"

    companion object {
        @JvmField
        val INSTANCE = RazomyLanguage()
    }
}

class RazomyFileType private constructor() : LanguageFileType(RazomyLanguage.INSTANCE) {
    override fun getName(): String = "Razomy File"
    override fun getDescription(): String = "Razomy language file (.rl)"
    override fun getDefaultExtension(): String = "rl"
    override fun getIcon(): Icon = RazomyIcons.FILE

    companion object {
        @JvmField
        val INSTANCE = RazomyFileType()
    }
}

// =============================================================================
// 2. TOOLCHAIN & НАСТРОЙКИ IDE (Settings -> Languages & Frameworks -> Razomy)
// =============================================================================

@Service(Service.Level.APP)
@State(name = "RazomySettings", storages = [Storage("razomy.xml")])
class RazomySettings : PersistentStateComponent<RazomySettings.State> {
    class State {
        var customExecutablePath: String = ""
        var workingDirectory: String = ""
    }

    private var myState = State()
    override fun getState(): State = myState
    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        fun getInstance(): RazomySettings = service()

        /**
         * Умный резолвер пути к компилятору/LSP Razomy:
         * Корректно различает универсальный CLI `razomy` и выделенный `razomy-lsp`.
         */
        fun resolveBinary(project: Project?, forLsp: Boolean): Pair<String, List<String>> {
            val isWin = System.getProperty("os.name").lowercase().contains("win")
            val exeSuffix = if (isWin) ".exe" else ""
            val userHome = System.getProperty("user.home")
            val basePath = project?.basePath

            val custom = getInstance().state.customExecutablePath.trim()
            val envPath = System.getenv("RAZOMY_LSP_PATH")?.trim()
            val envHome = System.getenv("RAZOMY_HOME")?.trim()

            val cliCandidates = listOfNotNull(
                custom.takeIf { it.isNotEmpty() }?.let { File(it) },
                envPath?.takeIf { it.isNotEmpty() }?.let { File(it) },
                envHome?.takeIf { it.isNotEmpty() }?.let { File(it, "bin/razomy$exeSuffix") },
                basePath?.let { File(it, "target/release/razomy$exeSuffix") },
                basePath?.let { File(it, "target/debug/razomy$exeSuffix") },
                File(userHome, ".cargo/bin/razomy$exeSuffix"),
                File("/usr/local/bin/razomy$exeSuffix"),
                File("/opt/homebrew/bin/razomy$exeSuffix")
            )

            val lspCandidates = listOfNotNull(
                basePath?.let { File(it, "target/release/razomy-lsp$exeSuffix") },
                basePath?.let { File(it, "target/debug/razomy-lsp$exeSuffix") },
                File(userHome, ".cargo/bin/razomy-lsp$exeSuffix"),
                File("/usr/local/bin/razomy-lsp$exeSuffix"),
                File("/opt/homebrew/bin/razomy-lsp$exeSuffix")
            )

            val pathDirs = (System.getenv("PATH") ?: "")
                .split(File.pathSeparatorChar)
                .filter { it.isNotBlank() }
            val pathCliCandidates = pathDirs.map { File(it, "razomy$exeSuffix") }
            val pathLspCandidates = pathDirs.map { File(it, "razomy-lsp$exeSuffix") }

            // Если в проекте есть и debug, и release сборка, выбираем более свежую по времени модификации
            val localCandidates = (if (forLsp) cliCandidates + lspCandidates else cliCandidates)
                .filter { it.exists() && it.isFile && it.canExecute() }
            val pathPool = (if (forLsp) pathCliCandidates + pathLspCandidates else pathCliCandidates)
                .filter { it.exists() && it.isFile && it.canExecute() }

            val found = if (custom.isNotEmpty() && File(custom).canExecute()) {
                File(custom)
            } else {
                localCandidates.maxByOrNull { it.lastModified() } ?: pathPool.firstOrNull()
            }

            if (found != null) {
                val isDedicatedLspBin = found.nameWithoutExtension.endsWith("razomy-lsp")
                // Если пользователь указал `razomy-lsp` в настройках, а запускает `run`/`debug`,
                // ищем рядом бинарник `razomy`
                if (!forLsp && isDedicatedLspBin) {
                    val siblingCli = File(found.parentFile, "razomy$exeSuffix")
                    if (siblingCli.exists() && siblingCli.canExecute()) {
                        return siblingCli.absolutePath to emptyList()
                    }
                }
                return found.absolutePath to emptyList()
            }

            // Fallback: если открыт сам Rust-проект компилятора Razomy
            if (basePath != null && File(basePath, "Cargo.toml").exists()) {
                return "cargo" to listOf("run", "--quiet", "--bin", "razomy", "--")
            }

            return "razomy" to emptyList()
        }

        fun createCommand(project: Project?, vararg subArgs: String): GeneralCommandLine {
            val isLspRequest = subArgs.firstOrNull() == "lsp"
            val (bin, prefixArgs) = resolveBinary(project, forLsp = isLspRequest)
            val allArgs = mutableListOf<String>()
            allArgs.addAll(prefixArgs)

            val isDedicatedLspBin = bin.endsWith("razomy-lsp") || bin.endsWith("razomy-lsp.exe")
            if (isDedicatedLspBin && isLspRequest) {
                // `razomy-lsp` запускается в режиме stdio без подкоманды `lsp`
                allArgs.addAll(subArgs.drop(1))
            } else {
                allArgs.addAll(subArgs)
            }

            val workDir = getInstance().state.workingDirectory.trim()
                .takeIf { it.isNotEmpty() }
                ?: project?.basePath

            return GeneralCommandLine(bin)
                .withParameters(allArgs)
                .withWorkDirectory(workDir)
                .withCharset(Charsets.UTF_8)
        }
    }
}

class RazomyConfigurable : BoundConfigurable("Razomy") {
    private val settings = RazomySettings.getInstance()

    override fun createPanel(): DialogPanel = panel {
        group("Razomy Toolchain") {
            row("Razomy executable:") {
                textFieldWithBrowseButton(
                    FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                        .withTitle("Select Razomy Executable")
                ).bindText(settings.state::customExecutablePath)
                    .align(AlignX.FILL)
                    .comment("Leave empty to auto-detect in ~/.cargo/bin/razomy or project target/release/")
            }
            row("Default working directory:") {
                textFieldWithBrowseButton(
                    FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle("Select Working Directory")
                ).bindText(settings.state::workingDirectory)
                    .align(AlignX.FILL)
                    .comment("Leave empty to use project root directory")
            }
        }
    }
}

// =============================================================================
// 3. ПОЛНАЯ ИНТЕГРАЦИЯ LSP СЕРВЕРА С JETBRAINS IDE
// =============================================================================

class RazomyLspServerFactory : LanguageServerFactory {
    override fun createConnectionProvider(project: Project): StreamConnectionProvider {
        // Используем ваш же умный резолвер бинарника!
        val commandLine = RazomySettings.createCommand(project, "lsp")
        return OSProcessStreamConnectionProvider(commandLine)
    }
}

// --- ВАРИАНТ Б: Для нативного JetBrains LSP (Работает в Ultimate/RustRover) ---
@Suppress("UnstableApiUsage")
class RazomyLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerSupportProvider.LspServerStarter
    ) {
        if (file.extension != "rl") return

        // Защита от дублирования: если у юзера УЖЕ стоит плагин от RedHat,
        // не запускаем второй сервер от JetBrains, чтобы не было двойных подсказок.
        val isLsp4ijInstalled = com.intellij.ide.plugins.PluginManagerCore.isPluginInstalled(
            com.intellij.openapi.extensions.PluginId.getId("com.redhat.devtools.lsp4ij")
        )
        if (isLsp4ijInstalled) return

        serverStarter.ensureServerStarted(RazomyLspServerDescriptor(project))
    }
}

@Suppress("UnstableApiUsage")
class RazomyLspServerDescriptor(project: Project) :
    ProjectWideLspServerDescriptor(project, "Razomy Language Server (V5.7)") {

    override fun isSupportedFile(file: VirtualFile): Boolean = file.extension == "rl"

    override val lspSemanticTokensSupport = LspSemanticTokensSupport()
    override val lspGoToDefinitionSupport = true
    override val lspHoverSupport = true
    override val lspCompletionSupport = LspCompletionSupport()
    override val lspDiagnosticsSupport = LspDiagnosticsSupport()
    override val lspFormattingSupport = LspFormattingSupport()
    override val lspFindReferencesSupport = LspFindReferencesSupport()
    override val lspCodeActionsSupport = LspCodeActionsSupport()

    override fun createCommandLine(): GeneralCommandLine {
        return RazomySettings.createCommand(project, "lsp")
    }
}

// =============================================================================
// 4. RUN / DEBUG КОНФИГУРАЦИИ, GREEN PLAY BUTTON & КОНТЕКСТНЫЙ ACTION
// =============================================================================

class RazomyRunConfigurationType : SimpleConfigurationType(
    "RazomyRunConfiguration",
    "Razomy",
    "Run or Debug Razomy (.rl) script",
    NotNullLazyValue.createValue { RazomyIcons.FILE }
) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return RazomyRunConfiguration(project, this, "Razomy Script")
    }
}

class RazomyRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : LocatableConfigurationBase<RunProfileState>(project, factory, name) {

    var scriptPath: String = ""

    override fun checkConfiguration() {
        if (scriptPath.isBlank()) {
            throw RuntimeConfigurationError("Script path (.rl) is not specified")
        }
        val file = File(scriptPath)
        if (!file.exists() || !file.isFile) {
            throw RuntimeConfigurationWarning("Script file does not exist: $scriptPath")
        }
    }

    override fun getConfigurationEditor(): SettingsEditor<RazomyRunConfiguration> =
        object : SettingsEditor<RazomyRunConfiguration>() {
            private var panel: DialogPanel? = null

            override fun resetEditorFrom(s: RazomyRunConfiguration) {
                scriptPath = s.scriptPath
                panel?.reset()
            }

            @Throws(ConfigurationException::class)
            override fun applyEditorTo(s: RazomyRunConfiguration) {
                panel?.apply()
                s.scriptPath = scriptPath
            }

            override fun createEditor(): JComponent {
                panel = panel {
                    row("Script (.rl):") {
                        textFieldWithBrowseButton(
                            FileChooserDescriptorFactory.createSingleFileDescriptor("rl")
                                .withTitle("Select Razomy Script"),
                            project
                        ).bindText(::scriptPath)
                            .align(AlignX.FILL)
                    }
                }
                return panel!!
            }
        }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.setAttribute("scriptPath", scriptPath)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        scriptPath = element.getAttributeValue("scriptPath") ?: ""
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return object : CommandLineState(environment) {
            override fun startProcess(): ProcessHandler {
                val mode = if (executor.id == DefaultDebugExecutor.EXECUTOR_ID) "debug" else "run"
                val cmd = RazomySettings.createCommand(project, mode, scriptPath)

                val handler = KillableColoredProcessHandler(cmd)
                ProcessTerminatedListener.attach(handler)
                return handler
            }
        }.apply {
            val builder = TextConsoleBuilderFactory.getInstance().createBuilder(project)
            builder.addFilter(RegexpFilter(project, "${RegexpFilter.FILE_PATH_MACROS}:${RegexpFilter.LINE_MACROS}"))
            consoleBuilder = builder
        }
    }
}

class RazomyRunConfigurationProducer : LazyRunConfigurationProducer<RazomyRunConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory =
        ConfigurationTypeUtil.findConfigurationType(RazomyRunConfigurationType::class.java)

    override fun isConfigurationFromContext(
        configuration: RazomyRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val file = context.location?.virtualFile ?: return false
        return file.extension == "rl" && configuration.scriptPath == file.path
    }

    override fun setupConfigurationFromContext(
        configuration: RazomyRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val file = context.location?.virtualFile ?: return false
        if (file.extension != "rl") return false
        configuration.scriptPath = file.path
        configuration.name = file.name
        return true
    }
}

class RazomyRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        val file = element.containingFile?.virtualFile ?: return null
        if (file.extension != "rl") return null

        if (element.textOffset == 0 && element.firstChild == null) {
            val actions = ExecutorAction.getActions(0)
            return Info(AllIcons.RunConfigurations.TestState.Run, actions) { "Run ${file.name}" }
        }
        return null
    }
}

/**
 * Реализация недостающего в вашем коде экшена `org.razomy.language.RunRazomyFileAction`,
 * который был объявлен в `plugin.xml` (`<action id="Razomy.RunFile".../>`).
 */
class RunRazomyFileAction : AnAction("Run Razomy Script", "Run current .rl file", RazomyIcons.FILE), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val project = e.project
        val isRlFile = project != null && file != null && !file.isDirectory && file.extension == "rl"
        e.presentation.isEnabledAndVisible = isRlFile
        if (isRlFile && file != null) {
            e.presentation.text = "Run '${file.name}'"
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        if (file.extension != "rl") return

        val runManager = RunManager.getInstance(project)
        val configType = ConfigurationTypeUtil.findConfigurationType(RazomyRunConfigurationType::class.java)

        val existing = runManager.getConfigurationSettingsList(configType).firstOrNull {
            (it.configuration as? RazomyRunConfiguration)?.scriptPath == file.path
        }

        val settings = existing ?: runManager.createConfiguration(file.name, configType).apply {
            (configuration as RazomyRunConfiguration).scriptPath = file.path
            runManager.addConfiguration(this)
        }

        runManager.selectedConfiguration = settings
        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance())
    }
}

// =============================================================================
// 5. КОММЕНТАРИИ (`Cmd+/`)
// =============================================================================

class RazomyCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "// "
    override fun getBlockCommentPrefix(): String = "#\n"
    override fun getBlockCommentSuffix(): String = "\n#"
    override fun getCommentedBlockCommentPrefix(): String? = null
    override fun getCommentedBlockCommentSuffix(): String? = null
}

// =============================================================================
// 6. ПАРНЫЕ СКОБКИ ТРИАДЫ RAZOMY (`{}`, `[]`, `()`)
// =============================================================================

class RazomyPairedBraceMatcher : PairedBraceMatcher {
    override fun getPairs(): Array<BracePair> = arrayOf(
        BracePair(RazomyTokenTypes.LBRACE, RazomyTokenTypes.RBRACE, true),
        BracePair(RazomyTokenTypes.LBRACKET, RazomyTokenTypes.RBRACKET, true),
        BracePair(RazomyTokenTypes.LPAREN, RazomyTokenTypes.RPAREN, false)
    )

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true
    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset
}


// Добавь в конец Kotlin-файла (раздел 6):
object RazomyTokenTypes {
    @JvmField val LBRACE = IElementType("LBRACE", RazomyLanguage.INSTANCE)
    @JvmField val RBRACE = IElementType("RBRACE", RazomyLanguage.INSTANCE)
    @JvmField val LBRACKET = IElementType("LBRACKET", RazomyLanguage.INSTANCE)
    @JvmField val RBRACKET = IElementType("RBRACKET", RazomyLanguage.INSTANCE)
    @JvmField val LPAREN = IElementType("LPAREN", RazomyLanguage.INSTANCE)
    @JvmField val RPAREN = IElementType("RPAREN", RazomyLanguage.INSTANCE)
    @JvmField val IDENTIFIER = IElementType("IDENTIFIER", RazomyLanguage.INSTANCE)
    @JvmField val OTHER = IElementType("OTHER", RazomyLanguage.INSTANCE)
    @JvmField val FILE = IFileElementType(RazomyLanguage.INSTANCE)
}

/**
 * Легковесный лексер для JetBrains PSI: разбивает файл на отдельные идентификаторы и скобки,
 * чтобы при Cmd+Hover (Go To Definition) подсвечивалось ТОЛЬКО одно слово под курсором, а не весь файл!
 */
class RazomyWordLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var startOffset = 0
    private var endOffset = 0
    private var curStart = 0
    private var curEnd = 0
    private var curType: IElementType? = null

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.startOffset = startOffset
        this.endOffset = endOffset
        this.curStart = startOffset
        this.curEnd = startOffset
        advance()
    }

    override fun getState(): Int = 0
    override fun getTokenType(): IElementType? = curType
    override fun getTokenStart(): Int = curStart
    override fun getTokenEnd(): Int = curEnd
    override fun getBufferSequence(): CharSequence = buffer
    override fun getBufferEnd(): Int = endOffset

    override fun advance() {
        curStart = curEnd
        if (curStart >= endOffset) {
            curType = null
            return
        }
        val ch = buffer[curStart]
        when {
            ch.isWhitespace() -> {
                curEnd = curStart + 1
                while (curEnd < endOffset && buffer[curEnd].isWhitespace()) curEnd++
                curType = TokenType.WHITE_SPACE
            }
            ch == '{' -> { curEnd = curStart + 1; curType = RazomyTokenTypes.LBRACE }
            ch == '}' -> { curEnd = curStart + 1; curType = RazomyTokenTypes.RBRACE }
            ch == '[' -> { curEnd = curStart + 1; curType = RazomyTokenTypes.LBRACKET }
            ch == ']' -> { curEnd = curStart + 1; curType = RazomyTokenTypes.RBRACKET }
            ch == '(' -> { curEnd = curStart + 1; curType = RazomyTokenTypes.LPAREN }
            ch == ')' -> { curEnd = curStart + 1; curType = RazomyTokenTypes.RPAREN }
            ch.isLetterOrDigit() || ch == '_' -> {
                curEnd = curStart + 1
                while (curEnd < endOffset && (buffer[curEnd].isLetterOrDigit() || buffer[curEnd] == '_')) {
                    curEnd++
                }
                curType = RazomyTokenTypes.IDENTIFIER
            }
            else -> {
                curEnd = curStart + 1
                curType = RazomyTokenTypes.OTHER
            }
        }
    }
}

class RazomyPsiFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, RazomyLanguage.INSTANCE) {
    override fun getFileType() = RazomyFileType.INSTANCE
}

class RazomyParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?) = RazomyWordLexer()
    override fun createParser(project: Project?) = PsiParser { root, builder ->
        val marker = builder.mark()
        while (!builder.eof()) {
            builder.advanceLexer()
        }
        marker.done(root)
        builder.treeBuilt
    }
    override fun getFileNodeType(): IFileElementType = RazomyTokenTypes.FILE
    override fun getCommentTokens(): TokenSet = TokenSet.EMPTY
    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY
    override fun createElement(node: ASTNode): PsiElement = com.intellij.extapi.psi.ASTWrapperPsiElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = RazomyPsiFile(viewProvider)
}

// =============================================================================
// 7. СОЗДАНИЕ НОВОГО ФАЙЛА (New -> Rala File)
// =============================================================================

class CreateRazomyFileAction : CreateFileFromTemplateAction(
    "Rala File", // Текст в меню New -> Rala File
    "Create a new Rala (.rl) file",
    RazomyIcons.FILE
), DumbAware {

    override fun buildDialog(
        project: Project,
        directory: PsiDirectory,
        builder: CreateFileFromTemplateDialog.Builder
    ) {
        builder
            .setTitle("New Rala File")
            // "Rala File" — это имя шаблона из папки fileTemplates/internal/ (без расширения .rl.ft)
            .addKind("Rala File", RazomyIcons.FILE, "Rala File")
    }

    override fun getActionName(
        directory: PsiDirectory?,
        newName: String,
        templateName: String?
    ): String = "Create Rala File: $newName"
}