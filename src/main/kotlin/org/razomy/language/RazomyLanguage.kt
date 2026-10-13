package org.razomy.language
import com.intellij.util.indexing.*
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.codeInspection.*
import com.intellij.formatting.*
import com.intellij.icons.AllIcons
import com.intellij.ide.structureView.*
import com.intellij.ide.structureView.StructureViewModel.ElementInfoProvider
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.*
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.lexer.FlexAdapter
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ProcessingContext
import org.razomy.language.psi.*
import javax.swing.Icon
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.codeInspection.util.IntentionName
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.FileTypeIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.tree.TokenSet

// --- LANGUAGE & FILE TYPE ---

// --- СИСТЕМА ТИПОВ И СЕМАНТИЧЕСКИЙ РЕЗОЛВЕР (Razomy Type System V2.5 - Logger Edition) ---

// --- GOTO DECLARATION ---


// --- SYNTAX HIGHLIGHTER ---

class RazomySyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = FlexAdapter(RazomyLexer(null))

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            RazomyTypes.TRUE, RazomyTypes.FALSE, RazomyTypes.UNDERSCORE -> arrayOf(DefaultLanguageHighlighterColors.KEYWORD)
            RazomyTypes.IDENTIFIER -> arrayOf(DefaultLanguageHighlighterColors.IDENTIFIER)
            RazomyTypes.STRING -> arrayOf(DefaultLanguageHighlighterColors.STRING)
            RazomyTypes.NUMBER -> arrayOf(DefaultLanguageHighlighterColors.NUMBER)
            RazomyTypes.LINE_COMMENT -> arrayOf(DefaultLanguageHighlighterColors.LINE_COMMENT)

            // Все операторы
            RazomyTypes.DOT_GT, RazomyTypes.DOT_AMP, RazomyTypes.MIGRATION,
            RazomyTypes.PIPE, RazomyTypes.AMP, RazomyTypes.SLASH,
            RazomyTypes.EQ_EQ, RazomyTypes.NOT_EQ, RazomyTypes.LTE, RazomyTypes.GTE,
            RazomyTypes.LANGLE, RazomyTypes.RANGLE, RazomyTypes.EQ,
            RazomyTypes.PLUS, RazomyTypes.MINUS, RazomyTypes.MUL -> arrayOf(DefaultLanguageHighlighterColors.OPERATION_SIGN)

            RazomyTypes.COMMA, RazomyTypes.SEMICOLON -> arrayOf(DefaultLanguageHighlighterColors.SEMICOLON)
            else -> emptyArray()
        }
    }
}

class RazomySyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?) = RazomySyntaxHighlighter()
}


// --- LINE MARKERS (Иерархия) ---

class RazomyLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (element !is RazomyPath || !RazomyUtil.isDeclaration(element)) return

        // ИСПРАВЛЕНИЕ WARNING'a: IntelliJ требует вешать маркер на листовой токен (IDENTIFIER)
        val leafElement = element.firstChild ?: element
        val file = element.containingFile ?: return
        val currentName = element.text

        // 1. Предки
        val statement = PsiTreeUtil.getParentOfType(element, RazomyDeclaration::class.java, RazomyFormItem::class.java)
        if (statement != null && PsiTreeUtil.findChildOfType(statement, RazomyPath::class.java) == element) {
            val rightExpr = statement.children.getOrNull(1)
            if (rightExpr != null) {
                val pathsInRight = PsiTreeUtil.findChildrenOfType(rightExpr, RazomyPath::class.java)
                val superTypes = pathsInRight
                    .filter { it.text.firstOrNull()?.isUpperCase() == true }
                    .mapNotNull { path ->
                        val allDecls = PsiTreeUtil.findChildrenOfType(file, RazomyDeclaration::class.java)
                        allDecls.firstNotNullOfOrNull { decl ->
                            val namePath = PsiTreeUtil.findChildOfType(decl, RazomyPath::class.java)
                            if (namePath?.text == path.text) namePath else null
                        }
                    }.distinct()

                if (superTypes.isNotEmpty()) {
                    val upBuilder = NavigationGutterIconBuilder.create(AllIcons.Gutter.OverridingMethod)
                        .setTargets(superTypes)
                        .setTooltipText("Наследует/Использует типы: " + superTypes.joinToString { it.text })
                    // Вешаем на leafElement!
                    result.add(upBuilder.createLineMarkerInfo(leafElement))
                }
            }
        }

        // 2. Наследники
        val allDecls = PsiTreeUtil.findChildrenOfType(file, RazomyDeclaration::class.java)
        val subTypes = mutableListOf<PsiElement>()

        for (decl in allDecls) {
            val rightExpr = decl.children.getOrNull(1)
            if (rightExpr != null) {
                val pathsInRight = PsiTreeUtil.findChildrenOfType(rightExpr, RazomyPath::class.java)
                if (pathsInRight.any { it.text == currentName }) {
                    val declName = PsiTreeUtil.findChildOfType(decl, RazomyPath::class.java)
                    if (declName != null && declName != element) {
                        subTypes.add(declName)
                    }
                }
            }
        }

        if (subTypes.isNotEmpty()) {
            val downBuilder = NavigationGutterIconBuilder.create(AllIcons.Gutter.OverridenMethod)
                .setTargets(subTypes)
                .setTooltipText("Используется в: " + subTypes.joinToString { it.text })
            // Вешаем на leafElement!
            result.add(downBuilder.createLineMarkerInfo(leafElement))
        }
    }
}

// --- DOCUMENTATION ---

class RazomyDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val target = when (element) {
            is RazomyPath -> element
            else -> PsiTreeUtil.getParentOfType(originalElement, RazomyPath::class.java)
        } ?: return null

        val file = target.containingFile ?: return null

        val decl = if (RazomyUtil.isDeclaration(target)) {
            target
        } else {
            val all = PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java)
            all.firstOrNull { it.text == target.text && RazomyUtil.isDeclaration(it) }
        } ?: return null

        val container = PsiTreeUtil.getParentOfType(
            decl,
            RazomyDeclaration::class.java,
            RazomyFormItem::class.java,
            RazomySlotItem::class.java
        ) ?: decl
        val comments = collectPrecedingComments(container)

        val doc = StringBuilder()
        doc.append("<div style='font-family: monospace; padding: 4px;'>")
        doc.append("<span>Type:</span> <br/> <code>${escapeHtml(decl.text)}</code><br/>")

        var presentationText = container.text
        if (presentationText.length > 120) presentationText = presentationText.substring(0, 120) + "..."
        doc.append("<span>Declaration:</span> <br/> <code>${escapeHtml(presentationText)}</code>")
        doc.append("</div>")

        if (comments.isNotBlank()) {
            doc.append("<hr/><div style='color: #666; font-style: italic;'>")
            doc.append(comments.replace("\n", "<br/>"))
            doc.append("</div>")
        }

        return doc.toString()
    }

    private fun collectPrecedingComments(element: PsiElement): String {
        val commentLines = mutableListOf<String>()
        var prev = PsiTreeUtil.prevLeaf(element)

        while (prev is PsiWhiteSpace) {
            prev = PsiTreeUtil.prevLeaf(prev)
        }

        while (prev != null) {
            if (prev.node.elementType == RazomyTypes.LINE_COMMENT) {
                val cleanLine = prev.text.removePrefix("//").trim()
                commentLines.add(0, escapeHtml(cleanLine))
            } else if (prev.node.elementType == RazomyTypes.CRLF2 || prev is PsiWhiteSpace) {
                // skip
            } else {
                break
            }
            prev = PsiTreeUtil.prevLeaf(prev)
        }
        return commentLines.joinToString("\n")
    }

    private fun escapeHtml(text: String) = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}


// --- STRUCTURE VIEW ---

class RazomyStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder {
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?) = RazomyStructureViewModel(psiFile)
        }
    }
}

