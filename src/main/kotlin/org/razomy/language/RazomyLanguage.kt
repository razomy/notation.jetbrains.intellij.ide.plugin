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
import com.intellij.util.ProcessingContext
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull
import org.razomy.language.psi.*
import javax.swing.Icon
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler

import com.intellij.psi.PsiFile
import org.razomy.language.psi.*
// --- LANGUAGE & FILE TYPE ---

// --- СИСТЕМА ТИПОВ И СЕМАНТИЧЕСКИЙ РЕЗОЛВЕР (Razomy Type System V2.1) ---
// --- СИСТЕМА ТИПОВ И СЕМАНТИЧЕСКИЙ РЕЗОЛВЕР (Razomy Type System V2.3) ---

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement

// --- СИСТЕМА ТИПОВ И СЕМАНТИЧЕСКИЙ РЕЗОЛВЕР (Razomy Type System V2.5 - Logger Edition) ---

object RazomyTypeSystem {
    private val LOG = Logger.getInstance("RazomyTypeSystem")

    fun inferTypeByName(name: String, context: PsiElement): RazomyType {
        val file = context.containingFile ?: return RazomyType.Unknown
        val globalDecls = PsiTreeUtil.findChildrenOfType(file, RazomyDeclaration::class.java)

        for (decl in globalDecls) {
            val path = PsiTreeUtil.findChildOfType(decl, RazomyPath::class.java)
            if (path?.text == name) {
                // 1. Форма { ... }
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

                // 2. Алиас / Наследование (User & Identifiable)
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
        // Если это путь, сначала проверим: не является ли он объявлением переменной в слоте?
        if (element is RazomyPath) {
            val slotItem = PsiTreeUtil.getParentOfType(element, RazomySlotItem::class.java)
            if (slotItem != null) {
                val paths = PsiTreeUtil.findChildrenOfType(slotItem, RazomyPath::class.java).toList()
                // Вариант 1: Явно указан тип в слоте, например: (Err BuildError)
                if (paths.size > 1 && paths[0] == element) {
                    return inferTypeByName(paths[1].text, element)
                }

                // Вариант 2: Захват в роутере, например: BuildError(NewErr)
                val callExpr = PsiTreeUtil.getParentOfType(slotItem, RazomyCallExpr::class.java)
                if (callExpr != null) {
                    val callerPath = PsiTreeUtil.getChildOfType(callExpr, RazomyPath::class.java)
                    if (callerPath != null && callerPath.text.firstOrNull()?.isUpperCase() == true) {
                        return inferTypeByName(callerPath.text, element)
                    }
                }
            }
        }

        // Если это глобальный тип (начинается с большой буквы и это не переменная из слота)
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
            // ПРАВИЛО 1: Авто-распаковка из Роутера
            if (current is RazomyFormItem) {
                val firstPath = PsiTreeUtil.findChildOfType(current, RazomyPath::class.java)
                if (firstPath != null && firstPath.text.firstOrNull()?.isUpperCase() == true) {
                    val resolvedType = inferTypeByName(firstPath.text, element)
                    scope.unpackedFields.putAll(RazomyType.extractFields(resolvedType))
                }
            }

            // ПРАВИЛО 2: Видимость "соседей" внутри одной формы
            if (current is RazomyFormBlock) {
                val items = PsiTreeUtil.findChildrenOfType(current, RazomyFormItem::class.java)
                for (item in items) {
                    val fieldPath = PsiTreeUtil.findChildOfType(item, RazomyPath::class.java)
                    if (fieldPath != null) {
                        scope.variables[fieldPath.text] = fieldPath
                    }
                }
            }

            // ПРАВИЛО 3: Проброс контекста в пайплайне [ ... ]
            if (current is RazomyFlowBlock) {
                val precedingForms = PsiTreeUtil.findChildrenOfType(current, RazomyFormBlock::class.java)
                for (formBlock in precedingForms) {
                    if (formBlock.textOffset < element.textOffset) {
                        val items = PsiTreeUtil.findChildrenOfType(formBlock, RazomyFormItem::class.java)
                        for (item in items) {
                            val fieldPath = PsiTreeUtil.findChildOfType(item, RazomyPath::class.java)
                            if (fieldPath != null) {
                                scope.unpackedFields[fieldPath.text] = fieldPath
                            }
                        }
                    }
                }
            }

            // ПРАВИЛО 4: ЗАХВАТ СЛОТОВ И ТИПОВ
            if (current is RazomyDeclaration || current is RazomyFormItem || current is RazomyCallExpr) {
                val slot = PsiTreeUtil.findChildOfType(current, RazomySlot::class.java)
                if (slot != null && slot.textOffset < element.textOffset) {
                    val items = PsiTreeUtil.findChildrenOfType(slot, RazomySlotItem::class.java)
                    for (item in items) {
                        val paths = PsiTreeUtil.findChildrenOfType(item, RazomyPath::class.java).toList()
                        if (paths.isNotEmpty()) {
                            val varName = paths[0]
                            scope.variables[varName.text] = varName

                            // Вычисляем тип для распаковки полей:
                            val resolvedType = inferTypeOfElement(varName)
                            if (resolvedType !is RazomyType.Unknown) {
                                scope.unpackedFields.putAll(RazomyType.extractFields(resolvedType))
                            } else {
                                // Фолбэк: если это имя само совпадает с типом (TaskPolicy -> TaskPolicy)
                                val fallbackType = inferTypeByName(varName.text, element)
                                scope.unpackedFields.putAll(RazomyType.extractFields(fallbackType))
                            }
                        }
                    }
                }
            }
            current = current.parent
        }

        // ГЛОБАЛЬНЫЙ СКОУП
        val file = element.containingFile
        if (file != null) {
            val globalDecls = PsiTreeUtil.findChildrenOfType(file, RazomyDeclaration::class.java)
            for (decl in globalDecls) {
                val path = PsiTreeUtil.findChildOfType(decl, RazomyPath::class.java)
                if (path != null) scope.variables[path.text] = path
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
        if (current == null) {
            return null
        }

        // Если это путь с точками (e.g. Shell.Log)
        for (i in 1 until parts.size) {
            val nextMember = parts[i]
            val resolvedMember = resolveMember(current!!, nextMember)
            if (resolvedMember == null) {
                LOG.warn("ОШИБКА: Поле '$nextMember' не найдено в '$rootName'")
                return null // Возвращаем null, чтобы Аннотатор понял, что путь сломан
            }
            current = resolvedMember
        }

        return current
    }

    private fun resolveMember(owner: PsiElement, memberName: String): PsiElement? {
        val type = RazomyTypeSystem.inferTypeOfElement(owner)
        val fields = RazomyType.extractFields(type)
        return fields[memberName]
    }
}

sealed class RazomyType {
    class Form(val name: String, val declaration: PsiElement, val fields: Map<String, PsiElement>) : RazomyType()
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
                else -> emptyMap()
            }
        }
    }
}

