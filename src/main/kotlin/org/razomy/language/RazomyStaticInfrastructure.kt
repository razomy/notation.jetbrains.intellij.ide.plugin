package org.razomy.language

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.codeInspection.*
import com.intellij.codeInspection.util.IntentionName
import com.intellij.icons.AllIcons
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel.ElementInfoProvider
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.Commenter
import com.intellij.lang.Language
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.lexer.FlexAdapter
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.LanguageFileType
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

// --- COMMENTER ---

class RazomyCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "//"
    override fun getBlockCommentPrefix(): String = "#"
    override fun getBlockCommentSuffix(): String = "#"
    override fun getCommentedBlockCommentPrefix(): String? = null
    override fun getCommentedBlockCommentSuffix(): String? = null
}

// --- FIND USAGES (ПОИСК ИСПОЛЬЗОВАНИЙ) ---

class RazomyFindUsagesProvider : FindUsagesProvider {

    override fun getWordsScanner(): WordsScanner {
        return DefaultWordsScanner(
            FlexAdapter(RazomyLexer(null)),
            TokenSet.create(RazomyTypes.IDENTIFIER),
            TokenSet.create(RazomyTypes.LINE_COMMENT, RazomyTypes.DOC_COMMENT),
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

    override fun getDescriptiveName(element: PsiElement): String = element.text

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String = element.text
}

// --- INDEXING ---

object RazomyIndexKeys {
    val EXPORTED_SYMBOLS = ID.create<String, Void>("razomy.exports.index")
}

class RazomyExportIndex : ScalarIndexExtension<String>() {

    override fun getName(): ID<String, Void> = RazomyIndexKeys.EXPORTED_SYMBOLS

    override fun getVersion(): Int = 3

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

// --- СИСТЕМА ИМПОРТОВ И ФАЙЛОВОГО РЕЗОЛВА МОДУЛЕЙ ---

object RazomyImportResolver {
    fun resolveImportedFile(context: PsiElement, importPath: String): PsiFile? {
        val project = context.project
        val cleanImport = importPath.trim()
        if (cleanImport.isEmpty()) return null

        val fileName = cleanImport.substringAfterLast('.') + ".rl"
        val expectedPathFragment = cleanImport.replace('.', '/') + ".rl"

        // 1. Проверяем относительный импорт (в текущей папке)
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

    /**
     * Находит все файлы в контексте (сначала в той же папке по алфавиту `a.rl`, `b.rl`, затем во всем проекте),
     * которые экспортируют `symbolName`.
     */
    fun findContextCandidateFiles(context: PsiElement, symbolName: String): List<PsiFile> {
        if (symbolName.isBlank() || symbolName == "_" || !symbolName.first().isUpperCase()) return emptyList()
        val currentFile = context.containingFile?.originalFile ?: return emptyList()
        val currentVf = currentFile.virtualFile
        val currentDir = currentVf?.parent
        val project = context.project
        val psiManager = PsiManager.getInstance(project)

        val resultFiles = LinkedHashSet<PsiFile>()

        // 1. Сначала проверяем все .rl файлы прямо в текущей папке (сортировка по имени: a.rl, b.rl...)
        if (currentDir != null) {
            val siblingVfs = currentDir.children
                .filter { !it.isDirectory && it.name.endsWith(".rl") && it != currentVf }
                .sortedBy { it.name }

            for (vf in siblingVfs) {
                val psiFile = psiManager.findFile(vf) ?: continue
                if (findExportedSymbol(psiFile, symbolName) != null) {
                    resultFiles.add(psiFile)
                }
            }
        }

        // 2. Если в текущей папке нашли — возвращаем их (контекст папки приоритетнее)
        if (resultFiles.isNotEmpty()) {
            return resultFiles.toList()
        }

        // 3. Иначе ищем по индексу во всем проекте (сортировка по имени файла)
        val indexedVfs = FileBasedIndex.getInstance().getContainingFiles(
            RazomyIndexKeys.EXPORTED_SYMBOLS,
            symbolName,
            GlobalSearchScope.projectScope(project)
        ).filter { it != currentVf }
            .sortedWith(compareBy({ it.name }, { it.path }))

        for (vf in indexedVfs) {
            val psiFile = psiManager.findFile(vf) ?: continue
            if (findExportedSymbol(psiFile, symbolName) != null) {
                resultFiles.add(psiFile)
            }
        }

        return resultFiles.toList()
    }

    /**
     * Возвращает ПЕРВЫЙ попавшийся символ из контекста (например, из `a.rl`)
     */
    fun findFirstContextSymbol(context: PsiElement, symbolName: String): PsiElement? {
        val firstFile = findContextCandidateFiles(context, symbolName).firstOrNull() ?: return null
        return findExportedSymbol(firstFile, symbolName)
    }

    fun getRelativeModulePath(contextFile: PsiFile, targetFile: PsiFile): String {
        val currentDir = contextFile.originalFile.virtualFile?.parent
        val targetVf = targetFile.originalFile.virtualFile
        if (currentDir != null && targetVf != null) {
            if (targetVf.parent == currentDir) {
                return targetVf.name.removeSuffix(".rl")
            }
            val rel = VfsUtilCore.getRelativePath(targetVf, currentDir)
            if (rel != null) {
                return rel.removeSuffix(".rl").replace('\\', '.').replace('/', '.')
            }
        }
        return targetFile.name.removeSuffix(".rl")
    }

    fun findModuleByExportedSymbol(project: Project, symbolName: String): List<String> {
        val result = mutableListOf<String>()

        val virtualFiles = FileBasedIndex.getInstance().getContainingFiles(
            RazomyIndexKeys.EXPORTED_SYMBOLS,
            symbolName,
            GlobalSearchScope.projectScope(project)
        ).sortedBy { it.name }

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

    /**
     * Проверяет, импортирован ли символ явно через `{ A } b` или переопределен через `_ b`
     */
    fun resolveExplicitOrWildcardImport(context: PsiElement, symbolRoot: String): PsiElement? {
        val file = context.containingFile ?: return null
        val declarations = file.children.filterIsInstance<RazomyDeclaration>()

        // Ищем с конца или начала (в зависимости от приоритета, берем последнее переопределение `_ b` перед использованием)
        var overriddenTarget: PsiElement? = null

        for (decl in declarations) {
            val leftExpr = decl.children.firstOrNull() ?: continue
            val rightExpr = decl.children.getOrNull(1) ?: continue

            val rightPath = PsiTreeUtil.findChildOfType(rightExpr, RazomyPath::class.java)
                ?.takeIf { it.text == rightExpr.text.trim() } ?: continue

            // 1. Случай `_ b` (Wildcard / Override import)
            if (leftExpr.text.trim() == "_") {
                val importedFile = resolveImportedFile(file, rightPath.text)
                if (importedFile != null && importedFile != file) {
                    val found = findExportedSymbol(importedFile, symbolRoot)
                    if (found != null) {
                        overriddenTarget = found
                    }
                }
            }

            // 2. Случай `{ A } b` (Деструктурирующий импорт)
            val slot = PsiTreeUtil.findChildOfType(leftExpr, RazomySlot::class.java)
            if (slot != null) {
                val pathsInSlot = PsiTreeUtil.findChildrenOfType(slot, RazomyPath::class.java)
                if (pathsInSlot.any { it.text == symbolRoot }) {
                    val importedFile = resolveImportedFile(file, rightPath.text)
                    if (importedFile != null && importedFile != file) {
                        val found = findExportedSymbol(importedFile, symbolRoot)
                        if (found != null) return found
                    }
                }
            }
        }

        return overriddenTarget
    }

    /**
     * Проверяет, есть ли в текущем файле явный импорт `{ A } b` или `_ b` для данного символа
     */
    fun isExplicitlyResolvedInFile(file: PsiFile, symbolName: String): Boolean {
        return resolveExplicitOrWildcardImport(file, symbolName) != null
    }

    /**
     * Находит все внешние файлы в контексте (сначала в той же папке, затем в проекте),
     * которые экспортируют `symbolName`, отсортированные по алфавиту (`a.rl`, `b.rl` и т.д.).
     */
    fun findCandidateDeclarationsInContext(context: PsiElement, symbolName: String): List<PsiElement> {
        if (symbolName.isBlank() || !symbolName.first().isUpperCase()) return emptyList()
        val currentFile = context.containingFile?.originalFile ?: return emptyList()
        val currentVf = currentFile.virtualFile
        val currentDir = currentVf?.parent
        val project = context.project

        val virtualFiles = FileBasedIndex.getInstance().getContainingFiles(
            RazomyIndexKeys.EXPORTED_SYMBOLS,
            symbolName,
            GlobalSearchScope.projectScope(project)
        ).filter { it != currentVf }

        if (virtualFiles.isEmpty()) return emptyList()

        // Сортируем: сначала файлы из той же папки по алфавиту (a.rl, b.rl), затем остальные по пути
        val sortedVfs = virtualFiles.sortedWith(
            compareBy<VirtualFile> { vf -> if (currentDir != null && vf.parent == currentDir) 0 else 1 }
                .thenBy { it.name }
                .thenBy { it.path }
        )

        val psiManager = PsiManager.getInstance(project)
        val targets = mutableListOf<PsiElement>()
        for (vf in sortedVfs) {
            val psiFile = psiManager.findFile(vf) ?: continue
            val symbol = findExportedSymbol(psiFile, symbolName)
            if (symbol != null) {
                targets.add(symbol)
            }
        }
        return targets
    }

    /**
     * Универсальный резолв:
     * 1. Стандартный локальный/обычный резолв через RazomyResolver.
     * 2. Проверка переопределения `_ b` или `{ A } b`.
     * 3. Если не найдено — берем ПЕРВЫЙ попавшийся из контекста (например, из `a.rl`).
     */
    fun resolveWithContextFallback(context: PsiElement, resolveText: String): PsiElement? {
        val parts = resolveText.split('.')
        val rootName = parts.firstOrNull() ?: return null

        // 1. Проверяем, не переопределен ли символ явно через `_ b`
        val explicitTarget = resolveExplicitOrWildcardImport(context, rootName)
        if (explicitTarget != null) {
            if (parts.size == 1) return explicitTarget
            // Если путь составной (например `A.Field`), резолвим через стандартный механизм или возвращаем корень
            return RazomyResolver.resolvePath(context, resolveText) ?: explicitTarget
        }

        // 2. Пробуем стандартный резолвер
        val standardResolved = RazomyResolver.resolvePath(context, resolveText)
        if (standardResolved != null) return standardResolved

        // 3. Автоматический резолв первого попавшегося из контекста (a.rl -> A)
        val candidates = findCandidateDeclarationsInContext(context, rootName)
        val firstCandidate = candidates.firstOrNull() ?: return null

        if (parts.size == 1) {
            return firstCandidate
        } else {
            // Поддержка вложенных путей `A.SubField` для неявно импортированного `A`
            var currentElem: PsiElement? = firstCandidate
            for (i in 1 until parts.size) {
                val part = parts[i]
                val type = if (currentElem is PsiFile) {
                    RazomyType.Module(currentElem)
                } else if (currentElem != null) {
                    RazomyTypeSystem.inferTypeOfElement(currentElem)
                } else break

                val fieldType = RazomyType.extractFields(type)[part]
                if (fieldType is RazomyType.Form) {
                    currentElem = fieldType.declaration
                } else {
                    val declContainer = PsiTreeUtil.getParentOfType(
                        currentElem,
                        RazomyDeclaration::class.java,
                        RazomyFormItem::class.java
                    )
                    val subForm = PsiTreeUtil.findChildrenOfType(declContainer, RazomyPath::class.java)
                        .firstOrNull { it.text == part && RazomyUtil.isDeclaration(it) }
                    currentElem = subForm
                }
            }
            return currentElem
        }
    }

}

// --- AUTO-IMPORT & OVERRIDE QUICK FIX ---

class RazomyImportQuickFix(
    private val symbolName: String,
    private val modulePath: String,
    private val useWildcardOverride: Boolean = false
) : LocalQuickFix, IntentionAction {
    override fun getFamilyName(): String = "Импортировать"
    override fun getName(): String = if (useWildcardOverride) {
        "Переопределить через '_ $modulePath' (для '$symbolName')"
    } else {
        "Импортировать '{ $symbolName } $modulePath'"
    }

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
        val dummyContent = if (useWildcardOverride) {
            "_ $modulePath\n"
        } else {
            "{ $symbolName } $modulePath\n"
        }
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

        // 2. Ссылки внутри строковых интерполяций "{Name}", "{Err.Log}", "{Retries - 1}"
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
                    for (match in RazomyUtil.INTERPOLATION_PATTERN.findAll(text)) {
                        val group = match.groups[1] ?: continue
                        val rawExpr = group.value
                        val groupStart = group.range.first

                        for ((token, tokenRange) in RazomyUtil.extractInterpolatedPaths(rawExpr)) {
                            val baseOffset = groupStart + tokenRange.first
                            val parts = token.split('.')

                            if (parts.size == 1) {
                                refs.add(
                                    RazomyStringInterpolationReference(
                                        element,
                                        TextRange(baseOffset, baseOffset + token.length),
                                        token
                                    )
                                )
                            } else {
                                var partOffset = 0
                                for (part in parts) {
                                    val endOffset = partOffset + part.length
                                    if (part.isNotBlank()) {
                                        val subRange = TextRange(baseOffset + partOffset, baseOffset + endOffset)
                                        val subPath = token.substring(0, endOffset)
                                        refs.add(RazomyStringInterpolationReference(element, subRange, subPath))
                                    }
                                    partOffset = endOffset + 1
                                }
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

        // Проверяем также правую часть для `_ b` (переопределение модуля)
        val parentDecl = PsiTreeUtil.getParentOfType(myElement, RazomyDeclaration::class.java)
        if (parentDecl != null &&
            parentDecl.children.firstOrNull()?.text?.trim() == "_" &&
            parentDecl.children.getOrNull(1)?.let { PsiTreeUtil.isAncestor(it, myElement, false) } == true
        ) {
            val file = RazomyImportResolver.resolveImportedFile(myElement, resolveText)
            if (file != null) return arrayOf(PsiElementResolveResult(file))
        }

        val resolved = RazomyImportResolver.resolveWithContextFallback(myElement, resolveText)
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
        val resolved = RazomyImportResolver.resolveWithContextFallback(myElement, resolveText)
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

// --- INSPECTIONS ---

/**
 * Инспекция конфликта имен из разных файлов контекста.
 * По умолчанию резолвится первый файл (например, `a.rl`), но если символ есть и в `a.rl`, и в `b.rl`,
 * и в текущем файле нет ни локального объявления, ни явного импорта (`_ b` или `{ A } b`),
 * то выводится требование уточнить импорт.
 */
class RazomyAmbiguousReferenceInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element !is RazomyPath) return
                if (element.text == "_" || element.text.startsWith("_.")) return
                if (RazomyUtil.isDeclaration(element)) return
                if (RazomyUtil.getImportContext(element) != null) return

                // Пропускаем правую часть в `_ b`
                val parentDecl = PsiTreeUtil.getParentOfType(element, RazomyDeclaration::class.java)
                if (parentDecl != null &&
                    parentDecl.children.firstOrNull()?.text?.trim() == "_" &&
                    parentDecl.children.getOrNull(1)?.let { PsiTreeUtil.isAncestor(it, element, false) } == true
                ) {
                    return
                }

                val rootName = element.text.substringBefore('.')
                if (rootName.isBlank() || !rootName.first().isUpperCase()) return

                val file = element.containingFile ?: return

                // 1. Если объявлено локально в этом же файле — конфликта нет
                val localMap = RazomyTypeSystem.getGlobalDeclarationsMap(file)
                if (localMap.containsKey(rootName)) return

                // 2. Если явно импортировано через `{ A } b` или переопределено через `_ b` — конфликта нет
                if (RazomyImportResolver.isExplicitlyResolvedInFile(file, rootName)) return

                // 3. Проверяем кандидатов в контексте
                val candidates = RazomyImportResolver.findCandidateDeclarationsInContext(element, rootName)
                if (candidates.size > 1) {
                    val defaultFile = candidates.first().containingFile
                    val moduleNames = candidates.map { RazomyImportResolver.getRelativeModulePath(file, it.containingFile) }

                    val fixes = mutableListOf<LocalQuickFix>()
                    for (candidate in candidates) {
                        val modPath = RazomyImportResolver.getRelativeModulePath(file, candidate.containingFile)
                        fixes.add(RazomyImportQuickFix(rootName, modPath, useWildcardOverride = true))
                        fixes.add(RazomyImportQuickFix(rootName, modPath, useWildcardOverride = false))
                    }

                    holder.registerProblem(
                        element,
                        "Конфликт имен '$rootName' (найдено в: ${moduleNames.joinToString(", ")}). " +
                                "По умолчанию выбран '${defaultFile.name}', укажите импорт явно (например, '_ ${moduleNames.last()}')",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        *fixes.toTypedArray()
                    )
                }
            }
        }
    }
}

class RazomyDuplicateDeclarationInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is RazomyPath && RazomyUtil.isDeclaration(element)) {
                    val name = element.text
                    if (name == "_") return // Разрешаем несколько `_ module` в одном файле

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
                    if (name == "_" || name.startsWith("_.") || name == "Main" || name == "Start") return

                    val file = element.containingFile ?: return

                    // 1. Мгновенная проверка внутри текущего файла (кэшированный список путей на весь файл)
                    val filePaths = CachedValuesManager.getCachedValue(file) {
                        val paths = PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java).toList()
                        CachedValueProvider.Result.create(paths, file, PsiModificationTracker.MODIFICATION_COUNT)
                    }

                    val isUsedLocally = filePaths.any { path ->
                        if (path == element) return@any false
                        if (RazomyUtil.isDeclaration(path) &&
                            RazomyUtil.getImportContext(path) != RazomyUtil.ImportContext.LEFT_ALIAS_OR_DESTRUCT
                        ) {
                            return@any false
                        }
                        val pathText = path.text
                        pathText == name || pathText.startsWith("$name.") || pathText.endsWith(".$name") || pathText.contains(
                            ".$name."
                        )
                    }

                    if (isUsedLocally) return

                    // 2. Проверка в строковых интерполяциях текущего файла
                    if (RazomyUtil.isUsedInStringInterpolations(file, name)) return

                    // 3. Проверка в других файлах через индекс ссылок IntelliJ (без перебора всех файлов вручную)
                    val crossFileRef =
                        ReferencesSearch.search(element, GlobalSearchScope.projectScope(element.project)).findFirst()
                    if (crossFileRef == null) {
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

// --- DOCUMENTATION (Компактный Hover + Вычисление итогового типа) ---

class RazomyDocumentationProvider : AbstractDocumentationProvider() {

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val origPath = PsiTreeUtil.getParentOfType(originalElement, RazomyPath::class.java)

        // 1. Контекстный символ `_`
        if (originalElement?.text == "_" || origPath?.text == "_") {
            val ctxElem = origPath ?: originalElement ?: return null
            val underscoreType = RazomyTypeSystem.inferUnderscoreType(ctxElem)
            val computedBlock = RazomyTypeSystem.renderComputedTypePreview(underscoreType, ctxElem)
            return buildString {
                append("<div style='font-family: monospace; font-size: 12px; line-height: 1.35;'>")
                append("<b>_</b> : <code>${escapeHtml(underscoreType.presentableName())}</code>")
                if (computedBlock.isNotBlank()) {
                    append("<pre style='margin: 4px 0 0 0; padding: 4px; background: rgba(128,128,128,0.1);'>")
                    append(escapeHtml(computedBlock))
                    append("</pre>")
                }
                append("</div>")
            }
        }

        val target = when {
            origPath != null && origPath.text.startsWith("_.") -> origPath
            element is RazomyPath -> element
            else -> origPath
        } ?: return null

        val decl = if (RazomyUtil.isDeclaration(target)) {
            target
        } else {
            (RazomyImportResolver.resolveWithContextFallback(target, target.text) as? RazomyPath)
                ?: target.containingFile?.let { file ->
                    PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java)
                        .firstOrNull { it.text == target.text && RazomyUtil.isDeclaration(it) }
                }
        } ?: return null

        val inferredType = RazomyTypeSystem.inferTypeOfElement(decl)
        val container = PsiTreeUtil.getParentOfType(
            decl,
            RazomyDeclaration::class.java,
            RazomyFormItem::class.java,
            RazomySlotItem::class.java
        ) ?: decl

        val kind = RazomyTypeSystem.getSymbolKindLabel(decl, inferredType)
        val signature = RazomyTypeSystem.getSymbolSignature(decl, container)
        val typeName = inferredType.presentableName()
        val computedPreview = RazomyTypeSystem.renderComputedTypePreview(inferredType, decl)
        val formattedComments = collectPrecedingCommentsHtml(container)

        return buildString {
            append("<div style='font-family: monospace; font-size: 12px; line-height: 1.35;'>")
            append("<span style='color: #888;'>$kind</span> <b>${escapeHtml(signature)}</b>")
            if (inferredType !is RazomyType.Unknown && typeName != signature) {
                append(" : <code>${escapeHtml(typeName)}</code>")
            }

            if (computedPreview.isNotBlank()) {
                append("<pre style='margin: 6px 0 2px 0; padding: 4px;'>")
                append(escapeHtml(computedPreview))
                append("</pre>")
            }
            append("</div>")

            if (formattedComments.isNotBlank()) {
                append("<hr style='margin: 4px 0;'/><div style='font-size: 12px;'>")
                append(formattedComments)
                append("</div>")
            }
        }
    }

    private fun collectPrecedingCommentsHtml(element: PsiElement): String {
        val htmlBlocks = mutableListOf<String>()
        var prev = PsiTreeUtil.prevLeaf(element)

        while (prev is PsiWhiteSpace) {
            prev = PsiTreeUtil.prevLeaf(prev)
        }

        while (prev != null) {
            val type = prev.node.elementType
            if (type == RazomyTypes.LINE_COMMENT) {
                val cleanLine = prev.text.removePrefix("//").trim()
                htmlBlocks.add(0, "<div style='color: #888; font-style: italic;'>${escapeHtml(cleanLine)}</div>")
            } else if (type == RazomyTypes.DOC_COMMENT) {
                val rawContent = prev.text.removeSurrounding("#").trim()
                val parsedDoc = rawContent.lines().mapNotNull { line ->
                    val trimmed = line.trim()
                    when {
                        trimmed.isEmpty() -> null
                        trimmed.startsWith("title ", ignoreCase = true) -> {
                            val titleText = trimmed.substring(6).trim()
                            "<div style='font-weight: bold;'>${escapeHtml(titleText)}</div>"
                        }

                        trimmed.startsWith("T ") -> {
                            val bodyText = trimmed.substring(2).trim()
                            "<div>${escapeHtml(bodyText)}</div>"
                        }

                        else -> "<div>${escapeHtml(trimmed)}</div>"
                    }
                }.joinToString("")
                htmlBlocks.add(0, parsedDoc)
            } else if (type == RazomyTypes.CRLF2 || prev is PsiWhiteSpace) {
                // skip
            } else {
                break
            }
            prev = PsiTreeUtil.prevLeaf(prev)
        }
        return htmlBlocks.joinToString("\n")
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

// --- LINE MARKERS (Иерархия) ---

class RazomyLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (element !is RazomyPath || !RazomyUtil.isDeclaration(element)) return
        if (RazomyUtil.getImportContext(element) != null) return

        val leafElement = element.firstChild ?: element
        val project = element.project
        val currentName = element.text
        if (currentName.isBlank() || !currentName.first().isUpperCase()) return

        // 1. Предки (Супертипы — в том числе из импортированных файлов)
        val statement = PsiTreeUtil.getParentOfType(element, RazomyDeclaration::class.java, RazomyFormItem::class.java)
        if (statement != null && PsiTreeUtil.findChildOfType(
                statement.children.firstOrNull(),
                RazomyPath::class.java
            ) == element
        ) {
            val rightExpr = statement.children.getOrNull(1)
            if (rightExpr != null) {
                val pathsInRight = PsiTreeUtil.findChildrenOfType(rightExpr, RazomyPath::class.java)
                val superTypes = pathsInRight
                    .filter { it.text.firstOrNull()?.isUpperCase() == true }
                    .mapNotNull { path ->
                        RazomyImportResolver.resolveWithContextFallback(path, path.text)
                    }
                    .filter { it != element && it !is PsiFile }
                    .distinct()

                if (superTypes.isNotEmpty()) {
                    val upBuilder = NavigationGutterIconBuilder.create(AllIcons.Gutter.OverridingMethod)
                        .setTargets(superTypes)
                        .setTooltipText("Наследует/Использует типы: " + superTypes.joinToString { it.text })
                    result.add(upBuilder.createLineMarkerInfo(leafElement))
                }
            }
        }

        // 2. Наследники через индекс ссылок (мгновенно!)
        val subTypes = mutableListOf<PsiElement>()
        ReferencesSearch.search(element, GlobalSearchScope.projectScope(project)).forEach { ref ->
            val refElement = ref.element
            val decl = PsiTreeUtil.getParentOfType(refElement, RazomyDeclaration::class.java)
            if (decl != null) {
                val rightExpr = decl.children.getOrNull(1)
                if (rightExpr != null && PsiTreeUtil.isAncestor(rightExpr, refElement, false)) {
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
                .setTooltipText("Используется в: " + subTypes.joinToString { "${it.text} (${it.containingFile.name})" })
            result.add(downBuilder.createLineMarkerInfo(leafElement))
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

                    // 1. КОНТЕКСТ ПОСЛЕ ТОЧКИ (Включая `_.`)
                    if (currentPath != null) {
                        val fullText = currentPath.text
                        val caretOffset = (parameters.offset - currentPath.textRange.startOffset)
                            .coerceIn(0, fullText.length)

                        val textBeforeCaret = fullText.substring(0, caretOffset).replace("IntellijIdeaRulezzz", "")

                        if (textBeforeCaret.contains(".")) {
                            val prefix = textBeforeCaret.substringBeforeLast(".")
                            if (prefix == "_") {
                                val underscoreType =
                                    RazomyTypeSystem.inferUnderscoreType(currentPath)
                                val fields = RazomyType.extractFields(underscoreType)
                                for ((name, _) in fields) {
                                    result.addElement(
                                        LookupElementBuilder.create(name)
                                            .withIcon(AllIcons.Nodes.Field)
                                            .withTypeText("Field of _: ${underscoreType.presentableName()}")
                                    )
                                }
                                return
                            }

                            val resolvedPrefix = RazomyImportResolver.resolveWithContextFallback(currentPath, prefix)
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

                    // 2. ГЛОБАЛЬНЫЙ И ЛОКАЛЬНЫЙ КОНТЕКСТ
                    val scope = RazomyTypeSystem.buildScope(position)
                    val addedNames = mutableSetOf<String>()

                    scope.variables.keys.forEach {
                        addedNames.add(it)
                        val isType = it.firstOrNull()?.isUpperCase() == true
                        val icon = if (isType) AllIcons.Nodes.Class else AllIcons.Nodes.Variable
                        result.addElement(
                            LookupElementBuilder.create(it)
                                .withIcon(icon)
                                .withTypeText(if (isType) "Type" else "Variable")
                        )
                    }

                    scope.unpackedFields.keys.forEach {
                        addedNames.add(it)
                        result.addElement(
                            LookupElementBuilder.create(it)
                                .withIcon(AllIcons.Nodes.Field)
                                .withTypeText("Auto-unpacked")
                        )
                    }

                    // 3. СИМВОЛЫ ИЗ ДРУГИХ ФАЙЛОВ КОНТЕКСТА (по умолчанию)
                    val project = position.project
                    val allExportedKeys = FileBasedIndex.getInstance().getAllKeys(
                        RazomyIndexKeys.EXPORTED_SYMBOLS,
                        project
                    )
                    for (key in allExportedKeys) {
                        if (key !in addedNames) {
                            val candidates = RazomyImportResolver.findCandidateDeclarationsInContext(position, key)
                            if (candidates.isNotEmpty()) {
                                val firstFile = candidates.first().containingFile.name
                                result.addElement(
                                    LookupElementBuilder.create(key)
                                        .withIcon(AllIcons.Nodes.Class)
                                        .withTypeText("Context ($firstFile)")
                                )
                            }
                        }
                    }
                }
            }
        )
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
        val file = sourceElement.containingFile ?: return null

        if (sourceElement.node.elementType == RazomyTypes.STRING) {
            val relOffset = offset - sourceElement.textRange.startOffset
            for (match in RazomyUtil.INTERPOLATION_PATTERN.findAll(sourceElement.text)) {
                val contentGroup = match.groups[1] ?: continue
                if (relOffset in contentGroup.range) {
                    val exprText = contentGroup.value.trim()
                    val resolved = RazomyImportResolver.resolveWithContextFallback(sourceElement, exprText)
                    if (resolved != null) return arrayOf(resolved)
                }
            }
            return null
        }

        val word = sourceElement.text
        if (word.isBlank() || word == "_") return null

        val currentPath = PsiTreeUtil.getParentOfType(sourceElement, RazomyPath::class.java) ?: return null

        if (RazomyUtil.getImportContext(currentPath) == RazomyUtil.ImportContext.RIGHT_PATH) {
            val fileTarget = RazomyImportResolver.resolveImportedFile(sourceElement, currentPath.text)
            if (fileTarget != null) return arrayOf(fileTarget)
            return null
        }

        // Переход на файл при клике на `b` в выражении `_ b`
        val parentDecl = PsiTreeUtil.getParentOfType(currentPath, RazomyDeclaration::class.java)
        if (parentDecl != null &&
            parentDecl.children.firstOrNull()?.text?.trim() == "_" &&
            parentDecl.children.getOrNull(1)?.let { PsiTreeUtil.isAncestor(it, currentPath, false) } == true
        ) {
            val fileTarget = RazomyImportResolver.resolveImportedFile(sourceElement, currentPath.text)
            if (fileTarget != null) return arrayOf(fileTarget)
        }

        if (RazomyUtil.isDeclaration(currentPath) &&
            RazomyUtil.getImportContext(currentPath) != RazomyUtil.ImportContext.LEFT_ALIAS_OR_DESTRUCT
        ) {
            val targets = findUsages(currentPath, file)
            return if (targets.isNotEmpty()) targets.toTypedArray() else null
        }

        val resolved = RazomyImportResolver.resolveWithContextFallback(currentPath, word)
            ?: RazomyImportResolver.resolveWithContextFallback(currentPath, currentPath.text)
            ?: resolveLocalDeclaration(currentPath, word)

        return if (resolved != null) arrayOf(resolved) else null
    }

    private fun findUsages(element: RazomyPath, file: PsiFile): List<PsiElement> {
        return ReferencesSearch.search(element, GlobalSearchScope.projectScope(element.project))
            .findAll()
            .map { it.element }
            .distinct()
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