class RazomyStructureViewModel(file: PsiFile) : StructureViewModelBase(file, null, RazomyStructureElement(file)),
    ElementInfoProvider {
    override fun isAlwaysShowsPlus(element: StructureViewTreeElement) = false
    override fun isAlwaysLeaf(element: StructureViewTreeElement) = false
}

class RazomyStructureElement(private val element: PsiElement) : StructureViewTreeElement, ItemPresentation {
    override fun getValue() = element
    override fun navigate(requestFocus: Boolean) =
        (element as? com.intellij.pom.Navigatable)?.navigate(requestFocus) ?: Unit

    override fun canNavigate() = (element as? com.intellij.pom.Navigatable)?.canNavigate() ?: false
    override fun canNavigateToSource() = (element as? com.intellij.pom.Navigatable)?.canNavigateToSource() ?: false

    override fun getPresentation() = this
    override fun getPresentableText() = if (element is PsiFile) element.name else element.text
    override fun getIcon(unused: Boolean): Icon {
        if (element.text == "_") return AllIcons.Nodes.Field
        return if (element.text.firstOrNull()?.isUpperCase() == true) AllIcons.Nodes.Class else AllIcons.Nodes.Field
    }

    override fun getChildren(): Array<TreeElement> {
        // Рекурсивное построение красивого дерева: Файл -> Декларации -> FormItem'ы
        if (element is PsiFile) {
            val decls = element.children.filterIsInstance<RazomyDeclaration>()
            return decls.mapNotNull { decl ->
                val firstExpr = decl.children.firstOrNull()
                val path = PsiTreeUtil.findChildOfType(firstExpr, RazomyPath::class.java)
                if (path != null && path.textOffset == firstExpr?.textOffset) RazomyStructureElement(path) else null
            }.toTypedArray()
        } else if (element is RazomyPath) {
            val decl = PsiTreeUtil.getParentOfType(element, RazomyDeclaration::class.java, RazomyFormItem::class.java)
            if (decl != null && decl.children.firstOrNull()?.textOffset == element.textOffset) {
                val rightExpr = decl.children.getOrNull(1)
                val formBlock = PsiTreeUtil.findChildOfType(rightExpr, RazomyFormBlock::class.java)
                if (formBlock != null) {
                    val formItems = formBlock.children.filterIsInstance<RazomyFormItem>()
                    return formItems.mapNotNull { it.children.firstOrNull() }
                        .mapNotNull { PsiTreeUtil.findChildOfType(it, RazomyPath::class.java) }
                        .filter { RazomyUtil.isDeclaration(it) }
                        .map { RazomyStructureElement(it) }
                        .toTypedArray()
                }
            }
        }
        return emptyArray()
    }
}


// --- FORMATTER ---

class RazomyFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val block = RazomyBlock(
            formattingContext.node,
            Wrap.createWrap(WrapType.NONE, false),
            Alignment.createAlignment()
        )
        return FormattingModelProvider.createFormattingModelForPsiFile(
            formattingContext.containingFile,
            block,
            formattingContext.codeStyleSettings
        )
    }
}

class RazomyBlock(
    private val myNode: ASTNode,
    private val myWrap: Wrap?,
    private val myAlignment: Alignment?
) : ASTBlock {

    override fun getNode() = myNode
    override fun getTextRange() = myNode.textRange
    override fun getWrap() = myWrap
    override fun getAlignment() = myAlignment

    override fun getSubBlocks(): List<Block> {
        val blocks = mutableListOf<Block>()
        var child = myNode.firstChildNode
        while (child != null) {
            if (child.elementType != TokenType.WHITE_SPACE &&
                child.elementType != RazomyTypes.CRLF2 &&
                child.textRange.length > 0
            ) {
                blocks.add(RazomyBlock(child, Wrap.createWrap(WrapType.NONE, false), null))
            }
            child = child.treeNext
        }
        return blocks
    }

    override fun getIndent(): Indent? {
        val parentType = myNode.treeParent?.elementType
        val groups = listOf(RazomyTypes.FLOW_BLOCK, RazomyTypes.FORM_BLOCK, RazomyTypes.SLOT)

        // Делаем отступ только для элементов внутри блоков {...}, [...] или (...)
        if (parentType in groups &&
            myNode.elementType != RazomyTypes.LBRACE && myNode.elementType != RazomyTypes.RBRACE &&
            myNode.elementType != RazomyTypes.LBRACKET && myNode.elementType != RazomyTypes.RBRACKET &&
            myNode.elementType != RazomyTypes.LPAREN && myNode.elementType != RazomyTypes.RPAREN
        ) {
            return Indent.getNormalIndent()
        }
        return Indent.getNoneIndent()
    }

    override fun getSpacing(child1: Block?, child2: Block): Spacing? {
        return null
    }

    // ИСПРАВЛЕНИЕ БАГА:
    // Добавляем отступ только если мы действительно внутри незакрытого блока скобок.
    // На верхнем уровне файла или в обычных выражениях возвращаем NoneIndent.
    override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        val blockTypes = listOf(RazomyTypes.FLOW_BLOCK, RazomyTypes.FORM_BLOCK, RazomyTypes.SLOT)
        if (myNode.elementType in blockTypes) {
            return ChildAttributes(Indent.getNormalIndent(), null)
        }
        return ChildAttributes(Indent.getNoneIndent(), null)
    }

    override fun isIncomplete(): Boolean {
        val lastChild = myNode.lastChildNode
        return when (myNode.elementType) {
            RazomyTypes.FORM_BLOCK -> lastChild?.elementType != RazomyTypes.RBRACE
            RazomyTypes.FLOW_BLOCK -> lastChild?.elementType != RazomyTypes.RBRACKET
            RazomyTypes.SLOT -> lastChild?.elementType != RazomyTypes.RPAREN
            else -> false
        }
    }

    override fun isLeaf() = myNode.firstChildNode == null
}

// --- INSPECTIONS ---

class RazomyDuplicateDeclarationInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is RazomyPath && RazomyUtil.isDeclaration(element)) {
                    val name = element.text
                    val file = element.containingFile
                    val myScope = PsiTreeUtil.getParentOfType(element, RazomyFormBlock::class.java)

                    val duplicate = PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java).firstOrNull { other ->
                        if (other != element && other.text == name && RazomyUtil.isDeclaration(other)) {
                            val otherScope = PsiTreeUtil.getParentOfType(other, RazomyFormBlock::class.java)
                            otherScope == myScope && other.textOffset < element.textOffset
                        } else false
                    }

                    if (duplicate != null) {
                        holder.registerProblem(
                            element,
                            "Имя '$name' уже объявлено в этой области видимости",
                            ProblemHighlightType.GENERIC_ERROR
                        )
                    }
                }
            }
        }
    }
}

class RazomyUnusedInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is RazomyPath && RazomyUtil.isDeclaration(element) && element.text != "_" && !element.text.startsWith(
                        "_."
                    )
                ) {
                    val name = element.text
                    val file = element.containingFile
                    val allPaths = PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java)

                    val isUsed = allPaths.any { path ->
                        if (path == element || RazomyUtil.isDeclaration(path)) return@any false
                        val pathText = path.text
                        pathText == name || pathText.startsWith("$name.") || pathText.split('.').contains(name)
                    }

                    if (!isUsed) {
                        holder.registerProblem(
                            element,
                            "Сущность '$name' нигде не используется",
                            ProblemHighlightType.LIKE_UNUSED_SYMBOL
                        )
                    }
                }
            }
        }
    }
}

// --- COMPLETION ---

class RazomyCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val position = parameters.position
                    val currentPath = PsiTreeUtil.getParentOfType(position, RazomyPath::class.java)

                    // 1. КОНТЕКСТ ПОСЛЕ ТОЧКИ (Например: User.|PasswordHash)
                    if (currentPath != null) {
                        val fullText = currentPath.text
                        // Вычисляем, где именно стоит курсор относительно начала слова
                        val caretOffset = (parameters.offset - currentPath.textRange.startOffset)
                            .coerceIn(0, fullText.length)

                        val textBeforeCaret = fullText.substring(0, caretOffset).replace("IntellijIdeaRulezzz", "")

                        if (textBeforeCaret.contains(".")) {
                            val prefix = textBeforeCaret.substringBeforeLast(".") // Получаем "User"
                            val resolvedPrefix = RazomyResolver.resolvePath(currentPath, prefix)

                            if (resolvedPrefix != null) {
                                val type = RazomyTypeSystem.inferTypeOfElement(resolvedPrefix)
                                val fields = RazomyType.extractFields(type)

                                for ((name, _) in fields) {
                                    result.addElement(
                                        LookupElementBuilder.create(name)
                                            .withIcon(AllIcons.Nodes.Field)
                                            .withTypeText("Field of $prefix")
                                    )
                                }
                            }
                            // ВАЖНО: Прерываем выполнение!
                            // Если мы после точки, глобальные сущности предлагать НЕЛЬЗЯ.
                            return
                        }
                    }

                    // 2. ГЛОБАЛЬНЫЙ И ЛОКАЛЬНЫЙ КОНТЕКСТ (Если точки нет)
                    val scope = RazomyTypeSystem.buildScope(position)

                    scope.variables.keys.forEach {
                        val isType = it.firstOrNull()?.isUpperCase() == true
                        val icon = if (isType) AllIcons.Nodes.Class else AllIcons.Nodes.Variable
                        result.addElement(
                            LookupElementBuilder.create(it)
                                .withIcon(icon)
                                .withTypeText(if (isType) "Type" else "Variable")
                        )
                    }

                    scope.unpackedFields.keys.forEach {
                        result.addElement(
                            LookupElementBuilder.create(it)
                                .withIcon(AllIcons.Nodes.Field)
                                .withTypeText("Auto-unpacked")
                        )
                    }
                }
            }
        )
    }
}

// --- BRACE MATCHER, FOLDING, COMMENTER ---

class RazomyBraceMatcher : PairedBraceMatcher {
    private val PAIRS = arrayOf(
        BracePair(RazomyTypes.LBRACE, RazomyTypes.RBRACE, true),
        BracePair(RazomyTypes.LPAREN, RazomyTypes.RPAREN, false),
        BracePair(RazomyTypes.LBRACKET, RazomyTypes.RBRACKET, false)
    )

    override fun getPairs(): Array<BracePair> = PAIRS
    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true
    override fun getCodeConstructStart(file: PsiFile?, openingOffset: Int): Int = openingOffset
}

class RazomyFoldingBuilder : FoldingBuilderEx(), DumbAware {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()

        val formBlocks = PsiTreeUtil.findChildrenOfType(root, RazomyFormBlock::class.java)
        for (group in formBlocks) {
            if (group.textRange.length > 2) descriptors.add(FoldingDescriptor(group.node, group.textRange))
        }

        val flowBlocks = PsiTreeUtil.findChildrenOfType(root, RazomyFlowBlock::class.java)
        for (group in flowBlocks) {
            if (group.textRange.length > 2) descriptors.add(FoldingDescriptor(group.node, group.textRange))
        }

        val slots = PsiTreeUtil.findChildrenOfType(root, RazomySlot::class.java)
        for (group in slots) {
            if (group.textRange.length > 2 && group.text.contains('\n')) descriptors.add(
                FoldingDescriptor(
                    group.node,
                    group.textRange
                )
            )
        }

        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String {
        return when (node.elementType) {
            RazomyTypes.FORM_BLOCK -> "{...}"
            RazomyTypes.FLOW_BLOCK -> "[...]"
            RazomyTypes.SLOT -> "(...)"
            else -> "..."
        }
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}

class RazomyCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "//"
    override fun getBlockCommentPrefix(): String? = null
    override fun getBlockCommentSuffix(): String? = null
    override fun getCommentedBlockCommentPrefix(): String? = null
    override fun getCommentedBlockCommentSuffix(): String? = null
}

// --- INLAY HINTS (Умные подсказки типов/параметров) ---

class RazomyInlayParameterHintsProvider : InlayParameterHintsProvider {

    override fun getParameterHints(element: PsiElement): MutableList<InlayInfo> {
        val hints = mutableListOf<InlayInfo>()

        // Ищем вызовы вида: Path ( Slot )
        if (element !is RazomyCallExpr) return hints

        val path = PsiTreeUtil.getChildOfType(element, RazomyPath::class.java) ?: return hints
        val slot = PsiTreeUtil.getChildOfType(element, RazomySlot::class.java) ?: return hints
        val providedArgs = PsiTreeUtil.findChildrenOfType(slot, RazomySlotItem::class.java).toList()

        if (providedArgs.isEmpty()) return hints

        // 1. НАДЕЖНОЕ РАЗРЕШЕНИЕ (Фоллбэк на случай сбоев Reference)
        var targetPath = path.reference?.resolve() as? RazomyPath
        if (targetPath == null) {
            val allPaths = PsiTreeUtil.findChildrenOfType(element.containingFile, RazomyPath::class.java)
            targetPath = allPaths.firstOrNull { it != path && it.text == path.text && RazomyUtil.isDeclaration(it) }
        }

        if (targetPath == null) return hints

        val expectedNames = mutableListOf<String>()

        // 2. СЦЕНАРИЙ А: Вызов Формы (Структуры) -> User { Id Str ... }
        val typedForm = PsiTreeUtil.getParentOfType(targetPath, RazomyTypedFormBlock::class.java)
        val formBlock = typedForm?.let { PsiTreeUtil.findChildOfType(it, RazomyFormBlock::class.java) }
            ?: PsiTreeUtil.getParentOfType(targetPath, RazomyDeclaration::class.java)
                ?.let { PsiTreeUtil.findChildOfType(it, RazomyFormBlock::class.java) }

        if (formBlock != null) {
            // Собираем первые пути (поля) из всех FormItem внутри
            val fields = PsiTreeUtil.findChildrenOfType(formBlock, RazomyFormItem::class.java)
                .mapNotNull { PsiTreeUtil.findChildOfType(it, RazomyPath::class.java)?.text }
            expectedNames.addAll(fields)
        }
        // 3. СЦЕНАРИЙ Б: Вызов Функции с параметрами -> AgentLoop(Err, Retries) [ ... ]
        else {
            val targetCall = PsiTreeUtil.getParentOfType(targetPath, RazomyCallExpr::class.java)
            // Убеждаемся, что targetPath — это именно имя функции (AgentLoop), а не что-то внутри
            if (targetCall != null && targetCall.textOffset == targetPath.textOffset) {
                val declSlot = PsiTreeUtil.getChildOfType(targetCall, RazomySlot::class.java)
                if (declSlot != null) {
                    // Собираем параметры слота (Err, Retries)
                    val params = PsiTreeUtil.findChildrenOfType(declSlot, RazomySlotItem::class.java)
                        .mapNotNull { PsiTreeUtil.findChildOfType(it, RazomyPath::class.java)?.text }
                    expectedNames.addAll(params)
                }
            }
        }

        // 4. Генерируем подсказки перед аргументами
        for (i in providedArgs.indices) {
            if (i < expectedNames.size) {
                hints.add(InlayInfo("${expectedNames[i]}:", providedArgs[i].textOffset))
            }
        }

        return hints
    }

    override fun getDefaultBlackList(): MutableSet<String> = mutableSetOf()
    override fun getHintInfo(element: PsiElement): com.intellij.codeInsight.hints.HintInfo? = null
}


// --- LANGUAGE & FILE TYPE ---

// --- СИСТЕМА ТИПОВ И СЕМАНТИЧЕСКИЙ РЕЗОЛВЕР (Razomy Type System V2.6 - Modules Edition) ---


// --- GOTO DECLARATION ---

