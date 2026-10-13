package org.razomy.language

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiElement

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
                    if (RazomyUtil.isDeclaration(path) || path.text == "_") return emptyArray()
                    return arrayOf(RazomyReference(path, TextRange(0, element.textLength)))
                }
            }
        )
    }
}

// --- REFERENCES ---

class RazomyReference(element: RazomyPath, textRange: TextRange) : PsiReferenceBase<RazomyPath>(element, textRange),
    PsiPolyVariantReference {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        // Используем новую семантическую систему типов
        val resolved = RazomyResolver.resolvePath(myElement, myElement.text)

        if (resolved != null) {
            return arrayOf(PsiElementResolveResult(resolved))
        }
        return emptyArray()
    }

    override fun resolve(): PsiElement? {
        val results = multiResolve(false)
        return if (results.isNotEmpty()) results[0].element else null
    }

    override fun getVariants(): Array<Any> {
        // Подсказки автокомплита из текущего Scope
        val scope = RazomyTypeSystem.buildScope(myElement)
        val variants = mutableListOf<Any>()

        scope.variables.keys.forEach {
            val isType = it.firstOrNull()?.isUpperCase() == true
            variants.add(
                LookupElementBuilder.create(it).withIcon(if (isType) AllIcons.Nodes.Class else AllIcons.Nodes.Variable)
            )
        }

        scope.unpackedFields.keys.forEach {
            variants.add(LookupElementBuilder.create(it).withIcon(AllIcons.Nodes.Field).withTypeText("Auto-unpacked"))
        }

        return variants.toTypedArray()
    }
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
}

// --- СИСТЕМА ИМПОРТОВ И РЕЗОЛВА МОДУЛЕЙ ---

object RazomyImportResolver {
    fun resolveImportedFile(context: PsiElement, importPath: String): PsiFile? {
        val project = context.project
        val fileName = importPath.substringAfterLast('.') + ".rl"
        val expectedPath = importPath.replace('.', '/') + ".rl"

        val virtualFiles = FilenameIndex.getVirtualFilesByName(fileName, GlobalSearchScope.projectScope(project))
        for (vf in virtualFiles) {
            if (vf.path.replace('\\', '/').endsWith(expectedPath)) {
                return PsiManager.getInstance(project).findFile(vf)
            }
        }
        return null
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
}

// --- СИСТЕМА ТИПОВ И СЕМАНТИЧЕСКИЙ РЕЗОЛВЕР (Razomy Type System V2.6 - Modules Edition) ---

object RazomyTypeSystem {
    private val LOG = Logger.getInstance("RazomyTypeSystem")

