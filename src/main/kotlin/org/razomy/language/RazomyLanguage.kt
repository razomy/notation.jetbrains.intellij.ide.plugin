package org.razomy.language

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.codeInspection.*
import com.intellij.codeInspection.util.IntentionName
import com.intellij.formatting.*
import com.intellij.icons.AllIcons
import com.intellij.ide.structureView.*
import com.intellij.ide.structureView.StructureViewModel.ElementInfoProvider
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.*
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.lexer.FlexAdapter
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.*
import com.intellij.util.io.EnumeratorStringDescriptor
import org.razomy.language.psi.*
import javax.swing.Icon

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
            RazomyTypes.PIPE_DOT, RazomyTypes.PIPE, RazomyTypes.AMP, RazomyTypes.SLASH,
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

        // Вешаем маркер строго на листовой токен (IDENTIFIER) по требованию IntelliJ SDK
        val leafElement = element.firstChild ?: element
        val file = element.containingFile ?: return
        val currentName = element.text
        if (currentName.isBlank() || !currentName.first().isUpperCase()) return

        // 1. Предки (Супертипы)
        val statement = PsiTreeUtil.getParentOfType(element, RazomyDeclaration::class.java, RazomyFormItem::class.java)
        if (statement != null && PsiTreeUtil.findChildOfType(statement, RazomyPath::class.java) == element) {
            val rightExpr = statement.children.getOrNull(1)
            if (rightExpr != null) {
                val pathsInRight = PsiTreeUtil.findChildrenOfType(rightExpr, RazomyPath::class.java)
                val allDecls = RazomyTypeSystem.getGlobalDeclarationsMap(file)
                val superTypes = pathsInRight
                    .filter { it.text.firstOrNull()?.isUpperCase() == true && !it.text.contains(".") }
                    .mapNotNull { path -> allDecls[path.text] }
                    .filter { it != element }
                    .distinct()

                if (superTypes.isNotEmpty()) {
                    val upBuilder = NavigationGutterIconBuilder.create(AllIcons.Gutter.OverridingMethod)
                        .setTargets(superTypes)
                        .setTooltipText("Наследует/Использует типы: " + superTypes.joinToString { it.text })
                    result.add(upBuilder.createLineMarkerInfo(leafElement))
                }
            }
        }

        // 2. Наследники (Где этот тип используется в других декларациях)
        val allDecls = PsiTreeUtil.findChildrenOfType(file, RazomyDeclaration::class.java)
        val subTypes = mutableListOf<PsiElement>()

        for (decl in allDecls) {
            val rightExpr = decl.children.getOrNull(1)
            if (rightExpr != null) {
                val pathsInRight = PsiTreeUtil.findChildrenOfType(rightExpr, RazomyPath::class.java)
                if (pathsInRight.any { it.text == currentName }) {
                    val declName = PsiTreeUtil.findChildOfType(decl.children.firstOrNull(), RazomyPath::class.java)
                    if (declName != null && declName != element && RazomyUtil.isDeclaration(declName)) {
                        subTypes.add(declName)
                    }
                }
            }
        }

        if (subTypes.isNotEmpty()) {
            val downBuilder = NavigationGutterIconBuilder.create(AllIcons.Gutter.OverridenMethod)
                .setTargets(subTypes)
                .setTooltipText("Используется в: " + subTypes.joinToString { it.text })
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

        val decl = if (RazomyUtil.isDeclaration(target)) {
            target
        } else {
            (RazomyResolver.resolvePath(target, target.text) as? RazomyPath)
                ?: target.containingFile?.let { file ->
                    PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java)
                        .firstOrNull { it.text == target.text && RazomyUtil.isDeclaration(it) }
                }
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
        if (element is PsiFile) {
            val decls = element.children.filterIsInstance<RazomyDeclaration>()
            return decls.mapNotNull { decl ->
                val firstExpr = decl.children.firstOrNull()
                val path = PsiTreeUtil.findChildOfType(firstExpr, RazomyPath::class.java)
                if (path != null && path.textOffset == firstExpr?.textOffset && RazomyUtil.isDeclaration(path)) {
                    RazomyStructureElement(path)
                } else null
            }.toTypedArray()
        } else if (element is RazomyPath) {
            val decl = PsiTreeUtil.getParentOfType(element, RazomyDeclaration::class.java, RazomyFormItem::class.java)
            if (decl != null && decl.children.firstOrNull()?.textOffset == element.textOffset) {
                val formBlock = PsiTreeUtil.findChildOfType(decl, RazomyFormBlock::class.java)
                if (formBlock != null && !RazomyUtil.isRouterFormBlock(formBlock)) {
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
        val settings = formattingContext.codeStyleSettings
        val spacingBuilder = SpacingBuilder(settings, RazomyLanguage.INSTANCE)
            .around(
                TokenSet.create(
                    RazomyTypes.PIPE_DOT, RazomyTypes.DOT_GT, RazomyTypes.DOT_AMP,
                    RazomyTypes.MIGRATION, RazomyTypes.AMP, RazomyTypes.PIPE,
                    RazomyTypes.SLASH, RazomyTypes.EQ, RazomyTypes.EQ_EQ,
                    RazomyTypes.NOT_EQ, RazomyTypes.LTE, RazomyTypes.GTE,
                    RazomyTypes.LANGLE, RazomyTypes.RANGLE, RazomyTypes.PLUS,
                    RazomyTypes.MINUS, RazomyTypes.MUL
                )
            ).spaces(1)
            .around(RazomyTypes.DOT).spaces(0)
            .before(RazomyTypes.COMMA).spaces(0)
            .after(RazomyTypes.COMMA).spaces(1)

        val block = RazomyBlock(
            formattingContext.node,
            Wrap.createWrap(WrapType.NONE, false),
            Alignment.createAlignment(),
            spacingBuilder
        )
        return FormattingModelProvider.createFormattingModelForPsiFile(
            formattingContext.containingFile,
            block,
            settings
        )
    }
}

class RazomyBlock(
    private val myNode: ASTNode,
    private val myWrap: Wrap?,
    private val myAlignment: Alignment?,
    private val mySpacingBuilder: SpacingBuilder
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
                blocks.add(RazomyBlock(child, Wrap.createWrap(WrapType.NONE, false), null, mySpacingBuilder))
            }
            child = child.treeNext
        }
        return blocks
    }

    override fun getIndent(): Indent? {
        val parentType = myNode.treeParent?.elementType
        val groups = listOf(RazomyTypes.FLOW_BLOCK, RazomyTypes.FORM_BLOCK, RazomyTypes.SLOT)

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
        return mySpacingBuilder.getSpacing(this, child1, child2)
    }

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
                    val file = element.containingFile ?: return
                    val myScope = PsiTreeUtil.getParentOfType(
                        element,
                        RazomyFormBlock::class.java,
                        RazomyFlowBlock::class.java,
                        RazomySlot::class.java
                    )

                    val duplicate = PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java).firstOrNull { other ->
                        if (other != element && other.text == name && RazomyUtil.isDeclaration(other)) {
                            val otherScope = PsiTreeUtil.getParentOfType(
                                other,
                                RazomyFormBlock::class.java,
                                RazomyFlowBlock::class.java,
                                RazomySlot::class.java
                            )
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
                if (element is RazomyPath && RazomyUtil.isDeclaration(element)) {
                    val name = element.text
                    if (name == "_" || name.startsWith("_.") || name == "Main") return

                    val file = element.containingFile ?: return

                    // 1. Быстрая локальная проверка (включая пути и строковую интерполяцию "{Name}")
                    val allPaths = PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java)
                    val isUsedLocallyInPaths = allPaths.any { path ->
                        if (path == element) return@any false
                        if (RazomyUtil.isDeclaration(path)) return@any false
                        val pathText = path.text
                        pathText == name || pathText.startsWith("$name.") || pathText.split('.').contains(name)
                    }

                    if (isUsedLocallyInPaths) return

                    val isUsedInStrings = RazomyUtil.isUsedInStringInterpolations(file, name)
                    if (isUsedInStrings) return

                    // 2. Кросс-файловая проверка через индекс ссылок IntelliJ
                    val isUsedCrossFile = ReferencesSearch.search(element, element.useScope).findFirst() != null
                    if (!isUsedCrossFile) {
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
                        val caretOffset = (parameters.offset - currentPath.textRange.startOffset)
                            .coerceIn(0, fullText.length)

                        val textBeforeCaret = fullText.substring(0, caretOffset).replace("IntellijIdeaRulezzz", "")

                        if (textBeforeCaret.contains(".")) {
                            val prefix = textBeforeCaret.substringBeforeLast(".")
                            val resolvedPrefix = RazomyResolver.resolvePath(currentPath, prefix)

                            if (resolvedPrefix != null) {
                                val type = if (resolvedPrefix is PsiFile) {
                                    RazomyType.Module(resolvedPrefix)
                                } else {
                                    RazomyTypeSystem.inferTypeOfElement(resolvedPrefix)
                                }
                                val fields = RazomyType.extractFields(type)

                                for ((name, _) in fields) {
                                    result.addElement(
                                        LookupElementBuilder.create(name)
                                            .withIcon(AllIcons.Nodes.Field)
                                            .withTypeText("Field of $prefix")
                                    )
                                }
                            }
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

        if (element !is RazomyCallExpr) return hints
        // Не показываем подсказки на самом объявлении параметров функции
        val slot = PsiTreeUtil.getChildOfType(element, RazomySlot::class.java) ?: return hints
        if (RazomyUtil.isParameterDeclarationSlot(slot)) return hints

        val path = PsiTreeUtil.getChildOfType(element, RazomyPath::class.java) ?: return hints
        val providedArgs = PsiTreeUtil.findChildrenOfType(slot, RazomySlotItem::class.java).toList()

        if (providedArgs.isEmpty()) return hints

        var targetPath = (path.reference?.resolve() as? RazomyPath)
            ?: (RazomyResolver.resolvePath(path, path.text) as? RazomyPath)

        if (targetPath == null) {
            val allPaths = PsiTreeUtil.findChildrenOfType(element.containingFile, RazomyPath::class.java)
            targetPath = allPaths.firstOrNull { it != path && it.text == path.text && RazomyUtil.isDeclaration(it) }
        }

        if (targetPath == null) return hints

        val expectedNames = mutableListOf<String>()

        // СЦЕНАРИЙ А: Вызов Формы (Структуры)
        val typedForm = PsiTreeUtil.getParentOfType(targetPath, RazomyTypedFormBlock::class.java)
        val formBlock = typedForm?.let { PsiTreeUtil.findChildOfType(it, RazomyFormBlock::class.java) }
            ?: PsiTreeUtil.getParentOfType(targetPath, RazomyDeclaration::class.java)
                ?.let { PsiTreeUtil.findChildOfType(it, RazomyFormBlock::class.java) }

        val targetCall = PsiTreeUtil.getParentOfType(targetPath, RazomyCallExpr::class.java)
        if (targetCall != null && targetCall.textOffset == targetPath.textOffset) {
            // СЦЕНАРИЙ Б: Вызов Функции/Конструктора с параметрами -> AgentLoop(Err, Retries) [ ... ]
            val declSlot = PsiTreeUtil.getChildOfType(targetCall, RazomySlot::class.java)
            if (declSlot != null) {
                val params = PsiTreeUtil.findChildrenOfType(declSlot, RazomySlotItem::class.java)
                    .mapNotNull { PsiTreeUtil.findChildOfType(it, RazomyPath::class.java)?.text }
                expectedNames.addAll(params)
            }
        } else if (formBlock != null) {
            val fields = PsiTreeUtil.findChildrenOfType(formBlock, RazomyFormItem::class.java)
                .mapNotNull { PsiTreeUtil.findChildOfType(it, RazomyPath::class.java)?.text }
            expectedNames.addAll(fields)
        }

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


// --- GOTO DECLARATION ---

class RazomyGotoDeclarationHandler : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor?
    ): Array<PsiElement>? {
        if (sourceElement == null) return null
        val file = sourceElement.containingFile ?: return null

        // 0. Навигация внутри строковой интерполяции "{Name}" или "{Err.Log}"
        if (sourceElement.node.elementType == RazomyTypes.STRING) {
            val relOffset = offset - sourceElement.textRange.startOffset
            for (match in RazomyAnnotator.INTERPOLATION_PATTERN.findAll(sourceElement.text)) {
                val contentGroup = match.groups[1] ?: continue
                if (relOffset in contentGroup.range) {
                    val exprText = contentGroup.value.trim()
                    val resolved = RazomyResolver.resolvePath(sourceElement, exprText)
                    if (resolved != null) return arrayOf(resolved)
                }
            }
            return null
        }

        val word = sourceElement.text
        if (word.isBlank() || word == "_") return null

        val currentPath = PsiTreeUtil.getParentOfType(sourceElement, RazomyPath::class.java) ?: return null

        // 1. Если кликаем по файловому пути в импорте (правая часть)
        if (RazomyUtil.getImportContext(currentPath) == RazomyUtil.ImportContext.RIGHT_PATH) {
            val fileTarget = RazomyImportResolver.resolveImportedFile(sourceElement, currentPath.text)
            if (fileTarget != null) return arrayOf(fileTarget)
            return null
        }

        // 2. Если кликаем по самому определению (кроме деструктурированного импорта, который ведет в модуль)
        if (RazomyUtil.isDeclaration(currentPath) &&
            RazomyUtil.getImportContext(currentPath) != RazomyUtil.ImportContext.LEFT_ALIAS_OR_DESTRUCT
        ) {
            val targets = findUsages(currentPath, file)
            return if (targets.isNotEmpty()) targets.toTypedArray() else null
        }

        // 3. Стандартный семантический резолв
        val resolved = RazomyResolver.resolvePath(currentPath, word)
            ?: RazomyResolver.resolvePath(currentPath, currentPath.text)
            ?: resolveLocalDeclaration(currentPath, word)

        return if (resolved != null) arrayOf(resolved) else null
    }

    private fun findUsages(element: RazomyPath, file: PsiFile): List<PsiElement> {
        val currentName = element.text
        val results = LinkedHashSet<PsiElement>()

        // Поиск в текущем файле
        val allPaths = PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java)
        for (path in allPaths) {
            if (path != element && !RazomyUtil.isDeclaration(path) && matchesName(path.text, currentName)) {
                results.add(path)
            }
        }

        // Поиск по всему проекту через индекс ссылок
        for (ref in ReferencesSearch.search(element, element.useScope)) {
            results.add(ref.element)
        }

        return results.toList()
    }

    private fun resolveLocalDeclaration(from: RazomyPath, name: String): RazomyPath? {
        var scope = from.parent
        while (scope != null) {
            if (scope is RazomyFormBlock || scope is RazomyFlowBlock) {
                val parentDecl =
                    PsiTreeUtil.getParentOfType(scope, RazomyDeclaration::class.java, RazomyFormItem::class.java)
                val leftExpr = parentDecl?.children?.firstOrNull()
                val slot = PsiTreeUtil.findChildOfType(leftExpr, RazomySlot::class.java)
                if (slot != null && RazomyUtil.isParameterDeclarationSlot(slot)) {
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


// --- LANGUAGE & FILE TYPE ---

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

    /**
     * Проверяет, является ли { ... } таблицей роутинга в пайплайне (например: Account . { Manager [...] })
     */
    fun isRouterFormBlock(formBlock: RazomyFormBlock): Boolean {
        if (formBlock.parent is RazomyTypedFormBlock) return false
        val pipelineExpr = PsiTreeUtil.getParentOfType(formBlock, RazomyPipelineExpr::class.java) ?: return false
        val ops = PsiTreeUtil.getChildrenOfType(pipelineExpr, RazomyPipelineOp::class.java) ?: return false
        return ops.any { it.textOffset < formBlock.textOffset }
    }

    /**
     * Проверяет, является ли TypeName { ... } созданием экземпляра внутри потока [] или миграции ->
     */
    fun isInstantiationFormBlock(formBlock: RazomyFormBlock): Boolean {
        val typedForm = formBlock.parent as? RazomyTypedFormBlock ?: return false
        if (PsiTreeUtil.getParentOfType(typedForm, RazomyFlowBlock::class.java) != null) {
            return true
        }
        val migrateExpr = PsiTreeUtil.getParentOfType(typedForm, RazomyMigrateExpr::class.java)
        if (migrateExpr != null && typedForm.textOffset > migrateExpr.textOffset) {
            return true
        }
        return false
    }

    /**
     * Проверяет, является ли слот (...) списком объявления параметров (например: AgentLoop(Err, Retries) [...],
     * Succ(N) {...} или захватом переменной в роутере CompileError(NewErr) [...]),
     * а не вызовом функции в потоке или кортежем Point2D (Float, Float).
     */
    fun isParameterDeclarationSlot(slot: RazomySlot): Boolean {
        val callExpr = slot.parent as? RazomyCallExpr ?: return false

        var curr: PsiElement? = callExpr.parent
        while (curr is RazomyExpr && curr !is RazomyUnionExpr && curr !is RazomyMergeExpr && curr !is RazomyPipelineExpr) {
            val parent = curr.parent
            if (parent is RazomyDeclaration || parent is RazomyFormItem) {
                val hasBody = parent.children.size >= 2
                val isLeftSide = parent.children.firstOrNull() == curr && callExpr.textOffset == parent.textOffset
                return hasBody && isLeftSide
            }
            curr = parent
        }
        return false
    }

    /**
     * Безошибочно определяет, является ли RazomyPath объявлением новой сущности/поля/параметра
     */
    fun isDeclaration(path: RazomyPath): Boolean {
        val text = path.text
        if (text == "_" || text.startsWith("_.") || text.contains(".")) return false

        // 1. Путь внутри слота (...)
        val slotItem = PsiTreeUtil.getParentOfType(path, RazomySlotItem::class.java)
        if (slotItem != null) {
            val slot = slotItem.parent as? RazomySlot ?: return false
            if (!isParameterDeclarationSlot(slot)) return false

            // Если в слоте записано слияние типов ShipToCustomer(Order & Reserved & Paid),
            // то Order, Reserved, Paid — это ссылки на глобальные типы, а не имена новых переменных
            if (PsiTreeUtil.findChildOfType(slotItem, RazomyMergeExpr::class.java) != null &&
                PsiTreeUtil.findChildrenOfType(slotItem, RazomyPath::class.java).size > 1
            ) {
                return false
            }

            val firstPath = PsiTreeUtil.findChildOfType(slotItem, RazomyPath::class.java)
            return firstPath == path && path.textOffset == slotItem.textOffset
        }

        // 2. Путь внутри формы { ... }
        val formItem = PsiTreeUtil.getParentOfType(path, RazomyFormItem::class.java)
        if (formItem != null) {
            val firstExpr = formItem.children.firstOrNull() ?: return false
            val firstPath = PsiTreeUtil.findChildOfType(firstExpr, RazomyPath::class.java)
            if (firstPath != path || path.textOffset != firstExpr.textOffset) return false

            val formBlock = formItem.parent as? RazomyFormBlock ?: return false

            // В роутере (S . { Circle [...] }) сам идентификатор Circle — это ссылка на существующий тип
            if (isRouterFormBlock(formBlock)) return false

            // При создании экземпляра (AiQuery { SystemPrompt "..." }) имя поля ссылается на поле типа AiQuery
            if (isInstantiationFormBlock(formBlock)) {
                val typedForm = formBlock.parent as? RazomyTypedFormBlock
                val typePath = typedForm?.let { PsiTreeUtil.getChildOfType(it, RazomyPath::class.java) }
                if (typePath != null) {
                    val targetType = RazomyTypeSystem.inferTypeByName(typePath.text, typePath)
                    if (RazomyType.extractFields(targetType).containsKey(text)) {
                        return false
                    }
                }
            }

            return true
        }

        // 3. Путь на верхнем уровне файла (RazomyDeclaration)
        val decl = PsiTreeUtil.getParentOfType(path, RazomyDeclaration::class.java)
        if (decl != null) {
            val firstExpr = decl.children.firstOrNull() ?: return false
            val firstPath = PsiTreeUtil.findChildOfType(firstExpr, RazomyPath::class.java)
            return firstPath == path && path.textOffset == firstExpr.textOffset
        }

        return false
    }

    fun isImportPath(text: String): Boolean {
        if (text.isBlank() || text == "_") return false
        return text.first().isLowerCase()
    }

    enum class ImportContext { RIGHT_PATH, LEFT_ALIAS_OR_DESTRUCT }

    fun getImportContext(path: RazomyPath): ImportContext? {
        val decl = PsiTreeUtil.getParentOfType(path, RazomyDeclaration::class.java) ?: return null
        if (PsiTreeUtil.getParentOfType(path, RazomyFlowBlock::class.java) != null) return null

        val leftExpr = decl.children.firstOrNull() ?: return null
        val rightExpr = decl.children.getOrNull(1) ?: return null

        val rightPath = PsiTreeUtil.findChildOfType(rightExpr, RazomyPath::class.java)
        val isRightImport = rightPath != null &&
                rightPath.textOffset == rightExpr.textOffset &&
                rightPath.textLength == rightExpr.textLength &&
                isImportPath(rightPath.text)

        if (!isRightImport) return null

        if (path == rightPath) {
            return ImportContext.RIGHT_PATH
        }
        if (PsiTreeUtil.isAncestor(leftExpr, path, false)) {
            return ImportContext.LEFT_ALIAS_OR_DESTRUCT
        }
        return null
    }

    fun isUsedInStringInterpolations(file: PsiFile, symbolName: String): Boolean {
        val literals = PsiTreeUtil.findChildrenOfType(file, RazomyLiteral::class.java)
        for (lit in literals) {
            val text = lit.text
            if (!text.contains('{')) continue
            for (match in RazomyAnnotator.INTERPOLATION_PATTERN.findAll(text)) {
                val expr = match.groups[1]?.value?.trim() ?: continue
                if (expr == symbolName || expr.startsWith("$symbolName.") || expr.split('.').contains(symbolName)) {
                    return true
                }
            }
        }
        return false
    }
}


// --- СИСТЕМА ИМПОРТОВ И РЕЗОЛВА МОДУЛЕЙ ---

object RazomyImportResolver {
    fun resolveImportedFile(context: PsiElement, importPath: String): PsiFile? {
        val project = context.project
        val cleanImport = importPath.trim()
        if (cleanImport.isEmpty()) return null

        val fileName = cleanImport.substringAfterLast('.') + ".rl"
        val expectedPathFragment = cleanImport.replace('.', '/') + ".rl"

        // 1. Проверяем относительный импорт (например: _ base относительно папки текущего файла)
        val currentFile = context.containingFile?.originalFile?.virtualFile
        val currentDir = currentFile?.parent
        if (currentDir != null) {
            val relativeVf = currentDir.findFileByRelativePath(expectedPathFragment)
            if (relativeVf != null) {
                return PsiManager.getInstance(project).findFile(relativeVf)
            }
        }

        // 2. Поиск по всему проекту
        val virtualFiles = FilenameIndex.getVirtualFilesByName(fileName, GlobalSearchScope.projectScope(project))

        val exactMatch = virtualFiles.firstOrNull {
            val normalized = it.path.replace('\\', '/')
            normalized.endsWith("/$expectedPathFragment") || normalized.endsWith(expectedPathFragment)
        }
        if (exactMatch != null) {
            return PsiManager.getInstance(project).findFile(exactMatch)
        }

        // 3. Мягкий фоллбэк, если такой файл один в проекте
        if (virtualFiles.size == 1) {
            return PsiManager.getInstance(project).findFile(virtualFiles.first())
        }

        return null
    }

    fun getExportedSymbols(file: PsiFile): Set<String> {
        return RazomyTypeSystem.getGlobalDeclarationsMap(file).keys
    }

    fun findExportedSymbol(file: PsiFile, name: String): PsiElement? {
        return RazomyTypeSystem.getGlobalDeclarationsMap(file)[name]
    }

    fun findModuleByExportedSymbol(project: Project, symbolName: String): List<String> {
        val result = mutableListOf<String>()

        val virtualFiles = FileBasedIndex.getInstance().getContainingFiles(
            RazomyIndexKeys.EXPORTED_SYMBOLS,
            symbolName,
            GlobalSearchScope.projectScope(project)
        )

        val fileIndex = ProjectRootManager.getInstance(project).fileIndex
        for (vf in virtualFiles) {
            val root = fileIndex.getSourceRootForFile(vf)
                ?: fileIndex.getContentRootForFile(vf)
                ?: project.guessProjectDir()

            var relPath = if (root != null) {
                VfsUtilCore.getRelativePath(vf, root) ?: vf.name
            } else {
                vf.name
            }
            relPath = relPath.removeSuffix(".rl").replace('\\', '.').replace('/', '.')
            result.add(relPath)
        }

        return result.distinct()
    }
}


// --- AUTO-IMPORT QUICK FIX ---

public class RazomyImportQuickFix(private val symbolName: String, private val modulePath: String) : LocalQuickFix,
    IntentionAction {

    override fun getFamilyName(): String = "Импортировать"

    override fun getName(): String = "Импортировать '$symbolName' из $modulePath"

    override fun getText(): @IntentionName String = name

    override fun startInWriteAction(): Boolean = true

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return file != null && file.language == RazomyLanguage.INSTANCE && file.manager.isInProject(file)
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val file = descriptor.psiElement.containingFile ?: return
        invoke(project, null, file)
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (file == null) return

        val factory = PsiFileFactory.getInstance(project)
        val dummyContent = "{ $symbolName } $modulePath\n"
        val dummyFile = factory.createFileFromText("dummy.rl", RazomyFileType.INSTANCE, dummyContent)

        val firstDecl = PsiTreeUtil.findChildOfType(file, RazomyDeclaration::class.java)

        if (firstDecl != null) {
            for (child in dummyFile.children) {
                file.addBefore(child, firstDecl)
            }
        } else {
            for (child in dummyFile.children) {
                file.add(child)
            }
        }
    }
}


// --- СИСТЕМА ТИПОВ И СЕМАНТИЧЕСКИЙ РЕЗОЛВЕР (Razomy Type System V2.8 - Cached & Algebraic Edition) ---

object RazomyTypeSystem {
    private val LOG = Logger.getInstance("RazomyTypeSystem")

    /**
     * Кэшированная карта глобальных объявлений файла (сбрасывается автоматически при изменении PSI)
     */
    fun getGlobalDeclarationsMap(file: PsiFile): Map<String, RazomyPath> {
        return CachedValuesManager.getCachedValue(file) {
            val map = LinkedHashMap<String, RazomyPath>()
            val globalDecls = PsiTreeUtil.findChildrenOfType(file, RazomyDeclaration::class.java)
            for (decl in globalDecls) {
                val leftExpr = decl.children.firstOrNull() ?: continue
                val path = PsiTreeUtil.findChildOfType(leftExpr, RazomyPath::class.java)
                if (path != null && path.textOffset == leftExpr.textOffset && path.text != "_") {
                    map.putIfAbsent(path.text, path)
                }
            }
            CachedValueProvider.Result.create(map, file, PsiModificationTracker.MODIFICATION_COUNT)
        }
    }

    fun inferTypeByName(
        name: String,
        context: PsiElement,
        visited: MutableSet<String> = mutableSetOf()
    ): RazomyType {
        if (name.isBlank() || name == "_" || !visited.add(name)) return RazomyType.Unknown

        val file = context.containingFile ?: return RazomyType.Unknown
        val globalDecls = PsiTreeUtil.findChildrenOfType(file, RazomyDeclaration::class.java)

        // 1. Поиск среди импортов
        for (decl in globalDecls) {
            val leftExpr = decl.children.firstOrNull()
            val rightExpr = decl.children.getOrNull(1)

            val leftPath = PsiTreeUtil.findChildOfType(leftExpr, RazomyPath::class.java)
            val rightPath = PsiTreeUtil.findChildOfType(rightExpr, RazomyPath::class.java)
            val leftForm = PsiTreeUtil.findChildOfType(leftExpr, RazomyFormBlock::class.java)

            if (rightPath != null && rightPath.textOffset == rightExpr?.textOffset && RazomyUtil.isImportPath(rightPath.text)) {
                // Wildcard import: _ root.base или _ base
                if (leftPath != null && leftPath.text == "_" && leftPath.textOffset == leftExpr?.textOffset) {
                    val importedFile = RazomyImportResolver.resolveImportedFile(context, rightPath.text)
                    if (importedFile != null) {
                        val foreignSymbol = RazomyImportResolver.findExportedSymbol(importedFile, name)
                        if (foreignSymbol != null) return inferTypeOfElement(foreignSymbol, visited)
                    }
                }
                // Named / Alias import: Base root.base
                else if (leftPath != null && leftPath.text == name && leftPath.textOffset == leftExpr?.textOffset) {
                    val importedFile = RazomyImportResolver.resolveImportedFile(context, rightPath.text)
                    return if (importedFile != null) RazomyType.Module(importedFile) else RazomyType.Unknown
                }
                // Element / Destructured import: { Str } root.base
                else if (leftForm != null && leftForm.textOffset == leftExpr?.textOffset) {
                    val items = PsiTreeUtil.findChildrenOfType(leftForm, RazomyFormItem::class.java)
                    if (items.any { PsiTreeUtil.findChildOfType(it, RazomyPath::class.java)?.text == name }) {
                        val importedFile = RazomyImportResolver.resolveImportedFile(context, rightPath.text)
                        if (importedFile != null) {
                            val foreignSymbol = RazomyImportResolver.findExportedSymbol(importedFile, name)
                            if (foreignSymbol != null) return inferTypeOfElement(foreignSymbol, visited)
                        }
                        return RazomyType.Unknown
                    }
                }
            }
        }

        // 2. Локальный поиск по декларациям файла с поддержкой алгебры типов (&, |, /)
        for (decl in globalDecls) {
            val leftExpr = decl.children.firstOrNull()
            val path = PsiTreeUtil.findChildOfType(leftExpr, RazomyPath::class.java)
            if (path?.text == name && path.textOffset == leftExpr?.textOffset) {
                return evaluateDeclarationType(name, path, decl, context, visited)
            }
        }
        return RazomyType.Unknown
    }

    private fun evaluateDeclarationType(
        name: String,
        declPath: RazomyPath,
        decl: RazomyDeclaration,
        context: PsiElement,
        visited: MutableSet<String>
    ): RazomyType {
        // Прямая форма: User { Id Str ... } или Cons(T) { Head T ... }
        val directForm = PsiTreeUtil.findChildOfType(decl, RazomyFormBlock::class.java)
        if (directForm != null && !RazomyUtil.isRouterFormBlock(directForm) &&
            PsiTreeUtil.getParentOfType(directForm, RazomyFlowBlock::class.java) == null
        ) {
            val fields = extractFormBlockFields(directForm)
            return RazomyType.Form(name, declPath, fields)
        }

        val rightExpr = decl.children.getOrNull(1) ?: return RazomyType.Unknown

        // Вычитание полей (/): PublicUser User / User.PasswordHash
        val subtractExpr = PsiTreeUtil.findChildOfType(rightExpr, RazomySubtractExpr::class.java, false)
        if (subtractExpr != null && subtractExpr.node.findChildByType(RazomyTypes.SLASH) != null) {
            val subChildren = subtractExpr.children
            if (subChildren.size >= 2) {
                val basePath = PsiTreeUtil.findChildOfType(subChildren[0], RazomyPath::class.java)
                val baseType = if (basePath != null) inferTypeByName(basePath.text, context, visited) else RazomyType.Unknown
                val baseFields = RazomyType.extractFields(baseType).toMutableMap()
                for (i in 1 until subChildren.size) {
                    val removedPath = PsiTreeUtil.findChildOfType(subChildren[i], RazomyPath::class.java)?.text ?: continue
                    val fieldName = removedPath.substringAfterLast('.')
                    baseFields.remove(fieldName)
                }
                return RazomyType.Form(name, declPath, baseFields)
            }
        }

        // Пересечение / Слияние типов (&): DatabaseUser User & Identifiable & Auditable, UserOrder User & Order
        val mergeExpr = PsiTreeUtil.findChildOfType(rightExpr, RazomyMergeExpr::class.java, false)
        if (mergeExpr != null && mergeExpr.node.findChildByType(RazomyTypes.AMP) != null) {
            val operands = mergeExpr.children
            var currentType: RazomyType = RazomyType.Unknown
            for (op in operands) {
                val opForm = PsiTreeUtil.findChildOfType(op, RazomyFormBlock::class.java)
                val nextType = if (opForm != null && PsiTreeUtil.getParentOfType(opForm, RazomyFlowBlock::class.java) == null) {
                    RazomyType.Form(name, declPath, extractFormBlockFields(opForm))
                } else {
                    val opPath = PsiTreeUtil.findChildOfType(op, RazomyPath::class.java)
                    if (opPath != null && opPath.text.firstOrNull()?.isUpperCase() == true) {
                        inferTypeByName(opPath.text, context, HashSet(visited))
                    } else RazomyType.Unknown
                }
                if (nextType !is RazomyType.Unknown) {
                    currentType = if (currentType is RazomyType.Unknown) nextType else RazomyType.Intersection(currentType, nextType)
                }
            }
            if (currentType !is RazomyType.Unknown) return currentType
        }

        // Сумма типов (|): Shape Circle | Rect
        val unionExpr = PsiTreeUtil.findChildOfType(rightExpr, RazomyUnionExpr::class.java, false)
        if (unionExpr != null && unionExpr.node.findChildByType(RazomyTypes.PIPE) != null) {
            val operands = unionExpr.children
            var currentType: RazomyType = RazomyType.Unknown
            for (op in operands) {
                val opPath = PsiTreeUtil.findChildOfType(op, RazomyPath::class.java)
                val nextType = if (opPath != null && opPath.text.firstOrNull()?.isUpperCase() == true) {
                    inferTypeByName(opPath.text, context, HashSet(visited))
                } else RazomyType.Unknown
                if (nextType !is RazomyType.Unknown) {
                    currentType = if (currentType is RazomyType.Unknown) nextType else RazomyType.Union(currentType, nextType)
                }
            }
            if (currentType !is RazomyType.Unknown) return currentType
        }

        // Одиночный базовый тип или конструктор: One Succ(Zero)
        val allPaths = PsiTreeUtil.findChildrenOfType(rightExpr, RazomyPath::class.java).toList()
        val baseTypePath = allPaths.firstOrNull { it.text.firstOrNull()?.isUpperCase() == true }
        if (baseTypePath != null) return inferTypeByName(baseTypePath.text, context, visited)

        return RazomyType.Unknown
    }

    private fun extractFormBlockFields(formBlock: RazomyFormBlock): Map<String, PsiElement> {
        val fields = LinkedHashMap<String, PsiElement>()
        val items = formBlock.children.filterIsInstance<RazomyFormItem>()
        for (item in items) {
            val firstExpr = item.children.firstOrNull() ?: continue
            val fieldPath = PsiTreeUtil.findChildOfType(firstExpr, RazomyPath::class.java)
            if (fieldPath != null && fieldPath.textOffset == firstExpr.textOffset) {
                fields[fieldPath.text] = fieldPath
            }
        }
        return fields
    }

    fun inferTypeOfElement(
        element: PsiElement,
        visited: MutableSet<String> = mutableSetOf()
    ): RazomyType {
        if (element is RazomyPath) {
            val slotItem = PsiTreeUtil.getParentOfType(element, RazomySlotItem::class.java)
            if (slotItem != null) {
                val paths = PsiTreeUtil.findChildrenOfType(slotItem, RazomyPath::class.java).toList()
                // Явная типизация слота: (O Order) или (Order Order)
                if (paths.size > 1 && paths[0] == element) {
                    return inferTypeByName(paths[1].text, element, visited)
                }

                // Захват переменной в роутере: CompileError(NewErr) [...] или FatalError(Err) [...]
                val callExpr = PsiTreeUtil.getParentOfType(slotItem, RazomyCallExpr::class.java)
                if (callExpr != null) {
                    val callerPath = PsiTreeUtil.getChildOfType(callExpr, RazomyPath::class.java)
                    if (callerPath != null && callerPath.text.firstOrNull()?.isUpperCase() == true) {
                        val callerType = inferTypeByName(callerPath.text, element, HashSet(visited))
                        if (callerType !is RazomyType.Unknown) return callerType
                    }
                }
            }
        }

        val text = element.text
        if (text.firstOrNull()?.isUpperCase() == true) {
            val resolvedType = inferTypeByName(text, element, visited)
            if (resolvedType !is RazomyType.Unknown) return resolvedType
        }
        return RazomyType.Unknown
    }

    fun buildScope(element: PsiElement): RazomyScope {
        val scope = RazomyScope()
        var current: PsiElement? = element.parent

        while (current != null) {
            // 1. Внутри ветки роутера или элемента формы: Circle [ ... ], Manager [ ... ]
            if (current is RazomyFormItem) {
                val firstExpr = current.children.firstOrNull()
                val firstPath = PsiTreeUtil.findChildOfType(firstExpr, RazomyPath::class.java)
                if (firstPath != null && firstPath.text.firstOrNull()?.isUpperCase() == true) {
                    val resolvedType = inferTypeByName(firstPath.text, element)
                    scope.unpackedFields.putAll(RazomyType.extractFields(resolvedType))
                }
            }

            // 2. Внутри блока формы { ... }
            if (current is RazomyFormBlock && !RazomyUtil.isRouterFormBlock(current)) {
                val items = current.children.filterIsInstance<RazomyFormItem>()
                for (item in items) {
                    val firstExpr = item.children.firstOrNull()
                    val fieldPath = PsiTreeUtil.findChildOfType(firstExpr, RazomyPath::class.java)
                    if (fieldPath != null && fieldPath.textOffset == firstExpr?.textOffset) {
                        scope.variables.putIfAbsent(fieldPath.text, fieldPath)
                    }
                }
            }

            // 3. Внутри потока [ ... ]: распаковка предшествующих форм и миграций ->
            if (current is RazomyFlowBlock) {
                val precedingForms = PsiTreeUtil.findChildrenOfType(current, RazomyFormBlock::class.java)
                for (formBlock in precedingForms) {
                    if (formBlock.textOffset < element.textOffset && !RazomyUtil.isRouterFormBlock(formBlock)) {
                        val items = formBlock.children.filterIsInstance<RazomyFormItem>()
                        for (item in items) {
                            val firstExpr = item.children.firstOrNull()
                            val fieldPath = PsiTreeUtil.findChildOfType(firstExpr, RazomyPath::class.java)
                            if (fieldPath != null && fieldPath.textOffset == firstExpr?.textOffset) {
                                scope.unpackedFields.putIfAbsent(fieldPath.text, fieldPath)
                            }
                        }
                    }
                }
            }

            // 4. Параметры слотов (...) у деклараций, функций и веток роутеров
            if (current is RazomyDeclaration || current is RazomyFormItem) {
                val leftExpr = current.children.firstOrNull()
                val slot = PsiTreeUtil.findChildOfType(leftExpr, RazomySlot::class.java)
                if (slot != null && slot.textOffset < element.textOffset && RazomyUtil.isParameterDeclarationSlot(slot)) {
                    val items = PsiTreeUtil.findChildrenOfType(slot, RazomySlotItem::class.java)
                    for (item in items) {
                        val paths = PsiTreeUtil.findChildrenOfType(item, RazomyPath::class.java).toList()
                        // Если слот — это слияние типов: ShipToCustomer(Order & Reserved & Paid)
                        if (PsiTreeUtil.findChildOfType(item, RazomyMergeExpr::class.java) != null && paths.size > 1) {
                            for (p in paths) {
                                val t = inferTypeByName(p.text, element)
                                scope.unpackedFields.putAll(RazomyType.extractFields(t))
                            }
                        } else if (paths.isNotEmpty()) {
                            val varName = paths[0]
                            scope.variables.putIfAbsent(varName.text, varName)

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

        // 5. ГЛОБАЛЬНЫЙ СКОУП ФАЙЛА И ИМПОРТЫ
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
                    // All / Relative import: _ root.base или _ base
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
                    // Named import: Base root.base
                    else if (leftPath != null && leftPath.textOffset == leftExpr?.textOffset) {
                        val alias = leftPath.text
                        scope.variables.putIfAbsent(alias, leftPath)
                        scope.imports[alias] = rightPath
                    }
                    // Element import: { Str } root.base
                    else if (leftForm != null && leftForm.textOffset == leftExpr?.textOffset) {
                        val items = PsiTreeUtil.findChildrenOfType(leftForm, RazomyFormItem::class.java)
                        for (item in items) {
                            val path = PsiTreeUtil.findChildOfType(item, RazomyPath::class.java)
                            if (path != null) {
                                scope.variables.putIfAbsent(path.text, path)
                                scope.imports[path.text] = rightPath
                                scope.isDestructuredImport[path.text] = true
                            }
                        }
                    }
                } else {
                    if (leftPath != null && leftPath.textOffset == leftExpr?.textOffset && leftPath.text != "_") {
                        scope.variables.putIfAbsent(leftPath.text, leftPath)
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

        // 0. Если это имя поля при инициализации структуры: AiQuery { SystemPrompt "..." } или ClientAccount { AccountId Id }
        if (parts.size == 1 && context is RazomyPath) {
            val formItem = context.parent?.let { PsiTreeUtil.getParentOfType(it, RazomyFormItem::class.java) }
            val formBlock = formItem?.parent as? RazomyFormBlock
            if (formBlock != null && RazomyUtil.isInstantiationFormBlock(formBlock)) {
                val firstExpr = formItem.children.firstOrNull()
                if (firstExpr != null && context.textOffset == firstExpr.textOffset) {
                    val typedForm = formBlock.parent as? RazomyTypedFormBlock
                    val typePath = typedForm?.let { PsiTreeUtil.getChildOfType(it, RazomyPath::class.java) }
                    if (typePath != null) {
                        val targetType = RazomyTypeSystem.inferTypeByName(typePath.text, typePath)
                        val targetField = RazomyType.extractFields(targetType)[parts[0]]
                        if (targetField != null) return targetField
                    }
                }
            }
        }

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

        if (current == null) {
            // 1. Wildcard фоллбэк
            if (scope.isDestructuredImport.containsKey(rootName)) {
                val importPath = scope.imports[rootName]!!
                val importedFile = RazomyImportResolver.resolveImportedFile(context, importPath.text)
                if (importedFile != null) {
                    current = RazomyImportResolver.findExportedSymbol(importedFile, rootName)
                }
            }

            // 2. Фоллбэк для модулей (root.base / base)
            if (current == null) {
                var fileRef: PsiFile? = null
                var memberStartIndex = 0
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
                    for (i in memberStartIndex until parts.size) {
                        val nextMember = parts[i]
                        current = resolveMember(current!!, nextMember)
                        if (current == null) return null
                    }
                    return current
                }
            }

            // 3. Мягкий фоллбэк для строковой интерполяции (например, "{Explanation}" из AiResponse)
            if (current == null && parts.size == 1 && context !is RazomyPath) {
                val file = context.containingFile
                if (file != null) {
                    val fieldMatch = PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java)
                        .firstOrNull { it.text == rootName && RazomyUtil.isDeclaration(it) }
                    if (fieldMatch != null) return fieldMatch
                }
            }

            if (current == null) return null
        }

        // Резолв вложенных свойств (например User.Id, Err.Log, Algebra.OnNum)
        for (i in 1 until parts.size) {
            val nextMember = parts[i]
            val resolvedMember = resolveMember(current!!, nextMember)
            if (resolvedMember == null) {
                LOG.debug("Поле '$nextMember' не найдено в '$rootName'")
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
        if (fields.containsKey(memberName)) {
            return fields[memberName]
        }

        // Утиная типизация (Duck-Typing фоллбэк) для нетипизированных параметров слотов, например AgentLoop(Err, Retries) -> Err.Log
        if (type is RazomyType.Unknown && owner is RazomyPath) {
            val slot = PsiTreeUtil.getParentOfType(owner, RazomySlot::class.java)
            if (slot != null && RazomyUtil.isParameterDeclarationSlot(slot)) {
                val file = owner.containingFile ?: return null
                for ((typeName, _) in RazomyTypeSystem.getGlobalDeclarationsMap(file)) {
                    val candidateType = RazomyTypeSystem.inferTypeByName(typeName, owner)
                    val candidateField = RazomyType.extractFields(candidateType)[memberName]
                    if (candidateField != null) return candidateField
                }
            }
        }
        return null
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
    class Union(val left: RazomyType, val right: RazomyType) : RazomyType() {
        fun getResolvedFields(): Map<String, PsiElement> {
            val result = LinkedHashMap<String, PsiElement>()
            result.putAll(extractFields(left))
            result.putAll(extractFields(right))
            return result
        }
    }

    class Intersection(val left: RazomyType, val right: RazomyType) : RazomyType() {
        fun getResolvedFields(): Map<String, PsiElement> {
            val leftFields = extractFields(left)
            val rightFields = extractFields(right)
            val result = LinkedHashMap<String, PsiElement>()
            // Правило генеалогии (Lineage Namespacing): уникальные поля доступны плоско
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
                is Union -> type.getResolvedFields()
                is Module -> RazomyTypeSystem.getGlobalDeclarationsMap(type.file)
                else -> emptyMap()
            }
        }
    }
}

// --- ANNOTATOR (Подсветка синтаксиса и ошибок) ---

class RazomyAnnotator : Annotator {

    companion object {
        val ESCAPE_PATTERN = Regex("""\\[ntr\"'\\{}]""")
        val INTERPOLATION_PATTERN = Regex("""(?<!\\)\{([^}]+)\}""")

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
        if (element.node.elementType == RazomyTypes.STRING) {
            annotateString(element, holder)
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

        // Если это функция с параметрами: Log(Msg) [ ... ] или AgentLoop(Err, Retries) [ ... ]
        if (parent is RazomyCallExpr) {
            val container = PsiTreeUtil.getParentOfType(parent, RazomyDeclaration::class.java, RazomyFormItem::class.java)
            val rightExpr = container?.children?.getOrNull(1)
            if (rightExpr != null && PsiTreeUtil.findChildOfType(rightExpr, RazomyFlowBlock::class.java) != null) {
                highlight(element, holder, COLOR_FUNCTION_DEF)
                return
            }
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
        // Если это имя поля при инициализации структуры: AiQuery { SystemPrompt "..." }
        val formItem = element.parent?.let { PsiTreeUtil.getParentOfType(it, RazomyFormItem::class.java) }
        val formBlock = formItem?.parent as? RazomyFormBlock
        if (formBlock != null && RazomyUtil.isInstantiationFormBlock(formBlock)) {
            val firstExpr = formItem.children.firstOrNull()
            if (firstExpr != null && element.textOffset == firstExpr.textOffset) {
                highlight(element, holder, COLOR_FIELD)
                return
            }
        }

        if (text.firstOrNull()?.isUpperCase() == true) {
            highlight(element, holder, COLOR_TYPE)
        } else {
            highlight(element, holder, COLOR_VARIABLE)
        }
    }

    private fun isParameterInsideSlot(element: RazomyPath, slot: RazomySlot): Boolean {
        return RazomyUtil.isParameterDeclarationSlot(slot) && RazomyUtil.isDeclaration(element)
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


// --- INDEXING ---

object RazomyIndexKeys {
    val EXPORTED_SYMBOLS = ID.create<String, Void>("razomy.exports.index")
}

class RazomyExportIndex : ScalarIndexExtension<String>() {

    override fun getName(): ID<String, Void> = RazomyIndexKeys.EXPORTED_SYMBOLS

    override fun getVersion(): Int = 2

    override fun dependsOnFileContent(): Boolean = true

    override fun getKeyDescriptor(): EnumeratorStringDescriptor = EnumeratorStringDescriptor.INSTANCE

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return DefaultFileTypeSpecificInputFilter(RazomyFileType.INSTANCE)
    }

    override fun getIndexer(): DataIndexer<String, Void, FileContent> {
        return DataIndexer { inputData ->
            val result = mutableMapOf<String, Void?>()
            val file = inputData.psiFile
            val globalDecls = PsiTreeUtil.findChildrenOfType(file, RazomyDeclaration::class.java)

            for (decl in globalDecls) {
                val leftExpr = decl.children.firstOrNull()
                val path = PsiTreeUtil.findChildOfType(leftExpr, RazomyPath::class.java)
                if (path != null && path.textOffset == leftExpr?.textOffset) {
                    val symbolName = path.text
                    if (symbolName != "_" && symbolName.firstOrNull()?.isUpperCase() == true) {
                        result[symbolName] = null
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
        // 1. Ссылки для стандартных путей RazomyPath
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(RazomyPath::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext
                ): Array<PsiReference> {
                    val path = element as RazomyPath
                    if (path.text == "_") return emptyArray()

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

        // 2. Ссылки внутри строковых интерполяций "{Name}", "{Err.Log}"
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(RazomyLiteral::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext
                ): Array<PsiReference> {
                    val text = element.text
                    if (!text.contains('{')) return emptyArray()

                    val refs = mutableListOf<PsiReference>()
                    for (match in RazomyAnnotator.INTERPOLATION_PATTERN.findAll(text)) {
                        val group = match.groups[1] ?: continue
                        val rawExpr = group.value
                        val trimmed = rawExpr.trim()
                        if (trimmed.isEmpty() || trimmed == "_") continue

                        val leadSpaces = rawExpr.indexOf(trimmed)
                        val baseOffset = group.range.first + leadSpaces
                        val parts = trimmed.split('.')

                        if (parts.size == 1) {
                            refs.add(
                                RazomyStringInterpolationReference(
                                    element,
                                    TextRange(baseOffset, baseOffset + trimmed.length),
                                    trimmed
                                )
                            )
                        } else {
                            var partOffset = 0
                            for (part in parts) {
                                val endOffset = partOffset + part.length
                                if (part.isNotBlank()) {
                                    val subRange = TextRange(baseOffset + partOffset, baseOffset + endOffset)
                                    val subPath = trimmed.substring(0, endOffset)
                                    refs.add(RazomyStringInterpolationReference(element, subRange, subPath))
                                }
                                partOffset = endOffset + 1
                            }
                        }
                    }
                    return refs.toTypedArray()
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
        return ResolveCache.getInstance(myElement.project).resolveWithCaching(
            this,
            ResolveCache.PolyVariantResolver { ref, _ -> ref.doMultiResolve() },
            false,
            incompleteCode
        )
    }

    private fun doMultiResolve(): Array<ResolveResult> {
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

    override fun getVariants(): Array<Any> = emptyArray()
}

class RazomyStringInterpolationReference(
    element: PsiElement,
    textRange: TextRange,
    private val resolveText: String
) : PsiReferenceBase<PsiElement>(element, textRange), PsiPolyVariantReference {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val resolved = RazomyResolver.resolvePath(myElement, resolveText)
        return if (resolved != null) arrayOf(PsiElementResolveResult(resolved)) else emptyArray()
    }

    override fun resolve(): PsiElement? {
        val results = multiResolve(false)
        return if (results.isNotEmpty()) results[0].element else null
    }

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

    override fun getVariants(): Array<Any> = emptyArray()
}


// --- FIND USAGES (ПОИСК ИСПОЛЬЗОВАНИЙ) ---

class RazomyFindUsagesProvider : FindUsagesProvider {

    override fun getWordsScanner(): WordsScanner {
        return DefaultWordsScanner(
            FlexAdapter(RazomyLexer(null)),
            TokenSet.create(RazomyTypes.IDENTIFIER),
            TokenSet.create(RazomyTypes.LINE_COMMENT),
            TokenSet.create(RazomyTypes.STRING)
        )
    }

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean {
        return psiElement is RazomyPath && RazomyUtil.isDeclaration(psiElement)
    }

    override fun getHelpId(psiElement: PsiElement): String? = null

    override fun getType(element: PsiElement): String {
        if (element !is RazomyPath) return ""
        return if (element.text.firstOrNull()?.isUpperCase() == true) "Type/Structure" else "Variable"
    }

    override fun getDescriptiveName(element: PsiElement): String {
        return element.text
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        return element.text
    }
}