class RazomyGotoDeclarationHandler : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor?
    ): Array<PsiElement>? {
        if (sourceElement == null) return null
        val word = sourceElement.text
        if (word.isBlank() || word == "_") return null

        val currentPath = PsiTreeUtil.getParentOfType(sourceElement, RazomyPath::class.java) ?: return null
        val file = sourceElement.containingFile ?: return null

        // 1. Если кликаем по файловому пути в импорте (правая часть)
        val parentDecl = currentPath.parent?.parent as? RazomyDeclaration
        if (parentDecl != null) {
            val rightExpr = parentDecl.children.getOrNull(1)
            val rightPath = PsiTreeUtil.findChildOfType(rightExpr, RazomyPath::class.java)
            if (currentPath == rightPath && rightPath.textOffset == rightExpr?.textOffset && RazomyUtil.isImportPath(
                    currentPath.text
                )
            ) {
                val fileTarget = RazomyImportResolver.resolveImportedFile(sourceElement, currentPath.text)
                if (fileTarget != null) return arrayOf(fileTarget)
                return null
            }
        }

        // 2. Если кликаем по самому определению
        if (RazomyUtil.isDeclaration(currentPath)) {
            val targets = findUsages(currentPath, file)
            return if (targets.isNotEmpty()) targets.toTypedArray() else null
        }

        // 3. Стандартный семантический резолв
        val resolved = RazomyResolver.resolvePath(currentPath, word)
            ?: RazomyResolver.resolvePath(currentPath, currentPath.text)
            ?: resolveLocalDeclaration(currentPath, word, file)

        return if (resolved != null) arrayOf(resolved) else null
    }

    private fun findUsages(element: RazomyPath, file: PsiFile): List<PsiElement> {
        val currentName = element.text
        val results = LinkedHashSet<PsiElement>()
        val allPaths = PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java)
        for (path in allPaths) {
            if (path != element && matchesName(path.text, currentName)) {
                results.add(path)
            }
        }
        return results.toList()
    }

    private fun resolveLocalDeclaration(from: RazomyPath, name: String, file: PsiFile): RazomyPath? {
        var scope = from.parent
        while (scope != null) {
            if (scope is RazomyFormBlock || scope is RazomyFlowBlock) {
                val parentDecl =
                    PsiTreeUtil.getParentOfType(scope, RazomyDeclaration::class.java, RazomyFormItem::class.java)
                val leftExpr = parentDecl?.children?.firstOrNull()
                val slot = PsiTreeUtil.findChildOfType(leftExpr, RazomySlot::class.java)
                if (slot != null) {
                    val slotItems = PsiTreeUtil.findChildrenOfType(slot, RazomySlotItem::class.java)
                    for (item in slotItems) {
                        val firstExpr = item.children.firstOrNull()
                        val paramPath = PsiTreeUtil.findChildOfType(firstExpr, RazomyPath::class.java)
                        if (paramPath != null && paramPath.textOffset == firstExpr?.textOffset && matchesName(
                                paramPath.text,
                                name
                            )
                        ) {
                            return paramPath
                        }
                    }
                }
            }
            scope = scope.parent
        }
        return null
    }

    private fun matchesName(declarationText: String, searchName: String): Boolean {
        return declarationText == searchName || declarationText.endsWith(".$searchName") || searchName.endsWith(".$declarationText")
    }
}


object RazomyIcons {
    val FILE: Icon = IconLoader.getIcon("icons/rl.svg", RazomyIcons::class.java)
}

class RazomyLanguage : Language("RL") {
    companion object {
        val INSTANCE = RazomyLanguage()
    }
}

class RazomyFileType : LanguageFileType(RazomyLanguage.INSTANCE) {
    override fun getName() = "Razomy File"
    override fun getDescription() = "Razomy language file"
    override fun getDefaultExtension() = "rl"
    override fun getIcon() = RazomyIcons.FILE

    companion object {
        val INSTANCE = RazomyFileType()
    }
}

// --- UTILITY (THE CORE ENGINE) ---

object RazomyUtil {
    fun isDeclaration(path: RazomyPath): Boolean {
        if (path.text == "_" || path.text.startsWith("_.")) return false

        val parentItem = PsiTreeUtil.getParentOfType(
            path,
            RazomyDeclaration::class.java,
            RazomyFormItem::class.java,
            RazomySlotItem::class.java
        ) ?: return false

        val firstPath = PsiTreeUtil.findChildOfType(parentItem, RazomyPath::class.java)
        return firstPath == path
    }

    fun isImportPath(text: String): Boolean {
        if (text.isBlank() || text == "_") return false
        return text.first().isLowerCase()
    }

    enum class ImportContext { RIGHT_PATH, LEFT_ALIAS_OR_DESTRUCT }

    fun getImportContext(path: RazomyPath): ImportContext? {
        val parentExpr = path.parent as? RazomyExpr ?: return null
        val decl = parentExpr.parent as? RazomyDeclaration ?: return null
        val leftExpr = decl.children.firstOrNull()
        val rightExpr = decl.children.getOrNull(1)

        if (rightExpr != null && PsiTreeUtil.isAncestor(rightExpr, path, false)) {
            return ImportContext.RIGHT_PATH
        }
        if (leftExpr != null && PsiTreeUtil.isAncestor(leftExpr, path, false)) {
            val rightPath = PsiTreeUtil.findChildOfType(rightExpr, RazomyPath::class.java)
            if (rightPath != null && isImportPath(rightPath.text)) {
                return ImportContext.LEFT_ALIAS_OR_DESTRUCT
            }
        }
        return null
    }
}

// --- СИСТЕМА ИМПОРТОВ И РЕЗОЛВА МОДУЛЕЙ ---

object RazomyImportResolver {
    fun resolveImportedFile(context: PsiElement, importPath: String): PsiFile? {
        val project = context.project
        val fileName = importPath.substringAfterLast('.') + ".rl" // например, base.rl
        val expectedPathFragment = importPath.replace('.', '/') + ".rl" // test/base.rl

        val virtualFiles = FilenameIndex.getVirtualFilesByName(fileName, GlobalSearchScope.projectScope(project))

        // 1. Пытаемся найти точное совпадение по хвосту пути
        val exactMatch = virtualFiles.firstOrNull { it.path.replace('\\', '/').endsWith(expectedPathFragment) }
        if (exactMatch != null) {
            return PsiManager.getInstance(project).findFile(exactMatch)
        }

        // 2. МЯГКИЙ ФОЛЛБЭК: Если файл с таким именем (base.rl) всего один в проекте,
        // 100% это он, даже если структура папок немного отличается.
        if (virtualFiles.size == 1) {
            return PsiManager.getInstance(project).findFile(virtualFiles.first())
        }

        return null
    }

    fun getExportedSymbols(file: PsiFile): Set<String> {
        val exports = mutableSetOf<String>()
        val globalDecls = PsiTreeUtil.findChildrenOfType(file, RazomyDeclaration::class.java)
        for (decl in globalDecls) {
            val leftExpr = decl.children.firstOrNull()
            val path = PsiTreeUtil.findChildOfType(leftExpr, RazomyPath::class.java)
            if (path != null && path.textOffset == leftExpr?.textOffset) {
                exports.add(path.text)
            }
        }
        return exports
    }

    fun findExportedSymbol(file: PsiFile, name: String): PsiElement? {
        val globalDecls = PsiTreeUtil.findChildrenOfType(file, RazomyDeclaration::class.java)
        for (decl in globalDecls) {
            val leftExpr = decl.children.firstOrNull()
            val path = PsiTreeUtil.findChildOfType(leftExpr, RazomyPath::class.java)
            if (path != null && path.text == name && path.textOffset == leftExpr?.textOffset) {
                return path
            }
        }
        return null
    }

