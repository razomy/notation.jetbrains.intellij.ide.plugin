package org.razomy.language

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon
import com.intellij.lexer.FlexAdapter
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull
import org.razomy.language.psi.RazomyTypes

import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import org.razomy.language.psi.RazomyBraceGroup

import com.intellij.lang.Commenter


import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import org.razomy.language.psi.RazomyAngleGroup
import org.razomy.language.psi.RazomyParenGroup
import org.razomy.language.psi.RazomyPath

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext


import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import org.razomy.language.psi.RazomyGroup
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import org.razomy.language.psi.RazomyStatement

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.psi.PsiComment
import com.intellij.codeInspection.*
import com.intellij.psi.PsiElementVisitor

import com.intellij.formatting.*
import com.intellij.psi.TokenType
import com.intellij.ide.structureView.*
import com.intellij.ide.structureView.StructureViewModel.ElementInfoProvider
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
class RazomyGotoDeclarationHandler : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor?
    ): Array<PsiElement>? {
        if (sourceElement == null) return null

        val word = sourceElement.text
        if (word.isBlank() || word == "match" || word == "instanceof") return null

        // Находим родительский Path для токена под курсором
        val currentPath = PsiTreeUtil.getParentOfType(sourceElement, RazomyPath::class.java) ?: return null

        // 1. ЕСЛИ КЛИКНУЛИ НА САМО ОБЪЯВЛЕНИЕ:
        // Не прыгаем никуда (IntelliJ сама предложит "Find Usages")
        if (isDeclaration(currentPath)) {
            return null
        }

        val file = sourceElement.containingFile ?: return null

        // 2. ЕСЛИ ЭТО ИСПОЛЬЗОВАНИЕ:
        // Ищем место, где этот символ был инициализирован/объявлен
        val target = resolveDeclaration(currentPath, word, file) ?: return null

        return arrayOf(target)
    }

    /**
     * Поиск объявления с учётом контекста (Scope):
     * 1. Сначала ищем локально в текущем блоке { ... }
     * 2. Если не найдено — ищем глобально в корне файла
     */
    private fun resolveDeclaration(from: RazomyPath, name: String, file: PsiFile): RazomyPath? {
        val parentBlock = PsiTreeUtil.getParentOfType(from, RazomyBraceGroup::class.java)

        // 1. Поиск в локальном контексте (если мы внутри { ... })
        if (parentBlock != null) {
            val localPaths = PsiTreeUtil.findChildrenOfType(parentBlock, RazomyPath::class.java)
            for (path in localPaths) {
                if (path != from && path.text == name && isDeclaration(path)) {
                    return path
                }
            }
        }

        // 2. Поиск в корне файла (глобальные объявления)
        val allPaths = PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java)
        for (path in allPaths) {
            if (path != from && path.text == name && isDeclaration(path)) {
                return path
            }
        }

        return null
    }

    companion object {
        /**
         * Определяет, является ли Path объявлением (инициализацией):
         */
        fun isDeclaration(path: RazomyPath): Boolean {
            val name = path.text
            if (name == "match" || name == "instanceof") return false

            val file = path.containingFile ?: return false
            val fileText = file.text

            // Должен быть первым словом на своей физической строке
            if (!isFirstOnLine(path, fileText)) return false

            // Проверяем уровень вложенности в { ... }
            val braceParents = PsiTreeUtil.collectParents(path, RazomyBraceGroup::class.java, false) { false }

            if (braceParents.isEmpty()) {
                // Корень файла: любое первое слово строки — это объявление типа/модуля!
                return true
            } else {
                // Внутри контекста { ... }:
                // Это объявление ТОЛЬКО если следом за ним идёт блок { ... } (как `Log { ... }`)
                val nextSibling = PsiTreeUtil.skipWhitespacesForward(path)
                return nextSibling is RazomyGroup && nextSibling.braceGroup != null
            }
        }

        /**
         * Проверяет, что перед токеном на текущей строке нет другого кода (только отступы).
         */
        private fun isFirstOnLine(element: PsiElement, fileText: CharSequence): Boolean {
            var i = element.textRange.startOffset - 1
            while (i >= 0) {
                val ch = fileText[i]
                if (ch == '\n' || ch == '\r') return true
                if (!ch.isWhitespace()) return false
                i--
            }
            return true
        }
    }
}




