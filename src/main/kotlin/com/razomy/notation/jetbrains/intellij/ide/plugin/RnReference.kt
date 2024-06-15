package com.razomy.notation.jetbrains.intellij.ide.plugin

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.*

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.containers.toArray

import java.util.ArrayList;


internal class RnReference(element: PsiElement, textRange: TextRange) :
    PsiReferenceBase<PsiElement?>(element, textRange), PsiPolyVariantReference {
    private val key = element.text.substring(textRange.getStartOffset(), textRange.getEndOffset())

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult?> {
        val project: Project = myElement!!.project
        val properties: List<RnProperty> = RnUtil.findProperties(project, key)
        val results: MutableList<ResolveResult> = ArrayList()
        for (property in properties) {
            results.add(PsiElementResolveResult(property))
        }
        return results.toArray(arrayOfNulls<ResolveResult>(0))
    }

    override fun resolve(): PsiElement? {
        val resolveResults: Array<ResolveResult?> = multiResolve(false)
        if (resolveResults.size == 1) return resolveResults[0]?.element
        if (resolveResults.size > 1)
            for (resolveResult in resolveResults) {
                if (resolveResult?.element?.text?.startsWith(key) == true)
                    return resolveResult.element
            }
        return null
    }

    override fun getVariants(): Array<out Any> {
        val project: Project = myElement!!.project
        val properties: List<RnProperty> = RnUtil.findProperties(project)
        val variants: MutableList<LookupElement> = ArrayList()
        for (property in properties) {
            if (property.getKey() != null && !property.getKey().isEmpty()) {
                variants.add(
                    LookupElementBuilder
                        .create(property).withIcon(RnIcons.FILE)
                        .withTypeText(property.getContainingFile().getName())
                )
            }
        }
        return variants.toTypedArray()
    }
}