    fun findModuleByExportedSymbol(project: Project, symbolName: String): List<String> {
        val result = mutableListOf<String>()

        // МГНОВЕННЫЙ ПОИСК ПО ИНДЕКСУ! IDE выдаст файлы за миллисекунды.
        val virtualFiles = FileBasedIndex.getInstance().getContainingFiles(
            RazomyIndexKeys.EXPORTED_SYMBOLS,
            symbolName,
            GlobalSearchScope.projectScope(project)
        )

        for (vf in virtualFiles) {
            val sourceRoot = ProjectRootManager.getInstance(project).fileIndex.getSourceRootForFile(vf)
            var relPath = if (sourceRoot != null) {
                VfsUtilCore.getRelativePath(vf, sourceRoot) ?: vf.name
            } else {
                vf.name
            }
            relPath = relPath.removeSuffix(".rl").replace('/', '.')
            result.add(relPath)
        }

        return result.distinct()
    }
}

// --- AUTO-IMPORT QUICK FIX ---

// --- AUTO-IMPORT QUICK FIX ---

public class RazomyImportQuickFix(private val symbolName: String, private val modulePath: String) : LocalQuickFix,
    IntentionAction {

    override fun getFamilyName(): String = "Импортировать"

    override fun getName(): String = "Импортировать '$symbolName' из $modulePath"

    // IntentionAction использует getText для отображения в меню (Alt+Enter)
    override fun getText(): @IntentionName String = name

    // Важно: возвращаем true, так как модификация AST должна происходить в режиме записи (Write-Action)
    override fun startInWriteAction(): Boolean = true

    // Проверяем, доступен ли QuickFix. Достаточно проверить, что мы работаем с нужным файлом.
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return file != null && file.language == RazomyLanguage.INSTANCE && file.manager.isInProject(file)
    }

    // Метод от LocalQuickFix (когда вызывается из Inspection)
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val file = descriptor.psiElement.containingFile ?: return
        invoke(project, null, file)
    }

    // Метод от IntentionAction (когда вызывается вручную пользователем через Alt+Enter)
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (file == null) return

        val factory = PsiFileFactory.getInstance(project)

        // Создаем кусочек AST с импортом
        val dummyContent = "{ $symbolName } $modulePath\n"
        val dummyFile = factory.createFileFromText("dummy.rl", RazomyFileType.INSTANCE, dummyContent)

        // Находим первую декларацию, чтобы вставить импорт перед ней
        val firstDecl = PsiTreeUtil.findChildOfType(file, RazomyDeclaration::class.java)

        if (firstDecl != null) {
            // Вставляем все токены из dummy перед первой декларацией (чтобы не терять перенос строки)
            for (child in dummyFile.children) {
                file.addBefore(child, firstDecl)
            }
        } else {
            // Если файл пуст (нет деклараций), просто добавляем
            for (child in dummyFile.children) {
                file.add(child)
            }
        }
    }
}
// --- СИСТЕМА ТИПОВ И СЕМАНТИЧЕСКИЙ РЕЗОЛВЕР (Razomy Type System V2.7 - Wildcard Edition) ---

object RazomyTypeSystem {
    private val LOG = Logger.getInstance("RazomyTypeSystem")

    fun inferTypeByName(name: String, context: PsiElement): RazomyType {
        val file = context.containingFile ?: return RazomyType.Unknown
        val globalDecls = PsiTreeUtil.findChildrenOfType(file, RazomyDeclaration::class.java)

        for (decl in globalDecls) {
            val leftExpr = decl.children.firstOrNull()
            val rightExpr = decl.children.getOrNull(1)

            val leftPath = PsiTreeUtil.findChildOfType(leftExpr, RazomyPath::class.java)
            val rightPath = PsiTreeUtil.findChildOfType(rightExpr, RazomyPath::class.java)
            val leftForm = PsiTreeUtil.findChildOfType(leftExpr, RazomyFormBlock::class.java)

            if (rightPath != null && rightPath.textOffset == rightExpr?.textOffset && RazomyUtil.isImportPath(rightPath.text)) {
                // Wildcard import: _ tests.base
                if (leftPath != null && leftPath.text == "_" && leftPath.textOffset == leftExpr?.textOffset) {
                    val importedFile = RazomyImportResolver.resolveImportedFile(context, rightPath.text)
                    if (importedFile != null) {
                        val foreignSymbol = RazomyImportResolver.findExportedSymbol(importedFile, name)
                        if (foreignSymbol != null) return inferTypeOfElement(foreignSymbol)
                    }
                }
                // Alias import: Age package.age
                else if (leftPath != null && leftPath.text == name && leftPath.textOffset == leftExpr?.textOffset) {
                    val importedFile = RazomyImportResolver.resolveImportedFile(context, rightPath.text)
                    return if (importedFile != null) RazomyType.Module(importedFile) else RazomyType.Unknown
                }
                // Destructured import: { Old } package.age
                else if (leftForm != null && leftForm.textOffset == leftExpr?.textOffset) {
                    val items = PsiTreeUtil.findChildrenOfType(leftForm, RazomyFormItem::class.java)
                    if (items.any { PsiTreeUtil.findChildOfType(it, RazomyPath::class.java)?.text == name }) {
                        val importedFile = RazomyImportResolver.resolveImportedFile(context, rightPath.text)
                        if (importedFile != null) {
                            val foreignSymbol = RazomyImportResolver.findExportedSymbol(importedFile, name)
                            if (foreignSymbol != null) return inferTypeOfElement(foreignSymbol)
                        }
                        return RazomyType.Unknown
                    }
                }
            }
        }

        // 2. Локальный поиск по файлу
        for (decl in globalDecls) {
            val leftExpr = decl.children.firstOrNull()
            val path = PsiTreeUtil.findChildOfType(leftExpr, RazomyPath::class.java)
            if (path?.text == name && path.textOffset == leftExpr?.textOffset) {
                val formBlock = PsiTreeUtil.findChildOfType(decl, RazomyFormBlock::class.java)
                if (formBlock != null) {
                    val fields = mutableMapOf<String, PsiElement>()
                    val items = PsiTreeUtil.findChildrenOfType(formBlock, RazomyFormItem::class.java)
                    for (item in items) {
                        val fieldPath = PsiTreeUtil.findChildOfType(item, RazomyPath::class.java)
                        if (fieldPath != null) fields[fieldPath.text] = fieldPath
                    }
                    return RazomyType.Form(name, path, fields)
                }

                val allPaths = PsiTreeUtil.findChildrenOfType(decl, RazomyPath::class.java).toList()
                val baseTypePath = allPaths.drop(1).firstOrNull { it.text.firstOrNull()?.isUpperCase() == true }
                if (baseTypePath != null) return inferTypeByName(baseTypePath.text, context)
            }
        }
        return RazomyType.Unknown
    }

    fun inferTypeOfElement(element: PsiElement): RazomyType {
        if (element is RazomyPath) {
            val slotItem = PsiTreeUtil.getParentOfType(element, RazomySlotItem::class.java)
            if (slotItem != null) {
                val paths = PsiTreeUtil.findChildrenOfType(slotItem, RazomyPath::class.java).toList()
                if (paths.size > 1 && paths[0] == element) return inferTypeByName(paths[1].text, element)

                val callExpr = PsiTreeUtil.getParentOfType(slotItem, RazomyCallExpr::class.java)
                if (callExpr != null) {
                    val callerPath = PsiTreeUtil.getChildOfType(callExpr, RazomyPath::class.java)
                    if (callerPath != null && callerPath.text.firstOrNull()?.isUpperCase() == true) {
                        return inferTypeByName(callerPath.text, element)
                    }
                }
            }
        }

        val text = element.text
        if (text.firstOrNull()?.isUpperCase() == true) {
            val resolvedType = inferTypeByName(text, element)
            if (resolvedType !is RazomyType.Unknown) return resolvedType
        }
        return RazomyType.Unknown
    }

