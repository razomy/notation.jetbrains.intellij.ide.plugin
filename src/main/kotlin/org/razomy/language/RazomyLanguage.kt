package org.razomy.language

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
import com.intellij.util.ProcessingContext
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull
import org.razomy.language.psi.*
import javax.swing.Icon


import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.psi.PsiElement
import org.razomy.language.psi.RazomyPath

class RazomyGotoDeclarationHandler : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor?
    ): Array<PsiElement>? {
        if (sourceElement == null) return null

        val word = sourceElement.text
        if (word.isBlank() || word == "match" || word == "instanceof" || word == "const") return null

        val currentPath = PsiTreeUtil.getParentOfType(sourceElement, RazomyPath::class.java) ?: return null
        val file = sourceElement.containingFile ?: return null

        // 1. ЕСЛИ КЛИКНУЛИ НА САМО ОБЪЯВЛЕНИЕ:
        // Возвращаем и наследников, и места обычного использования
        if (RazomyUtil.isDeclaration(currentPath)) {
            val targets = findSubTypesAndUsages(currentPath, file)
            return if (targets.isNotEmpty()) targets.toTypedArray() else null
        }

        // 2. ЕСЛИ ЭТО ИСПОЛЬЗОВАНИЕ (ПЕРЕХОД К ОБЪЯВЛЕНИЮ):
        val target = resolveDeclaration(currentPath, word, file)
            ?: resolveDeclaration(currentPath, currentPath.text, file)
            ?: return null

        return arrayOf(target)
    }

    /**
     * Находит:
     * 1) Объявления-наследники (типы/классы, наследующие данный)
     * 2) Места использования (вызовы, передачи в функции, ссылки в выражениях)
     */
    private fun findSubTypesAndUsages(element: RazomyPath, file: PsiFile): List<PsiElement> {
        val currentName = element.text
        val results = LinkedHashSet<PsiElement>()

        // 1. Ищем наследников (другие объявления, упоминающие текущий тип)
        val allStatements = PsiTreeUtil.findChildrenOfType(file, RazomyStatement::class.java)
        for (stmt in allStatements) {
            val otherStmtPaths = stmt.children.filterIsInstance<RazomyPath>()
            val otherDecl = otherStmtPaths.firstOrNull { RazomyUtil.isDeclaration(it) }

            if (otherDecl != null && otherDecl != element) {
                val inheritsCurrent = otherStmtPaths.any { it != otherDecl && matchesName(it.text, currentName) }
                if (inheritsCurrent) {
                    results.add(otherDecl)
                }
            }
        }

        // 2. Ищем все остальные места использования (вызовы, поля, аргументы и т.д.)
        val allPaths = PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java)
        for (path in allPaths) {
            if (path != element && matchesName(path.text, currentName) && !RazomyUtil.isDeclaration(path)) {
                // Исключаем дубли: если этот path находится в заголовке наследника,
                // которого мы уже добавили в пункт 1, пропускаем
                val parentStmt = PsiTreeUtil.getParentOfType(path, RazomyStatement::class.java)
                val isAlreadyInSubTypes = results.any { it.parent == parentStmt }

                if (!isAlreadyInSubTypes) {
                    results.add(path)
                }
            }
        }

        return results.toList()
    }

    private fun resolveDeclaration(from: RazomyPath, name: String, file: PsiFile): RazomyPath? {
        var scope = PsiTreeUtil.getParentOfType(from, RazomyBraceGroup::class.java)
        while (scope != null) {
            val localPaths = PsiTreeUtil.findChildrenOfType(scope, RazomyPath::class.java)
            for (path in localPaths) {
                if (path != from && matchesName(path.text, name) && RazomyUtil.isDeclaration(path)) {
                    return path
                }
            }
            scope = PsiTreeUtil.getParentOfType(scope, RazomyBraceGroup::class.java)
        }

        val allPaths = PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java)
        for (path in allPaths) {
            if (path != from && matchesName(path.text, name) && RazomyUtil.isDeclaration(path)) {
                return path
            }
        }

        return null
    }

    private fun matchesName(declarationText: String, searchName: String): Boolean {
        return declarationText == searchName ||
                declarationText.endsWith(".$searchName") ||
                searchName.endsWith(".$declarationText")
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

class RazomyTokenType(@NotNull @NonNls debugName: String) : IElementType(debugName, RazomyLanguage.INSTANCE)
class RazomyElementType(@NotNull @NonNls debugName: String) : IElementType(debugName, RazomyLanguage.INSTANCE)

// --- DUPLICATE DECLARATION INSPECTION ---

class RazomyDuplicateDeclarationInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is RazomyPath && RazomyUtil.isDeclaration(element)) {
                    val name = element.text
                    val file = element.containingFile
                    val myScope = PsiTreeUtil.getParentOfType(element, RazomyBraceGroup::class.java)

