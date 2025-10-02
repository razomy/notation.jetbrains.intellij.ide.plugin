package com.razomy.notation.jetbrains.intellij.ide.plugin

import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost

fun getLanguageKey(codeBlock: String): String {
    if (codeBlock.startsWith("`")) {
        val firstLine = codeBlock.substringBefore('\n')
        return firstLine.removePrefix("`")
    } else {
        return ""
    }
}

class RnMultiHostInjector : MultiHostInjector {
    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        if (context !is RnValue) {
            return
        }
        if (context.textRange.length == 0) {
            return
        }
        val host = context as PsiLanguageInjectionHost
        val hostText = host.text
        val languageId = getLanguageKey(hostText).uppercase();
        if (languageId == "") {
            return
        }
        val language = Language.findLanguageByID(languageId) ?: return

        val innerRange = TextRange(
            languageId.length + 2,
            hostText.length - 1
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
        return listOf(RnValue::class.java)
    }
}