    fun buildScope(element: PsiElement): RazomyScope {
        val scope = RazomyScope()
        var current: PsiElement? = element.parent

        while (current != null) {
            if (current is RazomyFormItem) {
                val firstPath = PsiTreeUtil.findChildOfType(current, RazomyPath::class.java)
                if (firstPath != null && firstPath.text.firstOrNull()?.isUpperCase() == true) {
                    val resolvedType = inferTypeByName(firstPath.text, element)
                    scope.unpackedFields.putAll(RazomyType.extractFields(resolvedType))
                }
            }
            if (current is RazomyFormBlock) {
                val items = PsiTreeUtil.findChildrenOfType(current, RazomyFormItem::class.java)
                for (item in items) {
                    val fieldPath = PsiTreeUtil.findChildOfType(item, RazomyPath::class.java)
                    if (fieldPath != null) scope.variables[fieldPath.text] = fieldPath
                }
            }
            if (current is RazomyFlowBlock) {
                val precedingForms = PsiTreeUtil.findChildrenOfType(current, RazomyFormBlock::class.java)
                for (formBlock in precedingForms) {
                    if (formBlock.textOffset < element.textOffset) {
                        val items = PsiTreeUtil.findChildrenOfType(formBlock, RazomyFormItem::class.java)
                        for (item in items) {
                            val fieldPath = PsiTreeUtil.findChildOfType(item, RazomyPath::class.java)
                            if (fieldPath != null) scope.unpackedFields[fieldPath.text] = fieldPath
                        }
                    }
                }
            }
            if (current is RazomyDeclaration || current is RazomyFormItem || current is RazomyCallExpr) {
                val slot = PsiTreeUtil.findChildOfType(current, RazomySlot::class.java)
                if (slot != null && slot.textOffset < element.textOffset) {
                    val items = PsiTreeUtil.findChildrenOfType(slot, RazomySlotItem::class.java)
                    for (item in items) {
                        val paths = PsiTreeUtil.findChildrenOfType(item, RazomyPath::class.java).toList()
                        if (paths.isNotEmpty()) {
                            val varName = paths[0]
                            scope.variables[varName.text] = varName

                            val resolvedType = inferTypeOfElement(varName)
                            if (resolvedType !is RazomyType.Unknown) {
                                scope.unpackedFields.putAll(RazomyType.extractFields(resolvedType))
                            } else {
                                val fallbackType = inferTypeByName(varName.text, element)
                                scope.unpackedFields.putAll(RazomyType.extractFields(fallbackType))
                            }
                        }
                    }
                }
            }
            current = current.parent
        }

        // ГЛОБАЛЬНЫЙ СКОУП С ИМПОРТАМИ
        val file = element.containingFile
        if (file != null) {
            val globalDecls = PsiTreeUtil.findChildrenOfType(file, RazomyDeclaration::class.java)
            for (decl in globalDecls) {
                val leftExpr = decl.children.firstOrNull()
                val rightExpr = decl.children.getOrNull(1)

                val leftPath = PsiTreeUtil.findChildOfType(leftExpr, RazomyPath::class.java)
                val rightPath = PsiTreeUtil.findChildOfType(rightExpr, RazomyPath::class.java)
                val leftForm = PsiTreeUtil.findChildOfType(leftExpr, RazomyFormBlock::class.java)

                if (rightPath != null && rightPath.textOffset == rightExpr?.textOffset && RazomyUtil.isImportPath(
                        rightPath.text
                    )
                ) {

                    // Wildcard ( _ app.core )
                    if (leftPath != null && leftPath.text == "_" && leftPath.textOffset == leftExpr?.textOffset) {
                        val importedFile = RazomyImportResolver.resolveImportedFile(element, rightPath.text)
                        if (importedFile != null) {
                            val exports = RazomyImportResolver.getExportedSymbols(importedFile)
                            for (exportName in exports) {
                                scope.isDestructuredImport[exportName] = true
                                scope.imports[exportName] = rightPath
                            }
                        }
                    }
                    // Alias ( Core app.core )
                    else if (leftPath != null && leftPath.textOffset == leftExpr?.textOffset) {
                        val alias = leftPath.text
                        scope.variables[alias] = leftPath
                        scope.imports[alias] = rightPath
                    }
                    // Destructured ( { Str, Int } app.core )
                    else if (leftForm != null && leftForm.textOffset == leftExpr?.textOffset) {
                        val items = PsiTreeUtil.findChildrenOfType(leftForm, RazomyFormItem::class.java)
                        for (item in items) {
                            val path = PsiTreeUtil.findChildOfType(item, RazomyPath::class.java)
                            if (path != null) {
                                scope.variables[path.text] = path
                                scope.imports[path.text] = rightPath
                                scope.isDestructuredImport[path.text] = true
                            }
                        }
                    }
                } else {
                    if (leftPath != null && leftPath.textOffset == leftExpr?.textOffset) {
                        scope.variables[leftPath.text] = leftPath
                    }
                }
            }
        }
        return scope
    }
}

object RazomyResolver {
    private val LOG = Logger.getInstance("RazomyResolver")

    fun resolvePath(context: PsiElement, pathText: String): PsiElement? {
        if (pathText == "_" || pathText.startsWith("_.")) return null

        val cleanPath = pathText.replace("\\s+\\.\\s+".toRegex(), ".")
        val parts = cleanPath.split(".")
        if (parts.isEmpty()) return null

        val rootName = parts[0]
        val scope = RazomyTypeSystem.buildScope(context)

        var current: PsiElement? = scope.variables[rootName] ?: scope.unpackedFields[rootName]

        // Если корень — это импорт
        if (current != null && scope.imports.containsKey(rootName)) {
            val importPath = scope.imports[rootName]!!
            val importedFile = RazomyImportResolver.resolveImportedFile(context, importPath.text)

            if (importedFile != null) {
                if (scope.isDestructuredImport[rootName] == true) {
                    current = RazomyImportResolver.findExportedSymbol(importedFile, rootName) ?: current
                } else {
                    if (parts.size == 1) return importedFile
                    current = importedFile
                }
            }
        }

        // --- НОВЫЙ БЛОК НАЧАЛО ---
        if (current == null) {
            // 1. Wildcard фолбэк
            if (scope.isDestructuredImport.containsKey(rootName)) {
                val importPath = scope.imports[rootName]!!
                val importedFile = RazomyImportResolver.resolveImportedFile(context, importPath.text)
                if (importedFile != null) {
                    current = RazomyImportResolver.findExportedSymbol(importedFile, rootName)
                }
            }

            // 2. ФОЛЛБЭК ДЛЯ МОДУЛЕЙ (tests.base): Ищем как физический путь проекта
            if (current == null) {
                var fileRef: PsiFile? = null
                var memberStartIndex = 0
                // Идем с конца, чтобы найти максимально длинный путь файла
                for (i in parts.size downTo 1) {
                    val prefix = parts.take(i).joinToString(".")
                    val file = RazomyImportResolver.resolveImportedFile(context, prefix)
                    if (file != null) {
                        fileRef = file
                        memberStartIndex = i
                        break
                    }
                }

                if (fileRef != null) {
                    current = fileRef
                    // Если нашли файл, дорезолвливаем оставшиеся поля (например .User)
                    for (i in memberStartIndex until parts.size) {
                        val nextMember = parts[i]
                        current = resolveMember(current!!, nextMember)
                        if (current == null) return null
                    }
                    return current
                }
            }

            // Если ничего не подошло
            if (current == null) return null
        }
        // --- НОВЫЙ БЛОК КОНЕЦ ---

        // Резолв вложенных свойств
        for (i in 1 until parts.size) {
            val nextMember = parts[i]
            val resolvedMember = resolveMember(current!!, nextMember)
            if (resolvedMember == null) {
                LOG.warn("ОШИБКА: Поле '$nextMember' не найдено в '$rootName'")
                return null
            }
            current = resolvedMember
        }
        return current
    }

    private fun resolveMember(owner: PsiElement, memberName: String): PsiElement? {
        if (owner is PsiFile) {
            return RazomyImportResolver.findExportedSymbol(owner, memberName)
        }
        val type = RazomyTypeSystem.inferTypeOfElement(owner)
        val fields = RazomyType.extractFields(type)
        return fields[memberName]
    }
}

