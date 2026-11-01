package org.razomy.language

import com.intellij.codeInsight.intention.IntentionAction
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
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.lang.*
import com.intellij.lexer.LexerBase
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.customization.*
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
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

// =============================================================================
// 1. ЯЗЫК, ИКОНКИ И ТИП ФАЙЛОВ RAZOMY v9.0 (.rl)
// =============================================================================

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
    override fun getDescription(): String = "Razomy v9.0 language file (.rl)"
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
                if (!forLsp && isDedicatedLspBin) {
                    val siblingCli = File(found.parentFile, "razomy$exeSuffix")
                    if (siblingCli.exists() && siblingCli.canExecute()) {
                        return siblingCli.absolutePath to emptyList()
                    }
                }
                return found.absolutePath to emptyList()
            }

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
        group("Razomy Toolchain (v9.0)") {
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
        val commandLine = RazomySettings.createCommand(project, "lsp")
        return OSProcessStreamConnectionProvider(commandLine)
    }
}

@Suppress("UnstableApiUsage")
class RazomyLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerSupportProvider.LspServerStarter
    ) {
        if (file.extension != "rl") return

        val isLsp4ijInstalled = com.intellij.ide.plugins.PluginManagerCore.isPluginInstalled(
            com.intellij.openapi.extensions.PluginId.getId("com.redhat.devtools.lsp4ij")
        )
        if (isLsp4ijInstalled) return

        serverStarter.ensureServerStarted(RazomyLspServerDescriptor(project))
    }
}

@Suppress("UnstableApiUsage")
class RazomyLspServerDescriptor(project: Project) :
    ProjectWideLspServerDescriptor(project, "Razomy Language Server (v9.0)") {

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
    "Run or Debug Razomy v9.0 (.rl) script",
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
// 5. КОММЕНТАРИИ RAZOMY v9.0 (`# ...` и `#\n...\n#` — Part I.3.E)
// =============================================================================

class RazomyCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "# "
    override fun getBlockCommentPrefix(): String = "#\n"
    override fun getBlockCommentSuffix(): String = "\n#"
    override fun getCommentedBlockCommentPrefix(): String? = null
    override fun getCommentedBlockCommentSuffix(): String? = null
}

// =============================================================================
// 6. ПАРНЫЕ СКОБКИ ФРАКТАЛЬНОЙ ТРИАДЫ RAZOMY (`{}`, `[]`, `()`) И PSI LEXER
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

object RazomyTokenTypes {
    @JvmField val LBRACE = IElementType("LBRACE", RazomyLanguage.INSTANCE)
    @JvmField val RBRACE = IElementType("RBRACE", RazomyLanguage.INSTANCE)
    @JvmField val LBRACKET = IElementType("LBRACKET", RazomyLanguage.INSTANCE)
    @JvmField val RBRACKET = IElementType("RBRACKET", RazomyLanguage.INSTANCE)
    @JvmField val LPAREN = IElementType("LPAREN", RazomyLanguage.INSTANCE)
    @JvmField val RPAREN = IElementType("RPAREN", RazomyLanguage.INSTANCE)
    @JvmField val IDENTIFIER = IElementType("IDENTIFIER", RazomyLanguage.INSTANCE)
    @JvmField val COMMENT = IElementType("COMMENT", RazomyLanguage.INSTANCE)
    @JvmField val STRING = IElementType("STRING", RazomyLanguage.INSTANCE)
    @JvmField val OTHER = IElementType("OTHER", RazomyLanguage.INSTANCE)
    @JvmField val FILE = IFileElementType(RazomyLanguage.INSTANCE)
}

