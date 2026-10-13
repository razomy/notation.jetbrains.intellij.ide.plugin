
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
import com.intellij.ide.structureView.*
import com.intellij.ide.structureView.StructureViewModel.ElementInfoProvider
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
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.tree.TokenSet
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
        return psiElement is RazomyPath && org.razomy.language.RazomyUtil.isDeclaration(psiElement)
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

        // 1. Проверяем относительный импорт
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
        return org.razomy.language.RazomyTypeSystem.getGlobalDeclarationsMap(file).keys
    }

    fun findExportedSymbol(file: PsiFile, name: String): PsiElement? {
        return org.razomy.language.RazomyTypeSystem.getGlobalDeclarationsMap(file)[name]
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

class RazomyImportQuickFix(private val symbolName: String, private val modulePath: String) : LocalQuickFix,
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

                    val isDecl = org.razomy.language.RazomyUtil.isDeclaration(path)
                    val importCtx = org.razomy.language.RazomyUtil.getImportContext(path)
                    if (isDecl && importCtx != org.razomy.language.RazomyUtil.ImportContext.LEFT_ALIAS_OR_DESTRUCT) {
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
                    for (match in org.razomy.language.RazomyAnnotator.INTERPOLATION_PATTERN.findAll(text)) {
                        val group = match.groups[1] ?: continue
                        val rawExpr = group.value
                        val groupStart = group.range.first

                        for ((token, tokenRange) in org.razomy.language.RazomyUtil.extractInterpolatedPaths(rawExpr)) {
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
        val importCtx = org.razomy.language.RazomyUtil.getImportContext(myElement)

        if (importCtx == org.razomy.language.RazomyUtil.ImportContext.RIGHT_PATH && resolveText == myElement.text) {
            val file = RazomyImportResolver.resolveImportedFile(myElement, resolveText)
            return if (file != null) arrayOf(PsiElementResolveResult(file)) else emptyArray()
        }

        val resolved = org.razomy.language.RazomyResolver.resolvePath(myElement, resolveText)
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
        val resolved = org.razomy.language.RazomyResolver.resolvePath(myElement, resolveText)
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

class RazomyDuplicateDeclarationInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is RazomyPath && org.razomy.language.RazomyUtil.isDeclaration(element)) {
                    val name = element.text
                    val file = element.containingFile ?: return
                    val myScope = PsiTreeUtil.getParentOfType(
                        element,
                        RazomyFormBlock::class.java,
                        RazomyFlowBlock::class.java,
                        RazomySlot::class.java
                    )

                    val duplicate = PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java).firstOrNull { other ->
                        if (other != element && other.text == name && org.razomy.language.RazomyUtil.isDeclaration(other)) {
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
                if (element is RazomyPath && org.razomy.language.RazomyUtil.isDeclaration(element)) {
                    val name = element.text
                    if (name == "_" || name.startsWith("_.") || name == "Main" || name == "Start") return

                    val file = element.containingFile ?: return

                    val allPaths = PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java)
                    val isUsedLocallyInPaths = allPaths.any { path ->
                        if (path == element) return@any false
                        if (org.razomy.language.RazomyUtil.isDeclaration(path)) return@any false
                        val pathText = path.text
                        pathText == name || pathText.startsWith("$name.") || pathText.split('.').contains(name)
                    }

                    if (isUsedLocallyInPaths) return

                    val isUsedInStrings = org.razomy.language.RazomyUtil.isUsedInStringInterpolations(file, name)
                    if (isUsedInStrings) return

                    val project = element.project
                    val psiManager = PsiManager.getInstance(project)
                    val allRlFiles = FileTypeIndex.getFiles(RazomyFileType.INSTANCE, GlobalSearchScope.projectScope(project))

                    val isUsedCrossFile = allRlFiles.any { vf ->
                        val otherFile = psiManager.findFile(vf)
                        if (otherFile == null || otherFile == file) false
                        else {
                            PsiTreeUtil.findChildrenOfType(otherFile, RazomyPath::class.java).any { path ->
                                val pathText = path.text
                                (pathText == name || pathText.endsWith(".$name") || pathText.startsWith("$name.")) &&
                                        org.razomy.language.RazomyResolver.resolvePath(path, pathText) == element
                            } || org.razomy.language.RazomyUtil.isUsedInStringInterpolations(otherFile, name)
                        }
                    }
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

// --- DOCUMENTATION ---

class RazomyDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val origPath = PsiTreeUtil.getParentOfType(originalElement, RazomyPath::class.java)

        // 1. Если навели курсор на `_` — показываем выведенный контекстный тип `_`
        if (originalElement?.text == "_" || origPath?.text == "_") {
            val ctxElem = origPath ?: originalElement ?: return null
            val underscoreType = org.razomy.language.RazomyTypeSystem.inferUnderscoreType(ctxElem)
            val fields = org.razomy.language.RazomyType.extractFields(underscoreType).keys
            return buildString {
                append("<div style='font-family: monospace; padding: 4px;'>")
                append("<span>Contextual Symbol:</span> <code><b>_</b></code><br/>")
                append("<span>Inferred Type:</span> <code><b>${escapeHtml(underscoreType.presentableName())}</b></code><br/>")
                if (fields.isNotEmpty()) {
                    append("<span>Available Fields:</span> <code>${escapeHtml(fields.joinToString(", "))}</code>")
                }
                append("</div>")
            }
        }

        val target = when {
            origPath != null && origPath.text.startsWith("_.") -> origPath
            element is RazomyPath -> element
            else -> origPath
        } ?: return null

        val decl = if (org.razomy.language.RazomyUtil.isDeclaration(target)) {
            target
        } else {
            (org.razomy.language.RazomyResolver.resolvePath(target, target.text) as? RazomyPath)
                ?: target.containingFile?.let { file ->
                    PsiTreeUtil.findChildrenOfType(file, RazomyPath::class.java)
                        .firstOrNull { it.text == target.text && org.razomy.language.RazomyUtil.isDeclaration(it) }
                }
        } ?: return null

        val inferredType = org.razomy.language.RazomyTypeSystem.inferTypeOfElement(decl)
        val container = PsiTreeUtil.getParentOfType(
            decl,
            RazomyDeclaration::class.java,
            RazomyFormItem::class.java,
            RazomySlotItem::class.java
        ) ?: decl
        val formattedComments = collectPrecedingCommentsHtml(container)

        val doc = StringBuilder()
        doc.append("<div style='font-family: monospace; padding: 4px;'>")
        doc.append("<span>Symbol:</span> <code><b>${escapeHtml(decl.text)}</b></code><br/>")
        if (inferredType !is org.razomy.language.RazomyType.Unknown) {
            doc.append("<span>Inferred Type:</span> <code>${escapeHtml(inferredType.presentableName())}</code><br/>")
        }

        var presentationText = container.text
        if (presentationText.length > 120) presentationText = presentationText.substring(0, 120) + "..."
        doc.append("<span>Declaration:</span> <br/> <code>${escapeHtml(presentationText)}</code>")
        doc.append("</div>")

        if (formattedComments.isNotBlank()) {
            doc.append("<hr/><div style='padding: 4px;'>")
            doc.append(formattedComments)
            doc.append("</div>")
        }

        return doc.toString()
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
                            "<div style='font-weight: bold; font-size: 1.1em; margin-bottom: 4px;'>${
                                escapeHtml(
                                    titleText
                                )
                            }</div>"
                        }

                        trimmed.startsWith("T ") -> {
                            val bodyText = trimmed.substring(2).trim()
                            "<div style='margin-bottom: 2px;'>${escapeHtml(bodyText)}</div>"
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
                if (path != null && path.textOffset == firstExpr?.textOffset && org.razomy.language.RazomyUtil.isDeclaration(path)) {
                    RazomyStructureElement(path)
                } else null
            }.toTypedArray()
        } else if (element is RazomyPath) {
            val decl = PsiTreeUtil.getParentOfType(element, RazomyDeclaration::class.java, RazomyFormItem::class.java)
            if (decl != null && decl.children.firstOrNull()?.textOffset == element.textOffset) {
                val formBlock = PsiTreeUtil.findChildOfType(decl, RazomyFormBlock::class.java)
                if (formBlock != null && !org.razomy.language.RazomyUtil.isRouterFormBlock(formBlock)) {
                    val formItems = formBlock.children.filterIsInstance<RazomyFormItem>()
                    return formItems.mapNotNull { it.children.firstOrNull() }
                        .mapNotNull { PsiTreeUtil.findChildOfType(it, RazomyPath::class.java) }
                        .filter { org.razomy.language.RazomyUtil.isDeclaration(it) }
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
        if (element !is RazomyPath || !org.razomy.language.RazomyUtil.isDeclaration(element)) return
        if (org.razomy.language.RazomyUtil.getImportContext(element) != null) return

        val leafElement = element.firstChild ?: element
        val project = element.project
        val currentName = element.text
        if (currentName.isBlank() || !currentName.first().isUpperCase()) return

        // 1. Предки (Супертипы — в том числе из импортированных файлов)
        val statement = PsiTreeUtil.getParentOfType(element, RazomyDeclaration::class.java, RazomyFormItem::class.java)
        if (statement != null && PsiTreeUtil.findChildOfType(statement.children.firstOrNull(), RazomyPath::class.java) == element) {
            val rightExpr = statement.children.getOrNull(1)
            if (rightExpr != null) {
                val pathsInRight = PsiTreeUtil.findChildrenOfType(rightExpr, RazomyPath::class.java)
                val superTypes = pathsInRight
                    .filter { it.text.firstOrNull()?.isUpperCase() == true }
                    .mapNotNull { path ->
                        org.razomy.language.RazomyResolver.resolvePath(path, path.text)
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

        // 2. Наследники (По всем .rl файлам проекта)
        val subTypes = mutableListOf<PsiElement>()
        val psiManager = PsiManager.getInstance(project)
        val allRlFiles = FileTypeIndex.getFiles(RazomyFileType.INSTANCE, GlobalSearchScope.projectScope(project))

        for (vf in allRlFiles) {
            val psiFile = psiManager.findFile(vf) ?: continue
            val allDecls = PsiTreeUtil.findChildrenOfType(psiFile, RazomyDeclaration::class.java)

            for (decl in allDecls) {
                val rightExpr = decl.children.getOrNull(1) ?: continue
                val pathsInRight = PsiTreeUtil.findChildrenOfType(rightExpr, RazomyPath::class.java)

                val usesCurrent = pathsInRight.any { p ->
                    val shortName = p.text.substringAfterLast('.')
                    shortName == currentName && org.razomy.language.RazomyResolver.resolvePath(p, p.text) == element
                }

                if (usesCurrent) {
                    val declName = PsiTreeUtil.findChildOfType(decl.children.firstOrNull(), RazomyPath::class.java)
                    if (declName != null && declName != element && org.razomy.language.RazomyUtil.isDeclaration(declName)) {
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
                                    org.razomy.language.RazomyTypeSystem.inferUnderscoreType(currentPath)
                                val fields = org.razomy.language.RazomyType.extractFields(underscoreType)
                                for ((name, _) in fields) {
                                    result.addElement(
                                        LookupElementBuilder.create(name)
                                            .withIcon(AllIcons.Nodes.Field)
                                            .withTypeText("Field of _: ${underscoreType.presentableName()}")
                                    )
                                }
                                return
                            }

                            val resolvedPrefix = org.razomy.language.RazomyResolver.resolvePath(currentPath, prefix)
                            if (resolvedPrefix != null) {
                                val type = if (resolvedPrefix is PsiFile) {
                                    org.razomy.language.RazomyType.Module(resolvedPrefix)
                                } else {
                                    org.razomy.language.RazomyTypeSystem.inferTypeOfElement(resolvedPrefix)
                                }
                                val fields = org.razomy.language.RazomyType.extractFields(type)

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
                    val scope = org.razomy.language.RazomyTypeSystem.buildScope(position)

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
            for (match in org.razomy.language.RazomyAnnotator.INTERPOLATION_PATTERN.findAll(sourceElement.text)) {
                val contentGroup = match.groups[1] ?: continue
                if (relOffset in contentGroup.range) {
                    val exprText = contentGroup.value.trim()
                    val resolved = org.razomy.language.RazomyResolver.resolvePath(sourceElement, exprText)
                    if (resolved != null) return arrayOf(resolved)
                }
            }
            return null
        }

        val word = sourceElement.text
        if (word.isBlank() || word == "_") return null

        val currentPath = PsiTreeUtil.getParentOfType(sourceElement, RazomyPath::class.java) ?: return null

        if (org.razomy.language.RazomyUtil.getImportContext(currentPath) == org.razomy.language.RazomyUtil.ImportContext.RIGHT_PATH) {
            val fileTarget = RazomyImportResolver.resolveImportedFile(sourceElement, currentPath.text)
            if (fileTarget != null) return arrayOf(fileTarget)
            return null
        }

        if (org.razomy.language.RazomyUtil.isDeclaration(currentPath) &&
            org.razomy.language.RazomyUtil.getImportContext(currentPath) != org.razomy.language.RazomyUtil.ImportContext.LEFT_ALIAS_OR_DESTRUCT
        ) {
            val targets = findUsages(currentPath, file)
            return if (targets.isNotEmpty()) targets.toTypedArray() else null
        }

        val resolved = org.razomy.language.RazomyResolver.resolvePath(currentPath, word)
            ?: org.razomy.language.RazomyResolver.resolvePath(currentPath, currentPath.text)
            ?: resolveLocalDeclaration(currentPath, word)

        return if (resolved != null) arrayOf(resolved) else null
    }

    private fun findUsages(element: RazomyPath, file: PsiFile): List<PsiElement> {
        val currentName = element.text
        val results = LinkedHashSet<PsiElement>()
        val project = element.project
        val psiManager = PsiManager.getInstance(project)

        val allRlFiles = FileTypeIndex.getFiles(RazomyFileType.INSTANCE, GlobalSearchScope.projectScope(project))

        for (vf in allRlFiles) {
            val psiFile = psiManager.findFile(vf) ?: continue

            // 1. Поиск среди обычных путей RazomyPath
            val allPaths = PsiTreeUtil.findChildrenOfType(psiFile, RazomyPath::class.java)
            for (path in allPaths) {
                if (path == element) continue
                val isDecl = org.razomy.language.RazomyUtil.isDeclaration(path)
                val importCtx = org.razomy.language.RazomyUtil.getImportContext(path)

                // Пропускаем чужие декларации (кроме деструктуризации импорта { LargeError } api)
                if (isDecl && importCtx != org.razomy.language.RazomyUtil.ImportContext.LEFT_ALIAS_OR_DESTRUCT) {
                    continue
                }

                if (matchesName(path.text, currentName)) {
                    val resolved = org.razomy.language.RazomyResolver.resolvePath(path, path.text)
                    if (resolved == element || (psiFile == file && resolved == null)) {
                        results.add(path)
                    }
                }
            }

            // 2. Поиск внутри строковых интерполяций "{Err.Log}"
            val allLiterals = PsiTreeUtil.findChildrenOfType(psiFile, RazomyLiteral::class.java)
            for (lit in allLiterals) {
                if (!lit.text.contains('{')) continue
                for (ref in lit.references) {
                    if (ref.isReferenceTo(element)) {
                        results.add(lit)
                    }
                }
            }
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
                if (slot != null && org.razomy.language.RazomyUtil.isParameterDeclarationSlot(slot)) {
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