class RazomyDocumentationProvider : AbstractDocumentationProvider() {

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val target = when (element) {
            is RazomyPath -> element
            else -> PsiTreeUtil.getParentOfType(originalElement, RazomyPath::class.java)
        } ?: return null

        val file = target.containingFile ?: return null

        // Находим объявление сущности
        val decl = if (RazomyGotoDeclarationHandler.isDeclaration(target)) {
            target
        } else {
            val all = PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java)
            all.firstOrNull { it.text == target.text && RazomyGotoDeclarationHandler.isDeclaration(it) }
        } ?: return null

        val statement = PsiTreeUtil.getParentOfType(decl, RazomyStatement::class.java) ?: decl

        // Ищем комментарий прямо перед объявлением
        val comment = findPrecedingComment(statement)

        val doc = StringBuilder()
        doc.append("<div style='font-family: monospace;'>")
        doc.append("<b>Type:</b> <code>${decl.text}</code><br/>")
        doc.append("<b>Declaration:</b> <code>${escapeHtml(statement.text)}</code>")
        doc.append("</div>")

        if (comment != null) {
            doc.append("<hr/><p style='color: gray;'>${escapeHtml(comment)}</p>")
        }

        return doc.toString()
    }

    private fun findPrecedingComment(element: PsiElement): String? {
        var prev = element.prevSibling
        while (prev != null && prev.text.isBlank()) {
            prev = prev.prevSibling
        }
        return if (prev is PsiComment) prev.text.removePrefix("//").trim() else null
    }

    private fun escapeHtml(text: String) =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}



class RazomyLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (element !is RazomyPath || !RazomyGotoDeclarationHandler.isDeclaration(element)) return

        val file = element.containingFile ?: return
        val currentName = element.text
        val statement = PsiTreeUtil.getParentOfType(element, RazomyStatement::class.java) ?: return

        // 1. ИЩЕМ ПРЕДКОВ: берем только ПРЯМЫХ потомков statement (не заглядывая в { ... })
        val directPaths = statement.children.filterIsInstance<RazomyPath>()
        val superTypes = mutableListOf<PsiElement>()

        for (path in directPaths) {
            // Если это не само объявление и начинается с большой буквы (Тип)
            if (path != element && path.text.firstOrNull()?.isUpperCase() == true) {
                val superDecl = findDecl(file, path.text)
                if (superDecl != null) superTypes.add(superDecl)
            }
        }

        if (superTypes.isNotEmpty()) {
            val upBuilder = NavigationGutterIconBuilder.create(AllIcons.Gutter.OverridingMethod)
                .setTargets(superTypes)
                .setTooltipText("Наследует: " + superTypes.joinToString { it.text })
            result.add(upBuilder.createLineMarkerInfo(element))
        }

        // 2. ИЩЕМ НАСЛЕДНИКОВ:
        val allStatements = PsiTreeUtil.findChildrenOfType(file, RazomyStatement::class.java)
        val subTypes = mutableListOf<PsiElement>()

        for (stmt in allStatements) {
            val stmtPaths = stmt.children.filterIsInstance<RazomyPath>()
            val declPath = stmtPaths.firstOrNull()

            if (declPath != null && declPath != element && RazomyGotoDeclarationHandler.isDeclaration(declPath)) {
                // Если наш тип упоминается в списке базовых типов этого statement
                if (stmtPaths.any { it != declPath && it.text == currentName }) {
                    subTypes.add(declPath)
                }
            }
        }

        if (subTypes.isNotEmpty()) {
            val downBuilder = NavigationGutterIconBuilder.create(AllIcons.Gutter.OverridenMethod)
                .setTargets(subTypes)
                .setTooltipText("Наследники: ${subTypes.size}")
            result.add(downBuilder.createLineMarkerInfo(element))
        }
    }

    private fun findDecl(file: PsiElement, name: String): PsiElement? {
        val allPaths = PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java)
        return allPaths.firstOrNull { it.text == name && RazomyGotoDeclarationHandler.isDeclaration(it) }
    }
}




class RazomyStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder {
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?) = RazomyStructureViewModel(psiFile)
        }
    }
}

class RazomyStructureViewModel(file: PsiFile) : StructureViewModelBase(file, null, RazomyStructureElement(file)), ElementInfoProvider {
    override fun isAlwaysShowsPlus(element: StructureViewTreeElement) = false
    override fun isAlwaysLeaf(element: StructureViewTreeElement) = element.value is RazomyPath
}

class RazomyStructureElement(private val element: PsiElement) : StructureViewTreeElement, ItemPresentation {
    override fun getValue() = element
    override fun navigate(requestFocus: Boolean) = (element as? com.intellij.pom.Navigatable)?.navigate(requestFocus) ?: Unit
    override fun canNavigate() = (element as? com.intellij.pom.Navigatable)?.canNavigate() ?: false
    override fun canNavigateToSource() = (element as? com.intellij.pom.Navigatable)?.canNavigateToSource() ?: false

    override fun getPresentation() = this
    override fun getPresentableText() = if (element is PsiFile) element.name else element.text
    override fun getIcon(unused: Boolean): Icon = if (element.text.firstOrNull()?.isUpperCase() == true) AllIcons.Nodes.Class else AllIcons.Nodes.Field

    override fun getChildren(): Array<TreeElement> {
        if (element is PsiFile) {
            val allPaths = PsiTreeUtil.findChildrenOfType(element, RazomyPath::class.java)
            // Показываем в структуре только объявления!
            return allPaths.filter { RazomyGotoDeclarationHandler.isDeclaration(it) }
                .map { RazomyStructureElement(it) }
                .toTypedArray()
        }
        return emptyArray()
    }
}



class RazomyFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val block = RazomyBlock(formattingContext.node, Wrap.createWrap(WrapType.NONE, false), Alignment.createAlignment())
        return FormattingModelProvider.createFormattingModelForPsiFile(formattingContext.containingFile, block, formattingContext.codeStyleSettings)
    }
}

class RazomyBlock(private val myNode: ASTNode, private val myWrap: Wrap?, private val myAlignment: Alignment?) : ASTBlock {
    override fun getNode() = myNode
    override fun getTextRange() = myNode.textRange
    override fun getWrap() = myWrap
    override fun getAlignment() = myAlignment

    override fun getSubBlocks(): List<Block> {
        val blocks = mutableListOf<Block>()
        var child = myNode.firstChildNode
        while (child != null) {
            if (child.elementType != TokenType.WHITE_SPACE && child.textRange.length > 0) {
                blocks.add(RazomyBlock(child, Wrap.createWrap(WrapType.NONE, false), null))
            }
            child = child.treeNext
        }
        return blocks
    }

    override fun getIndent(): Indent? {
        val parentType = myNode.treeParent?.elementType
        val groups = listOf(RazomyTypes.BRACE_GROUP, RazomyTypes.PAREN_GROUP, RazomyTypes.ANGLE_GROUP, RazomyTypes.BRACKET_GROUP)

        // Даем отступ всему, что находится внутри фигурных/круглых скобок
        if (parentType in groups && myNode.elementType != RazomyTypes.LBRACE && myNode.elementType != RazomyTypes.RBRACE) {
            return Indent.getNormalIndent()
        }
        return Indent.getNoneIndent()
    }

    override fun getSpacing(child1: Block?, child2: Block) = null
    override fun getChildAttributes(newChildIndex: Int) = ChildAttributes(Indent.getNormalIndent(), null)
    override fun isIncomplete() = false
    override fun isLeaf() = myNode.firstChildNode == null
}



class RazomyUnusedInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is RazomyPath && RazomyGotoDeclarationHandler.isDeclaration(element)) {
                    val name = element.text
                    val file = element.containingFile

