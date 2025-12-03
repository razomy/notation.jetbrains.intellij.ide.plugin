package com.razomy.notation.jetbrains.intellij.ide.plugin


import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.intellij.psi.PsiElement
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.bridge.RnTypes

internal class RnCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(RnTypes.VALUE_),
            object : CompletionProvider<CompletionParameters?>() {
                public override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val currentElement: PsiElement = parameters.position
                    val cursorOffset: Int = parameters.offset
                    val elementStartOffset: Int = currentElement.textRange.startOffset

                    if (cursorOffset > elementStartOffset + 3) {
                        return
                    }

                    val prefix: String = result.getPrefixMatcher().getPrefix()
                    if (prefix == "") {
                        LanguageAliases.forEach { result.addElement(LookupElementBuilder.create(it.key)) }
                    }
                }
            }
        )
    }
}