                    val duplicate = PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java).firstOrNull { other ->
                        if (other != element && other.text == name && RazomyUtil.isDeclaration(other)) {
                            val otherScope = PsiTreeUtil.getParentOfType(other, RazomyBraceGroup::class.java)
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
// --- HIGHLIGHTER ---

class RazomySyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = FlexAdapter(RazomyLexer(null))

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            RazomyTypes.MATCH_KW,
            RazomyTypes.INSTANCEOF_KW,
            RazomyTypes.CONST_KW -> arrayOf(DefaultLanguageHighlighterColors.KEYWORD)
            RazomyTypes.IDENTIFIER -> arrayOf(DefaultLanguageHighlighterColors.IDENTIFIER)
            RazomyTypes.STRING -> arrayOf(DefaultLanguageHighlighterColors.STRING)
            RazomyTypes.NUMBER -> arrayOf(DefaultLanguageHighlighterColors.NUMBER)
            RazomyTypes.LINE_COMMENT -> arrayOf(DefaultLanguageHighlighterColors.LINE_COMMENT)
            RazomyTypes.OP -> arrayOf(DefaultLanguageHighlighterColors.OPERATION_SIGN)
            else -> emptyArray()
        }
    }
}

class RazomySyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?) = RazomySyntaxHighlighter()
}

// --- УТИЛИТА: ОПРЕДЕЛЕНИЕ ОБЪЯВЛЕНИЙ (ТВОЙ ОРИГИНАЛЬНЫЙ АЛГОРИТМ) ---


object RazomyUtil {

    fun isDeclaration(path: RazomyPath): Boolean {
        val name = path.text
        if (name == "match" || name == "instanceof" || name == "const") return false

        val isFirst = isFirstOnLine(path)
        val hasConst = isPrecededByConst(path)

        if (!isFirst && !hasConst) return false

        val braceParents = PsiTreeUtil.collectParents(path, RazomyBraceGroup::class.java, false) { false }

        return if (braceParents.isEmpty()) {
            true // В корне файла
        } else {
            if (hasConst) {
                true // Локальный const внутри { ... }
            } else {
                val nextSibling = PsiTreeUtil.skipWhitespacesForward(path)
                nextSibling is RazomyGroup && nextSibling.braceGroup != null
            }
        }
    }

    private fun isPrecededByConst(element: PsiElement): Boolean {
        var prev = PsiTreeUtil.prevLeaf(element)
        while (prev != null) {
            if (prev.node.elementType == RazomyTypes.CRLF2) return false
            if (prev.node.elementType == RazomyTypes.CONST_KW) {
                return isFirstOnLine(prev)
            }
            if (prev !is PsiWhiteSpace && prev.node.elementType != RazomyTypes.LINE_COMMENT) {
                return false
            }
            prev = PsiTreeUtil.prevLeaf(prev)
        }
        return false
    }

    private fun isFirstOnLine(element: PsiElement): Boolean {
        var prev = PsiTreeUtil.prevLeaf(element)
        while (prev != null) {
            if (prev.node.elementType == RazomyTypes.CRLF2) return true
            if (prev !is PsiWhiteSpace && prev.node.elementType != RazomyTypes.LINE_COMMENT) {
                return false
            }
            prev = PsiTreeUtil.prevLeaf(prev)
        }
        return true
    }
}
// --- REFERENCES & NAVIGATION ---

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
                    if (RazomyUtil.isDeclaration(path)) return emptyArray()
                    return arrayOf(RazomyReference(path, TextRange(0, element.textLength)))
                }
            }
        )
    }
}

// --- LINE MARKERS (Иерархия) ---

class RazomyLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        // Иконки вешаем ТОЛЬКО на имя объявляемой сущности
        if (element !is RazomyPath || !RazomyUtil.isDeclaration(element)) return

        val file = element.containingFile ?: return
        val currentName = element.text
        val statement = PsiTreeUtil.getParentOfType(element, RazomyStatement::class.java) ?: return

        // 1. ИЩЕМ ПРЕДКОВ (Стрелка вверх: Наследует)
        val stmtPaths = statement.children.filterIsInstance<RazomyPath>()
        val superTypes = mutableListOf<PsiElement>()

        for (path in stmtPaths) {
            // Если в строке объявления есть другой тип с большой буквы
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

        // 2. ИЩЕМ НАСЛЕДНИКОВ (Стрелка вниз: Является предком для...)
        val allStatements = PsiTreeUtil.findChildrenOfType(file, RazomyStatement::class.java)
        val subTypes = mutableListOf<PsiElement>()

        for (stmt in allStatements) {
            val otherStmtPaths = stmt.children.filterIsInstance<RazomyPath>()
            val otherDecl = otherStmtPaths.firstOrNull { RazomyUtil.isDeclaration(it) }

            // Если другое объявление использует имя текущего типа
            if (otherDecl != null && otherDecl != element) {
                if (otherStmtPaths.any { it != otherDecl && it.text == currentName }) {
                    subTypes.add(otherDecl)
                }
            }
        }

        if (subTypes.isNotEmpty()) {
            val downBuilder = NavigationGutterIconBuilder.create(AllIcons.Gutter.OverridenMethod)
                .setTargets(subTypes)
                .setTooltipText("Наследники: " + subTypes.joinToString { it.text })
            result.add(downBuilder.createLineMarkerInfo(element))
        }
    }

    private fun findDecl(file: PsiElement, name: String): PsiElement? {
        val allPaths = PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java)
        return allPaths.firstOrNull { it.text == name && RazomyUtil.isDeclaration(it) }
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

        // Находим само объявление (если курсор стоит на использовании)
        val decl = if (RazomyUtil.isDeclaration(target)) {
            target
        } else {
            val all = PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java)
            all.firstOrNull { it.text == target.text && RazomyUtil.isDeclaration(it) }
        } ?: return null

        val statement = PsiTreeUtil.getParentOfType(decl, RazomyStatement::class.java) ?: decl
        val comments = collectPrecedingComments(statement)

        val doc = StringBuilder()
        doc.append("<div style='font-family: monospace; padding: 4px;'>")
        doc.append("<b style='color: #4A86E8;'>Type / Symbol:</b> <code>${escapeHtml(decl.text)}</code><br/>")
        doc.append("<b style='color: #4A86E8;'>Declaration:</b> <code>${escapeHtml(statement.text)}</code>")
        doc.append("</div>")

        if (comments.isNotBlank()) {
            doc.append("<hr/><div style='color: #666; font-style: italic;'>")
            doc.append(comments.replace("\n", "<br/>"))
            doc.append("</div>")
        }

        return doc.toString()
    }

    /**
     * Собирает все строки комментариев (//), идущие подряд непосредственно перед элементом
     */
    private fun collectPrecedingComments(element: PsiElement): String {
        val commentLines = mutableListOf<String>()
        var prev = PsiTreeUtil.prevLeaf(element)

        // Пропускаем отступы прямо перед началом стейтмента
        while (prev is PsiWhiteSpace) {
            prev = PsiTreeUtil.prevLeaf(prev)
        }

        // Собираем все строки комментариев вверх
        while (prev != null) {
            if (prev.node.elementType == RazomyTypes.LINE_COMMENT) {
                val cleanLine = prev.text.removePrefix("//").trim()
                commentLines.add(0, escapeHtml(cleanLine)) // добавляем в начало списка
            } else if (prev.node.elementType == RazomyTypes.CRLF2 || prev is PsiWhiteSpace) {
                // переходы строк между комментариями допустимы
            } else {
                break // наткнулись на другой код
            }
            prev = PsiTreeUtil.prevLeaf(prev)
        }

        return commentLines.joinToString("\n")
    }

    private fun escapeHtml(text: String) =
        text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
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
    override fun isAlwaysLeaf(element: StructureViewTreeElement) = element.value is RazomyPath
}

class RazomyStructureElement(private val element: PsiElement) : StructureViewTreeElement, ItemPresentation {
    override fun getValue() = element
    override fun navigate(requestFocus: Boolean) =
        (element as? com.intellij.pom.Navigatable)?.navigate(requestFocus) ?: Unit

    override fun canNavigate() = (element as? com.intellij.pom.Navigatable)?.canNavigate() ?: false
    override fun canNavigateToSource() = (element as? com.intellij.pom.Navigatable)?.canNavigateToSource() ?: false

