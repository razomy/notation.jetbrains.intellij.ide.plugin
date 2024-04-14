package com.razomy.notation.idea.plugin

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext

val RN_SEPARATOR_STR: String = "."

internal class RnReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement<PsiLiteralExpression>(
                PsiLiteralExpression::class.java
            ),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    p1: ProcessingContext
                ): Array<out PsiReference> {
                    val literalExpression = element as PsiLiteralExpression
                    val value = if (literalExpression.value is String) literalExpression.value as String? else null
                    if ((value != null && value.startsWith(RN_SEPARATOR_STR))) {
                        val property: TextRange = TextRange(
                            RN_SEPARATOR_STR.length + 1,
                            value.length + 1
                        )
                        return arrayOf(RnReference(element, property))
                    }
                    return PsiReference.EMPTY_ARRAY
                }
            })
    }
}