/**
 * Лексер для JetBrains PSI (v9.0):
 * Корректно токенизирует идентификаторы (включая отверстия `_`, `_T`, `_a`),
 * скобки триады `{}`, `[]`, `()`, комментарии `# ...` / `#\n...\n#`,
 * обычные строки `"..."`, символы `'symbol'` и сырые строки `r"..."` / `r1"..."1` / `r2"..."2` / `r3"..."3`.
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

        // 1. Пробельные символы
        if (ch.isWhitespace()) {
            curEnd = curStart + 1
            while (curEnd < endOffset && buffer[curEnd].isWhitespace()) curEnd++
            curType = TokenType.WHITE_SPACE
            return
        }

        // 2. Комментарии v9.0: блочный `#\n...\n#` или строчный `# ...`
        if (ch == '#') {
            val nextCh = if (curStart + 1 < endOffset) buffer[curStart + 1] else '\u0000'
            if (nextCh == '\n' || (nextCh == '\r' && curStart + 2 < endOffset && buffer[curStart + 2] == '\n')) {
                curEnd = curStart + 2
                while (curEnd < endOffset) {
                    if (buffer[curEnd] == '#' && buffer[curEnd - 1] == '\n') {
                        curEnd++
                        break
                    }
                    curEnd++
                }
            } else {
                curEnd = curStart + 1
                while (curEnd < endOffset && buffer[curEnd] != '\n') curEnd++
            }
            curType = RazomyTokenTypes.COMMENT
            return
        }

        // 3. Raw Verbatim Strings v9.0: `r"..."`, `r0"..."0`, `r1"..."1`, `r2"..."2`, `r3"..."3`
        if (ch == 'r' && curStart + 1 < endOffset) {
            var idx = curStart + 1
            var levelDigits = ""
            while (idx < endOffset && buffer[idx].isDigit()) {
                levelDigits += buffer[idx]
                idx++
            }
            if (idx < endOffset && buffer[idx] == '"') {
                val closingSeq = "\"$levelDigits"
                curEnd = idx + 1
                while (curEnd < endOffset) {
                    if (matchesAt(curEnd, closingSeq)) {
                        curEnd += closingSeq.length
                        break
                    }
                    curEnd++
                }
                curType = RazomyTokenTypes.STRING
                return
            }
        }

        // 4. Строковые литералы `"..."` и символьные константы `'symbol'`
        if (ch == '"' || ch == '\'') {
            val quote = ch
            curEnd = curStart + 1
            while (curEnd < endOffset) {
                val c = buffer[curEnd]
                if (c == '\\') {
                    curEnd = (curEnd + 2).coerceAtMost(endOffset)
                    continue
                }
                curEnd++
                if (c == quote) break
            }
            curType = RazomyTokenTypes.STRING
            return
        }

        // 5. Скобки Фрактальной Триады `{}`, `[]`, `()`
        when (ch) {
            '{' -> { curEnd = curStart + 1; curType = RazomyTokenTypes.LBRACE; return }
            '}' -> { curEnd = curStart + 1; curType = RazomyTokenTypes.RBRACE; return }
            '[' -> { curEnd = curStart + 1; curType = RazomyTokenTypes.LBRACKET; return }
            ']' -> { curEnd = curStart + 1; curType = RazomyTokenTypes.RBRACKET; return }
            '(' -> { curEnd = curStart + 1; curType = RazomyTokenTypes.LPAREN; return }
            ')' -> { curEnd = curStart + 1; curType = RazomyTokenTypes.RPAREN; return }
        }

        // 6. Идентификаторы (`PascalCase`, `snake_case`, Holes `_`, `_T`, `_a`)
        if (ch.isLetterOrDigit() || ch == '_') {
            curEnd = curStart + 1
            while (curEnd < endOffset && (buffer[curEnd].isLetterOrDigit() || buffer[curEnd] == '_')) {
                curEnd++
            }
            curType = RazomyTokenTypes.IDENTIFIER
            return
        }

        curEnd = curStart + 1
        curType = RazomyTokenTypes.OTHER
    }

    private fun matchesAt(pos: Int, target: String): Boolean {
        if (pos + target.length > endOffset) return false
        for (i in target.indices) {
            if (buffer[pos + i] != target[i]) return false
        }
        return true
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
    override fun getCommentTokens(): TokenSet = TokenSet.create(RazomyTokenTypes.COMMENT)
    override fun getStringLiteralElements(): TokenSet = TokenSet.create(RazomyTokenTypes.STRING)
    override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = RazomyPsiFile(viewProvider)
}

// =============================================================================
// 7. СОЗДАНИЕ НОВОГО ФАЙЛА (New -> Rl File)
// =============================================================================

class CreateRazomyFileAction : CreateFileFromTemplateAction(
    "Rl File",
    "Create a new Razomy v9.0 (.rl) file",
    RazomyIcons.FILE
), DumbAware {

    override fun buildDialog(
        project: Project,
        directory: PsiDirectory,
        builder: CreateFileFromTemplateDialog.Builder
    ) {
        builder
            .setTitle("New Rl File")
            .addKind("Rl File", RazomyIcons.FILE, "Rl File")
    }

    override fun getActionName(
        directory: PsiDirectory?,
        newName: String,
        templateName: String?
    ): String = "Create Rl File: $newName"
}

// =============================================================================
// 8. ЭКСПОРТ ОБЪЕКТА В ОТДЕЛЬНЫЙ ФАЙЛ ПО КЛИКУ НА ИМЯ (`<Name>.rl` — v9.0)
// =============================================================================

object RazomyObjectExtractor {
    private val V9_DIRECTIVES = setOf(
        "Public", "Private", "Bypass", "Linear", "Watch", "Compile", "Extern"
    )

    private val CONTINUATION_ENDINGS = listOf(
        ".|", ".>", ".~", ".&", "./", ".?", ".!", ".=", "+=", "-=", "*=", "/=", "&=",
        "&&", "||", "|", "&", "/", "."
    )

    data class ExtractedObject(
        val name: String,
        val startOffset: Int,
        val endOffset: Int,
        val fullText: String
    )

    /**
     * Находит объект v9.0 (Форму, Функцию, Тип, Конструктор или Привязку) под курсором
     * и вычисляет точные границы его определения, включая:
     * - каноническую триаду `Name(Tuple) {Form} [Stream]` (Axiom II.1)
     * - хвостовые законы сохранения Нётер `} & [ start <= end ]` (Axiom III.1)
     * - хвостовые директивы `] & Watch`, `] & Compile`, `} & Linear`, `} & Bypass`
     * - гомоиконичные комментарии `# ...` и сырые строки `r"..."` / `r1"..."1`.
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
        if (name.first().isDigit() || name in V9_DIRECTIVES) return null

        // Проверяем, что идентификатор стоит в начале строки определения
        val lineNum = doc.getLineNumber(idStart)
        val lineStart = doc.getLineStartOffset(lineNum)
        val prefixOnLine = text.subSequence(lineStart, idStart).toString().trim()
        if (prefixOnLine.isNotEmpty() && prefixOnLine !in V9_DIRECTIVES) {
            return null
        }

        val declStart = if (prefixOnLine.isNotEmpty()) {
            lineStart + text.subSequence(lineStart, idStart).indexOfFirst { !it.isWhitespace() }
        } else {
            idStart
        }

        // 2. Ищем конец всего объекта с учетом сбалансированных {}, [], () и хвостовых `& ...`
        var pos = idEnd
        var braceDepth = 0
        var bracketDepth = 0
        var parenDepth = 0
        var inString: Char? = null
        var rawQuoteClosing: String? = null

        while (pos < text.length) {
            val ch = text[pos]

            // Обработка закрытия raw string `r1"..."1`
            if (rawQuoteClosing != null) {
                if (pos + rawQuoteClosing.length <= text.length &&
                    text.subSequence(pos, pos + rawQuoteClosing.length).toString() == rawQuoteClosing
                ) {
                    pos += rawQuoteClosing.length
                    rawQuoteClosing = null
                    continue
                }
                pos++
                continue
            }

            // Обработка обычных строк `"..."` и символов `'...'`
            if (inString != null) {
                if (ch == '\\') {
                    pos += 2
                    continue
                }
                if (ch == inString) inString = null
                pos++
                continue
            }

            // Проверка на старт сырой строки `r"..."` или `r1"..."1`
            if (ch == 'r' && pos + 1 < text.length && ( pos == 0 || !text[pos - 1].isLetterOrDigit() )) {
                var rIdx = pos + 1
                val digits = StringBuilder()
                while (rIdx < text.length && text[rIdx].isDigit()) {
                    digits.append(text[rIdx])
                    rIdx++
                }
                if (rIdx < text.length && text[rIdx] == '"') {
                    rawQuoteClosing = "\"$digits"
                    pos = rIdx + 1
                    continue
                }
            }

            // Пропускаем комментарии v9.0 (`# ...` и `#\n...\n#`) и legacy `// ...`
            if (ch == '#') {
                val nextCh = if (pos + 1 < text.length) text[pos + 1] else '\u0000'
                if (nextCh == '\n' || (nextCh == '\r' && pos + 2 < text.length && text[pos + 2] == '\n')) {
                    pos += 2
                    while (pos < text.length) {
                        if (text[pos] == '#' && text[pos - 1] == '\n') {
                            pos++
                            break
                        }
                        pos++
                    }
                } else {
                    while (pos < text.length && text[pos] != '\n') pos++
                }
                continue
            }
            if (ch == '/' && pos + 1 < text.length && text[pos + 1] == '/') {
                while (pos < text.length && text[pos] != '\n') pos++
                continue
            }

            when (ch) {
                '"', '\'' -> inString = ch
                '{' -> braceDepth++
                '}' -> braceDepth = (braceDepth - 1).coerceAtLeast(0)
                '[' -> bracketDepth++
                ']' -> bracketDepth = (bracketDepth - 1).coerceAtLeast(0)
                '(' -> parenDepth++
                ')' -> parenDepth = (parenDepth - 1).coerceAtLeast(0)
                '\n' -> {
                    if (braceDepth == 0 && bracketDepth == 0 && parenDepth == 0) {
                        // Проверяем, не заканчивается ли текущая строка на оператор продолжения
                        val codeBeforeNewline = stripTrailingCommentOnLine(
                            text.subSequence(declStart, pos).toString()
                        ).trimEnd()

                        val continuesOnNextLine = CONTINUATION_ENDINGS.any {
                            codeBeforeNewline.endsWith(it) && !codeBeforeNewline.endsWith("..")
                        }

                        // Также проверяем, не начинается ли следующая строка с продолжения (`&`, `|`, `.`)
                        var lookahead = pos + 1
                        while (lookahead < text.length && (text[lookahead] == ' ' || text[lookahead] == '\t' || text[lookahead] == '\r')) {
                            lookahead++
                        }
                        val nextStartsContinuation = lookahead < text.length &&
                                (text[lookahead] == '&' || text[lookahead] == '|' ||
                                        (text[lookahead] == '.' && (lookahead + 1 >= text.length || text[lookahead + 1] != '.')))

                        if (!continuesOnNextLine && !nextStartsContinuation) {
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

    private fun stripTrailingCommentOnLine(textSlice: String): String {
        val lastNewline = textSlice.lastIndexOf('\n')
        val lastLine = if (lastNewline >= 0) textSlice.substring(lastNewline + 1) else textSlice
        var inQuote: Char? = null
        for (i in lastLine.indices) {
            val c = lastLine[i]
            if (inQuote != null) {
                if (c == '\\') continue
                if (c == inQuote) inQuote = null
            } else {
                if (c == '"' || c == '\'') inQuote = c
                else if (c == '#') return lastLine.substring(0, i)
            }
        }
        return lastLine
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

            // В Razomy v9.0 (Section 4.6) включение модуля из соседнего файла записывается через `..Path"./<Name>.rl"`
            editor.document.replaceString(
                extracted.startOffset,
                extracted.endOffset,
                "..Path\"./$targetFileName\""
            )
        })

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
    "Extract selected Razomy v9.0 Form/Function into <Name>.rl",
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