    override fun getPresentation() = this
    override fun getPresentableText() = if (element is PsiFile) element.name else element.text
    override fun getIcon(unused: Boolean): Icon =
        if (element.text.firstOrNull()?.isUpperCase() == true) AllIcons.Nodes.Class else AllIcons.Nodes.Field

    override fun getChildren(): Array<TreeElement> {
        if (element is PsiFile) {
            val allPaths = PsiTreeUtil.findChildrenOfType(element, RazomyPath::class.java)
            return allPaths.filter { RazomyUtil.isDeclaration(it) }
                .map { RazomyStructureElement(it) }
                .toTypedArray()
        }
        return emptyArray()
    }
}

// --- FORMATTER ---

class RazomyFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val block =
            RazomyBlock(formattingContext.node, Wrap.createWrap(WrapType.NONE, false), Alignment.createAlignment())
        return FormattingModelProvider.createFormattingModelForPsiFile(
            formattingContext.containingFile,
            block,
            formattingContext.codeStyleSettings
        )
    }
}

class RazomyBlock(private val myNode: ASTNode, private val myWrap: Wrap?, private val myAlignment: Alignment?) :
    ASTBlock {
    override fun getNode() = myNode
    override fun getTextRange() = myNode.textRange
    override fun getWrap() = myWrap
    override fun getAlignment() = myAlignment

    override fun getSubBlocks(): List<Block> {
        val blocks = mutableListOf<Block>()
        var child = myNode.firstChildNode
        while (child != null) {
            if (child.elementType != TokenType.WHITE_SPACE && child.elementType != RazomyTypes.CRLF2 && child.textRange.length > 0) {
                blocks.add(RazomyBlock(child, Wrap.createWrap(WrapType.NONE, false), null))
            }
            child = child.treeNext
        }
        return blocks
    }

    override fun getIndent(): Indent? {
        val parentType = myNode.treeParent?.elementType
        val groups =
            listOf(RazomyTypes.BRACE_GROUP, RazomyTypes.PAREN_GROUP, RazomyTypes.ANGLE_GROUP, RazomyTypes.BRACKET_GROUP)

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

// --- UNUSED INSPECTION ---

class RazomyUnusedInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is RazomyPath && RazomyUtil.isDeclaration(element)) {
                    val name = element.text
                    val file = element.containingFile

                    val allPaths = PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java)

                    // Считаем использованным, если имя встречается в файле где-либо кроме самого этого объявления
                    val isUsed = allPaths.any { path ->
                        path != element && path.text == name
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
// --- ANNOTATOR ---

class RazomyAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is RazomyPath) return
        val text = element.text
        if (text.isEmpty()) return

        // 1. ОШИБКА ДУБЛИКАТА
        if (RazomyUtil.isDeclaration(element)) {
            val file = element.containingFile
            val myScope = PsiTreeUtil.getParentOfType(element, RazomyBraceGroup::class.java)

            val isDuplicate = PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java).any { other ->
                if (other != element && other.text == text && RazomyUtil.isDeclaration(other)) {
                    val otherScope = PsiTreeUtil.getParentOfType(other, RazomyBraceGroup::class.java)
                    otherScope == myScope && other.textOffset < element.textOffset
                } else false
            }

            if (isDuplicate) {
                holder.newAnnotation(HighlightSeverity.ERROR, "Имя '$text' уже объявлено в этой области видимости")
                    .range(element.textRange)
                    .create()
                return
            }
        }

        // 2. ДЖЕНЕРИКИ / МЕТАДАННЫЕ <...>
        val insideAngleGroup = PsiTreeUtil.getParentOfType(element, RazomyAngleGroup::class.java) != null
        if (insideAngleGroup) {
            highlight(element, holder, DefaultLanguageHighlighterColors.METADATA)
            return
        }

        val isDecl = RazomyUtil.isDeclaration(element)
        val nextNonSpace = PsiTreeUtil.skipWhitespacesForward(element)
        val isFunctionCall = nextNonSpace is RazomyParenGroup
        val isType = text.firstOrNull()?.isUpperCase() == true

        // 3. СТОЙКАЯ ПОДСВЕТКА СИНТАКСИСА
        when {
            // Вызовы функций: func(...) -> Нежно-желтый
            isFunctionCall -> {
                highlight(element, holder, DefaultLanguageHighlighterColors.FUNCTION_CALL)
            }

            // Константы: const foo -> Фиолетовый
            isPrecededByConst(element) -> {
                highlight(element, holder, DefaultLanguageHighlighterColors.CONSTANT)
            }

            // ОБЪЯВЛЕНИЕ типа с большой буквы (TextMessage, Message { ...)
            // -> ЗОЛОТОЙ / ЖЕЛТЫЙ (как заголовок сущности)
            isDecl && isType -> {
                highlight(element, holder, DefaultLanguageHighlighterColors.FUNCTION_DECLARATION)
            }

            // ИСПОЛЬЗОВАНИЕ типа с большой буквы (Message, Text, DefaultStr)
            // -> БИРЮЗОВЫЙ / ЦВЕТ АННОТАЦИЙ
            isType -> {
                highlight(element, holder, DefaultLanguageHighlighterColors.METADATA)
            }

            // Поля (str, text, foo.bar)
            // -> ПУРПУРНЫЙ / ФИОЛЕТОВЫЙ
            isField(element) || (isDecl && !isType) -> {
                highlight(element, holder, DefaultLanguageHighlighterColors.INSTANCE_FIELD)
            }

            // Обычные локальные переменные
            else -> {
                highlight(element, holder, DefaultLanguageHighlighterColors.IDENTIFIER)
            }
        }
    }

    /**
     * TEXT_ATTRIBUTES не сбрасывается фоновыми демонами IDE!
     */
    private fun highlight(element: PsiElement, holder: AnnotationHolder, colorKey: TextAttributesKey) {
//        println(">>> КРАСИМ: '${element.text}' в цвет [${colorKey.externalName}]")
        holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
            .range(element.textRange)
            .textAttributes(colorKey)
            .create()
    }

    private fun isField(element: PsiElement): Boolean {
        val prevLeaf = PsiTreeUtil.skipWhitespacesBackward(element)
        if (prevLeaf?.text == ".") return true

        val insideBraces = PsiTreeUtil.getParentOfType(element, RazomyBraceGroup::class.java) != null
        if (insideBraces) {
            val nextSibling = PsiTreeUtil.skipWhitespacesForward(element)
            if (nextSibling is RazomyPath && nextSibling.text.firstOrNull()?.isUpperCase() == true) {
                return true
            }
        }
        return false
    }

    private fun isPrecededByConst(element: PsiElement): Boolean {
        var prev = PsiTreeUtil.prevLeaf(element)
        while (prev != null) {
            if (prev.node.elementType == RazomyTypes.CRLF2) return false
            if (prev.node.elementType == RazomyTypes.CONST_KW) return true
            if (prev !is PsiWhiteSpace && prev.node.elementType != RazomyTypes.LINE_COMMENT) return false
            prev = PsiTreeUtil.prevLeaf(prev)
        }
        return false
    }
}// --- COMPLETION ---

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
                    val existingPaths = PsiTreeUtil.findChildrenOfType(currentFile, RazomyPath::class.java)
                    val uniqueWords = existingPaths.map { it.text }.toSet()
                    val currentWord = parameters.position.text.replace("IntellijIdeaRulezzz", "")

                    for (word in uniqueWords) {
                        if (word != currentWord) {
                            val isType = word.firstOrNull()?.isUpperCase() == true
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

// --- BRACE MATCHER, FOLDING, COMMENTER ---

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
        val braceGroups = PsiTreeUtil.findChildrenOfType(root, RazomyBraceGroup::class.java)
        for (group in braceGroups) {
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

class RazomyReference(element: RazomyPath, textRange: TextRange) : PsiReferenceBase<RazomyPath>(element, textRange),
    PsiPolyVariantReference {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val file = myElement.containingFile ?: return emptyArray()
        val name = myElement.text

        // Ищем по всему файлу объявления с таким же именем
        val allPaths = PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java)
        val results = mutableListOf<ResolveResult>()

        for (path in allPaths) {
            // Имя совпадает, это не мы сами, и это точно объявление
            if (path != myElement && path.text == name && RazomyUtil.isDeclaration(path)) {
                results.add(PsiElementResolveResult(path))
            }
        }

        return results.toTypedArray()
    }

    override fun resolve(): PsiElement? {
        val results = multiResolve(false)
        return if (results.isNotEmpty()) results[0].element else null
    }

    override fun getVariants(): Array<Any> = emptyArray()
}