                    // Ищем все Path в файле
                    val allPaths = PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java)

                    // Проверяем, есть ли хоть один Path с таким же именем, который НЕ является объявлением
                    val isUsed = allPaths.any {
                        it != element && it.text == name && !RazomyGotoDeclarationHandler.isDeclaration(it)
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
class RazomyLanguage : Language("RL") {
    companion object {
        val INSTANCE = RazomyLanguage()
    }
}

object RazomyIcons {
    val FILE: Icon = IconLoader.getIcon("icons/rl.svg", RazomyIcons::class.java)
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

class RazomySyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = FlexAdapter(RazomyLexer(null))

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            RazomyTypes.IDENTIFIER -> arrayOf(DefaultLanguageHighlighterColors.CLASS_NAME)
            RazomyTypes.STRING -> arrayOf(DefaultLanguageHighlighterColors.STRING)
            RazomyTypes.NUMBER -> arrayOf(DefaultLanguageHighlighterColors.NUMBER)
            RazomyTypes.LINE_COMMENT -> arrayOf(DefaultLanguageHighlighterColors.LINE_COMMENT)
            RazomyTypes.OP -> arrayOf(DefaultLanguageHighlighterColors.OPERATION_SIGN)
            else -> emptyArray()
        }
    }
}


class RazomyTokenType(@NotNull @NonNls debugName: String) : IElementType(debugName, RazomyLanguage.INSTANCE) {

    override fun toString(): String {
        return "RnTokenType." + super.toString()
    }
}

class RazomyElementType(@NotNull @NonNls debugName: String) : IElementType(debugName, RazomyLanguage.INSTANCE)


class RazomySyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        return RazomySyntaxHighlighter()
    }
}


class RazomyBraceMatcher : PairedBraceMatcher {
    private val PAIRS = arrayOf(
        BracePair(RazomyTypes.LBRACE, RazomyTypes.RBRACE, true),
        BracePair(RazomyTypes.LPAREN, RazomyTypes.RPAREN, false),
        BracePair(RazomyTypes.LBRACKET, RazomyTypes.RBRACKET, false),
        BracePair(RazomyTypes.LANGLE, RazomyTypes.RANGLE, false)
    )

    override fun getPairs(): Array<BracePair> = PAIRS
    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true
    override fun getCodeConstructStart(file: PsiFile?, openingOffset: Int): Int = openingOffset
}


class RazomyFoldingBuilder : FoldingBuilderEx(), DumbAware {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()

        // Находим все фигурные скобки { ... } в файле
        val braceGroups = PsiTreeUtil.findChildrenOfType(root, RazomyBraceGroup::class.java)
        for (group in braceGroups) {
            // Сворачиваем, только если блок занимает больше одной строки
            if (group.textRange.length > 2) {
                descriptors.add(FoldingDescriptor(group.node, group.textRange))
            }
        }

        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String = "{...}"
    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}


class RazomyCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "//"
    override fun getBlockCommentPrefix(): String? = null
    override fun getBlockCommentSuffix(): String? = null
    override fun getCommentedBlockCommentPrefix(): String? = null
    override fun getCommentedBlockCommentSuffix(): String? = null
}

class RazomyAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // Мы анализируем каждый узел пути (Path)
        if (element !is RazomyPath) return

        val text = element.text
        if (text.isEmpty()) return

        // 1. Ключевые слова языка
        if (text == "match" || text == "instanceof") {
            highlight(element, holder, DefaultLanguageHighlighterColors.KEYWORD)
            return
        }

        // 2. Слоты/Шаблоны: если узел находится внутри < ... > (например <T>, <Query>)
        val insideAngleGroup = PsiTreeUtil.getParentOfType(element, RazomyAngleGroup::class.java) != null
        if (insideAngleGroup) {
            highlight(element, holder, DefaultLanguageHighlighterColors.METADATA) // Фиолетовый/Оранжевый
            return
        }

        // 3. Вызов метода/команды: если сразу после идет скобка ( ... ) (например Process.Log(...))
        val nextNonSpace = PsiTreeUtil.skipWhitespacesForward(element)
        if (nextNonSpace is RazomyParenGroup) {
            highlight(element, holder, DefaultLanguageHighlighterColors.FUNCTION_CALL)
            return
        }

        // 4. Поля против Типов (по первой букве)
        if (text.first().isLowerCase()) {
            // text, name -> цвет поля структуры (обычно голубой)
            highlight(element, holder, DefaultLanguageHighlighterColors.INSTANCE_FIELD)
        } else if (text.first().isUpperCase()) {
            // DefaultStr, AiContext -> цвет класса/типа
            highlight(element, holder, DefaultLanguageHighlighterColors.CLASS_NAME)
        }
    }

    private fun highlight(element: PsiElement, holder: AnnotationHolder, colorKey: TextAttributesKey) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element.textRange)
            .textAttributes(colorKey)
            .create()
    }
}


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
                    val currentFile = parameters.originalFile

                    // 1. Предлагаем ключевые слова языка
                    result.addElement(
                        LookupElementBuilder.create("match")
                            .bold()
                            .withTypeText("keyword")
                    )
                    result.addElement(
                        LookupElementBuilder.create("instanceof")
                            .bold()
                            .withTypeText("operator")
                    )
                    result.addElement(
                        LookupElementBuilder.create("Default")
                            .bold()
                            .withTypeText("builtin")
                    )

                    // 2. Умный сбор всех типов/путей, уже объявленных в текущем файле!
                    val existingPaths = PsiTreeUtil.findChildrenOfType(currentFile, RazomyPath::class.java)
                    val uniqueWords = existingPaths.map { it.text }.toSet()

                    val currentWord = parameters.position.text.replace("IntellijIdeaRulezzz", "")

                    for (word in uniqueWords) {
                        // Не предлагаем то же самое слово, которое сейчас пишем, и ключевые слова
                        if (word != currentWord && word != "match" && word != "instanceof") {

                            val isType = word.first().isUpperCase()
                            val icon = if (isType) AllIcons.Nodes.Class else AllIcons.Nodes.Field
                            val typeLabel = if (isType) "Type" else "Field"

                            result.addElement(
                                LookupElementBuilder.create(word)
                                    .withIcon(icon)
                                    .withTypeText(typeLabel)
                            )
                        }
                    }
                }
            }
        )
    }
}


object RazomyUtil {

    /**
     * Проверяет, является ли идентификатор объявлением:
     * - Он первый осмысленный элемент на своей строке
     * - И это не ключевое слово
     */
    fun isDeclaration(element: PsiElement): Boolean {
        val text = element.text
        if (text == "match" || text == "instanceof") return false

        val file = element.containingFile ?: return false
        val doc = PsiDocumentManager.getInstance(file.project).getDocument(file) ?: return false
        val offset = element.textRange.startOffset
        val lineNumber = doc.getLineNumber(offset)
        val lineStartOffset = doc.getLineStartOffset(lineNumber)

        // Текст строки от начала до нашего элемента
        val prefix = doc.charsSequence.subSequence(lineStartOffset, offset).toString().trim()

        // Если перед словом на строке ничего нет (только отступы) — это объявление!
        return prefix.isEmpty()
    }

    /**
     * Находит объявление идентификатора по имени во всем файле.
     */
    fun findDeclaration(file: PsiFile, name: String): PsiElement? {
        val allIdentifiers = PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)

        for (id in allIdentifiers) {
            if (id.node.elementType == RazomyTypes.IDENTIFIER && id.text == name) {
                if (isDeclaration(id)) {
                    return id
                }
            }
        }
        return null
    }
}

class RazomyReference(element: PsiElement, textRange: TextRange) :
    PsiReferenceBase<PsiElement>(element, textRange), PsiPolyVariantReference {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val file = myElement.containingFile ?: return emptyArray()
        val name = myElement.text

        if (name == "match" || name == "instanceof") return emptyArray()

        val target = RazomyUtil.findDeclaration(file, name)
        return if (target != null) {
            arrayOf(PsiElementResolveResult(target))
        } else {
            emptyArray()
        }
    }

    override fun resolve(): PsiElement? {
        val results = multiResolve(false)
        return if (results.isNotEmpty()) results[0].element else null
    }

    override fun getVariants(): Array<Any> = emptyArray()
}

class RazomyReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // Слушаем именно клик по листовому токену IDENTIFIER
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(RazomyTypes.IDENTIFIER),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext
                ): Array<PsiReference> {
                    return arrayOf(RazomyReference(element, TextRange(0, element.textLength)))
                }
            }
        )
    }
}