class RazomyScope(
    val variables: MutableMap<String, PsiElement> = mutableMapOf(),
    val unpackedFields: MutableMap<String, PsiElement> = mutableMapOf()
)


class RazomyLanguage : Language("RL") {
    companion object {
        val INSTANCE = RazomyLanguage()
    }
}

object RazomyIcons {
    val FILE: Icon = IconLoader.getIcon("icons/rl.svg", RazomyIcons::class.java)
}
// --- SEMANTIC RESOLVER (Контекстный анализ и области видимости) ---


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

    fun getQualifiedName(path: RazomyPath): String {
        val parts = mutableListOf(path.text)
        var current: PsiElement? = path

        while (current != null) {
            val parentBlock = PsiTreeUtil.getParentOfType(current, RazomyFormBlock::class.java) ?: break
            val parentItem =
                PsiTreeUtil.getParentOfType(parentBlock, RazomyDeclaration::class.java, RazomyFormItem::class.java)

            // Находим самый первый путь в родителе
            val parentDecl = PsiTreeUtil.findChildOfType(parentItem, RazomyPath::class.java)

            if (parentDecl != null && parentDecl != current) {
                parts.add(0, parentDecl.text)
                current = parentDecl
            } else {
                current = parentBlock
            }
        }
        return parts.joinToString(".")
    }

    fun isDeclaration(path: RazomyPath): Boolean {
        if (path.text == "_" || path.text.startsWith("_.")) return false // Анонимный контекст

        val parentItem = PsiTreeUtil.getParentOfType(
            path,
            RazomyDeclaration::class.java,
            RazomyFormItem::class.java,
            RazomySlotItem::class.java
        ) ?: return false

        // Надежная проверка (без textOffset):
        // Если мы являемся САМЫМ ПЕРВЫМ путем в нашем Statement-контейнере, значит мы - его Декларация.
        val firstPath = PsiTreeUtil.findChildOfType(parentItem, RazomyPath::class.java)
        return firstPath == path
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

        if (RazomyUtil.isDeclaration(currentPath)) {
            val targets = findUsages(currentPath, file)
            return if (targets.isNotEmpty()) targets.toTypedArray() else null
        }

        val target = resolveDeclaration(currentPath, word, file)
            ?: resolveDeclaration(currentPath, currentPath.text, file)
            ?: return null

        return arrayOf(target)
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

    private fun resolveDeclaration(from: RazomyPath, name: String, file: PsiFile): RazomyPath? {
        // 1. Поиск локальных переменных (в слотах, по иерархии вверх)
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

        // 2. Поиск свойств внутри соседних FormBlock
        var formScope = PsiTreeUtil.getParentOfType(from, RazomyFormBlock::class.java)
        while (formScope != null) {
            val formItems = PsiTreeUtil.findChildrenOfType(formScope, RazomyFormItem::class.java)
            for (item in formItems) {
                val firstExpr = item.children.firstOrNull()
                val localPath = PsiTreeUtil.findChildOfType(firstExpr, RazomyPath::class.java)
                if (localPath != null && localPath != from && localPath.textOffset == firstExpr?.textOffset && matchesName(
                        localPath.text,
                        name
                    )
                ) {
                    return localPath
                }
            }
            formScope = PsiTreeUtil.getParentOfType(formScope, RazomyFormBlock::class.java)
        }

        // 3. Глобальный поиск
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


// --- ANNOTATOR ---
class RazomyAnnotator : Annotator {

    companion object {
        private val ESCAPE_PATTERN = Regex("""\\[ntr\"'\\{}]""")
        private val INTERPOLATION_PATTERN = Regex("""(?<!\\)\{([^}]+)\}""")

        // Контрастные атрибуты IntelliJ, которые гарантированно видны в стандартных темах:
        val COLOR_MODULE = DefaultLanguageHighlighterColors.CLASS_NAME              // Модули (Shell)
        val COLOR_TYPE = DefaultLanguageHighlighterColors.CLASS_REFERENCE           // Типы/Структуры (AiContext, BuildSuccess)
        val COLOR_FUNCTION_DEF = DefaultLanguageHighlighterColors.FUNCTION_DECLARATION // Определение функции (RunBuild)
        val COLOR_METHOD_CALL = DefaultLanguageHighlighterColors.STATIC_METHOD      // Вызов функции/метода (Shell.Log, . AttemptFix)
        val COLOR_FIELD = DefaultLanguageHighlighterColors.INSTANCE_FIELD           // Поля структур (CleanLog, Retries, Head)
        val COLOR_PARAM = DefaultLanguageHighlighterColors.PARAMETER                // Параметры функций (ErrorLog, Cmd)
        val COLOR_VARIABLE = DefaultLanguageHighlighterColors.LOCAL_VARIABLE        // Переменные / аргументы
        val COLOR_NATIVE = DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL       // Системные вызовы (native_print)
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // 1. Строки, Escape-последовательности и интерполяция
        if (element.node.elementType == RazomyTypes.STRING || element.textContains('\"') || element.textContains('\'')) {
            if (element.node.elementType == RazomyTypes.STRING) annotateString(element, holder)
            return
        }

        if (element !is RazomyPath) return
        val text = element.text.trim()

        if (text.isEmpty() || text.endsWith(".") || text.contains("IntellijIdeaRulezzz")) return

        // 2. Wildcard и подстановки (_ и _.)
        if (text == "_" || text.startsWith("_.")) {
            highlight(element, holder, DefaultLanguageHighlighterColors.KEYWORD)
            return
        }

        // 3. Нативные вызовы
        if (text.startsWith("native_") || text.contains("native_")) {
            highlight(element, holder, COLOR_NATIVE)
            return
        }

        val isDecl = RazomyUtil.isDeclaration(element)
        val isMemberAccess = text.contains(".")

        // =========================================================================
        // 4. СТРОГАЯ ВАЛИДАЦИЯ И ПРОВЕРКА ОШИБОК
        // =========================================================================
        if (!isDecl) {
            val resolved = RazomyResolver.resolvePath(element, text)
            val scope = RazomyTypeSystem.buildScope(element)
            val isResolvedLocally = !isMemberAccess && (scope.variables.containsKey(text) || scope.unpackedFields.containsKey(text))

            if (resolved == null && !isResolvedLocally) {
                if (isMemberAccess) {
                    val parts = text.split(".")
                    holder.newAnnotation(HighlightSeverity.ERROR, "Неразрешенное поле: '${parts.last()}' в '${parts.dropLast(1).joinToString(".")}'")
                        .range(element.textRange)
                        .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                        .create()
                } else {
                    holder.newAnnotation(HighlightSeverity.ERROR, "Неразрешенная ссылка: '$text'")
                        .range(element.textRange)
                        .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                        .create()
                }
                return // При ошибке прерываем цепочку, чтобы не перетирать красное подчеркивание
            }
        }

        // =========================================================================
        // 5. СЕМАНТИЧЕСКАЯ ОТРИСОВКА
        // =========================================================================

        // А. Обращение через точку (Shell.Log, NewErr.Log) — красим каждую часть раздельно
        if (isMemberAccess) {
            annotateMemberAccess(element, holder)
            return
        }

        // Б. Параметры в слотах: AttemptFix(ErrorLog) или BuildError(NewErr)
        val enclosingSlot = PsiTreeUtil.getParentOfType(element, RazomySlot::class.java)
        if (enclosingSlot != null && isParameterInsideSlot(element, enclosingSlot)) {
            highlight(element, holder, COLOR_PARAM)
            return
        }

        // В. Объявления (Декларации)
        if (isDecl) {
            annotateDeclaration(element, holder)
            return
        }

        // Г. Использования (Ссылки и вызовы)
        annotateUsage(element, holder, text)
    }

    // -------------------------------------------------------------------------
    // Составные пути: Shell.Log, User.Id, NewErr.Log
    // -------------------------------------------------------------------------
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
                // Левая часть: Shell (Модуль) или NewErr (Переменная)
                if (partText.firstOrNull()?.isUpperCase() == true) {
                    highlightRange(node.textRange, holder, COLOR_MODULE)
                } else {
                    highlightRange(node.textRange, holder, COLOR_VARIABLE)
                }
            } else {
                // Правая часть: Log / Exec (Метод) или Size / Id (Поле)
                val isMethod = partText.firstOrNull()?.isUpperCase() == true
                val rightColor = if (isMethod) COLOR_METHOD_CALL else COLOR_FIELD
                highlightRange(node.textRange, holder, rightColor)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Определение сущностей при объявлении
    // -------------------------------------------------------------------------
    private fun annotateDeclaration(element: RazomyPath, holder: AnnotationHolder) {
        val parent = element.parent

        // 1. Прямая связь: Name [ ... ] или Name { ... }
        if (parent is RazomyTypedFlowBlock) {
            highlight(element, holder, COLOR_FUNCTION_DEF)
            return
        }
        if (parent is RazomyTypedFormBlock) {
            if (isModuleForm(parent.formBlock)) {
                highlight(element, holder, COLOR_MODULE)
            } else {
                highlight(element, holder, COLOR_TYPE)
            }
            return
        }

        // 2. Сигнатуры с аргументами: Func(A) [ ... ] или Type(T) { ... }
        val grandParent = parent?.parent
        if (grandParent is RazomyTypedFlowBlock && parent is RazomyCallExpr) {
            highlight(element, holder, COLOR_FUNCTION_DEF)
            return
        }
        if (grandParent is RazomyTypedFormBlock && parent is RazomyCallExpr) {
            highlight(element, holder, COLOR_TYPE)
            return
        }

        // 3. Анализ элементов деклараций через выражения: expr expr?
        val targetItem = PsiTreeUtil.getParentOfType(element, RazomyDeclaration::class.java, RazomyFormItem::class.java)
        if (targetItem != null) {
            val expressions = PsiTreeUtil.getChildrenOfType(targetItem, RazomyExpr::class.java)
            if (expressions != null && expressions.size >= 2) {
                val valueExpr = expressions[1]

                // За именем следует блок потока [ ... ] -> Функция
                if (PsiTreeUtil.findChildOfType(valueExpr, RazomyFlowBlock::class.java) != null) {
                    highlight(element, holder, COLOR_FUNCTION_DEF)
                    return
                }

                // За именем следует форма { ... } -> Структура или Модуль
                val formBlock = PsiTreeUtil.findChildOfType(valueExpr, RazomyFormBlock::class.java)
                if (formBlock != null) {
                    if (isModuleForm(formBlock)) {
                        highlight(element, holder, COLOR_MODULE)
                    } else {
                        highlight(element, holder, COLOR_TYPE)
                    }
                    return
                }

                // Внутри формы левая часть — поле: CleanLog String
                if (targetItem is RazomyFormItem) {
                    highlight(element, holder, COLOR_FIELD)
                    return
                }
            } else if (expressions != null && expressions.size == 1 && targetItem is RazomyFormItem) {
                highlight(element, holder, COLOR_FIELD)
                return
            }
        }

        // По умолчанию
        if (element.text.firstOrNull()?.isUpperCase() == true) {
            highlight(element, holder, COLOR_TYPE)
        } else {
            highlight(element, holder, COLOR_FIELD)
        }
    }

    // -------------------------------------------------------------------------
    // Определение сущностей при использовании
    // -------------------------------------------------------------------------
    private fun annotateUsage(element: RazomyPath, holder: AnnotationHolder, text: String) {
        val parent = element.parent

        // 1. Поле инициализации в форме: TaskPolicy { CleanLog Log, Retries 3 }
        if (parent is RazomyFormItem) {
            val firstExpr = PsiTreeUtil.findChildOfType(parent, RazomyExpr::class.java)
            if (firstExpr != null && PsiTreeUtil.isAncestor(firstExpr, element, false)) {
                highlight(element, holder, COLOR_FIELD)
                return
            }
        }

        // 2. Шаг в пайплайне: ErrorLog . BuildSystemPrompt или . RetryLoop
        if (isInsidePipeline(element)) {
            highlight(element, holder, COLOR_METHOD_CALL)
            return
        }

        // 3. Вызов или инициализация типа
        if (parent is RazomyTypedFormBlock || (parent is RazomyCallExpr && text.firstOrNull()?.isUpperCase() == true)) {
            highlight(element, holder, COLOR_TYPE)
            return
        }

        // 4. По регистру
        if (text.firstOrNull()?.isUpperCase() == true) {
            highlight(element, holder, COLOR_TYPE)
        } else {
            highlight(element, holder, COLOR_VARIABLE)
        }
    }

    // -------------------------------------------------------------------------
    // Вспомогательные методы
    // -------------------------------------------------------------------------

    private fun isModuleForm(formBlock: RazomyFormBlock?): Boolean {
        if (formBlock == null) return false
        return PsiTreeUtil.findChildOfType(formBlock, RazomyFlowBlock::class.java) != null
    }

    private fun isParameterInsideSlot(element: RazomyPath, slot: RazomySlot): Boolean {
        val slotParent = slot.parent
        return slotParent is RazomyCallExpr || slotParent is RazomyTypedFlowBlock || slotParent is RazomyTypedFormBlock
    }

    private fun isInsidePipeline(element: RazomyPath): Boolean {
        val pipelineExpr = PsiTreeUtil.getParentOfType(element, RazomyPipelineExpr::class.java) ?: return false
        return pipelineExpr.text.contains(".") && pipelineExpr.firstChild != element
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
                .textAttributes(DefaultLanguageHighlighterColors.BRACES)
                .create()

            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(offset + match.range.last, offset + match.range.last + 1))
                .textAttributes(DefaultLanguageHighlighterColors.BRACES)
                .create()

            val innerRange = TextRange(offset + match.range.first + 1, offset + match.range.last)
            if (!innerRange.isEmpty) {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(innerRange)
                    .textAttributes(DefaultLanguageHighlighterColors.IDENTIFIER)
                    .create()
            }
        }
    }
}// --- COMPLETION ---

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