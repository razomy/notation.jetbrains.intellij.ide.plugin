package org.razomy.language

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.lexer.FlexAdapter
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import org.razomy.language.psi.*

// --- SYNTAX HIGHLIGHTER (Зависит от токенов в .flex / .bnf) ---

class RazomySyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = FlexAdapter(RazomyLexer(null))

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            RazomyTypes.TRUE, RazomyTypes.FALSE, RazomyTypes.UNDERSCORE -> arrayOf(DefaultLanguageHighlighterColors.KEYWORD)
            RazomyTypes.IDENTIFIER -> arrayOf(DefaultLanguageHighlighterColors.IDENTIFIER)
            RazomyTypes.STRING -> arrayOf(DefaultLanguageHighlighterColors.STRING)
            RazomyTypes.NUMBER -> arrayOf(DefaultLanguageHighlighterColors.NUMBER)
            RazomyTypes.LINE_COMMENT -> arrayOf(DefaultLanguageHighlighterColors.LINE_COMMENT)

            // Все операторы синтаксиса
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

// --- FORMATTER (Зависит от операторов и блочных конструкций) ---

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

// --- BRACE MATCHER & FOLDING (Зависят от скобочных блоков синтаксиса) ---

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

// --- INLAY HINTS (Зависит от синтаксиса вызовов и форм) ---

class RazomyInlayParameterHintsProvider : InlayParameterHintsProvider {

