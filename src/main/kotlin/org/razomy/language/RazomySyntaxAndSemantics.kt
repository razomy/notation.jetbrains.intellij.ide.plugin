package org.razomy.language

import com.intellij.codeInsight.hints.HintInfo
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
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import org.razomy.language.psi.*

// =============================================================================
// 0. PSI EXTENSIONS FOR RAZOMY POSTFIX EXPR
// =============================================================================

val RazomyPostfixExpr.slots: List<RazomySlot>
    get() = children.filterIsInstance<RazomySlot>()

val RazomyPostfixExpr.formBlocks: List<RazomyFormBlock>
    get() = children.filterIsInstance<RazomyFormBlock>()

val RazomyPostfixExpr.flowBlocks: List<RazomyFlowBlock>
    get() = children.filterIsInstance<RazomyFlowBlock>()

// =============================================================================
// 1. SYNTAX HIGHLIGHTER & FORMATTER
// =============================================================================

class RazomySyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = FlexAdapter(RazomyLexer(null))

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            RazomyTypes.TRUE, RazomyTypes.FALSE, RazomyTypes.UNDERSCORE -> arrayOf(DefaultLanguageHighlighterColors.KEYWORD)
            RazomyTypes.IDENTIFIER -> arrayOf(DefaultLanguageHighlighterColors.IDENTIFIER)
            RazomyTypes.STRING -> arrayOf(DefaultLanguageHighlighterColors.STRING)
            RazomyTypes.NUMBER -> arrayOf(DefaultLanguageHighlighterColors.NUMBER)
            RazomyTypes.LINE_COMMENT -> arrayOf(DefaultLanguageHighlighterColors.LINE_COMMENT)
            RazomyTypes.DOC_COMMENT -> arrayOf(DefaultLanguageHighlighterColors.DOC_COMMENT)

            RazomyTypes.DOT_GT, RazomyTypes.DOT_AMP, RazomyTypes.DOT_PIPE,
            RazomyTypes.DOT_QM, RazomyTypes.DOT_RANGE,
            RazomyTypes.PIPE_DOT, RazomyTypes.PIPE, RazomyTypes.AMP, RazomyTypes.SLASH,
            RazomyTypes.EQ_EQ, RazomyTypes.NOT_EQ, RazomyTypes.LTE, RazomyTypes.GTE,
            RazomyTypes.LANGLE, RazomyTypes.RANGLE, RazomyTypes.EQ,
            RazomyTypes.AND_AND, RazomyTypes.OR_OR, RazomyTypes.POW,
            RazomyTypes.SHIFT_L, RazomyTypes.SHIFT_R,
            RazomyTypes.PLUS, RazomyTypes.MINUS, RazomyTypes.MUL, RazomyTypes.MOD,
            RazomyTypes.BANG, RazomyTypes.QM -> arrayOf(DefaultLanguageHighlighterColors.OPERATION_SIGN)

            RazomyTypes.COMMA, RazomyTypes.SEMICOLON, RazomyTypes.COLON -> arrayOf(DefaultLanguageHighlighterColors.SEMICOLON)
            else -> emptyArray()
        }
    }
}

class RazomySyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?) = RazomySyntaxHighlighter()
}

class RazomyFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val settings = formattingContext.codeStyleSettings
        val spacingBuilder = SpacingBuilder(settings, RazomyLanguage.INSTANCE)
            .around(
                TokenSet.create(
                    RazomyTypes.PIPE_DOT, RazomyTypes.DOT_GT, RazomyTypes.DOT_AMP,
                    RazomyTypes.DOT_PIPE, RazomyTypes.DOT_QM, RazomyTypes.DOT_RANGE,
                    RazomyTypes.AMP, RazomyTypes.PIPE,
                    RazomyTypes.SLASH, RazomyTypes.EQ, RazomyTypes.EQ_EQ,
                    RazomyTypes.NOT_EQ, RazomyTypes.LTE, RazomyTypes.GTE,
                    RazomyTypes.LANGLE, RazomyTypes.RANGLE,
                    RazomyTypes.AND_AND, RazomyTypes.OR_OR, RazomyTypes.POW,
                    RazomyTypes.SHIFT_L, RazomyTypes.SHIFT_R,
                    RazomyTypes.PLUS, RazomyTypes.MINUS, RazomyTypes.MUL, RazomyTypes.MOD
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

    override fun getNode(): ASTNode = myNode
    override fun getTextRange(): TextRange = myNode.textRange
    override fun getWrap(): Wrap? = myWrap
    override fun getAlignment(): Alignment? = myAlignment

    override fun getSubBlocks(): List<Block> {
        val blocks = mutableListOf<Block>()
        var child: ASTNode? = myNode.firstChildNode
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

    override fun getSpacing(child1: Block?, child2: Block): Spacing? =
        mySpacingBuilder.getSpacing(this, child1, child2)

    override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        val blockTypes = listOf(RazomyTypes.FLOW_BLOCK, RazomyTypes.FORM_BLOCK, RazomyTypes.SLOT)
        if (myNode.elementType in blockTypes) {
            return ChildAttributes(Indent.getNormalIndent(), null)
        }
        return ChildAttributes(Indent.getNoneIndent(), null)
    }

    override fun isIncomplete(): Boolean {
        val lastChild: ASTNode? = myNode.lastChildNode
        return when (myNode.elementType) {
            RazomyTypes.FORM_BLOCK -> lastChild?.elementType != RazomyTypes.RBRACE
            RazomyTypes.FLOW_BLOCK -> lastChild?.elementType != RazomyTypes.RBRACKET
            RazomyTypes.SLOT -> lastChild?.elementType != RazomyTypes.RPAREN
            else -> false
        }
    }

    override fun isLeaf(): Boolean = myNode.firstChildNode == null
}

// =============================================================================
// 2. BRACE MATCHER, FOLDING & INLAY HINTS
// =============================================================================

class RazomyBraceMatcher : PairedBraceMatcher {
    private val pairs = arrayOf(
        BracePair(RazomyTypes.LBRACE, RazomyTypes.RBRACE, true),
        BracePair(RazomyTypes.LPAREN, RazomyTypes.RPAREN, false),
        BracePair(RazomyTypes.LBRACKET, RazomyTypes.RBRACKET, false)
    )

    override fun getPairs(): Array<BracePair> = pairs
    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true
    override fun getCodeConstructStart(file: PsiFile?, openingOffset: Int): Int = openingOffset
}

class RazomyFoldingBuilder : FoldingBuilderEx(), DumbAware {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()

        for (group in PsiTreeUtil.findChildrenOfType(root, RazomyFormBlock::class.java)) {
            if (group.textRange.length > 2) descriptors.add(FoldingDescriptor(group.node, group.textRange))
        }
        for (group in PsiTreeUtil.findChildrenOfType(root, RazomyFlowBlock::class.java)) {
            if (group.textRange.length > 2) descriptors.add(FoldingDescriptor(group.node, group.textRange))
        }
        for (group in PsiTreeUtil.findChildrenOfType(root, RazomySlot::class.java)) {
            if (group.textRange.length > 2 && group.text.contains('\n')) {
                descriptors.add(FoldingDescriptor(group.node, group.textRange))
            }
        }

        PsiTreeUtil.processElements(root) { el ->
            if (el.node.elementType == RazomyTypes.DOC_COMMENT && el.textRange.length > 2 && el.text.contains('\n')) {
                descriptors.add(FoldingDescriptor(el.node, el.textRange))
            }
            true
        }

        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String = when (node.elementType) {
        RazomyTypes.FORM_BLOCK -> "{...}"
        RazomyTypes.FLOW_BLOCK -> "[...]"
        RazomyTypes.SLOT -> "(...)"
        RazomyTypes.DOC_COMMENT -> "#...#"
        else -> "..."
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}

@Suppress("UnstableApiUsage")
class RazomyInlayParameterHintsProvider : InlayParameterHintsProvider {
    override fun getParameterHints(element: PsiElement): MutableList<InlayInfo> {
        val hints = mutableListOf<InlayInfo>()
        if (element !is RazomyPostfixExpr) return hints

        val slot = element.slots.firstOrNull() ?: return hints
        if (RazomyUtil.isParameterDeclarationSlot(slot)) return hints

        val path = RazomyUtil.getPostfixHeadPath(element) ?: return hints
        val providedArgs = PsiTreeUtil.getChildrenOfTypeAsList(slot, RazomySlotItem::class.java)
        if (providedArgs.isEmpty()) return hints

        var targetPath = (path.reference?.resolve() as? RazomyPath)
            ?: (RazomyResolver.resolvePath(path, path.text) as? RazomyPath)

        if (targetPath == null) {
            val file = element.containingFile
            if (file != null) {
                targetPath = RazomyTypeSystem.getGlobalDeclarationsMap(file)[path.text]
            }
        }
        if (targetPath == null) return hints

        val expectedNames = mutableListOf<String>()
        val container = PsiTreeUtil.getParentOfType(
            targetPath,
            RazomyDeclaration::class.java,
            RazomyFormItem::class.java,
            RazomyFlowItem::class.java
        )
        val (headerExpr, bodyExpr) = container?.let { RazomyUtil.splitDeclaration(it) } ?: (null to null)

        val declSlot = (headerExpr as? RazomyPostfixExpr)?.slots?.firstOrNull()
            ?: headerExpr?.let { PsiTreeUtil.findChildOfType(it, RazomySlot::class.java, false) }
        if (declSlot != null) {
            val params = PsiTreeUtil.getChildrenOfTypeAsList(declSlot, RazomySlotItem::class.java)
                .mapNotNull { RazomyUtil.firstPath(it)?.text }
            expectedNames.addAll(params)
        } else if (bodyExpr is RazomyFormBlock) {
            val fields = bodyExpr.children.filterIsInstance<RazomyFormItem>()
                .mapNotNull { RazomyUtil.getDeclaredPath(it)?.text }
            expectedNames.addAll(fields)
        }

        for ((i, arg) in providedArgs.withIndex()) {
            if (i < expectedNames.size) {
                hints.add(InlayInfo("${expectedNames[i]}:", arg.textOffset))
            }
        }
        return hints
    }

    override fun getDefaultBlackList(): MutableSet<String> = mutableSetOf()
    override fun getHintInfo(element: PsiElement): HintInfo? = null
}

// =============================================================================
// 3. AST UTILITIES (НАВИГАЦИЯ И РАСПОЗНАВАНИЕ КОНСТРУКЦИЙ V4.0)
// =============================================================================

object RazomyUtil {

    val IDENTIFIER_REGEX = Regex("""[a-zA-Z_][a-zA-Z0-9_.]*""")
    val ESCAPE_PATTERN = Regex("""\\[ntr"'\\{}]""")
    val INTERPOLATION_PATTERN = Regex("""(?<!\\)\{([^}]+)}""")

    val COLOR_MODULE: TextAttributesKey = DefaultLanguageHighlighterColors.CLASS_NAME
    val COLOR_TYPE: TextAttributesKey = DefaultLanguageHighlighterColors.CLASS_REFERENCE
    val COLOR_FUNCTION_DEF: TextAttributesKey = DefaultLanguageHighlighterColors.FUNCTION_DECLARATION
    val COLOR_METHOD_CALL: TextAttributesKey = DefaultLanguageHighlighterColors.STATIC_METHOD
    val COLOR_FIELD: TextAttributesKey = DefaultLanguageHighlighterColors.INSTANCE_FIELD
    val COLOR_PARAM: TextAttributesKey = DefaultLanguageHighlighterColors.PARAMETER
    val COLOR_VARIABLE: TextAttributesKey = DefaultLanguageHighlighterColors.LOCAL_VARIABLE
    val COLOR_NATIVE: TextAttributesKey = DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL
    val COLOR_IMPORT: TextAttributesKey = DefaultLanguageHighlighterColors.STRING

    fun unwrapExpr(element: PsiElement?): PsiElement? {
        var curr = element
        while (curr != null && curr.children.size == 1 &&
            curr !is RazomyFormBlock &&
            curr !is RazomyFlowBlock &&
            curr !is RazomyPostfixExpr &&
            curr !is RazomySlot &&
            curr !is RazomyLiteral &&
            curr !is RazomyPath
        ) {
            curr = curr.children[0]
        }
        return curr
    }

    fun firstPath(element: PsiElement?): RazomyPath? {
        if (element == null) return null
        return (element as? RazomyPath) ?: PsiTreeUtil.findChildOfType(element, RazomyPath::class.java)
    }

    fun getPostfixHeadPath(postfix: RazomyPostfixExpr): RazomyPath? {
        return postfix.children.firstOrNull() as? RazomyPath
    }

    fun isTypedFormPostfix(element: PsiElement?): Boolean {
        val p = element as? RazomyPostfixExpr ?: return false
        return getPostfixHeadPath(p) != null && p.formBlocks.isNotEmpty()
    }

    fun isCallPostfix(element: PsiElement?): Boolean {
        val p = element as? RazomyPostfixExpr ?: return false
        return getPostfixHeadPath(p) != null && p.slots.isNotEmpty() && p.formBlocks.isEmpty() && p.flowBlocks.isEmpty()
    }

    fun splitDeclaration(container: PsiElement): Pair<PsiElement, PsiElement?>? {
        val children = container.children
        if (children.isEmpty()) return null
        if (children.size >= 2) {
            return (unwrapExpr(children[0]) ?: children[0]) to unwrapExpr(children[1])
        }
        val single: PsiElement = unwrapExpr(children[0]) ?: children[0]
        if (single is RazomyPostfixExpr) {
            val headPath = getPostfixHeadPath(single)
            val flowBlock: PsiElement? = single.flowBlocks.lastOrNull()
            val formBlock: PsiElement? = single.formBlocks.lastOrNull()

            if (headPath != null && flowBlock != null) {
                return Pair<PsiElement, PsiElement?>(single, flowBlock)
            }
            if (headPath != null && formBlock != null) {
                return Pair<PsiElement, PsiElement?>(single, formBlock)
            }
        } else {
            val firstPostfix = PsiTreeUtil.findChildOfType(single, RazomyPostfixExpr::class.java)
            if (firstPostfix != null && firstPostfix.textOffset == single.textOffset) {
                val headPath = getPostfixHeadPath(firstPostfix)
                if (headPath != null && (firstPostfix.formBlocks.isNotEmpty() || firstPostfix.flowBlocks.isNotEmpty())) {
                    return Pair<PsiElement, PsiElement?>(headPath, single)
                }
            }
        }
        return Pair<PsiElement, PsiElement?>(single, null)
    }

    fun getDeclaredPath(container: PsiElement): RazomyPath? {
        val (header, _) = splitDeclaration(container) ?: return null
        val path = firstPath(header)
        return if (path != null && path.textOffset == header.textOffset) path else null
    }

    fun getPrecedingMergeOperands(element: PsiElement): List<PsiElement> {
        val result = mutableListOf<PsiElement>()
        var curr: PsiElement? = element
        while (curr != null && curr !is PsiFile) {
            if (curr is RazomyMergeExpr && curr.node.findChildByType(RazomyTypes.AMP) != null) {
                for (op in curr.children) {
                    if (op.textOffset < element.textOffset && !PsiTreeUtil.isAncestor(op, element, false)) {
                        result.add(unwrapExpr(op) ?: op)
                    }
                }
            }
            curr = curr.parent
        }
        return result
    }

    fun isRouterFormBlock(formBlock: RazomyFormBlock): Boolean {
        val parentPostfix = formBlock.parent as? RazomyPostfixExpr
        if (parentPostfix != null && getPostfixHeadPath(parentPostfix) != null) return false
        val pipelineExpr = PsiTreeUtil.getParentOfType(formBlock, RazomyPipelineExpr::class.java) ?: return false
        val ops = PsiTreeUtil.getChildrenOfType(pipelineExpr, RazomyPipelineOp::class.java) ?: return false
        return ops.any { it.textOffset < formBlock.textOffset }
    }

    fun isInstantiationFormBlock(formBlock: RazomyFormBlock): Boolean {
        val typedForm = formBlock.parent as? RazomyPostfixExpr ?: return false
        if (getPostfixHeadPath(typedForm) == null) return false

        if (PsiTreeUtil.getParentOfType(typedForm, RazomyFlowBlock::class.java) != null) return true
        if (PsiTreeUtil.getParentOfType(typedForm, RazomySlot::class.java) != null) return true

        val pipelineExpr = PsiTreeUtil.getParentOfType(typedForm, RazomyPipelineExpr::class.java)
        if (pipelineExpr != null) {
            val ops = PsiTreeUtil.getChildrenOfType(pipelineExpr, RazomyPipelineOp::class.java)
            if (ops != null && ops.any { it.textOffset < typedForm.textOffset }) {
                return true
            }
        }

        val mergeExpr = PsiTreeUtil.getParentOfType(typedForm, RazomyMergeExpr::class.java)
        if (mergeExpr != null && mergeExpr.node.findChildByType(RazomyTypes.AMP) != null) {
            val firstOp = unwrapExpr(mergeExpr.children.firstOrNull())
            if (firstOp != null && typedForm.textOffset > firstOp.textOffset) {
                return true
            }
        }

        val container =
            PsiTreeUtil.getParentOfType(typedForm, RazomyDeclaration::class.java, RazomyFormItem::class.java)
        if (container != null && container.children.size >= 2) {
            val rightExpr = container.children[1]
            if (PsiTreeUtil.isAncestor(rightExpr, typedForm, false)) {
                return true
            }
        }

        return false
    }

    fun isParameterDeclarationSlot(slot: RazomySlot): Boolean {
        val postfixExpr = slot.parent as? RazomyPostfixExpr ?: return false
        if (getPostfixHeadPath(postfixExpr) == null) return false

        val container = PsiTreeUtil.getParentOfType(
            postfixExpr,
            RazomyDeclaration::class.java,
            RazomyFormItem::class.java,
            RazomyFlowItem::class.java,
            RazomyFlowBlock::class.java,
            RazomySlot::class.java
        ) ?: return false

        if (container is RazomyDeclaration || container is RazomyFormItem || container is RazomyFlowItem) {
            val (leftExpr, bodyExpr) = splitDeclaration(container) ?: return false
            return bodyExpr != null && (leftExpr == postfixExpr || PsiTreeUtil.isAncestor(leftExpr, postfixExpr, false)) &&
                    postfixExpr.textOffset == leftExpr.textOffset
        }
        return false
    }

    fun isDeclaration(path: RazomyPath): Boolean {
        val text = path.text
        if (text == "_" || text.startsWith("_.") || text.contains(".")) return false

        // 1. Путь внутри слота параметров `(T)` или `(Source)`
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
                val file: PsiFile? = path.containingFile
                if (file != null) {
                    val globalMap = RazomyTypeSystem.getGlobalDeclarationsMap(file)
                    val globalDecl = globalMap[text]
                    if (globalDecl != null) {
                        val parentDecl =
                            PsiTreeUtil.getParentOfType(slot, RazomyDeclaration::class.java, RazomyFormItem::class.java)
                        if (globalDecl != path && !PsiTreeUtil.isAncestor(parentDecl, globalDecl, false)) {
                            return false
                        }
                    }
                }
            }

            val firstExpr = slotItem.children.firstOrNull() ?: return false
            val firstPath = firstPath(firstExpr)
            return firstPath == path && path.textOffset == firstExpr.textOffset
        }

        // 2. Путь внутри формы { ... }
        val formItem = PsiTreeUtil.getParentOfType(path, RazomyFormItem::class.java)
        if (formItem != null) {
            if (getDeclaredPath(formItem) != path) return false
            val formBlock = formItem.parent as? RazomyFormBlock ?: return false
            return !isRouterFormBlock(formBlock) && !isInstantiationFormBlock(formBlock)
        }

        // 3. Путь на верхнем уровне файла (RazomyDeclaration)
        val decl = PsiTreeUtil.getParentOfType(path, RazomyDeclaration::class.java)
        return decl != null && getDeclaredPath(decl) == path
    }

    fun isImportPath(text: String): Boolean {
        return !(text.isBlank() || text == "_" || text in RazomyTypeSystem.BUILTIN_TYPES) && text.first().isLowerCase()
    }

    enum class ImportContext { RIGHT_PATH, LEFT_ALIAS_OR_DESTRUCT }

    fun getImportContext(path: RazomyPath): ImportContext? {
        val decl = PsiTreeUtil.getParentOfType(path, RazomyDeclaration::class.java) ?: return null
        if (PsiTreeUtil.getParentOfType(path, RazomyFlowBlock::class.java) != null) return null
        if (PsiTreeUtil.getParentOfType(path, RazomyFormBlock::class.java) != null) {
            val leftExpr = decl.children.firstOrNull()
            if (leftExpr == null || !PsiTreeUtil.isAncestor(leftExpr, path, false)) return null
        }

        val leftExpr = decl.children.firstOrNull() ?: return null
        val rightExpr = decl.children.getOrNull(1) ?: return null

        val rightPath = firstPath(rightExpr)
        val isRightImport = rightPath != null &&
                rightPath.textOffset == rightExpr.textOffset &&
                rightPath.textLength == rightExpr.textLength &&
                isImportPath(rightPath.text)

        if (!isRightImport) return null

        if (path == rightPath) return ImportContext.RIGHT_PATH
        if (PsiTreeUtil.isAncestor(leftExpr, path, false)) return ImportContext.LEFT_ALIAS_OR_DESTRUCT
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
            for (match in INTERPOLATION_PATTERN.findAll(text)) {
                val rawExpr = match.groups[1]?.value ?: continue
                for ((token, _) in extractInterpolatedPaths(rawExpr)) {
                    if (token == symbolName || token.startsWith("$symbolName.") || token.split('.')
                            .contains(symbolName)
                    ) {
                        return true
                    }
                }
            }
        }
        return false
    }
}

// =============================================================================
// 4. ANNOTATOR (СЕМАНТИЧЕСКАЯ ПОДСВЕТКА И ВАЛИДАЦИЯ ТИПОВ/СХЕМ)
// =============================================================================

class RazomyAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element.node.elementType == RazomyTypes.STRING) {
            annotateString(element, holder)
            return
        }

        if (element is RazomyPipelineExpr && PsiTreeUtil.getChildOfType(
                element,
                RazomyPipelineOp::class.java
            ) != null
        ) {
            checkPipelineTypes(element, holder)
            return
        }

        // 1. Валидация слияния со схемой без тела: `Cli { ... } & Cli.Schema`
        if (element is RazomyMergeExpr && element.node.findChildByType(RazomyTypes.AMP) != null) {
            checkSchemaMergeImplementation(element, holder)
        }

        // 2. Валидация инстанцирования формы/схемы: `ClientAccount { ... }`, `Cli Cli.Schema { ... }`
        if (element is RazomyPostfixExpr && RazomyUtil.isTypedFormPostfix(element)) {
            val formBlock = element.formBlocks.firstOrNull()
            if (formBlock != null && RazomyUtil.isInstantiationFormBlock(formBlock)) {
                checkInstantiationFormTypes(element, formBlock, holder)
            }
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
                highlight(element, holder, RazomyUtil.COLOR_IMPORT)
            }
            return
        } else if (importCtx == RazomyUtil.ImportContext.LEFT_ALIAS_OR_DESTRUCT) {
            when {
                text == "_" -> highlight(element, holder, DefaultLanguageHighlighterColors.KEYWORD)
                text.firstOrNull()?.isUpperCase() == true -> highlight(element, holder, RazomyUtil.COLOR_TYPE)
                else -> highlight(element, holder, RazomyUtil.COLOR_MODULE)
            }
            return
        }

        if (text == "_") {
            highlight(element, holder, DefaultLanguageHighlighterColors.KEYWORD)
            return
        }

        if (text.startsWith("_.")) {
            val underscoreType = RazomyTypeSystem.inferUnderscoreType(element)
            if (underscoreType !is RazomyType.Unknown) {
                val resolved = RazomyResolver.resolvePath(element, text)
                if (resolved == null) {
                    val fieldName = text.substringAfterLast('.')
                    holder.newAnnotation(
                        HighlightSeverity.ERROR,
                        "Поле '$fieldName' не найдено в контекстном типе '_: ${underscoreType.presentableName()}'"
                    )
                        .range(element.textRange)
                        .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                        .create()
                    return
                }
            }
            annotateMemberAccess(element, holder)
            return
        }

        if (text.startsWith("native_") || text.contains("native_")) {
            highlight(element, holder, RazomyUtil.COLOR_NATIVE)
            return
        }

        val isDecl = RazomyUtil.isDeclaration(element)
        val isMemberAccess = text.contains(".")

        // 3. Проверка имени поля внутри инстанцирования
        val formItem = element.parent?.let { PsiTreeUtil.getParentOfType(it, RazomyFormItem::class.java) }
        val parentFormBlock = formItem?.parent as? RazomyFormBlock
        if (parentFormBlock != null && RazomyUtil.isInstantiationFormBlock(parentFormBlock)) {
            if (RazomyUtil.getDeclaredPath(formItem) == element) {
                val typedForm = parentFormBlock.parent as? RazomyPostfixExpr
                val typePath = typedForm?.let { RazomyUtil.getPostfixHeadPath(it) }
                if (typePath != null) {
                    val targetType = RazomyTypeSystem.inferTypeByName(typePath.text, typePath)
                    if (targetType !is RazomyType.Unknown) {
                        val targetFields = RazomyType.extractFields(targetType)
                        if (!targetFields.containsKey(text)) {
                            holder.newAnnotation(
                                HighlightSeverity.ERROR,
                                "Поле '$text' отсутствует в типе '${typePath.text}'. Для объявления нового поля используйте расширение '& { $text ... }'"
                            )
                                .range(element.textRange)
                                .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                                .create()
                            return
                        }
                    }
                }
                highlight(element, holder, RazomyUtil.COLOR_FIELD)
                return
            }
        }

        if (!isDecl) {
            val rootName = text.substringBefore('.')
            val file = element.containingFile
            val scope = RazomyTypeSystem.buildScope(element)

            // Проверка на конфликт 2+ имен в контексте (если нет локального объявления и нет явного импорта `_ b` или `{ A } b`)
            if (file != null &&
                rootName.firstOrNull()?.isUpperCase() == true &&
                !RazomyTypeSystem.getGlobalDeclarationsMap(file).containsKey(rootName) &&
                !scope.imports.containsKey(rootName) &&
                !scope.isDestructuredImport.containsKey(rootName)
            ) {
                val candidateFiles = RazomyImportResolver.findContextCandidateFiles(element, rootName)
                if (candidateFiles.size > 1) {
                    val moduleNames = candidateFiles.map { RazomyImportResolver.getRelativeModulePath(file, it) }
                    val defaultFile = candidateFiles.first()
                    val conflictBuilder = holder.newAnnotation(
                        HighlightSeverity.ERROR,
                        "Конфликт имен '$rootName' в файлах [${moduleNames.joinToString(", ")}] (по умолчанию взят '${defaultFile.name}'). Укажите импорт явно, например: '_ ${moduleNames.last()}'"
                    )
                        .range(element.textRange)
                        .highlightType(ProblemHighlightType.GENERIC_ERROR)

                    for (candFile in candidateFiles) {
                        val modPath = RazomyImportResolver.getRelativeModulePath(file, candFile)
                        conflictBuilder.withFix(RazomyImportQuickFix(rootName, modPath, useWildcardOverride = true))
                        conflictBuilder.withFix(RazomyImportQuickFix(rootName, modPath, useWildcardOverride = false))
                    }
                    conflictBuilder.create()
                }
            }

            val resolved = RazomyResolver.resolvePath(element, text)
            if (resolved == null) {
                val isResolvedLocally = if (!isMemberAccess) {
                    scope.variables.containsKey(text) ||
                            scope.unpackedFields.containsKey(text) ||
                            scope.isDestructuredImport.containsKey(text) ||
                            scope.builtInTypes.contains(text)
                } else false

                if (!isResolvedLocally) {
                    val unresolvedName = if (isMemberAccess) text.split(".").last() else text
                    val errorBuilder =
                        holder.newAnnotation(HighlightSeverity.ERROR, "Неразрешенная ссылка: '$unresolvedName'")
                            .range(element.textRange)
                            .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)

                    if (!isMemberAccess) {
                        val candidates =
                            RazomyImportResolver.findModuleByExportedSymbol(element.project, unresolvedName)
                        for (modulePath in candidates) {
                            errorBuilder.withFix(RazomyImportQuickFix(unresolvedName, modulePath, useWildcardOverride = true))
                            errorBuilder.withFix(RazomyImportQuickFix(unresolvedName, modulePath, useWildcardOverride = false))
                        }
                    }
                    errorBuilder.create()
                    return
                }
            }
        }

        if (isMemberAccess) {
            annotateMemberAccess(element, holder)
            return
        }

        val enclosingSlot = PsiTreeUtil.getParentOfType(element, RazomySlot::class.java)
        if (enclosingSlot != null && isParameterInsideSlot(element, enclosingSlot)) {
            highlight(element, holder, RazomyUtil.COLOR_PARAM)
            return
        }

        if (isDecl) {
            annotateDeclaration(element, holder)
            return
        }
        annotateUsage(element, holder, text)
    }

    private fun checkSchemaMergeImplementation(mergeExpr: RazomyMergeExpr, holder: AnnotationHolder) {
        val operands = mergeExpr.children
        if (operands.size < 2) return

        val implFormBlocks = mutableListOf<RazomyFormBlock>()
        val schemaOperands = mutableListOf<Pair<PsiElement, RazomyType>>()

        for (rawOp in operands) {
            val op: PsiElement = RazomyUtil.unwrapExpr(rawOp) ?: rawOp
            val directForm: RazomyFormBlock? = when (op) {
                is RazomyPostfixExpr -> if (RazomyUtil.isTypedFormPostfix(op)) op.formBlocks.firstOrNull() else null
                is RazomyFormBlock -> op
                else -> null
            }
            if (directForm != null) {
                implFormBlocks.add(directForm)
            } else if (op !is RazomyFlowBlock && PsiTreeUtil.findChildOfType(op, RazomyFlowBlock::class.java) == null) {
                val opType = RazomyTypeSystem.evalTypeExpr(op, "Schema", null, emptyMap(), mutableSetOf())
                if (opType !is RazomyType.Unknown) {
                    schemaOperands.add(op to opType)
                }
            }
        }

        if (implFormBlocks.isEmpty() || schemaOperands.isEmpty()) return

        val implementedFields = LinkedHashMap<String, PsiElement>()
        var hasConcreteImplementation = false

        for (fb in implFormBlocks) {
            for (item in fb.children.filterIsInstance<RazomyFormItem>()) {
                val fieldPath = RazomyUtil.getDeclaredPath(item) ?: continue
                implementedFields[fieldPath.text] = fieldPath
                if (!RazomyTypeSystem.isAbstractSchemaField(fieldPath)) {
                    hasConcreteImplementation = true
                }
            }
        }

        if (!hasConcreteImplementation) return

        for ((schemaOp, schemaType) in schemaOperands) {
            validateSchemaFieldsAgainstImplementation(schemaOp.text, schemaOp, schemaType, implementedFields, holder)
        }
    }

    private fun checkInstantiationFormTypes(
        typedForm: RazomyPostfixExpr,
        formBlock: RazomyFormBlock,
        holder: AnnotationHolder
    ) {
        val targetTypePath = RazomyUtil.getPostfixHeadPath(typedForm) ?: return
        val targetType = RazomyTypeSystem.inferTypeByName(targetTypePath.text, typedForm)
        if (targetType is RazomyType.Unknown) return

        val targetFields = RazomyType.extractFields(targetType)
        if (targetFields.isEmpty()) return

        val typeEnv = targetType.collectTypeArgs()
        val implementedItems = LinkedHashMap<String, RazomyPath>()

        for (item in formBlock.children.filterIsInstance<RazomyFormItem>()) {
            val fieldPath = RazomyUtil.getDeclaredPath(item) ?: continue
            implementedItems[fieldPath.text] = fieldPath

            val expectedFieldDecl = targetFields[fieldPath.text]
            if (expectedFieldDecl != null) {
                val expectedType = RazomyTypeSystem.inferFieldType(expectedFieldDecl, typeEnv, mutableSetOf())
                val actualType = RazomyTypeSystem.inferFieldType(fieldPath, emptyMap(), mutableSetOf())
                if (expectedType !is RazomyType.Unknown &&
                    actualType !is RazomyType.Unknown &&
                    !RazomyTypeSystem.isTypeCompatible(actualType, expectedType, fieldPath)
                ) {
                    holder.newAnnotation(
                        HighlightSeverity.ERROR,
                        "Несовпадение типа поля '${fieldPath.text}' с типом '${targetTypePath.text}': ожидается '${expectedType.presentableName()}', получено '${actualType.presentableName()}'"
                    ).range(item.textRange).create()
                }
            }
        }

        val scope = RazomyTypeSystem.buildScope(typedForm)
        val underscoreType = RazomyTypeSystem.inferUnderscoreType(typedForm)
        val availableImplicitFields = scope.unpackedFields.keys +
                scope.variables.keys +
                RazomyType.extractFields(underscoreType).keys

        val missingFields = targetFields.keys.filter {
            it !in implementedItems && it !in availableImplicitFields
        }

        if (missingFields.isNotEmpty()) {
            holder.newAnnotation(
                HighlightSeverity.WARNING,
                "Неполная реализация типа '${targetTypePath.text}': поля [${missingFields.joinToString()}] не заданы явно и отсутствуют в текущем контексте"
            ).range(targetTypePath.textRange).create()
        }
    }

    private fun validateSchemaFieldsAgainstImplementation(
        schemaLabel: String,
        rangeElement: PsiElement,
        schemaType: RazomyType,
        implementedFields: Map<String, PsiElement>,
        holder: AnnotationHolder
    ) {
        val expectedFields = RazomyTypeSystem.extractAbstractSchemaRequirements(schemaType)
        if (expectedFields.isEmpty()) return

        val missingFields = mutableListOf<String>()

        for ((fieldName, expectedType) in expectedFields) {
            val implFieldPath = implementedFields[fieldName]
            if (implFieldPath == null) {
                val typeSuffix = if (expectedType !is RazomyType.Unknown) ": ${expectedType.presentableName()}" else ""
                missingFields.add("$fieldName$typeSuffix")
            } else if (expectedType !is RazomyType.Unknown) {
                val actualType = RazomyTypeSystem.inferFieldType(implFieldPath, emptyMap(), mutableSetOf())
                if (actualType !is RazomyType.Unknown &&
                    !RazomyTypeSystem.isTypeCompatible(actualType, expectedType, implFieldPath)
                ) {
                    val implItem: PsiElement =
                        PsiTreeUtil.getParentOfType(implFieldPath, RazomyFormItem::class.java) ?: implFieldPath
                    holder.newAnnotation(
                        HighlightSeverity.ERROR,
                        "Несовпадение типа поля '$fieldName' со схемой '$schemaLabel': ожидается '${expectedType.presentableName()}', получено '${actualType.presentableName()}'"
                    ).range(implItem.textRange).create()
                }
            }
        }

        if (missingFields.isNotEmpty()) {
            holder.newAnnotation(
                HighlightSeverity.WARNING,
                "Схема '$schemaLabel' реализована не полностью: отсутствуют поля [${missingFields.joinToString(", ")}]"
            ).range(rangeElement.textRange).create()
        }
    }

    private fun checkPipelineTypes(pipeline: RazomyPipelineExpr, holder: AnnotationHolder) {
        val stages = pipeline.children.filter { it !is RazomyPipelineOp }
        val ops = pipeline.children.filterIsInstance<RazomyPipelineOp>()
        if (stages.size < 2) return

        var currentType = RazomyTypeSystem.inferExprType(stages[0])

        for ((i, op) in ops.withIndex()) {
            val opText = op.text.trim()
            val nextStage = stages.getOrNull(i + 1) ?: break

            val stagePath = RazomyUtil.firstPath(nextStage)
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
                        "Несовпадение типов конвейера: этап '${nextStage.text.take(30)}' ожидает '${expectedInput.presentableName()}', но текущий тип потока — '${currentType.presentableName()}'"
                    ).range(nextStage.textRange).create()
                }
            }

            val stageOut = RazomyTypeSystem.inferExprType(nextStage, currentType)
            currentType = when (opText) {
                ".>", ".?" -> currentType
                ".&" -> if (currentType !is RazomyType.Unknown && stageOut !is RazomyType.Unknown) {
                    RazomyType.Intersection(currentType, stageOut)
                } else stageOut

                else -> stageOut
            }
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
                when {
                    partText == "_" -> highlightRange(node.textRange, holder, DefaultLanguageHighlighterColors.KEYWORD)
                    partText.firstOrNull()?.isUpperCase() == true -> highlightRange(
                        node.textRange,
                        holder,
                        RazomyUtil.COLOR_MODULE
                    )

                    else -> highlightRange(node.textRange, holder, RazomyUtil.COLOR_VARIABLE)
                }
            } else {
                val isMethod = partText.firstOrNull()?.isUpperCase() == true
                highlightRange(
                    node.textRange,
                    holder,
                    if (isMethod) RazomyUtil.COLOR_METHOD_CALL else RazomyUtil.COLOR_FIELD
                )
            }
        }
    }

    private fun annotateDeclaration(element: RazomyPath, holder: AnnotationHolder) {
        val parent = element.parent
        if (parent is RazomyPostfixExpr) {
            if (parent.flowBlocks.isNotEmpty()) {
                highlight(element, holder, RazomyUtil.COLOR_FUNCTION_DEF)
                return
            }
            if (parent.slots.isNotEmpty()) {
                val container =
                    PsiTreeUtil.getParentOfType(parent, RazomyDeclaration::class.java, RazomyFormItem::class.java)
                val (_, bodyExpr) = container?.let { RazomyUtil.splitDeclaration(it) } ?: (null to null)
                if (bodyExpr is RazomyFlowBlock) {
                    highlight(element, holder, RazomyUtil.COLOR_FUNCTION_DEF)
                    return
                }
            }
            val formBlock = parent.formBlocks.firstOrNull()
            if (formBlock != null) {
                if (PsiTreeUtil.findChildOfType(formBlock, RazomyFlowBlock::class.java) != null) {
                    highlight(element, holder, RazomyUtil.COLOR_MODULE)
                } else {
                    highlight(element, holder, RazomyUtil.COLOR_TYPE)
                }
                return
            }
        }

        if (element.text.firstOrNull()?.isUpperCase() == true) {
            highlight(element, holder, RazomyUtil.COLOR_TYPE)
        } else {
            highlight(element, holder, RazomyUtil.COLOR_FIELD)
        }
    }

    private fun annotateUsage(element: RazomyPath, holder: AnnotationHolder, text: String) {
        if (text.firstOrNull()?.isUpperCase() == true) {
            highlight(element, holder, RazomyUtil.COLOR_TYPE)
        } else {
            highlight(element, holder, RazomyUtil.COLOR_VARIABLE)
        }
    }

    private fun isParameterInsideSlot(element: RazomyPath, slot: RazomySlot): Boolean =
        RazomyUtil.isParameterDeclarationSlot(slot) && RazomyUtil.isDeclaration(element)

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
        for (match in RazomyUtil.ESCAPE_PATTERN.findAll(text)) {
            val range = TextRange(offset + match.range.first, offset + match.range.last + 1)
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(range)
                .textAttributes(DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE)
                .create()
        }
        for (match in RazomyUtil.INTERPOLATION_PATTERN.findAll(text)) {
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

// =============================================================================
// 5. ЕДИНЫЙ ДВИЖОК ТИПОВ (RAZOMY TYPE SYSTEM V4.0 — OPTIMIZED & CACHED)
// =============================================================================

class RazomyScope(
    val variables: MutableMap<String, PsiElement> = mutableMapOf(),
    val unpackedFields: MutableMap<String, PsiElement> = mutableMapOf(),
    val imports: MutableMap<String, RazomyPath> = mutableMapOf(),
    val isDestructuredImport: MutableMap<String, Boolean> = mutableMapOf(),
    val builtInTypes: MutableSet<String> = mutableSetOf()
) {
    fun copyFrom(other: RazomyScope) {
        variables.putAll(other.variables)
        unpackedFields.putAll(other.unpackedFields)
        imports.putAll(other.imports)
        isDestructuredImport.putAll(other.isDestructuredImport)
        builtInTypes.addAll(other.builtInTypes)
    }
}

sealed class RazomyType {
    class Primitive(val name: String, val supertypes: Set<String> = setOf(name)) : RazomyType()
    class Form(
        val name: String,
        val declaration: PsiElement,
        val fields: Map<String, PsiElement>,
        val supertypes: Set<String> = setOf(name),
        val typeParams: List<String> = emptyList(),
        val typeArgs: Map<String, RazomyType> = emptyMap()
    ) : RazomyType()

    class Module(val file: PsiFile) : RazomyType()
    class Union(val left: RazomyType, val right: RazomyType, val aliasName: String? = null) : RazomyType() {
        fun getResolvedFields(): Map<String, PsiElement> = LinkedHashMap<String, PsiElement>().apply {
            putAll(extractFields(left))
            putAll(extractFields(right))
        }

        fun flattenUnion(): List<RazomyType> = buildList {
            if (left is Union) addAll(left.flattenUnion()) else add(left)
            if (right is Union) addAll(right.flattenUnion()) else add(right)
        }
    }

    class Intersection(val left: RazomyType, val right: RazomyType, val aliasName: String? = null) : RazomyType() {
        fun getResolvedFields(): Map<String, PsiElement> = LinkedHashMap<String, PsiElement>().apply {
            putAll(extractFields(left))
            putAll(extractFields(right))
        }

        fun flattenIntersection(): List<RazomyType> = buildList {
            if (left is Intersection) addAll(left.flattenIntersection()) else add(left)
            if (right is Intersection) addAll(right.flattenIntersection()) else add(right)
        }
    }

    object Unknown : RazomyType()

    fun presentableName(): String = when (this) {
        is Primitive -> name
        is Form -> if (typeArgs.isNotEmpty()) {
            "$name(${typeArgs.values.joinToString(", ") { it.presentableName() }})"
        } else name

        is Module -> "Module(${file.name})"
        is Union -> aliasName ?: "${left.presentableName()} | ${right.presentableName()}"
        is Intersection -> aliasName ?: "${left.presentableName()} & ${right.presentableName()}"
        is Unknown -> "Unknown"
    }

    fun allTypeNames(): Set<String> = when (this) {
        is Primitive -> if (name == "Str" || name == "String") setOf("Str", "String") else supertypes + setOf(
            "Str",
            "String"
        ).filter { it in supertypes }

        is Form -> supertypes + name
        is Union -> setOfNotNull(aliasName) + left.allTypeNames() + right.allTypeNames()
        is Intersection -> setOfNotNull(aliasName) + left.allTypeNames() + right.allTypeNames()
        else -> emptySet()
    }

    fun withAlias(newAlias: String, extraSupertypes: Set<String>): RazomyType = when (this) {
        is Form -> Form(
            newAlias,
            declaration,
            fields,
            supertypes + extraSupertypes + name + newAlias,
            typeParams,
            typeArgs
        )

        is Primitive -> Primitive(newAlias, supertypes + extraSupertypes + name + newAlias)
        is Union -> Union(left, right, newAlias)
        is Intersection -> Intersection(left, right, newAlias)
        else -> this
    }

    fun withTypeArgs(newTypeArgs: Map<String, RazomyType>): RazomyType = when (this) {
        is Form -> Form(name, declaration, fields, supertypes, typeParams, typeArgs + newTypeArgs)
        is Intersection -> Intersection(left.withTypeArgs(newTypeArgs), right.withTypeArgs(newTypeArgs), aliasName)
        is Union -> Union(left.withTypeArgs(newTypeArgs), right.withTypeArgs(newTypeArgs), aliasName)
        else -> this
    }

    fun collectTypeArgs(): Map<String, RazomyType> = when (this) {
        is Form -> typeArgs
        is Intersection -> left.collectTypeArgs() + right.collectTypeArgs()
        is Union -> left.collectTypeArgs() + right.collectTypeArgs()
        else -> emptyMap()
    }

    companion object {
        fun extractFields(type: RazomyType): Map<String, PsiElement> = when (type) {
            is Form -> type.fields
            is Intersection -> type.getResolvedFields()
            is Union -> type.getResolvedFields()
            is Module -> RazomyTypeSystem.getGlobalDeclarationsMap(type.file)
            else -> emptyMap()
        }
    }
}

object RazomyTypeSystem {
    val BUILTIN_TYPES = setOf("String", "Number", "Bool", "None", "Any", "Int", "Float", "Str")

    /**
     * Быстрый кэшированный список всех верхнеуровневых деклараций файла.
     */
    fun getGlobalDeclarationsMap(file: PsiFile): Map<String, RazomyPath> {
        return CachedValuesManager.getCachedValue(file) {
            val map = LinkedHashMap<String, RazomyPath>()
            for (decl in PsiTreeUtil.getChildrenOfTypeAsList(file, RazomyDeclaration::class.java)) {
                val path = RazomyUtil.getDeclaredPath(decl)
                if (path != null && path.text != "_") {
                    map.putIfAbsent(path.text, path)
                }
            }
            CachedValueProvider.Result.create(map, file, PsiModificationTracker.MODIFICATION_COUNT)
        }
    }

    /**
     * Кэшированный базовый скоуп уровня файла (импорты, переопределения `_ b` + глобальные объявления).
     */
    private fun getFileBaseScope(file: PsiFile): RazomyScope {
        return CachedValuesManager.getCachedValue(file) {
            val scope = RazomyScope()
            scope.builtInTypes.addAll(BUILTIN_TYPES)

            for (decl in PsiTreeUtil.getChildrenOfTypeAsList(file, RazomyDeclaration::class.java)) {
                val leftExpr = decl.children.firstOrNull()
                val rightExpr = decl.children.getOrNull(1)

                val leftPath = RazomyUtil.firstPath(leftExpr)
                val rightPath = RazomyUtil.firstPath(rightExpr)
                val leftForm =
                    (leftExpr as? RazomyFormBlock) ?: PsiTreeUtil.findChildOfType(leftExpr, RazomyFormBlock::class.java)
                val leftSlot =
                    (leftExpr as? RazomySlot) ?: PsiTreeUtil.findChildOfType(leftExpr, RazomySlot::class.java)

                val isWildcardImport = leftExpr?.text?.trim() == "_" && rightPath != null && RazomyUtil.isImportPath(rightPath.text)
                val isRegularImport = rightPath != null && rightPath.textOffset == rightExpr?.textOffset && RazomyUtil.isImportPath(rightPath.text)

                if (isWildcardImport || isRegularImport) {
                    val impPath = rightPath!!
                    if (leftExpr?.text?.trim() == "_" || (leftPath != null && leftPath.text == "_" && leftPath.textOffset == leftExpr?.textOffset)) {
                        // Переопределение `_ b` перезаписывает экспортируемые символы в скоупе!
                        val importedFile = RazomyImportResolver.resolveImportedFile(file, impPath.text)
                        if (importedFile != null) {
                            for ((exportName, exportElem) in getGlobalDeclarationsMap(importedFile)) {
                                scope.variables[exportName] = exportElem
                                scope.isDestructuredImport[exportName] = true
                                scope.imports[exportName] = impPath
                            }
                        }
                    } else if (leftForm != null && leftForm.textOffset == leftExpr?.textOffset) {
                        for (item in PsiTreeUtil.getChildrenOfTypeAsList(leftForm, RazomyFormItem::class.java)) {
                            val path = RazomyUtil.firstPath(item)
                            if (path != null) {
                                scope.variables[path.text] = path
                                scope.imports[path.text] = impPath
                                scope.isDestructuredImport[path.text] = true
                            }
                        }
                    } else if (leftSlot != null && leftSlot.textOffset == leftExpr?.textOffset) {
                        for (item in PsiTreeUtil.getChildrenOfTypeAsList(leftSlot, RazomySlotItem::class.java)) {
                            val path = RazomyUtil.firstPath(item)
                            if (path != null) {
                                scope.variables[path.text] = path
                                scope.imports[path.text] = impPath
                                scope.isDestructuredImport[path.text] = true
                            }
                        }
                    } else if (leftPath != null && leftPath.textOffset == leftExpr?.textOffset) {
                        val alias = leftPath.text
                        scope.variables[alias] = leftPath
                        scope.imports[alias] = impPath
                    }
                } else {
                    val declared = RazomyUtil.getDeclaredPath(decl)
                    if (declared != null && declared.text != "_") {
                        scope.variables.putIfAbsent(declared.text, declared)
                    }
                }
            }
            CachedValueProvider.Result.create(scope, PsiModificationTracker.MODIFICATION_COUNT)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun renderComputedTypePreview(type: RazomyType, context: PsiElement): String {
        if (type is RazomyType.Unknown || type is RazomyType.Primitive) return ""
        if (type is RazomyType.Module) {
            val exports = getGlobalDeclarationsMap(type.file).keys
            return "{\n" + exports.joinToString("\n") { "  $it" } + "\n}"
        }
        if (type is RazomyType.Union) {
            val branches = type.flattenUnion()
            if (branches.size > 1) {
                return branches.joinToString("\n| ") { branch ->
                    val fields = RazomyType.extractFields(branch)
                    val env = branch.collectTypeArgs()
                    if (fields.isEmpty()) branch.presentableName()
                    else {
                        val inlineFields = fields.entries.joinToString(", ") { (k, v) ->
                            val ft = inferFieldType(v, env, mutableSetOf())
                            if (ft is RazomyType.Unknown) k else "$k: ${ft.presentableName()}"
                        }
                        "${branch.presentableName()} { $inlineFields }"
                    }
                }
            }
        }

        val fields = RazomyType.extractFields(type)
        if (fields.isEmpty()) return ""
        val typeEnv = type.collectTypeArgs()

        val missingInImpl = mutableSetOf<String>()
        if (type is RazomyType.Intersection) {
            val parts = type.flattenIntersection()
            val concreteFields = mutableSetOf<String>()
            var hasConcrete = false
            for (p in parts) {
                if (p is RazomyType.Form) {
                    for ((fname, fdecl) in p.fields) {
                        if (!isAbstractSchemaField(fdecl)) {
                            hasConcrete = true
                            concreteFields.add(fname)
                        }
                    }
                }
            }
            if (hasConcrete) {
                for (fname in fields.keys) {
                    if (fname !in concreteFields) missingInImpl.add(fname)
                }
            }
        }

        val lines = fields.entries.map { (fieldName, fieldElement) ->
            val fieldContainer =
                PsiTreeUtil.getParentOfType(fieldElement, RazomyFormItem::class.java, RazomyDeclaration::class.java)
            val sig = if (fieldContainer != null && fieldElement is RazomyPath) getSymbolSignature(
                fieldElement,
                fieldContainer
            ) else fieldName
            val fieldType = inferFieldType(fieldElement, typeEnv, mutableSetOf())
            val typeStr = if (fieldType !is RazomyType.Unknown) ": ${fieldType.presentableName()}" else ""
            val missingTag = if (fieldName in missingInImpl) " // [не реализовано]" else ""
            "  $sig$typeStr$missingTag"
        }

        return "{\n${lines.joinToString("\n")}\n}"
    }

    fun getSymbolKindLabel(decl: RazomyPath, inferredType: RazomyType): String {
        val container = PsiTreeUtil.getParentOfType(
            decl,
            RazomyDeclaration::class.java,
            RazomyFormItem::class.java,
            RazomyFlowItem::class.java,
            RazomySlotItem::class.java
        )
        if (container is RazomySlotItem) return "param"
        if (container != null) {
            val (_, body) = RazomyUtil.splitDeclaration(container) ?: (decl to null)
            if (body is RazomyFlowBlock) return "fn"
        }
        return when (inferredType) {
            is RazomyType.Module -> "module"
            is RazomyType.Form -> if (inferredType.typeParams.isNotEmpty()) "schema" else "type"
            is RazomyType.Intersection, is RazomyType.Union -> "type"
            else -> if (decl.text.firstOrNull()?.isUpperCase() == true) "type" else "field"
        }
    }

    fun getSymbolSignature(decl: RazomyPath, container: PsiElement): String {
        val (header, _) = RazomyUtil.splitDeclaration(container) ?: return decl.text
        val slot = (header as? RazomyPostfixExpr)?.slots?.firstOrNull()
            ?: PsiTreeUtil.findChildOfType(header, RazomySlot::class.java, false)
        return if (slot != null) "${decl.text}${slot.text}" else decl.text
    }

    private fun doInferUnderscoreType(
        element: PsiElement,
        visited: MutableSet<String>
    ): RazomyType {
        val file: PsiFile? = element.containingFile
        val fileKey = file?.virtualFile?.path ?: file?.name ?: ""
        val elemKey = "underscore:$fileKey:${element.textRange.startOffset}"
        if (!visited.add(elemKey)) return RazomyType.Unknown

        if (element is RazomyPath && RazomyUtil.getImportContext(element) == RazomyUtil.ImportContext.LEFT_ALIAS_OR_DESTRUCT) {
            val decl = PsiTreeUtil.getParentOfType(element, RazomyDeclaration::class.java)
            val rightPath = RazomyUtil.firstPath(decl?.children?.getOrNull(1))
            if (rightPath != null) {
                val importedFile = RazomyImportResolver.resolveImportedFile(element, rightPath.text)
                if (importedFile != null) return RazomyType.Module(importedFile)
            }
        }

        var current: PsiElement? = element
        while (current != null && current !is PsiFile) {
            if (current is RazomyMergeExpr && current.node.findChildByType(RazomyTypes.AMP) != null) {
                val mergeKey = "underscore_merge:$fileKey:${current.textRange.startOffset}"
                if (visited.add(mergeKey)) {
                    var mergedType: RazomyType = RazomyType.Unknown
                    for (op in current.children) {
                        if (op.textOffset < element.textOffset && !PsiTreeUtil.isAncestor(op, element, false)) {
                            val nextType = evalTypeExpr(op, "AnonymousForm", null, emptyMap(), visited)
                            if (nextType !is RazomyType.Unknown) {
                                mergedType =
                                    if (mergedType is RazomyType.Unknown) nextType else RazomyType.Intersection(
                                        mergedType,
                                        nextType
                                    )
                            }
                        }
                    }
                    if (mergedType !is RazomyType.Unknown) return mergedType
                }
            }

            if (current is RazomyFormItem) {
                val formBlock = current.parent as? RazomyFormBlock
                if (formBlock != null && RazomyUtil.isRouterFormBlock(formBlock)) {
                    val firstExpr = current.children.firstOrNull()
                    if (firstExpr != null && element.textOffset > firstExpr.textOffset && !PsiTreeUtil.isAncestor(
                            firstExpr,
                            element,
                            false
                        )
                    ) {
                        val branchPath = RazomyUtil.firstPath(firstExpr)
                        if (branchPath != null && branchPath.text != "_" && branchPath.text.firstOrNull()
                                ?.isUpperCase() == true
                        ) {
                            val branchType = inferTypeByName(branchPath.text, current, visited)
                            if (branchType !is RazomyType.Unknown) return branchType
                        }
                    }
                }
            }

            if (current is RazomyPipelineExpr && PsiTreeUtil.getChildOfType(
                    current,
                    RazomyPipelineOp::class.java
                ) != null
            ) {
                val stages = current.children.filter { it !is RazomyPipelineOp }
                val ops = current.children.filterIsInstance<RazomyPipelineOp>()
                val stageIndex = stages.indexOfFirst { PsiTreeUtil.isAncestor(it, element, false) }
                if (stageIndex > 0) {
                    var streamType = inferExprType(stages[0], RazomyType.Unknown, visited)
                    for (i in 0 until stageIndex - 1) {
                        val opText = ops.getOrNull(i)?.text?.trim() ?: "."
                        val nextStage = stages[i + 1]
                        val stageOut = inferExprType(nextStage, streamType, visited)
                        streamType = when (opText) {
                            ".>", ".?" -> streamType
                            ".&" -> if (streamType !is RazomyType.Unknown && stageOut !is RazomyType.Unknown) {
                                RazomyType.Intersection(streamType, stageOut)
                            } else stageOut

                            else -> applyBypassRule(streamType, nextStage, stageOut, visited)
                        }
                    }
                    if (streamType !is RazomyType.Unknown) return streamType
                }
            }

            if (current is RazomyDeclaration || current is RazomyFormItem || current is RazomyFlowItem) {
                val (leftExpr, _) = RazomyUtil.splitDeclaration(current) ?: (null to null)
                val slot = (leftExpr as? RazomyPostfixExpr)?.slots?.firstOrNull()
                    ?: leftExpr?.let { PsiTreeUtil.findChildOfType(it, RazomySlot::class.java, false) }
                if (slot != null && slot.textOffset < element.textOffset && !PsiTreeUtil.isAncestor(
                        slot,
                        element,
                        false
                    ) && RazomyUtil.isParameterDeclarationSlot(slot)
                ) {
                    val firstItem = PsiTreeUtil.findChildOfType(slot, RazomySlotItem::class.java)
                    if (firstItem != null) {
                        val paths = PsiTreeUtil.findChildrenOfType(firstItem, RazomyPath::class.java).toList()
                        if (paths.size >= 2) {
                            return inferTypeByName(paths[1].text, current, visited)
                        } else if (paths.size == 1) {
                            return inferTypeOfElement(paths[0], visited)
                        }
                    }
                }
            }
            current = current.parent
        }
        return RazomyType.Unknown
    }


    fun evaluateDeclarationContainer(
        name: String,
        declPath: RazomyPath,
        container: PsiElement,
        typeEnv: Map<String, RazomyType> = emptyMap(),
        visited: MutableSet<String> = mutableSetOf()
    ): RazomyType {
        val (headerExpr, bodyExpr) = RazomyUtil.splitDeclaration(container)
            ?: return RazomyType.Primitive(if (name == "String") "Str" else name)

        val typeParams = extractTypeParameters(headerExpr)

        if (bodyExpr == null) {
            return RazomyType.Primitive(if (name == "String") "Str" else name)
        }

        if (bodyExpr is RazomyFlowBlock) {
            if (!visited.add("fn:$name:${container.textOffset}")) return RazomyType.Unknown
            return inferExprType(bodyExpr, RazomyType.Unknown, visited)
        }

        if (container is RazomyDeclaration && name in BUILTIN_TYPES && bodyExpr is RazomyLiteral) {
            return RazomyType.Primitive(if (name == "String") "Str" else name)
        }

        val evaluated = evalTypeExpr(bodyExpr, name, declPath, typeEnv, visited)
        if (evaluated is RazomyType.Form && typeParams.isNotEmpty()) {
            return RazomyType.Form(
                evaluated.name,
                evaluated.declaration,
                evaluated.fields,
                evaluated.supertypes,
                typeParams,
                evaluated.typeArgs
            )
        }
        return evaluated
    }

    private fun extractTypeParameters(headerExpr: PsiElement): List<String> {
        val postfix = (headerExpr as? RazomyPostfixExpr)
            ?: PsiTreeUtil.findChildOfType(headerExpr, RazomyPostfixExpr::class.java)
            ?: return emptyList()
        val slot = postfix.slots.firstOrNull() ?: return emptyList()
        return PsiTreeUtil.getChildrenOfTypeAsList(slot, RazomySlotItem::class.java).mapNotNull { item ->
            RazomyUtil.firstPath(item.children.firstOrNull())?.text
        }
    }

    fun isAbstractSchemaField(fieldDecl: PsiElement): Boolean {
        val formItem = PsiTreeUtil.getParentOfType(fieldDecl, RazomyFormItem::class.java) ?: return false
        val (_, rawBody) = RazomyUtil.splitDeclaration(formItem) ?: return true
        val rightExpr = RazomyUtil.unwrapExpr(rawBody) ?: return true

        if (rightExpr is RazomyLiteral || rightExpr is RazomyFlowBlock || rightExpr is RazomyFormBlock || RazomyUtil.isTypedFormPostfix(
                rightExpr
            )
        ) {
            return false
        }
        if (rightExpr is RazomyPostfixExpr && RazomyUtil.isCallPostfix(rightExpr)) {
            val headPath = RazomyUtil.getPostfixHeadPath(rightExpr)
            if (headPath != null) {
                val resolved = RazomyResolver.resolvePath(rightExpr, headPath.text)
                if (resolved != null) {
                    val container =
                        PsiTreeUtil.getParentOfType(resolved, RazomyDeclaration::class.java, RazomyFormItem::class.java)
                    val (_, body) = container?.let { RazomyUtil.splitDeclaration(it) } ?: (null to null)
                    if (body is RazomyFlowBlock) return false
                }
            }
            return true
        }
        if (rightExpr is RazomyPath) {
            val text = rightExpr.text
            return text in BUILTIN_TYPES || text.firstOrNull()?.isUpperCase() == true
        }
        return false
    }

    fun extractAbstractSchemaRequirements(schemaType: RazomyType): Map<String, RazomyType> {
        val result = LinkedHashMap<String, RazomyType>()
        val fields = RazomyType.extractFields(schemaType)
        val typeEnv = schemaType.collectTypeArgs()

        for ((fieldName, fieldDecl) in fields) {
            if (isAbstractSchemaField(fieldDecl)) {
                val expectedType = inferFieldType(fieldDecl, typeEnv, mutableSetOf())
                result[fieldName] = expectedType
            }
        }
        return result
    }

    fun inferFieldType(
        fieldDecl: PsiElement,
        typeEnv: Map<String, RazomyType>,
        visited: MutableSet<String>
    ): RazomyType {
        val container =
            PsiTreeUtil.getParentOfType(fieldDecl, RazomyFormItem::class.java, RazomyDeclaration::class.java)
                ?: return inferTypeOfElement(fieldDecl, visited)
        val declaredPath = RazomyUtil.getDeclaredPath(container) ?: return inferTypeOfElement(fieldDecl, visited)
        if (declaredPath != fieldDecl) return inferTypeOfElement(fieldDecl, visited)

        val (_, bodyExpr) = RazomyUtil.splitDeclaration(container) ?: return RazomyType.Unknown
        if (bodyExpr == null) return RazomyType.Unknown

        if (bodyExpr is RazomyPath && typeEnv.containsKey(bodyExpr.text)) {
            return typeEnv[bodyExpr.text]!!
        }
        return evaluateDeclarationContainer(declaredPath.text, declaredPath, container, typeEnv, visited)
    }

    fun inferExprType(
        rawExpr: PsiElement?,
        inputType: RazomyType = RazomyType.Unknown,
        visited: MutableSet<String> = mutableSetOf()
    ): RazomyType {
        val expr: PsiElement = RazomyUtil.unwrapExpr(rawExpr) ?: return RazomyType.Unknown
        if (inputType is RazomyType.Unknown && visited.isEmpty()) {
            return CachedValuesManager.getCachedValue(expr) {
                val res = doInferExprType(expr, RazomyType.Unknown, mutableSetOf())
                CachedValueProvider.Result.create(res, PsiModificationTracker.MODIFICATION_COUNT)
            }
        }
        return doInferExprType(expr, inputType, visited)
    }

    private fun doInferExprType(
        expr: PsiElement,
        inputType: RazomyType,
        visited: MutableSet<String>
    ): RazomyType {
        val containingFile: PsiFile? = expr.containingFile
        val exprKey = "expr:${containingFile?.name}:${expr.textRange.startOffset}:${expr.textRange.endOffset}"
        if (!visited.add(exprKey)) return RazomyType.Unknown

        if (expr is RazomyLiteral) {
            val t = expr.text
            return when {
                t.startsWith("\"") || t.startsWith("'") -> RazomyType.Primitive("Str")
                t == "true" || t == "false" -> RazomyType.Primitive("Bool")
                t.contains(".") -> RazomyType.Primitive("Float")
                else -> RazomyType.Primitive("Int")
            }
        }

        if (expr is RazomyPipelineExpr && PsiTreeUtil.getChildOfType(expr, RazomyPipelineOp::class.java) != null) {
            val stages = expr.children.filter { it !is RazomyPipelineOp }
            val ops = expr.children.filterIsInstance<RazomyPipelineOp>()
            if (stages.isNotEmpty()) {
                var current = inferExprType(stages[0], inputType, visited)
                for ((i, op) in ops.withIndex()) {
                    val opText = op.text.trim()
                    val nextStage = stages.getOrNull(i + 1) ?: break
                    val stageOut = inferExprType(nextStage, current, visited)
                    current = when (opText) {
                        ".>", ".?" -> current
                        ".&" -> if (current !is RazomyType.Unknown && stageOut !is RazomyType.Unknown) {
                            RazomyType.Intersection(current, stageOut)
                        } else stageOut

                        else -> applyBypassRule(current, nextStage, stageOut, visited)
                    }
                }
                return current
            }
        }

        if (expr is RazomyPostfixExpr) {
            val headPath = RazomyUtil.getPostfixHeadPath(expr)
            if (headPath != null) {
                val firstFormBlock = expr.formBlocks.firstOrNull()
                if (firstFormBlock != null) {
                    val resolved = inferTypeByName(headPath.text, expr, visited)
                    if (resolved !is RazomyType.Unknown) return resolved
                    val fields = extractFormBlockFields(firstFormBlock)
                    return RazomyType.Form(headPath.text, headPath, fields)
                }
                if (expr.slots.isNotEmpty()) {
                    return evalTypeExpr(expr, headPath.text, null, emptyMap(), visited)
                }
            }
        }

        if (expr is RazomyFormBlock) {
            if (RazomyUtil.isRouterFormBlock(expr)) {
                var unionResult: RazomyType = RazomyType.Unknown
                for (item in expr.children.filterIsInstance<RazomyFormItem>()) {
                    val branchBody = item.children.getOrNull(1) ?: continue
                    val branchType = inferExprType(branchBody, inputType, visited)
                    if (branchType !is RazomyType.Unknown) {
                        unionResult = if (unionResult is RazomyType.Unknown) branchType else RazomyType.Union(
                            unionResult,
                            branchType
                        )
                    }
                }
                return unionResult
            } else {
                return RazomyType.Form("AnonymousForm", expr, extractFormBlockFields(expr))
            }
        }

        if (expr is RazomyFlowBlock) {
            val flowItems = expr.children.filterIsInstance<RazomyFlowItem>()
            val lastItem = flowItems.lastOrNull() ?: return RazomyType.Unknown
            return inferExprType(lastItem.children.firstOrNull() ?: lastItem, inputType, visited)
        }

        if (expr is RazomyPath) {
            if (expr.text == "_") {
                return if (inputType !is RazomyType.Unknown) inputType else inferUnderscoreType(expr, visited)
            }
            if (expr.text.contains("native_print") || expr.text.endsWith("Shell.Log")) {
                return RazomyType.Primitive("Unit")
            }
            if (expr.text.contains("native_exec") || expr.text.endsWith("Shell.Exec")) {
                return RazomyType.Primitive("Str")
            }

            val resolved = RazomyResolver.resolvePath(expr, expr.text, visited)
            if (resolved != null) {
                return inferTypeOfElement(resolved, visited)
            }
        }

        return RazomyType.Unknown
    }

    private fun extractFormBlockFields(formBlock: RazomyFormBlock): Map<String, PsiElement> {
        val fields = LinkedHashMap<String, PsiElement>()
        for (item in formBlock.children.filterIsInstance<RazomyFormItem>()) {
            val fieldPath = RazomyUtil.getDeclaredPath(item)
            if (fieldPath != null) {
                fields[fieldPath.text] = fieldPath
            }
        }
        return fields
    }

    private fun applyBypassRule(
        inputType: RazomyType,
        stage: PsiElement,
        stageOutput: RazomyType,
        visited: MutableSet<String> = mutableSetOf()
    ): RazomyType {
        if (inputType !is RazomyType.Union || stageOutput is RazomyType.Unknown) return stageOutput
        val expectedInput = getExpectedInputTypeOfStage(stage, visited) ?: return stageOutput
        val bypassedBranches = inputType.flattenUnion().filter { !isTypeCompatible(it, expectedInput, stage) }
        var result = stageOutput
        for (b in bypassedBranches) {
            result = RazomyType.Union(result, b)
        }
        return result
    }

    fun getExpectedInputTypeOfStage(
        rawStage: PsiElement,
        visited: MutableSet<String> = mutableSetOf()
    ): RazomyType? {
        val stage: PsiElement = RazomyUtil.unwrapExpr(rawStage) ?: rawStage

        if (stage is RazomyFormBlock && RazomyUtil.isRouterFormBlock(stage)) {
            var expectedUnion: RazomyType = RazomyType.Unknown
            for (item in stage.children.filterIsInstance<RazomyFormItem>()) {
                val path = RazomyUtil.firstPath(item.children.firstOrNull()) ?: continue
                if (path.text == "_") return null
                if (path.text.firstOrNull()?.isUpperCase() == true) {
                    val branchType = inferTypeByName(path.text, stage, visited)
                    if (branchType !is RazomyType.Unknown) {
                        expectedUnion = if (expectedUnion is RazomyType.Unknown) branchType else RazomyType.Union(
                            expectedUnion,
                            branchType
                        )
                    }
                }
            }
            return if (expectedUnion !is RazomyType.Unknown) expectedUnion else null
        }

        val path = RazomyUtil.firstPath(stage)
        if (path != null && path.textOffset == stage.textOffset) {
            if (path.text.startsWith("native_")) return null

            val resolved = RazomyResolver.resolvePath(path, path.text, visited) ?: return null

            val slotParent = PsiTreeUtil.getParentOfType(resolved, RazomySlot::class.java)
            if (slotParent != null && RazomyUtil.isParameterDeclarationSlot(slotParent)) {
                return inferTypeOfElement(resolved, visited)
            }

            val decl = PsiTreeUtil.getParentOfType(resolved, RazomyDeclaration::class.java, RazomyFormItem::class.java)
            if (decl != null && RazomyUtil.getDeclaredPath(decl) == resolved) {
                val (leftExpr, rightBody) = RazomyUtil.splitDeclaration(decl) ?: return null
                val slot = (leftExpr as? RazomyPostfixExpr)?.slots?.firstOrNull()
                    ?: PsiTreeUtil.findChildOfType(leftExpr, RazomySlot::class.java, false)
                if (slot != null && RazomyUtil.isParameterDeclarationSlot(slot)) {
                    val firstParam = PsiTreeUtil.getChildrenOfTypeAsList(slot, RazomySlotItem::class.java).firstOrNull()
                        ?: return null
                    val paths = PsiTreeUtil.findChildrenOfType(firstParam, RazomyPath::class.java).toList()

                    if (PsiTreeUtil.findChildOfType(
                            firstParam,
                            RazomyMergeExpr::class.java
                        ) != null && paths.size > 1
                    ) {
                        var merged: RazomyType = RazomyType.Unknown
                        for (p in paths) {
                            val t = inferTypeByName(p.text, stage, visited)
                            if (t !is RazomyType.Unknown) {
                                merged = if (merged is RazomyType.Unknown) t else RazomyType.Intersection(merged, t)
                            }
                        }
                        return if (merged !is RazomyType.Unknown) merged else null
                    }

                    if (paths.size >= 2) {
                        return inferTypeByName(paths[1].text, stage, visited)
                    }

                    if (paths.size == 1 && paths[0].text.firstOrNull()?.isUpperCase() == true) {
                        val directType = inferTypeByName(paths[0].text, stage, visited)
                        if (directType !is RazomyType.Unknown) return directType
                    }

                    val paramName = paths.firstOrNull()?.text
                    if (paramName != null && rightBody != null && visited.add("expected:${resolved.text}")) {
                        for (pipe in PsiTreeUtil.findChildrenOfType(rightBody, RazomyPipelineExpr::class.java)) {
                            val pipeChildren = pipe.children.filter { it !is RazomyPipelineOp }
                            if (pipeChildren.size >= 2 && pipeChildren[0].text == paramName) {
                                val inferredFromRouter = getExpectedInputTypeOfStage(pipeChildren[1], visited)
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
        return expectedFields.isNotEmpty() && actualFields.isNotEmpty() && actualFields.keys.containsAll(expectedFields.keys)
    }

    fun buildScope(
        element: PsiElement,
        visited: MutableSet<String> = mutableSetOf()
    ): RazomyScope {
        val scope = RazomyScope()
        var current: PsiElement? = element.parent

        val underscoreType = inferUnderscoreType(element, visited)
        if (underscoreType !is RazomyType.Unknown) {
            scope.unpackedFields.putAll(RazomyType.extractFields(underscoreType))
        }

        // 1. Включаем в скоуп все поля и функции из левых операндов слияния `Cli { BuildCommand(...) } & Cli.Schema { ... }`
        for (precedingOp in RazomyUtil.getPrecedingMergeOperands(element)) {
            val formBlock: RazomyFormBlock? = when (precedingOp) {
                is RazomyPostfixExpr -> if (RazomyUtil.isTypedFormPostfix(precedingOp)) precedingOp.formBlocks.firstOrNull() else null
                is RazomyFormBlock -> precedingOp
                else -> null
            }
            if (formBlock != null && !RazomyUtil.isRouterFormBlock(formBlock)) {
                for (item in formBlock.children.filterIsInstance<RazomyFormItem>()) {
                    val fieldPath = RazomyUtil.getDeclaredPath(item)
                    if (fieldPath != null) {
                        scope.variables.putIfAbsent(fieldPath.text, fieldPath)
                    }
                }
            }
        }

        // 2. Идем вверх по родительским блокам AST
        while (current != null && current !is PsiFile) {
            if (current is RazomyFormItem) {
                val firstPath = RazomyUtil.firstPath(current.children.firstOrNull())
                if (firstPath != null && firstPath.text.firstOrNull()?.isUpperCase() == true) {
                    val parentForm = current.parent as? RazomyFormBlock
                    if (parentForm != null && RazomyUtil.isRouterFormBlock(parentForm)) {
                        val resolvedType = inferTypeByName(firstPath.text, element, visited)
                        scope.unpackedFields.putAll(RazomyType.extractFields(resolvedType))
                    }
                }
            }

            if (current is RazomyFormBlock && !RazomyUtil.isRouterFormBlock(current)) {
                for (item in current.children.filterIsInstance<RazomyFormItem>()) {
                    val fieldPath = RazomyUtil.getDeclaredPath(item)
                    if (fieldPath != null && (!PsiTreeUtil.isAncestor(item, element, false) || fieldPath == element)) {
                        scope.variables.putIfAbsent(fieldPath.text, fieldPath)
                    }
                }
            }

            if (current is RazomyFlowBlock) {
                for (flowItem in current.children.filterIsInstance<RazomyFlowItem>()) {
                    if (flowItem.textOffset < element.textOffset && !PsiTreeUtil.isAncestor(flowItem, element, false)) {
                        val formBlock = PsiTreeUtil.findChildOfType(flowItem, RazomyFormBlock::class.java)
                        if (formBlock != null && !RazomyUtil.isRouterFormBlock(formBlock)) {
                            for (item in formBlock.children.filterIsInstance<RazomyFormItem>()) {
                                val fieldPath = RazomyUtil.getDeclaredPath(item)
                                if (fieldPath != null) {
                                    scope.unpackedFields.putIfAbsent(fieldPath.text, fieldPath)
                                }
                            }
                        }
                    }
                }
            }

            if (current is RazomyDeclaration || current is RazomyFormItem || current is RazomyFlowItem) {
                val (leftExpr, _) = RazomyUtil.splitDeclaration(current) ?: (null to null)
                val slot = (leftExpr as? RazomyPostfixExpr)?.slots?.firstOrNull()
                    ?: leftExpr?.let { PsiTreeUtil.findChildOfType(it, RazomySlot::class.java, false) }
                if (slot != null && slot.textOffset < element.textOffset && RazomyUtil.isParameterDeclarationSlot(slot)) {
                    val declName = RazomyUtil.getDeclaredPath(current)?.text
                    if (declName != null) {
                        visited.add(declName)
                        visited.add("fn:$declName")
                    }

                    for (item in PsiTreeUtil.getChildrenOfTypeAsList(slot, RazomySlotItem::class.java)) {
                        val paths = PsiTreeUtil.findChildrenOfType(item, RazomyPath::class.java).toList()
                        val mergeExpr = PsiTreeUtil.findChildOfType(item, RazomyMergeExpr::class.java)
                        if (mergeExpr != null && mergeExpr.node.findChildByType(RazomyTypes.AMP) != null && paths.size > 1) {
                            for (p in paths) {
                                val t = inferTypeByName(p.text, element, visited)
                                scope.unpackedFields.putAll(RazomyType.extractFields(t))
                            }
                        } else if (paths.isNotEmpty()) {
                            val varName = paths[0]
                            scope.variables.putIfAbsent(varName.text, varName)

                            val resolvedType = inferTypeOfElement(varName, visited)
                            if (resolvedType !is RazomyType.Unknown) {
                                scope.unpackedFields.putAll(RazomyType.extractFields(resolvedType))
                            } else if (paths.size == 1 && varName.text != declName) {
                                val fallbackType = inferTypeByName(varName.text, element, visited)
                                scope.unpackedFields.putAll(RazomyType.extractFields(fallbackType))
                            }
                        }
                    }
                }
            }
            current = current.parent
        }

        // 3. Мгновенно добавляем кэшированные глобальные декларации и импорты файла
        val file: PsiFile? = element.containingFile
        if (file != null) {
            val baseScope = getFileBaseScope(file)
            for ((k, v) in baseScope.variables) scope.variables.putIfAbsent(k, v)
            scope.imports.putAll(baseScope.imports)
            scope.isDestructuredImport.putAll(baseScope.isDestructuredImport)
            scope.builtInTypes.addAll(baseScope.builtInTypes)
        }
        return scope
    }

    fun inferUnderscoreType(
        element: PsiElement,
        visited: MutableSet<String> = mutableSetOf()
    ): RazomyType {
        val file: PsiFile? = element.containingFile
        val fileKey = file?.virtualFile?.path ?: file?.name ?: ""
        val elemKey = "underscore:$fileKey:${element.textRange.startOffset}"
        if (!visited.add(elemKey)) return RazomyType.Unknown

        return try {
            CachedValuesManager.getCachedValue(element) {
                val res = doInferUnderscoreType(element, mutableSetOf(elemKey))
                CachedValueProvider.Result.create(res, PsiModificationTracker.MODIFICATION_COUNT)
            }
        } finally {
            visited.remove(elemKey)
        }
    }

    fun inferTypeByName(
        name: String,
        context: PsiElement,
        visited: MutableSet<String> = mutableSetOf()
    ): RazomyType {
        val file: PsiFile = context.containingFile ?: return RazomyType.Unknown
        val visitKey = "type:${file.virtualFile?.path ?: file.name}:$name"
        if (name.isBlank() || name == "_" || !visited.add(visitKey)) return RazomyType.Unknown

        try {
            if (name.contains(".")) {
                val resolved = RazomyResolver.resolvePath(context, name, visited)
                if (resolved != null) return inferTypeOfElement(resolved, visited)
            }

            val normalizedName = if (name == "String") "Str" else name

            // 1. Быстрый поиск по кэшированным глобальным декларациям файла
            val globalMap = getGlobalDeclarationsMap(file)
            val directDeclPath = globalMap[name] ?: globalMap[normalizedName]
            if (directDeclPath != null) {
                val decl = PsiTreeUtil.getParentOfType(directDeclPath, RazomyDeclaration::class.java)
                if (decl != null && RazomyUtil.getImportContext(directDeclPath) == null) {
                    return evaluateDeclarationContainer(directDeclPath.text, directDeclPath, decl, emptyMap(), visited)
                }
            }

            // 2. Быстрый поиск среди кэшированных импортов файла (включая `_ b`)
            val fileScope = getFileBaseScope(file)
            val importRightPath = fileScope.imports[name]
            if (importRightPath != null) {
                val importedFile = RazomyImportResolver.resolveImportedFile(context, importRightPath.text)
                if (importedFile != null) {
                    if (fileScope.isDestructuredImport[name] == true) {
                        val foreignSymbol = RazomyImportResolver.findExportedSymbol(importedFile, name)
                        if (foreignSymbol != null) return inferTypeOfElement(foreignSymbol, visited)
                    } else {
                        return RazomyType.Module(importedFile)
                    }
                }
                return RazomyType.Unknown
            }

            // 3. Автоматический резолв первого попавшегося символа из других файлов контекста (`a.rl`, `b.rl`)
            val contextSymbol = RazomyImportResolver.findFirstContextSymbol(context, name)
            if (contextSymbol != null) {
                return inferTypeOfElement(contextSymbol, visited)
            }

            if (name in BUILTIN_TYPES) {
                return RazomyType.Primitive(normalizedName)
            }

            return RazomyType.Unknown
        } finally {
            visited.remove(visitKey)
        }
    }

    fun evalTypeExpr(
        rawExpr: PsiElement,
        aliasName: String,
        declPath: RazomyPath?,
        typeEnv: Map<String, RazomyType>,
        visited: MutableSet<String>
    ): RazomyType {
        val expr: PsiElement = RazomyUtil.unwrapExpr(rawExpr) ?: rawExpr
        val file: PsiFile? = expr.containingFile
        val fileKey = file?.virtualFile?.path ?: file?.name ?: ""
        val evalKey = "eval:$fileKey:${expr.textRange.startOffset}:${expr.textRange.endOffset}"
        if (!visited.add(evalKey)) return RazomyType.Unknown

        try {
            if (expr is RazomyFormBlock && !RazomyUtil.isRouterFormBlock(expr)) {
                val fields = extractFormBlockFields(expr)
                return RazomyType.Form(aliasName, declPath ?: expr, fields)
            }

            if (expr is RazomyPostfixExpr) {
                val headPath = RazomyUtil.getPostfixHeadPath(expr)
                val formBlock = expr.formBlocks.firstOrNull()
                val slot = expr.slots.firstOrNull()

                if (headPath != null && formBlock != null) {
                    if (declPath != null && headPath == declPath) {
                        val fields = extractFormBlockFields(formBlock)
                        return RazomyType.Form(aliasName, declPath, fields)
                    }
                    val baseType = if (!headPath.text.contains(".")) {
                        inferTypeByName(headPath.text, headPath, visited)
                    } else {
                        evalTypeExpr(headPath, headPath.text, null, typeEnv, visited)
                    }
                    val ownFields = extractFormBlockFields(formBlock)
                    val ownForm = RazomyType.Form(headPath.text, headPath, ownFields)
                    return if (baseType !is RazomyType.Unknown) {
                        RazomyType.Intersection(baseType, ownForm, aliasName)
                    } else ownForm
                }

                if (headPath != null && slot != null) {
                    val resolvedCallee = RazomyResolver.resolvePath(expr, headPath.text, visited)
                    if (resolvedCallee != null) {
                        val baseType = inferTypeOfElement(resolvedCallee, visited)
                        if (baseType is RazomyType.Form && baseType.typeParams.isNotEmpty()) {
                            val slotArgs = PsiTreeUtil.getChildrenOfTypeAsList(slot, RazomySlotItem::class.java)
                            val boundArgs = LinkedHashMap<String, RazomyType>()
                            for (i in baseType.typeParams.indices) {
                                val paramName = baseType.typeParams[i]
                                val argItem = slotArgs.getOrNull(i)?.children?.firstOrNull()
                                if (argItem != null) {
                                    val argType = evalTypeExpr(argItem, paramName, null, typeEnv, visited)
                                    if (argType !is RazomyType.Unknown) {
                                        boundArgs[paramName] = argType
                                    }
                                }
                            }
                            return baseType.withTypeArgs(boundArgs)
                        }
                        return baseType
                    }
                }
            }

            if (expr is RazomySubtractExpr && expr.node.findChildByType(RazomyTypes.SLASH) != null) {
                val children = expr.children
                if (children.size >= 2) {
                    val baseType = evalTypeExpr(children[0], aliasName, declPath, typeEnv, visited)
                    val baseFields = RazomyType.extractFields(baseType).toMutableMap()
                    for (i in 1 until children.size) {
                        val removedText = RazomyUtil.firstPath(children[i])?.text ?: continue
                        val fieldName = removedText.substringAfterLast('.')
                        baseFields.remove(fieldName)
                    }
                    return RazomyType.Form(
                        aliasName,
                        declPath ?: expr,
                        baseFields,
                        supertypes = baseType.allTypeNames() + aliasName,
                        typeArgs = baseType.collectTypeArgs()
                    )
                }
            }

            if (expr is RazomyMergeExpr && expr.node.findChildByType(RazomyTypes.AMP) != null) {
                var currentType: RazomyType = RazomyType.Unknown
                val parentNames = mutableSetOf(aliasName)

                for (rawOp in expr.children) {
                    val op = RazomyUtil.unwrapExpr(rawOp) ?: rawOp
                    if (op is RazomyFlowBlock || (PsiTreeUtil.findChildOfType(
                            op,
                            RazomyFlowBlock::class.java
                        ) != null && !RazomyUtil.isTypedFormPostfix(op) && op !is RazomyFormBlock)
                    ) {
                        continue
                    }
                    val nextType = evalTypeExpr(op, aliasName, declPath, typeEnv, visited)
                    if (nextType !is RazomyType.Unknown) {
                        parentNames.addAll(nextType.allTypeNames())
                        currentType = if (currentType is RazomyType.Unknown) {
                            nextType.withAlias(aliasName, parentNames)
                        } else {
                            RazomyType.Intersection(currentType, nextType, aliasName)
                        }
                    }
                }
                return currentType
            }

            if (expr is RazomyUnionExpr && expr.node.findChildByType(RazomyTypes.PIPE) != null) {
                var currentType: RazomyType = RazomyType.Unknown
                for (rawOp in expr.children) {
                    val op = RazomyUtil.unwrapExpr(rawOp) ?: rawOp
                    val nextType = evalTypeExpr(op, aliasName, declPath, typeEnv, visited)
                    if (nextType !is RazomyType.Unknown) {
                        currentType = if (currentType is RazomyType.Unknown) {
                            nextType
                        } else {
                            RazomyType.Union(currentType, nextType, aliasName)
                        }
                    }
                }
                return currentType
            }

            if (expr is RazomyPath) {
                val text = expr.text
                if (typeEnv.containsKey(text)) return typeEnv[text]!!
                if (text in BUILTIN_TYPES) return RazomyType.Primitive(if (text == "String") "Str" else text)

                val resolved = RazomyResolver.resolvePath(expr, text, visited)
                if (resolved != null && resolved != declPath) {
                    return inferTypeOfElement(resolved, visited).withAlias(aliasName, setOf(aliasName, text))
                }
                return inferTypeByName(text, expr, visited).withAlias(aliasName, setOf(aliasName, text))
            }

            return inferExprType(expr, RazomyType.Unknown, visited)
        } finally {
            visited.remove(evalKey)
        }
    }

    fun inferTypeOfElement(
        element: PsiElement,
        visited: MutableSet<String> = mutableSetOf()
    ): RazomyType {
        val containingFile: PsiFile? = element.containingFile
        val elemKey = "elem:${containingFile?.name}:${element.textOffset}"
        if (!visited.add(elemKey)) return RazomyType.Unknown

        return try {
            CachedValuesManager.getCachedValue(element) {
                val res = doInferTypeOfElement(element, mutableSetOf(elemKey))
                CachedValueProvider.Result.create(res, PsiModificationTracker.MODIFICATION_COUNT)
            }
        } finally {
            visited.remove(elemKey)
        }
    }

    private fun doInferTypeOfElement(
        element: PsiElement,
        visited: MutableSet<String>
    ): RazomyType {
        val containingFile: PsiFile? = element.containingFile

        if (element is RazomyPath) {
            if (element.text == "_") return inferUnderscoreType(element, visited)
            if (element.text.startsWith("_.")) {
                val resolved = RazomyResolver.resolvePath(element, element.text, visited)
                if (resolved != null && resolved != element) {
                    return inferTypeOfElement(resolved, visited)
                }
                return RazomyType.Unknown
            }

            // 1. Элемент внутри слота параметров `(A VectorFloat3)` или `(T)`
            val slotItem = PsiTreeUtil.getParentOfType(element, RazomySlotItem::class.java)
            if (slotItem != null) {
                val paths = PsiTreeUtil.findChildrenOfType(slotItem, RazomyPath::class.java).toList()
                if (paths.size > 1 && paths[0] == element) {
                    return inferTypeByName(paths[1].text, element, visited)
                }

                val slot = slotItem.parent as? RazomySlot
                if (slot != null && RazomyUtil.isParameterDeclarationSlot(slot)) {
                    val parentDecl = PsiTreeUtil.getParentOfType(
                        slot,
                        RazomyDeclaration::class.java,
                        RazomyFormItem::class.java,
                        RazomyFlowItem::class.java
                    )
                    if (paths.size == 1 && paths[0] == element) {
                        val globalDecl = containingFile?.let { getGlobalDeclarationsMap(it)[element.text] }
                        if (globalDecl == null || globalDecl == element || PsiTreeUtil.isAncestor(
                                parentDecl,
                                globalDecl,
                                false
                            )
                        ) {
                            return RazomyType.Unknown
                        }
                    }
                } else {
                    val postfixExpr = PsiTreeUtil.getParentOfType(slotItem, RazomyPostfixExpr::class.java)
                    val callerPath = postfixExpr?.let { RazomyUtil.getPostfixHeadPath(it) }
                    if (callerPath != null && callerPath.text.firstOrNull()?.isUpperCase() == true) {
                        val callerType = inferTypeByName(callerPath.text, element, visited)
                        if (callerType !is RazomyType.Unknown) return callerType
                    }
                }
            }

            // 2. Элемент является именем декларации
            val container = PsiTreeUtil.getParentOfType(
                element,
                RazomyFormItem::class.java,
                RazomyFlowItem::class.java,
                RazomyDeclaration::class.java
            )
            if (container != null) {
                val declaredPath = RazomyUtil.getDeclaredPath(container)
                if (declaredPath == element) {
                    return evaluateDeclarationContainer(element.text, element, container, emptyMap(), visited)
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
}

// =============================================================================
// 6. СЕМАНТИЧЕСКИЙ РЕЗОЛВЕР ПУТЕЙ (RAZOMY RESOLVER — CACHED)
// =============================================================================

object RazomyResolver {

    fun resolvePath(
        context: PsiElement,
        pathText: String,
        visited: MutableSet<String> = mutableSetOf()
    ): PsiElement? {
        val cleanPath = pathText.replace("\\s+\\.\\s+".toRegex(), ".")
        val file: PsiFile? = context.containingFile
        val fileKey = file?.virtualFile?.path ?: file?.name ?: ""
        val resolveKey = "resolve:$fileKey:${context.textRange.startOffset}:$cleanPath"
        if (!visited.add(resolveKey)) return null

        return try {
            if (context is RazomyPath && context.text == pathText) {
                CachedValuesManager.getCachedValue(context) {
                    val res = doResolvePath(context, cleanPath, mutableSetOf(resolveKey))
                    CachedValueProvider.Result.create(res, PsiModificationTracker.MODIFICATION_COUNT)
                }
            } else {
                doResolvePath(context, cleanPath, visited)
            }
        } finally {
            visited.remove(resolveKey)
        }
    }

    private fun doResolvePath(
        context: PsiElement,
        cleanPath: String,
        visited: MutableSet<String>
    ): PsiElement? {
        val parts = cleanPath.split(".")
        if (parts.isEmpty()) return null
        val file: PsiFile? = context.containingFile

        // 1. Резолв `_` и `_.Field`
        if (parts[0] == "_") {
            val underscoreType = RazomyTypeSystem.inferUnderscoreType(context, visited)
            if (parts.size == 1) {
                return when (underscoreType) {
                    is RazomyType.Form -> underscoreType.declaration
                    is RazomyType.Module -> underscoreType.file
                    else -> null
                }
            }
            var currentElem: PsiElement? = RazomyType.extractFields(underscoreType)[parts[1]] ?: return null
            for (i in 2 until parts.size) {
                currentElem = resolveMember(currentElem!!, parts[i], visited) ?: return null
            }
            return currentElem
        }

        // 2. Резолв имени поля при инстанцировании типа (`VectorFloat3 { X (A.X + B.X) }`)
        if (parts.size == 1 && context is RazomyPath) {
            val formItem = context.parent?.let { PsiTreeUtil.getParentOfType(it, RazomyFormItem::class.java) }
            val formBlock = formItem?.parent as? RazomyFormBlock
            if (formBlock != null && RazomyUtil.isInstantiationFormBlock(formBlock)) {
                if (RazomyUtil.getDeclaredPath(formItem) == context) {
                    val typedForm = formBlock.parent as? RazomyPostfixExpr
                    val typePath = typedForm?.let { RazomyUtil.getPostfixHeadPath(it) }
                    if (typePath != null) {
                        val targetType = RazomyTypeSystem.inferTypeByName(typePath.text, typePath, visited)
                        val targetField = RazomyType.extractFields(targetType)[parts[0]]
                        if (targetField != null) return targetField
                    }
                }
            }
        }

        val rootName = parts[0]
        val scope = RazomyTypeSystem.buildScope(context, visited)

        var current: PsiElement? = scope.variables[rootName] ?: scope.unpackedFields[rootName]

        // 3. Если есть явный импорт или переопределение `_ b`
        if (scope.imports.containsKey(rootName)) {
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

            if (current == null && parts.size > 1 && RazomyUtil.isImportPath(rootName)) {
                var fileRef: PsiFile? = null
                var memberStartIndex = 0
                for (i in parts.size - 1 downTo 1) {
                    val prefix = parts.take(i).joinToString(".")
                    val resolvedFile = RazomyImportResolver.resolveImportedFile(context, prefix)
                    if (resolvedFile != null) {
                        fileRef = resolvedFile
                        memberStartIndex = i
                        break
                    }
                }

                if (fileRef != null) {
                    current = fileRef
                    for (i in memberStartIndex until parts.size) {
                        current = resolveMember(current!!, parts[i], visited) ?: return null
                    }
                    return current
                }
            }

            if (current == null && parts.size == 1 && context !is RazomyPath && file != null) {
                val fieldMatch = RazomyTypeSystem.getGlobalDeclarationsMap(file)[rootName]
                if (fieldMatch != null) return fieldMatch
            }

            // 4. ГЛАВНОЕ: Если символ не объявлен и не импортирован явно — берем ПЕРВЫЙ ПОПАВШИЙСЯ из контекста (`a.rl`)!
            if (current == null) {
                current = RazomyImportResolver.findFirstContextSymbol(context, rootName)
            }

            if (current == null) return null
        }

        // 5. Пошаговый резолв цепочки `A.B.C`
        var resolvedChain = resolveChainFrom(current, parts, visited)
        if (resolvedChain == null && parts.size > 1) {
            val globalFallback = file?.let { RazomyTypeSystem.getGlobalDeclarationsMap(it)[rootName] }
                ?: RazomyImportResolver.findFirstContextSymbol(context, rootName)
            if (globalFallback != null && globalFallback != current) {
                resolvedChain = resolveChainFrom(globalFallback, parts, visited)
            }
        }
        return resolvedChain
    }

    private fun resolveChainFrom(
        rootElement: PsiElement,
        parts: List<String>,
        visited: MutableSet<String>
    ): PsiElement? {
        var current: PsiElement = rootElement
        for (i in 1 until parts.size) {
            current = resolveMember(current, parts[i], visited) ?: return null
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
        val type = RazomyTypeSystem.inferTypeOfElement(owner, visited)
        val fields = RazomyType.extractFields(type)
        if (fields.containsKey(memberName)) {
            return fields[memberName]
        }
        return null
    }
}