class RazomyScope(
    val variables: MutableMap<String, PsiElement> = mutableMapOf(),
    val unpackedFields: MutableMap<String, PsiElement> = mutableMapOf(),
    val imports: MutableMap<String, RazomyPath> = mutableMapOf(),
    val isDestructuredImport: MutableMap<String, Boolean> = mutableMapOf()
)

sealed class RazomyType {
    class Form(val name: String, val declaration: PsiElement, val fields: Map<String, PsiElement>) : RazomyType()
    class Module(val file: PsiFile) : RazomyType()
    class Union(val left: RazomyType, val right: RazomyType) : RazomyType()
    class Intersection(val left: RazomyType, val right: RazomyType) : RazomyType() {
        fun getResolvedFields(): Map<String, PsiElement> {
            val leftFields = extractFields(left)
            val rightFields = extractFields(right)
            val result = mutableMapOf<String, PsiElement>()
            result.putAll(leftFields.filterKeys { it !in rightFields.keys })
            result.putAll(rightFields.filterKeys { it !in leftFields.keys })
            return result
        }
    }

    object Unknown : RazomyType()

    companion object {
        fun extractFields(type: RazomyType): Map<String, PsiElement> {
            return when (type) {
                is Form -> type.fields
                is Intersection -> type.getResolvedFields()
                is Module -> {
                    val result = mutableMapOf<String, PsiElement>()
                    val globalDecls = PsiTreeUtil.findChildrenOfType(type.file, RazomyDeclaration::class.java)
                    for (decl in globalDecls) {
                        val leftExpr = decl.children.firstOrNull()
                        val path = PsiTreeUtil.findChildOfType(leftExpr, RazomyPath::class.java)
                        if (path != null && path.textOffset == leftExpr?.textOffset) {
                            result[path.text] = path
                        }
                    }
                    result
                }

                else -> emptyMap()
            }
        }
    }
}


// --- ANNOTATOR (Подсветка синтаксиса и ошибок) ---

class RazomyAnnotator : Annotator {

    companion object {
        private val ESCAPE_PATTERN = Regex("""\\[ntr\"'\\{}]""")
        private val INTERPOLATION_PATTERN = Regex("""(?<!\\)\{([^}]+)\}""")

        val COLOR_MODULE = DefaultLanguageHighlighterColors.CLASS_NAME
        val COLOR_TYPE = DefaultLanguageHighlighterColors.CLASS_REFERENCE
        val COLOR_FUNCTION_DEF = DefaultLanguageHighlighterColors.FUNCTION_DECLARATION
        val COLOR_METHOD_CALL = DefaultLanguageHighlighterColors.STATIC_METHOD
        val COLOR_FIELD = DefaultLanguageHighlighterColors.INSTANCE_FIELD
        val COLOR_PARAM = DefaultLanguageHighlighterColors.PARAMETER
        val COLOR_VARIABLE = DefaultLanguageHighlighterColors.LOCAL_VARIABLE
        val COLOR_NATIVE = DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL
        val COLOR_IMPORT = DefaultLanguageHighlighterColors.STRING
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element.node.elementType == RazomyTypes.STRING || element.textContains('\"') || element.textContains('\'')) {
            if (element.node.elementType == RazomyTypes.STRING) annotateString(element, holder)
            return
        }

        if (element !is RazomyPath) return
        val text = element.text.trim()
        if (text.isEmpty() || text.endsWith(".") || text.contains("IntellijIdeaRulezzz")) return

        val importCtx = RazomyUtil.getImportContext(element)
        if (importCtx == RazomyUtil.ImportContext.RIGHT_PATH) {
            val file = RazomyImportResolver.resolveImportedFile(element, text)
            if (file == null) {
                holder.newAnnotation(HighlightSeverity.ERROR, "Файл '$text.rl' не найден")
                    .range(element.textRange)
                    .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                    .create()
            } else {
                highlight(element, holder, COLOR_IMPORT)
            }
            return
        } else if (importCtx == RazomyUtil.ImportContext.LEFT_ALIAS_OR_DESTRUCT) {
            if (text == "_") {
                highlight(element, holder, DefaultLanguageHighlighterColors.KEYWORD)
            } else if (text.firstOrNull()?.isUpperCase() == true) {
                highlight(element, holder, COLOR_TYPE)
            } else {
                highlight(element, holder, COLOR_MODULE)
            }
            return
        }

        if (text == "_" || text.startsWith("_.")) {
            highlight(element, holder, DefaultLanguageHighlighterColors.KEYWORD)
            return
        }

        if (text.startsWith("native_") || text.contains("native_")) {
            highlight(element, holder, COLOR_NATIVE)
            return
        }

        val isDecl = RazomyUtil.isDeclaration(element)
        val isMemberAccess = text.contains(".")

        if (!isDecl) {
            val resolved = RazomyResolver.resolvePath(element, text)
            val scope = RazomyTypeSystem.buildScope(element)
            val isResolvedLocally =
                !isMemberAccess && (scope.variables.containsKey(text) || scope.unpackedFields.containsKey(text) || scope.isDestructuredImport.containsKey(
                    text
                ))

            if (resolved == null && !isResolvedLocally) {
                val unresolvedName = if (isMemberAccess) text.split(".").last() else text

                val errorBuilder =
                    holder.newAnnotation(HighlightSeverity.ERROR, "Неразрешенная ссылка: '$unresolvedName'")
                        .range(element.textRange)
                        .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)

                // ПОДСКАЗКИ АВТО-ИМПОРТА (Quick Fix)
                if (!isMemberAccess) {
                    val candidates = RazomyImportResolver.findModuleByExportedSymbol(element.project, unresolvedName)
                    for (modulePath in candidates) {
                        errorBuilder.withFix(RazomyImportQuickFix(unresolvedName, modulePath))
                    }
                }

                errorBuilder.create()
                return
            }
        }

        if (isMemberAccess) {
            annotateMemberAccess(element, holder)
            return
        }

        val enclosingSlot = PsiTreeUtil.getParentOfType(element, RazomySlot::class.java)
        if (enclosingSlot != null && isParameterInsideSlot(element, enclosingSlot)) {
            highlight(element, holder, COLOR_PARAM)
            return
        }

        if (isDecl) {
            annotateDeclaration(element, holder)
            return
        }
        annotateUsage(element, holder, text)
    }

    private fun annotateMemberAccess(element: RazomyPath, holder: AnnotationHolder) {
        val children = element.node.getChildren(null).filter {
            it.elementType != TokenType.WHITE_SPACE && it.elementType != RazomyTypes.CRLF2
        }

        var isAfterDot = false
        for (node in children) {
            if (node.elementType == RazomyTypes.DOT) {
                isAfterDot = true
                continue
            }
            val partText = node.text
            if (!isAfterDot) {
                if (partText.firstOrNull()?.isUpperCase() == true) {
                    highlightRange(node.textRange, holder, COLOR_MODULE)
                } else {
                    highlightRange(node.textRange, holder, COLOR_VARIABLE)
                }
            } else {
                val isMethod = partText.firstOrNull()?.isUpperCase() == true
                val rightColor = if (isMethod) COLOR_METHOD_CALL else COLOR_FIELD
                highlightRange(node.textRange, holder, rightColor)
            }
        }
    }

    private fun annotateDeclaration(element: RazomyPath, holder: AnnotationHolder) {
        val parent = element.parent
        if (parent is RazomyTypedFlowBlock) {
            highlight(element, holder, COLOR_FUNCTION_DEF)
            return
        }
        if (parent is RazomyTypedFormBlock) {
            if (PsiTreeUtil.findChildOfType(parent.formBlock, RazomyFlowBlock::class.java) != null) {
                highlight(element, holder, COLOR_MODULE)
            } else {
                highlight(element, holder, COLOR_TYPE)
            }
            return
        }

        if (element.text.firstOrNull()?.isUpperCase() == true) {
            highlight(element, holder, COLOR_TYPE)
        } else {
            highlight(element, holder, COLOR_FIELD)
        }
    }

    private fun annotateUsage(element: RazomyPath, holder: AnnotationHolder, text: String) {
        if (element.text.firstOrNull()?.isUpperCase() == true) {
            highlight(element, holder, COLOR_TYPE)
        } else {
            highlight(element, holder, COLOR_VARIABLE)
        }
    }

    private fun isParameterInsideSlot(element: RazomyPath, slot: RazomySlot): Boolean {
        val slotParent = slot.parent
        return slotParent is RazomyCallExpr || slotParent is RazomyTypedFlowBlock || slotParent is RazomyTypedFormBlock
    }

    private fun highlight(element: PsiElement, holder: AnnotationHolder, colorKey: TextAttributesKey) {
        holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
            .range(element.textRange)
            .textAttributes(colorKey)
            .create()
    }

    private fun highlightRange(range: TextRange, holder: AnnotationHolder, colorKey: TextAttributesKey) {
        holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
            .range(range)
            .textAttributes(colorKey)
            .create()
    }

    private fun annotateString(element: PsiElement, holder: AnnotationHolder) {
        val text = element.text
        val offset = element.textOffset
        for (match in ESCAPE_PATTERN.findAll(text)) {
            val range = TextRange(offset + match.range.first, offset + match.range.last + 1)
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(range)
                .textAttributes(DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE)
                .create()
        }
        for (match in INTERPOLATION_PATTERN.findAll(text)) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(offset + match.range.first, offset + match.range.first + 1))
                .textAttributes(DefaultLanguageHighlighterColors.BRACES).create()
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(offset + match.range.last, offset + match.range.last + 1))
                .textAttributes(DefaultLanguageHighlighterColors.BRACES).create()
            val innerRange = TextRange(offset + match.range.first + 1, offset + match.range.last)
            if (!innerRange.isEmpty) {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(innerRange)
                    .textAttributes(DefaultLanguageHighlighterColors.IDENTIFIER).create()
            }
        }
    }
}