    override fun getParameterHints(element: PsiElement): MutableList<InlayInfo> {
        val hints = mutableListOf<InlayInfo>()

        if (element !is RazomyCallExpr) return hints
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

        val typedForm = PsiTreeUtil.getParentOfType(targetPath, RazomyTypedFormBlock::class.java)
        val formBlock = typedForm?.let { PsiTreeUtil.findChildOfType(it, RazomyFormBlock::class.java) }
            ?: PsiTreeUtil.getParentOfType(targetPath, RazomyDeclaration::class.java)
                ?.let { PsiTreeUtil.findChildOfType(it, RazomyFormBlock::class.java) }

        val targetCall = PsiTreeUtil.getParentOfType(targetPath, RazomyCallExpr::class.java)
        if (targetCall != null && targetCall.textOffset == targetPath.textOffset) {
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

// --- UTILITY (РАСПОЗНАВАНИЕ СИНТАКСИЧЕСКИХ КОНСТРУКЦИЙ AST) ---

object RazomyUtil {

    val IDENTIFIER_REGEX = Regex("""[a-zA-Z_][a-zA-Z0-9_.]*""")

    fun isRouterFormBlock(formBlock: RazomyFormBlock): Boolean {
        if (formBlock.parent is RazomyTypedFormBlock) return false
        val pipelineExpr = PsiTreeUtil.getParentOfType(formBlock, RazomyPipelineExpr::class.java) ?: return false
        val ops = PsiTreeUtil.getChildrenOfType(pipelineExpr, RazomyPipelineOp::class.java) ?: return false
        return ops.any { it.textOffset < formBlock.textOffset }
    }

    fun isInstantiationFormBlock(formBlock: RazomyFormBlock): Boolean {
        val typedForm = formBlock.parent as? RazomyTypedFormBlock ?: return false
        if (PsiTreeUtil.getParentOfType(typedForm, RazomyFlowBlock::class.java) != null) {
            return true
        }
        val migrateExpr = PsiTreeUtil.getParentOfType(typedForm, RazomyMigrateExpr::class.java)
        if (migrateExpr != null && typedForm.textOffset > migrateExpr.textOffset) {
            return true
        }
        val container = PsiTreeUtil.getParentOfType(typedForm, RazomyDeclaration::class.java, RazomyFormItem::class.java)
        if (container != null && container.children.size >= 2) {
            val rightExpr = container.children[1]
            if (PsiTreeUtil.isAncestor(rightExpr, typedForm, false)) {
                return true
            }
        }
        if (PsiTreeUtil.getParentOfType(typedForm, RazomySlot::class.java) != null) {
            return true
        }
        return false
    }

    fun isParameterDeclarationSlot(slot: RazomySlot): Boolean {
        val callExpr = slot.parent as? RazomyCallExpr ?: return false

        val container = PsiTreeUtil.getParentOfType(
            callExpr,
            RazomyDeclaration::class.java,
            RazomyFormItem::class.java,
            RazomyFlowBlock::class.java,
            RazomySlot::class.java
        ) ?: return false

        if (container is RazomyDeclaration || container is RazomyFormItem) {
            val children = container.children
            if (children.size < 2) return false
            val leftExpr = children[0]
            return PsiTreeUtil.isAncestor(leftExpr, callExpr, false) &&
                    callExpr.textOffset == leftExpr.textOffset &&
                    callExpr.textLength == leftExpr.textLength
        }
        return false
    }

    fun isDeclaration(path: RazomyPath): Boolean {
        val text = path.text
        if (text == "_" || text.startsWith("_.") || text.contains(".")) return false

        // 1. Путь внутри слота (...)
        val slotItem = PsiTreeUtil.getParentOfType(path, RazomySlotItem::class.java)
        if (slotItem != null) {
            val slot = slotItem.parent as? RazomySlot ?: return false
            if (!isParameterDeclarationSlot(slot)) return false

            val mergeExpr = PsiTreeUtil.findChildOfType(slotItem, RazomyMergeExpr::class.java)
            if (mergeExpr != null && mergeExpr.node.findChildByType(RazomyTypes.AMP) != null) {
                return false
            }

            val slotPaths = PsiTreeUtil.findChildrenOfType(slotItem, RazomyPath::class.java).toList()
            if (slotPaths.size == 1 && text.firstOrNull()?.isUpperCase() == true) {
                val file = path.containingFile
                if (file != null && RazomyTypeSystem.getGlobalDeclarationsMap(file).containsKey(text)) {
                    return false
                }
            }

            val firstExpr = slotItem.children.firstOrNull() ?: return false
            val firstPath = PsiTreeUtil.findChildOfType(firstExpr, RazomyPath::class.java)
            return firstPath == path && path.textOffset == firstExpr.textOffset
        }

        // 2. Путь внутри формы { ... }
        val formItem = PsiTreeUtil.getParentOfType(path, RazomyFormItem::class.java)
        if (formItem != null) {
            val firstExpr = formItem.children.firstOrNull() ?: return false
            val firstPath = PsiTreeUtil.findChildOfType(firstExpr, RazomyPath::class.java)
            if (firstPath != path || path.textOffset != firstExpr.textOffset) return false

            val formBlock = formItem.parent as? RazomyFormBlock ?: return false

            if (isRouterFormBlock(formBlock)) return false

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
        if (text.isBlank() || text == "_" || text in RazomyTypeSystem.BUILTIN_TYPES) return false
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

    fun extractInterpolatedPaths(exprText: String): List<Pair<String, IntRange>> {
        val result = mutableListOf<Pair<String, IntRange>>()
        for (m in IDENTIFIER_REGEX.findAll(exprText)) {
            val token = m.value
            if (token != "true" && token != "false" && token != "_" && !token.first().isDigit()) {
                result.add(token to m.range)
            }
        }
        return result
    }

    fun isUsedInStringInterpolations(file: PsiFile, symbolName: String): Boolean {
        if (symbolName == "Start" || symbolName == "Main") return true
        val literals = PsiTreeUtil.findChildrenOfType(file, RazomyLiteral::class.java)
        for (lit in literals) {
            val text = lit.text
            if (!text.contains('{')) continue
            for (match in RazomyAnnotator.INTERPOLATION_PATTERN.findAll(text)) {
                val rawExpr = match.groups[1]?.value ?: continue
                for ((token, _) in extractInterpolatedPaths(rawExpr)) {
                    if (token == symbolName || token.startsWith("$symbolName.") || token.split('.').contains(symbolName)) {
                        return true
                    }
                }
            }
        }
        return false
    }
}

// --- ANNOTATOR (Семантическая подсветка и проверка типов выражений) ---

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

        if (element is RazomyPipelineExpr && PsiTreeUtil.getChildOfType(element, RazomyPipelineOp::class.java) != null) {
            checkPipelineTypes(element, holder)
            return
        }

        if (element is RazomyMigrateExpr && element.node.findChildByType(RazomyTypes.MIGRATION) != null) {
            checkMigrationTypes(element, holder)
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
                !isMemberAccess && (
                        scope.variables.containsKey(text) ||
                                scope.unpackedFields.containsKey(text) ||
                                scope.isDestructuredImport.containsKey(text) ||
                                scope.builtInTypes.contains(text)
                        )

            if (resolved == null && !isResolvedLocally) {
                val unresolvedName = if (isMemberAccess) text.split(".").last() else text

                val errorBuilder =
                    holder.newAnnotation(HighlightSeverity.ERROR, "Неразрешенная ссылка: '$unresolvedName'")
                        .range(element.textRange)
                        .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)

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

    private fun checkPipelineTypes(pipeline: RazomyPipelineExpr, holder: AnnotationHolder) {
        val stages = pipeline.children.filter { it !is RazomyPipelineOp }
        val ops = pipeline.children.filterIsInstance<RazomyPipelineOp>()
        if (stages.size < 2) return

        var currentType = RazomyTypeSystem.inferExprType(stages[0])

        for (i in 0 until ops.size) {
            val opText = ops[i].text.trim()
            val nextStage = stages.getOrNull(i + 1) ?: break

            val stagePath = (nextStage as? RazomyPath) ?: PsiTreeUtil.findChildOfType(nextStage, RazomyPath::class.java)
            if (opText == "." && stagePath != null && stagePath.textOffset == nextStage.textOffset && stagePath.textLength == nextStage.textLength) {
                val resolved = RazomyResolver.resolvePath(stagePath, stagePath.text)
                val paramSlot = resolved?.let { PsiTreeUtil.getParentOfType(it, RazomySlot::class.java) }
                if (paramSlot != null && RazomyUtil.isParameterDeclarationSlot(paramSlot) && currentType !is RazomyType.Unknown) {
                    val varType = RazomyTypeSystem.inferTypeOfElement(resolved)
                    holder.newAnnotation(
                        HighlightSeverity.WARNING,
                        "Переменная '${stagePath.text}' (${varType.presentableName()}) не является функцией и не принимает входящий тип '${currentType.presentableName()}'. Используйте '.>' перед предыдущим сайд-эффектом или передайте '${stagePath.text}' с новой строки."
                    ).range(nextStage.textRange).create()
                    currentType = varType
                    continue
                }
            }

            val expectedInput = RazomyTypeSystem.getExpectedInputTypeOfStage(nextStage)
            if (expectedInput != null && currentType !is RazomyType.Unknown) {
                if (!RazomyTypeSystem.isTypeCompatible(currentType, expectedInput, nextStage)) {
                    holder.newAnnotation(
                        HighlightSeverity.WARNING,
                        "Несовпадение типов конвейера: этап '${nextStage.text.take(30)}' ожидает '${expectedInput.presentableName()}', но текущий тип потока — '${currentType.presentableName()}' (нет прямого совпадения или маппинга ->)"
                    ).range(nextStage.textRange).create()
                }
            }

            val stageOut = RazomyTypeSystem.inferExprType(nextStage, currentType)
            currentType = when (opText) {
                ".>" -> currentType
                ".&" -> if (currentType !is RazomyType.Unknown && stageOut !is RazomyType.Unknown) {
                    RazomyType.Intersection(currentType, stageOut)
                } else stageOut
                else -> stageOut
            }
        }
    }

    private fun checkMigrationTypes(migrate: RazomyMigrateExpr, holder: AnnotationHolder) {
        val parts = migrate.children
        if (parts.size < 2) return

        val sourceExpr = parts[0]
        val targetExpr = parts.last()

        val sourceType = RazomyTypeSystem.inferExprType(sourceExpr)
        if (sourceType is RazomyType.Unknown) return

        val typedForm = (targetExpr as? RazomyTypedFormBlock)
            ?: PsiTreeUtil.findChildOfType(targetExpr, RazomyTypedFormBlock::class.java) ?: return
        val targetTypePath = PsiTreeUtil.getChildOfType(typedForm, RazomyPath::class.java) ?: return
        val targetType = RazomyTypeSystem.inferTypeByName(targetTypePath.text, migrate)
        if (targetType is RazomyType.Unknown) return

        val sourceFields = RazomyType.extractFields(sourceType).keys
        val requiredTargetFields = RazomyType.extractFields(targetType).keys

        val formBlock = PsiTreeUtil.getChildOfType(typedForm, RazomyFormBlock::class.java) ?: return
        val explicitlyMappedFields = formBlock.children.filterIsInstance<RazomyFormItem>().mapNotNull { item ->
            val firstExpr = item.children.firstOrNull()
            PsiTreeUtil.findChildOfType(firstExpr, RazomyPath::class.java)?.text
        }.toSet()

        val missingFields = requiredTargetFields.filter { it !in explicitlyMappedFields && it !in sourceFields }
        if (missingFields.isNotEmpty()) {
            holder.newAnnotation(
                HighlightSeverity.WARNING,
                "Миграция в '${targetTypePath.text}' неполная: поля ${missingFields.joinToString()} отсутствуют в исходном типе '${sourceType.presentableName()}' и не заданы явно в блоке миграции"
            ).range(targetTypePath.textRange).create()
        }
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

// --- СИСТЕМА ТИПОВ И СЕМАНТИЧЕСКИЙ РЕЗОЛВЕР ---

class RazomyScope(
    val variables: MutableMap<String, PsiElement> = mutableMapOf(),
    val unpackedFields: MutableMap<String, PsiElement> = mutableMapOf(),
    val imports: MutableMap<String, RazomyPath> = mutableMapOf(),
    val isDestructuredImport: MutableMap<String, Boolean> = mutableMapOf(),
    val builtInTypes: MutableSet<String> = mutableSetOf()
)

sealed class RazomyType {
    class Primitive(val name: String, val supertypes: Set<String> = setOf(name)) : RazomyType()
    class Form(
        val name: String,
        val declaration: PsiElement,
        val fields: Map<String, PsiElement>,
        val supertypes: Set<String> = setOf(name)
    ) : RazomyType()

    class Module(val file: PsiFile) : RazomyType()
    class Union(val left: RazomyType, val right: RazomyType, val aliasName: String? = null) : RazomyType() {
        fun getResolvedFields(): Map<String, PsiElement> {
            val result = LinkedHashMap<String, PsiElement>()
            result.putAll(extractFields(left))
            result.putAll(extractFields(right))
            return result
        }

        fun flattenUnion(): List<RazomyType> {
            val list = mutableListOf<RazomyType>()
            if (left is Union) list.addAll(left.flattenUnion()) else list.add(left)
            if (right is Union) list.addAll(right.flattenUnion()) else list.add(right)
            return list
        }
    }

    class Intersection(val left: RazomyType, val right: RazomyType, val aliasName: String? = null) : RazomyType() {
        fun getResolvedFields(): Map<String, PsiElement> {
            val leftFields = extractFields(left)
            val rightFields = extractFields(right)
            val result = LinkedHashMap<String, PsiElement>()
            result.putAll(leftFields.filterKeys { it !in rightFields.keys })
            result.putAll(rightFields.filterKeys { it !in leftFields.keys })
            return result
        }
    }

    object Unknown : RazomyType()

    fun presentableName(): String = when (this) {
        is Primitive -> name
        is Form -> name
        is Module -> "Module(${file.name})"
        is Union -> aliasName ?: "${left.presentableName()} | ${right.presentableName()}"
        is Intersection -> aliasName ?: "${left.presentableName()} & ${right.presentableName()}"
        is Unknown -> "Unknown"
    }

    fun allTypeNames(): Set<String> = when (this) {
        is Primitive -> if (name == "Str" || name == "String") setOf("Str", "String") else supertypes
        is Form -> supertypes + name
        is Union -> setOfNotNull(aliasName) + left.allTypeNames() + right.allTypeNames()
        is Intersection -> setOfNotNull(aliasName) + left.allTypeNames() + right.allTypeNames()
        else -> emptySet()
    }

    fun withAlias(newAlias: String, extraSupertypes: Set<String>): RazomyType = when (this) {
        is Form -> Form(newAlias, declaration, fields, supertypes + extraSupertypes + name + newAlias)
        is Primitive -> Primitive(newAlias, supertypes + extraSupertypes + name + newAlias)
        is Union -> Union(left, right, newAlias)
        is Intersection -> Intersection(left, right, newAlias)
        else -> this
    }

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

object RazomyTypeSystem {
    val BUILTIN_TYPES = setOf("String", "Number", "Bool", "None", "Any")

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

        val normalizedName = if (name == "String") "Str" else name
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
                if (leftPath != null && leftPath.text == "_" && leftPath.textOffset == leftExpr?.textOffset) {
                    val importedFile = RazomyImportResolver.resolveImportedFile(context, rightPath.text)
                    if (importedFile != null) {
                        val foreignSymbol = RazomyImportResolver.findExportedSymbol(importedFile, name)
                        if (foreignSymbol != null) return inferTypeOfElement(foreignSymbol, HashSet(visited))
                    }
                } else if (leftPath != null && leftPath.text == name && leftPath.textOffset == leftExpr?.textOffset) {
                    val importedFile = RazomyImportResolver.resolveImportedFile(context, rightPath.text)
                    return if (importedFile != null) RazomyType.Module(importedFile) else RazomyType.Unknown
                } else if (leftForm != null && leftForm.textOffset == leftExpr?.textOffset) {
                    val items = PsiTreeUtil.findChildrenOfType(leftForm, RazomyFormItem::class.java)
                    if (items.any { PsiTreeUtil.findChildOfType(it, RazomyPath::class.java)?.text == name }) {
                        val importedFile = RazomyImportResolver.resolveImportedFile(context, rightPath.text)
                        if (importedFile != null) {
                            val foreignSymbol = RazomyImportResolver.findExportedSymbol(importedFile, name)
                            if (foreignSymbol != null) return inferTypeOfElement(foreignSymbol, HashSet(visited))
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
            if ((path?.text == name || path?.text == normalizedName) && path.textOffset == leftExpr?.textOffset) {
                return evaluateDeclarationType(path.text, path, decl, context, HashSet(visited))
            }
        }

        if (name in BUILTIN_TYPES) {
            return RazomyType.Primitive(normalizedName)
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
        val typedFlow = PsiTreeUtil.findChildOfType(decl, RazomyTypedFlowBlock::class.java)
        if (typedFlow != null && typedFlow.textOffset == decl.textOffset) {
            if (!visited.add("fn:$name")) return RazomyType.Unknown
            return inferExprType(typedFlow.flowBlock, RazomyType.Unknown, HashSet(visited))
        }

        val directForm = PsiTreeUtil.findChildOfType(decl, RazomyFormBlock::class.java)
        if (directForm != null && !RazomyUtil.isRouterFormBlock(directForm) &&
            PsiTreeUtil.getParentOfType(directForm, RazomyFlowBlock::class.java) == null
        ) {
            val fields = extractFormBlockFields(directForm)
            return RazomyType.Form(name, declPath, fields)
        }

        val rightExpr = decl.children.getOrNull(1) ?: return RazomyType.Primitive(name)

        val rightFlow = (rightExpr as? RazomyFlowBlock)
            ?: PsiTreeUtil.findChildOfType(rightExpr, RazomyFlowBlock::class.java)
        if (rightFlow != null && rightFlow.textOffset == rightExpr.textOffset) {
            if (!visited.add("fn:$name")) return RazomyType.Unknown
            return inferExprType(rightFlow, RazomyType.Unknown, HashSet(visited))
        }

        val literal = PsiTreeUtil.findChildOfType(rightExpr, RazomyLiteral::class.java)
        if (literal != null && literal.textOffset == rightExpr.textOffset && literal.textLength == rightExpr.textLength) {
            return RazomyType.Primitive(if (name == "String") "Str" else name)
        }

        val subtractExpr = PsiTreeUtil.findChildOfType(rightExpr, RazomySubtractExpr::class.java, false)
        if (subtractExpr != null && subtractExpr.node.findChildByType(RazomyTypes.SLASH) != null) {
            val subChildren = subtractExpr.children
            if (subChildren.size >= 2) {
                val basePath = PsiTreeUtil.findChildOfType(subChildren[0], RazomyPath::class.java)
                val baseType = if (basePath != null) inferTypeByName(basePath.text, context, HashSet(visited)) else RazomyType.Unknown
                val baseFields = RazomyType.extractFields(baseType).toMutableMap()
                for (i in 1 until subChildren.size) {
                    val removedPath = PsiTreeUtil.findChildOfType(subChildren[i], RazomyPath::class.java)?.text ?: continue
                    val fieldName = removedPath.substringAfterLast('.')
                    baseFields.remove(fieldName)
                }
                return RazomyType.Form(name, declPath, baseFields, supertypes = setOfNotNull(basePath?.text))
            }
        }

        val mergeExpr = PsiTreeUtil.findChildOfType(rightExpr, RazomyMergeExpr::class.java, false)
        if (mergeExpr != null && mergeExpr.node.findChildByType(RazomyTypes.AMP) != null) {
            val operands = mergeExpr.children
            var currentType: RazomyType = RazomyType.Unknown
            val parentNames = mutableSetOf(name)

            for (op in operands) {
                val opForm = PsiTreeUtil.findChildOfType(op, RazomyFormBlock::class.java)
                val nextType = if (opForm != null && PsiTreeUtil.getParentOfType(opForm, RazomyFlowBlock::class.java) == null) {
                    RazomyType.Form(name, declPath, extractFormBlockFields(opForm))
                } else if (PsiTreeUtil.findChildOfType(op, RazomyFlowBlock::class.java) != null) {
                    continue
                } else {
                    val opPath = PsiTreeUtil.findChildOfType(op, RazomyPath::class.java)
                    if (opPath != null && opPath.text.firstOrNull()?.isUpperCase() == true) {
                        parentNames.add(opPath.text)
                        inferTypeByName(opPath.text, context, HashSet(visited))
                    } else RazomyType.Unknown
                }
                if (nextType !is RazomyType.Unknown) {
                    currentType = if (currentType is RazomyType.Unknown) {
                        nextType.withAlias(name, parentNames)
                    } else {
                        RazomyType.Intersection(currentType, nextType, name)
                    }
                }
            }
            if (currentType !is RazomyType.Unknown) return currentType
        }

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
                    currentType = if (currentType is RazomyType.Unknown) nextType else RazomyType.Union(currentType, nextType, name)
                }
            }
            if (currentType !is RazomyType.Unknown) return currentType
        }

        val allPaths = PsiTreeUtil.findChildrenOfType(rightExpr, RazomyPath::class.java).toList()
        val baseTypePath = allPaths.firstOrNull { it.text.firstOrNull()?.isUpperCase() == true }
        if (baseTypePath != null && PsiTreeUtil.findChildOfType(rightExpr, RazomyFlowBlock::class.java) == null) {
            return inferTypeByName(baseTypePath.text, context, HashSet(visited)).withAlias(name, setOf(name, baseTypePath.text))
        }

        return RazomyType.Unknown
    }

    fun inferExprType(
        expr: PsiElement?,
        inputType: RazomyType = RazomyType.Unknown,
        visited: MutableSet<String> = mutableSetOf()
    ): RazomyType {
        if (expr == null) return RazomyType.Unknown

        val exprKey = "expr:${expr.containingFile?.name}:${expr.textRange.startOffset}:${expr.textRange.endOffset}"
        if (!visited.add(exprKey)) return RazomyType.Unknown

        val literal = (expr as? RazomyLiteral) ?: PsiTreeUtil.findChildOfType(expr, RazomyLiteral::class.java)
        if (literal != null && literal.textOffset == expr.textOffset && literal.textLength == expr.textLength) {
            val t = literal.text
            return when {
                t.startsWith("\"") || t.startsWith("'") -> RazomyType.Primitive("Str")
                t == "true" || t == "false" -> RazomyType.Primitive("Bool")
                t.contains(".") -> RazomyType.Primitive("Float")
                else -> RazomyType.Primitive("Int")
            }
        }

        val pipeline = (expr as? RazomyPipelineExpr)
            ?: if (expr.children.size == 1) expr.children[0] as? RazomyPipelineExpr else null
        if (pipeline != null && PsiTreeUtil.getChildOfType(pipeline, RazomyPipelineOp::class.java) != null) {
            val stages = pipeline.children.filter { it !is RazomyPipelineOp }
            val ops = pipeline.children.filterIsInstance<RazomyPipelineOp>()
            if (stages.isNotEmpty()) {
                var current = inferExprType(stages[0], inputType, HashSet(visited))
                for (i in 0 until ops.size) {
                    val opText = ops[i].text.trim()
                    val nextStage = stages.getOrNull(i + 1) ?: break
                    val stageOut = inferExprType(nextStage, current, HashSet(visited))
                    current = when (opText) {
                        ".>" -> current
                        ".&" -> if (current !is RazomyType.Unknown && stageOut !is RazomyType.Unknown) {
                            RazomyType.Intersection(current, stageOut)
                        } else stageOut
                        else -> applyBypassRule(current, nextStage, stageOut, HashSet(visited))
                    }
                }
                return current
            }
        }

        val migrate = (expr as? RazomyMigrateExpr) ?: PsiTreeUtil.findChildOfType(expr, RazomyMigrateExpr::class.java)
        if (migrate != null && migrate.node.findChildByType(RazomyTypes.MIGRATION) != null && migrate.textOffset == expr.textOffset) {
            val parts = migrate.children
            if (parts.size >= 2) {
                return inferExprType(parts.last(), inputType, HashSet(visited))
            }
        }

        val typedForm = (expr as? RazomyTypedFormBlock) ?: PsiTreeUtil.findChildOfType(expr, RazomyTypedFormBlock::class.java)
        if (typedForm != null && typedForm.textOffset == expr.textOffset && typedForm.textLength == expr.textLength) {
            val typePath = PsiTreeUtil.getChildOfType(typedForm, RazomyPath::class.java)
            if (typePath != null) {
                val resolved = inferTypeByName(typePath.text, expr, HashSet(visited))
                if (resolved !is RazomyType.Unknown) return resolved
                val formBlock = PsiTreeUtil.getChildOfType(typedForm, RazomyFormBlock::class.java)
                val fields = if (formBlock != null) extractFormBlockFields(formBlock) else emptyMap()
                return RazomyType.Form(typePath.text, typePath, fields)
            }
        }

        val formBlock = (expr as? RazomyFormBlock) ?: PsiTreeUtil.findChildOfType(expr, RazomyFormBlock::class.java)
        if (formBlock != null && formBlock.textOffset == expr.textOffset && formBlock.textLength == expr.textLength) {
            if (RazomyUtil.isRouterFormBlock(formBlock)) {
                var unionResult: RazomyType = RazomyType.Unknown
                val items = formBlock.children.filterIsInstance<RazomyFormItem>()
                for (item in items) {
                    val branchBody = item.children.getOrNull(1) ?: continue
                    val branchType = inferExprType(branchBody, inputType, HashSet(visited))
                    if (branchType !is RazomyType.Unknown) {
                        unionResult = if (unionResult is RazomyType.Unknown) branchType else RazomyType.Union(unionResult, branchType)
                    }
                }
                return unionResult
            } else {
                return RazomyType.Form("AnonymousForm", formBlock, extractFormBlockFields(formBlock))
            }
        }

        val flowBlock = (expr as? RazomyFlowBlock) ?: PsiTreeUtil.findChildOfType(expr, RazomyFlowBlock::class.java)
        if (flowBlock != null && flowBlock.textOffset == expr.textOffset && flowBlock.textLength == expr.textLength) {
            val flowItems = flowBlock.children.filterIsInstance<RazomyFlowItem>()
            val lastItem = flowItems.lastOrNull() ?: return RazomyType.Unknown
            return inferExprType(lastItem.children.firstOrNull() ?: lastItem, inputType, HashSet(visited))
        }

        val path = (expr as? RazomyPath) ?: PsiTreeUtil.findChildOfType(expr, RazomyPath::class.java)
        if (path != null && path.textOffset == expr.textOffset) {
            if (path.text.contains("native_print") || path.text.endsWith("Shell.Log")) {
                return RazomyType.Primitive("Unit")
            }
            if (path.text.contains("native_exec") || path.text.endsWith("Shell.Exec")) {
                return RazomyType.Primitive("Str")
            }

            val resolved = RazomyResolver.resolvePath(path, path.text, HashSet(visited))
            if (resolved != null) {
                val typedFlow = resolved.parent as? RazomyTypedFlowBlock
                if (typedFlow != null && typedFlow.path == resolved) {
                    if (visited.add("fn:${resolved.text}")) {
                        return inferExprType(typedFlow.flowBlock, inputType, HashSet(visited))
                    }
                    return RazomyType.Unknown
                }

                val decl = PsiTreeUtil.getParentOfType(resolved, RazomyDeclaration::class.java, RazomyFormItem::class.java)
                if (decl != null && decl.children.firstOrNull()?.textOffset == resolved.textOffset && decl.children.size >= 2) {
                    val rightBody = decl.children[1]
                    val bodyFlow = (rightBody as? RazomyFlowBlock) ?: PsiTreeUtil.findChildOfType(rightBody, RazomyFlowBlock::class.java)
                    if (bodyFlow != null) {
                        if (visited.add("fn:${resolved.text}")) {
                            return inferExprType(bodyFlow, inputType, HashSet(visited))
                        }
                        return RazomyType.Unknown
                    }
                }
                return inferTypeOfElement(resolved, HashSet(visited))
            }
        }

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
        val elemKey = "elem:${element.containingFile?.name}:${element.textOffset}"
        if (!visited.add(elemKey)) return RazomyType.Unknown

        if (element is RazomyPath) {
            val slotItem = PsiTreeUtil.getParentOfType(element, RazomySlotItem::class.java)
            if (slotItem != null) {
                val paths = PsiTreeUtil.findChildrenOfType(slotItem, RazomyPath::class.java).toList()
                if (paths.size > 1 && paths[0] == element) {
                    return inferTypeByName(paths[1].text, element, HashSet(visited))
                }

                val slot = slotItem.parent as? RazomySlot
                if (slot != null && RazomyUtil.isParameterDeclarationSlot(slot)) {
                    val parentDecl = PsiTreeUtil.getParentOfType(slot, RazomyDeclaration::class.java, RazomyFormItem::class.java)
                    val declName = PsiTreeUtil.findChildOfType(parentDecl?.children?.firstOrNull(), RazomyPath::class.java)?.text
                    if (declName != null) {
                        visited.add(declName)
                        visited.add("fn:$declName")
                    }
                    if (paths.size == 1 && paths[0] == element) {
                        val file = element.containingFile
                        val globalDecl = file?.let { getGlobalDeclarationsMap(it)[element.text] }
                        if (globalDecl == null || globalDecl == element || PsiTreeUtil.isAncestor(parentDecl, globalDecl, false)) {
                            return RazomyType.Unknown
                        }
                    }
                } else {
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

            val formItem = PsiTreeUtil.getParentOfType(element, RazomyFormItem::class.java)
            if (formItem != null && formItem.children.size >= 2) {
                val firstExpr = formItem.children[0]
                if (element.textOffset == firstExpr.textOffset) {
                    val rightExpr = formItem.children[1]
                    val typePath = PsiTreeUtil.findChildOfType(rightExpr, RazomyPath::class.java)
                    if (typePath != null && typePath.text.firstOrNull()?.isUpperCase() == true) {
                        return inferTypeByName(typePath.text, element, HashSet(visited))
                    }
                    return inferExprType(rightExpr, RazomyType.Unknown, HashSet(visited))
                }
            }
        }

        val text = element.text
        if (text.firstOrNull()?.isUpperCase() == true) {
            val resolvedType = inferTypeByName(text, element, HashSet(visited))
            if (resolvedType !is RazomyType.Unknown) return resolvedType
        }
        return RazomyType.Unknown
    }

    private fun applyBypassRule(
        inputType: RazomyType,
        stage: PsiElement,
        stageOutput: RazomyType,
        visited: MutableSet<String> = mutableSetOf()
    ): RazomyType {
        if (inputType !is RazomyType.Union || stageOutput is RazomyType.Unknown) return stageOutput
        val expectedInput = getExpectedInputTypeOfStage(stage, HashSet(visited)) ?: return stageOutput
        val bypassedBranches = inputType.flattenUnion().filter { !isTypeCompatible(it, expectedInput, stage) }
        var result = stageOutput
        for (b in bypassedBranches) {
            result = RazomyType.Union(result, b)
        }
        return result
    }

    fun getExpectedInputTypeOfStage(
        stage: PsiElement,
        visited: MutableSet<String> = mutableSetOf()
    ): RazomyType? {
        val formBlock = (stage as? RazomyFormBlock) ?: PsiTreeUtil.findChildOfType(stage, RazomyFormBlock::class.java)
        if (formBlock != null && formBlock.textOffset == stage.textOffset && RazomyUtil.isRouterFormBlock(formBlock)) {
            val items = formBlock.children.filterIsInstance<RazomyFormItem>()
            var expectedUnion: RazomyType = RazomyType.Unknown
            for (item in items) {
                val firstExpr = item.children.firstOrNull() ?: continue
                val path = PsiTreeUtil.findChildOfType(firstExpr, RazomyPath::class.java)
                if (path != null) {
                    if (path.text == "_") return null
                    if (path.text.firstOrNull()?.isUpperCase() == true) {
                        val branchType = inferTypeByName(path.text, stage, HashSet(visited))
                        if (branchType !is RazomyType.Unknown) {
                            expectedUnion = if (expectedUnion is RazomyType.Unknown) branchType else RazomyType.Union(expectedUnion, branchType)
                        }
                    }
                }
            }
            return if (expectedUnion !is RazomyType.Unknown) expectedUnion else null
        }

        val path = (stage as? RazomyPath) ?: PsiTreeUtil.findChildOfType(stage, RazomyPath::class.java)
        if (path != null && path.textOffset == stage.textOffset) {
            if (path.text.startsWith("native_")) return null

            val resolved = RazomyResolver.resolvePath(path, path.text, HashSet(visited)) ?: return null

            val slotParent = PsiTreeUtil.getParentOfType(resolved, RazomySlot::class.java)
            if (slotParent != null && RazomyUtil.isParameterDeclarationSlot(slotParent)) {
                return inferTypeOfElement(resolved, HashSet(visited))
            }

            val decl = PsiTreeUtil.getParentOfType(resolved, RazomyDeclaration::class.java, RazomyFormItem::class.java)
            if (decl != null && decl.children.firstOrNull()?.textOffset == resolved.textOffset) {
                val leftExpr = decl.children.firstOrNull()
                val slot = PsiTreeUtil.findChildOfType(leftExpr, RazomySlot::class.java)
                if (slot != null && RazomyUtil.isParameterDeclarationSlot(slot)) {
                    val slotItems = PsiTreeUtil.findChildrenOfType(slot, RazomySlotItem::class.java).toList()
                    val firstParam = slotItems.firstOrNull() ?: return null
                    val paths = PsiTreeUtil.findChildrenOfType(firstParam, RazomyPath::class.java).toList()

                    if (PsiTreeUtil.findChildOfType(firstParam, RazomyMergeExpr::class.java) != null && paths.size > 1) {
                        var merged: RazomyType = RazomyType.Unknown
                        for (p in paths) {
                            val t = inferTypeByName(p.text, stage, HashSet(visited))
                            if (t !is RazomyType.Unknown) {
                                merged = if (merged is RazomyType.Unknown) t else RazomyType.Intersection(merged, t)
                            }
                        }
                        return if (merged !is RazomyType.Unknown) merged else null
                    }

                    if (paths.size >= 2) {
                        return inferTypeByName(paths[1].text, stage, HashSet(visited))
                    }

                    if (paths.size == 1 && paths[0].text.firstOrNull()?.isUpperCase() == true) {
                        val directType = inferTypeByName(paths[0].text, stage, HashSet(visited))
                        if (directType !is RazomyType.Unknown) return directType
                    }

                    val paramName = paths.firstOrNull()?.text
                    val rightBody = decl.children.getOrNull(1)
                    if (paramName != null && rightBody != null && visited.add("expected:${resolved.text}")) {
                        val pipelines = PsiTreeUtil.findChildrenOfType(rightBody, RazomyPipelineExpr::class.java)
                        for (pipe in pipelines) {
                            val pipeChildren = pipe.children.filter { it !is RazomyPipelineOp }
                            if (pipeChildren.size >= 2 && pipeChildren[0].text == paramName) {
                                val inferredFromRouter = getExpectedInputTypeOfStage(pipeChildren[1], HashSet(visited))
                                if (inferredFromRouter != null) return inferredFromRouter
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    fun isTypeCompatible(actual: RazomyType, expected: RazomyType, context: PsiElement): Boolean {
        if (actual is RazomyType.Unknown || expected is RazomyType.Unknown) return true
        if (expected is RazomyType.Primitive && expected.name == "Any") return true

        if (actual is RazomyType.Union) {
            return actual.flattenUnion().any { isTypeCompatible(it, expected, context) }
        }

        if (expected is RazomyType.Union) {
            return expected.flattenUnion().any { isTypeCompatible(actual, it, context) }
        }

        if (expected is RazomyType.Intersection) {
            return isTypeCompatible(actual, expected.left, context) && isTypeCompatible(actual, expected.right, context)
        }

        val actualNames = actual.allTypeNames()
        val expectedNames = expected.allTypeNames()
        if (actualNames.intersect(expectedNames).isNotEmpty()) return true

        val expectedFields = RazomyType.extractFields(expected)
        val actualFields = RazomyType.extractFields(actual)
        if (expectedFields.isNotEmpty() && actualFields.isNotEmpty()) {
            if (actualFields.keys.containsAll(expectedFields.keys)) return true
        }

        return false
    }

    fun buildScope(
        element: PsiElement,
        visited: MutableSet<String> = mutableSetOf()
    ): RazomyScope {
        val scope = RazomyScope()
        var current: PsiElement? = element.parent

        while (current != null) {
            if (current is RazomyFormItem) {
                val firstExpr = current.children.firstOrNull()
                val firstPath = PsiTreeUtil.findChildOfType(firstExpr, RazomyPath::class.java)
                if (firstPath != null && firstPath.text.firstOrNull()?.isUpperCase() == true) {
                    val resolvedType = inferTypeByName(firstPath.text, element, HashSet(visited))
                    scope.unpackedFields.putAll(RazomyType.extractFields(resolvedType))
                }
            }

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

            if (current is RazomyMigrateExpr && current.node.findChildByType(RazomyTypes.MIGRATION) != null) {
                val leftSource = current.children.firstOrNull()
                if (leftSource != null && leftSource.textOffset < element.textOffset) {
                    val sourceType = inferExprType(leftSource, RazomyType.Unknown, HashSet(visited))
                    scope.unpackedFields.putAll(RazomyType.extractFields(sourceType))
                }
            }

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

            if (current is RazomyDeclaration || current is RazomyFormItem) {
                val leftExpr = current.children.firstOrNull()
                val slot = PsiTreeUtil.findChildOfType(leftExpr, RazomySlot::class.java)
                if (slot != null && slot.textOffset < element.textOffset && RazomyUtil.isParameterDeclarationSlot(slot)) {
                    val declName = PsiTreeUtil.findChildOfType(leftExpr, RazomyPath::class.java)?.text
                    val slotVisited = HashSet(visited)
                    if (declName != null) {
                        slotVisited.add(declName)
                        slotVisited.add("fn:$declName")
                    }

                    val items = PsiTreeUtil.findChildrenOfType(slot, RazomySlotItem::class.java)
                    for (item in items) {
                        val paths = PsiTreeUtil.findChildrenOfType(item, RazomyPath::class.java).toList()
                        val mergeExpr = PsiTreeUtil.findChildOfType(item, RazomyMergeExpr::class.java)
                        if (mergeExpr != null && mergeExpr.node.findChildByType(RazomyTypes.AMP) != null && paths.size > 1) {
                            for (p in paths) {
                                val t = inferTypeByName(p.text, element, HashSet(slotVisited))
                                scope.unpackedFields.putAll(RazomyType.extractFields(t))
                            }
                        } else if (paths.isNotEmpty()) {
                            val varName = paths[0]
                            scope.variables.putIfAbsent(varName.text, varName)

                            val resolvedType = inferTypeOfElement(varName, HashSet(slotVisited))
                            if (resolvedType !is RazomyType.Unknown) {
                                scope.unpackedFields.putAll(RazomyType.extractFields(resolvedType))
                            } else if (paths.size == 1 && varName.text != declName) {
                                val fallbackType = inferTypeByName(varName.text, element, HashSet(slotVisited))
                                scope.unpackedFields.putAll(RazomyType.extractFields(fallbackType))
                            }
                        }
                    }
                }
            }
            current = current.parent
        }

        val file = element.containingFile
        if (file != null) {
            for (builtin in BUILTIN_TYPES) {
                scope.builtInTypes.add(builtin)
            }

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
                    if (leftPath != null && leftPath.text == "_" && leftPath.textOffset == leftExpr?.textOffset) {
                        val importedFile = RazomyImportResolver.resolveImportedFile(element, rightPath.text)
                        if (importedFile != null) {
                            val exports = RazomyImportResolver.getExportedSymbols(importedFile)
                            for (exportName in exports) {
                                scope.isDestructuredImport[exportName] = true
                                scope.imports[exportName] = rightPath
                            }
                        }
                    } else if (leftPath != null && leftPath.textOffset == leftExpr?.textOffset) {
                        val alias = leftPath.text
                        scope.variables.putIfAbsent(alias, leftPath)
                        scope.imports[alias] = rightPath
                    } else if (leftForm != null && leftForm.textOffset == leftExpr?.textOffset) {
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

    fun resolvePath(
        context: PsiElement,
        pathText: String,
        visited: MutableSet<String> = mutableSetOf()
    ): PsiElement? {
        if (pathText == "_" || pathText.startsWith("_.")) return null

        val cleanPath = pathText.replace("\\s+\\.\\s+".toRegex(), ".")
        val parts = cleanPath.split(".")
        if (parts.isEmpty()) return null

        if (parts.size == 1 && context is RazomyPath) {
            val formItem = context.parent?.let { PsiTreeUtil.getParentOfType(it, RazomyFormItem::class.java) }
            val formBlock = formItem?.parent as? RazomyFormBlock
            if (formBlock != null && RazomyUtil.isInstantiationFormBlock(formBlock)) {
                val firstExpr = formItem.children.firstOrNull()
                if (firstExpr != null && context.textOffset == firstExpr.textOffset) {
                    val typedForm = formBlock.parent as? RazomyTypedFormBlock
                    val typePath = typedForm?.let { PsiTreeUtil.getChildOfType(it, RazomyPath::class.java) }
                    if (typePath != null) {
                        val targetType = RazomyTypeSystem.inferTypeByName(typePath.text, typePath, HashSet(visited))
                        val targetField = RazomyType.extractFields(targetType)[parts[0]]
                        if (targetField != null) return targetField
                    }
                }
            }
        }

        val rootName = parts[0]
        val scope = RazomyTypeSystem.buildScope(context, HashSet(visited))

        var current: PsiElement? = scope.variables[rootName] ?: scope.unpackedFields[rootName]

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
            if (scope.isDestructuredImport.containsKey(rootName)) {
                val importPath = scope.imports[rootName]!!
                val importedFile = RazomyImportResolver.resolveImportedFile(context, importPath.text)
                if (importedFile != null) {
                    current = RazomyImportResolver.findExportedSymbol(importedFile, rootName)
                }
            }

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
                        current = resolveMember(current!!, nextMember, HashSet(visited))
                        if (current == null) return null
                    }
                    return current
                }
            }

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

        for (i in 1 until parts.size) {
            val nextMember = parts[i]
            val resolvedMember = resolveMember(current!!, nextMember, HashSet(visited))
            if (resolvedMember == null) {
                LOG.debug("Поле '$nextMember' не найдено в '$rootName'")
                return null
            }
            current = resolvedMember
        }
        return current
    }

    private fun resolveMember(
        owner: PsiElement,
        memberName: String,
        visited: MutableSet<String>
    ): PsiElement? {
        if (owner is PsiFile) {
            return RazomyImportResolver.findExportedSymbol(owner, memberName)
        }
        val type = RazomyTypeSystem.inferTypeOfElement(owner, HashSet(visited))
        val fields = RazomyType.extractFields(type)
        if (fields.containsKey(memberName)) {
            return fields[memberName]
        }

        if (type is RazomyType.Unknown && owner is RazomyPath) {
            val slot = PsiTreeUtil.getParentOfType(owner, RazomySlot::class.java)
            if (slot != null && RazomyUtil.isParameterDeclarationSlot(slot)) {
                val file = owner.containingFile ?: return null
                for ((typeName, _) in RazomyTypeSystem.getGlobalDeclarationsMap(file)) {
                    val candidateType = RazomyTypeSystem.inferTypeByName(typeName, owner, HashSet(visited))
                    val candidateField = RazomyType.extractFields(candidateType)[memberName]
                    if (candidateField != null) return candidateField
                }
            }
        }
        return null
    }
}