    fun inferTypeByName(name: String, context: PsiElement): RazomyType {
        val file = context.containingFile ?: return RazomyType.Unknown
        val globalDecls = PsiTreeUtil.findChildrenOfType(file, RazomyDeclaration::class.java)

        // 1. Проверяем импорты в начале файла
        for (decl in globalDecls) {
            val leftExpr = decl.children.firstOrNull()
            val rightExpr = decl.children.getOrNull(1)

            val leftPath = PsiTreeUtil.findChildOfType(leftExpr, RazomyPath::class.java)
            val rightPath = PsiTreeUtil.findChildOfType(rightExpr, RazomyPath::class.java)
            val leftForm = PsiTreeUtil.findChildOfType(leftExpr, RazomyFormBlock::class.java)

            if (rightPath != null && rightPath.textOffset == rightExpr?.textOffset && RazomyUtil.isImportPath(rightPath.text)) {
                // Alias import: Age package.age
                if (leftPath != null && leftPath.text == name && leftPath.textOffset == leftExpr?.textOffset) {
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

                // Форма { ... }
                val formBlock = PsiTreeUtil.findChildOfType(decl, RazomyFormBlock::class.java)
                if (formBlock != null) {
                    val fields = mutableMapOf<String, PsiElement>()
                    val items = PsiTreeUtil.findChildrenOfType(formBlock, RazomyFormItem::class.java)
                    for (item in items) {
                        val fieldPath = PsiTreeUtil.findChildOfType(item, RazomyPath::class.java)
                        if (fieldPath != null) {
                            fields[fieldPath.text] = fieldPath
                        }
                    }
                    return RazomyType.Form(name, path, fields)
                }

                // Алиас / Наследование (User & Identifiable)
                val allPaths = PsiTreeUtil.findChildrenOfType(decl, RazomyPath::class.java).toList()
                val baseTypePath = allPaths.drop(1).firstOrNull { it.text.firstOrNull()?.isUpperCase() == true }
                if (baseTypePath != null) {
                    return inferTypeByName(baseTypePath.text, context)
                }
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

                if (leftPath != null && rightPath != null && leftPath.textOffset == leftExpr?.textOffset && rightPath.textOffset == rightExpr?.textOffset && RazomyUtil.isImportPath(
                        rightPath.text
                    )
                ) {
                    val alias = leftPath.text
                    scope.variables[alias] = leftPath
                    scope.imports[alias] = rightPath
                } else if (leftForm != null && rightPath != null && leftForm.textOffset == leftExpr?.textOffset && rightPath.textOffset == rightExpr?.textOffset && RazomyUtil.isImportPath(
                        rightPath.text
                    )
                ) {
                    val items = PsiTreeUtil.findChildrenOfType(leftForm, RazomyFormItem::class.java)
                    for (item in items) {
                        val path = PsiTreeUtil.findChildOfType(item, RazomyPath::class.java)
                        if (path != null) {
                            scope.variables[path.text] = path
                            scope.imports[path.text] = rightPath
                            scope.isDestructuredImport[path.text] = true
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

        if (current == null) return null

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


// --- ANNOTATOR ---

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

        // ВЫДЕЛЕНИЕ ИМПОРТОВ (Спец. Обработка)
        val parentExpr = element.parent as? RazomyExpr
        val grandParent = parentExpr?.parent
        if (grandParent is RazomyDeclaration) {
            val leftExpr = grandParent.children.firstOrNull()
            val rightExpr = grandParent.children.getOrNull(1)

            val rightPath = PsiTreeUtil.findChildOfType(rightExpr, RazomyPath::class.java)
            if (rightPath != null && rightPath.textOffset == rightExpr?.textOffset && RazomyUtil.isImportPath(rightPath.text)) {
                // Если красим правую часть (Путь к файлу)
                if (element == rightPath) {
                    highlight(element, holder, COLOR_IMPORT)
                    return
                }
                // Если красим левую часть (Алиас модуля)
                if (leftExpr != null && leftExpr.textOffset == element.textOffset) {
                    highlight(element, holder, COLOR_MODULE)
                    return
                }
                // Если красим внутренности деструктуризации { Old }
                val leftForm = PsiTreeUtil.findChildOfType(leftExpr, RazomyFormBlock::class.java)
                if (leftForm != null && PsiTreeUtil.isAncestor(leftForm, element, false)) {
                    highlight(element, holder, COLOR_TYPE)
                    return
                }
            }
        }

        if (!isDecl) {
            val resolved = RazomyResolver.resolvePath(element, text)
            val scope = RazomyTypeSystem.buildScope(element)
            val isResolvedLocally =
                !isMemberAccess && (scope.variables.containsKey(text) || scope.unpackedFields.containsKey(text))

            if (resolved == null && !isResolvedLocally) {
                if (isMemberAccess) {
                    val parts = text.split(".")
                    holder.newAnnotation(
                        HighlightSeverity.ERROR,
                        "Неразрешенное поле: '${parts.last()}' в '${parts.dropLast(1).joinToString(".")}'"
                    )
                        .range(element.textRange)
                        .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                        .create()
                } else {
                    holder.newAnnotation(HighlightSeverity.ERROR, "Неразрешенная ссылка: '$text'")
                        .range(element.textRange)
                        .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                        .create()
                }
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