// --- REFERENCES ---


object RazomyIndexKeys {
    val EXPORTED_SYMBOLS = ID.create<String, Void>("razomy.exports.index")
}

class RazomyExportIndex : ScalarIndexExtension<String>() {

    override fun getName(): ID<String, Void> = RazomyIndexKeys.EXPORTED_SYMBOLS

    override fun getVersion(): Int = 1 // Увеличивайте это число, если меняете логику индексатора

    override fun dependsOnFileContent(): Boolean = true

    override fun getKeyDescriptor(): EnumeratorStringDescriptor = EnumeratorStringDescriptor.INSTANCE

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return DefaultFileTypeSpecificInputFilter(RazomyFileType.INSTANCE)
    }

    override fun getIndexer(): DataIndexer<String, Void, FileContent> {
        return DataIndexer { inputData ->
            val result = mutableMapOf<String, Void?>()

            // Во время индексации мы разбираем файл.
            // IDE делает это в фоне, не блокируя UI.
            val file = inputData.psiFile
            val globalDecls = com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(file, org.razomy.language.psi.RazomyDeclaration::class.java)

            for (decl in globalDecls) {
                val leftExpr = decl.children.firstOrNull()
                val path = com.intellij.psi.util.PsiTreeUtil.findChildOfType(leftExpr, org.razomy.language.psi.RazomyPath::class.java)
                if (path != null && path.textOffset == leftExpr?.textOffset) {
                    val symbolName = path.text
                    if (symbolName != "_" && symbolName.firstOrNull()?.isUpperCase() == true) {
                        result[symbolName] = null // Void? значит нам нужен только ключ
                    }
                }
            }
            result
        }
    }
}

// --- REFERENCES ---

class RazomyReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(RazomyPath::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext
                ): Array<PsiReference> {
                    val path = element as RazomyPath
                    if (path.text == "_") return emptyArray()

                    // ВАЖНОЕ ИЗМЕНЕНИЕ: Разрешаем создавать ссылки, если это Алиас Импорта!
                    val isDecl = RazomyUtil.isDeclaration(path)
                    val importCtx = RazomyUtil.getImportContext(path)
                    if (isDecl && importCtx != RazomyUtil.ImportContext.LEFT_ALIAS_OR_DESTRUCT) {
                        return emptyArray()
                    }

                    val text = path.text
                    val parts = text.split('.')

                    if (parts.size == 1) {
                        return arrayOf(RazomyReference(path, TextRange(0, text.length), text))
                    }

                    val references = mutableListOf<PsiReference>()
                    var offset = 0
                    for (part in parts) {
                        val endOffset = offset + part.length
                        if (part.isNotBlank()) {
                            val range = TextRange(offset, endOffset)
                            val subPathText = text.substring(0, endOffset)
                            references.add(RazomyReference(path, range, subPathText))
                        }
                        offset = endOffset + 1
                    }
                    return references.toTypedArray()
                }
            }
        )
    }
}

class RazomyReference(
    element: RazomyPath,
    textRange: TextRange,
    private val resolveText: String
) : PsiReferenceBase<RazomyPath>(element, textRange), PsiPolyVariantReference {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val importCtx = RazomyUtil.getImportContext(myElement)

        if (importCtx == RazomyUtil.ImportContext.RIGHT_PATH && resolveText == myElement.text) {
            val file = RazomyImportResolver.resolveImportedFile(myElement, resolveText)
            return if (file != null) arrayOf(PsiElementResolveResult(file)) else emptyArray()
        }

        val resolved = RazomyResolver.resolvePath(myElement, resolveText)
        if (resolved != null) {
            return arrayOf(PsiElementResolveResult(resolved))
        }
        return emptyArray()
    }

    override fun resolve(): PsiElement? {
        val results = multiResolve(false)
        return if (results.isNotEmpty()) results[0].element else null
    }

    // ВАЖНОЕ ИЗМЕНЕНИЕ: Безопасное сравнение через VirtualFile!
    // Теперь IDE 100% поймет, что элементы из разных файлов совпадают.
    override fun isReferenceTo(element: PsiElement): Boolean {
        val resolved = resolve()
        if (resolved == element) return true

        if (element is RazomyPath && resolved is RazomyPath) {
            val file1 = element.containingFile?.originalFile?.virtualFile
            val file2 = resolved.containingFile?.originalFile?.virtualFile
            return element.text == resolved.text &&
                    element.textOffset == resolved.textOffset &&
                    file1 != null && file1 == file2
        }
        return false
    }

    override fun getVariants(): Array<Any> = emptyArray() // Делегируем CompletionContributor'у
}


// --- FIND USAGES (ПОИСК ИСПОЛЬЗОВАНИЙ) ---

class RazomyFindUsagesProvider : FindUsagesProvider {

    // Объясняем IDE, какие токены можно искать (идентификаторы), а какие игнорировать (комментарии, строки)
    override fun getWordsScanner(): WordsScanner {
        return DefaultWordsScanner(
            FlexAdapter(RazomyLexer(null)), // Ваш лексер
            TokenSet.create(RazomyTypes.IDENTIFIER), // Что можно искать
            TokenSet.create(RazomyTypes.LINE_COMMENT), // Комментарии (игнорируем)
            TokenSet.create(RazomyTypes.STRING) // Строки (игнорируем)
        )
    }

    // Разрешаем поиск только для деклараций
    override fun canFindUsagesFor(psiElement: PsiElement): Boolean {
        return psiElement is RazomyPath && RazomyUtil.isDeclaration(psiElement)
    }

    override fun getHelpId(psiElement: PsiElement): String? = null

    // Название типа сущности в окне "Find Usages"
    override fun getType(element: PsiElement): String {
        if (element !is RazomyPath) return ""
        return if (element.text.firstOrNull()?.isUpperCase() == true) "Type/Structure" else "Variable"
    }

    // Имя сущности
    override fun getDescriptiveName(element: PsiElement): String {
        return element.text
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        return element.text
    }
}