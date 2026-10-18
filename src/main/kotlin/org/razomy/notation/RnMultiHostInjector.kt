package org.razomy.notation

import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.util.elementType
import org.razomy.notation.parser.RnProperty
import org.razomy.notation.parser.RnTypes

fun createLanguageAliases(): MutableMap<String, String> {
// TODO: copy from https://github.com/JetBrains/intellij-community/blob/894f26c7e910695bc75eaf10dddced1444ba9214/plugins/markdown/core/src/org/intellij/plugins/markdown/injection/aliases/CodeFenceLanguageAliases.kt
    val aliases = mutableMapOf(
        // ApacheConfig
        "apacheconf" to "ApacheConfig",
        "aconf" to "ApacheConfig",
        "apache" to "ApacheConfig",
        "apacheconfig" to "ApacheConfig",

        // Batch
        "bat" to "Batch",
        "batch" to "Batch",
        "batchfile" to "Batch",

        // C#
        "csharp" to "C#",
        "cs" to "C#",
        "c#" to "C#",

        // CoffeeScript
        "coffeescript" to "CoffeeScript",
        "coffee" to "CoffeeScript",
        "coffee-script" to "CoffeeScript",

        // F#
        "fsharp" to "F#",
        "fs" to "F#",
        "f#" to "F#",

        // Go
        "golang" to "go",

        // HCL
        "hcl" to "HCL",

        // HCL-Terraform
        "terraform" to "HCL-Terraform",
        "hcl-terraform" to "HCL-Terraform",
        "tf" to "HCL-Terraform",

        // JavaScript
        "javascript" to "JavaScript",
        "js" to "JavaScript",
        "node" to "JavaScript",

        // Kotlin
        "kotlin" to "Kotlin",
        "kt" to "Kotlin",
        "kts" to "Kotlin",

        // Markdown
        "markdown" to "Markdown",
        "md" to "Markdown",

        // PowerShell
        "powershell" to "PowerShell",
        "posh" to "PowerShell",
        "pwsh" to "PowerShell",

        // Python
        "python" to "Python",
        "python2" to "Python",
        "python3" to "Python",
        "py" to "Python",

        // R
        "r" to "R",
        "rlang" to "R",
        "rscript" to "R",

        // RegExp
        "regexp" to "RegExp",
        "regex" to "RegExp",

        // Ruby
        "ruby" to "Ruby",
        "rb" to "Ruby",

        // Shell Script
        "shell" to "Shell Script",
        "shell script" to "Shell Script",
        "bash" to "Shell Script",
        "zsh" to "Shell Script",
        "sh" to "Shell Script",

        // Yaml
        "yaml" to "Yaml",
        "yml" to "Yaml",
        // My Extension
        "ts" to "TypeScript"
    )
    var system = Language.getRegisteredLanguages();
    system.remove(Language.ANY)
    system.forEach { value -> aliases[value.id.lowercase()] = value.id };

    return aliases;
}

val LanguageAliases = createLanguageAliases();

fun langAliases(codeBlock: String): Language? {
    val key = codeBlock.lowercase()
    if (LanguageAliases.containsKey(key)) {
        val id = LanguageAliases.getValue(key);
        return Language.findLanguageByID(id);
    }
    return null
}

class RnMultiHostInjector : MultiHostInjector {
    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        if (context !is RnProperty) {
            return
        }
        if (context.textRange.length == 0) {
            return
        }
        val host = context as PsiLanguageInjectionHost

        if (host.elementType !== RnTypes.PROPERTY) {
            return
        }
        val key = host.node.firstChildNode
        val assign = key?.treeNext
        val value = assign?.treeNext
        if (key == null || assign == null || value == null) {
            return
        }

        if (value.elementType !== RnTypes.STRING
            && value.elementType !== RnTypes.VALUE_
            && value.elementType !== RnTypes.TAIL_VALUE
            && value.elementType !== RnTypes.MULTILINE_VALUE
        ) {
            return
        }

        val languageId = key.text
        if (languageId == "") {
            return
        }

        val language = langAliases(languageId)
        if (language == null) {
            return
        }

        var innerStartOffset = value.textRange.startOffset - host.textRange.startOffset
        var innerEndOffset = value.textRange.endOffset - host.textRange.startOffset

        val innerRange = TextRange(
            innerStartOffset + 2,//TODO: replace +2-2 with lexer
            innerEndOffset - 2
        )

        if (innerRange.isEmpty) {
            return
        }

        registrar.startInjecting(language)
        registrar.addPlace(
            null,
            null,
            host,
            innerRange
        )
        registrar.doneInjecting()
    }


    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return listOf(RnProperty::class.java)
    }
}