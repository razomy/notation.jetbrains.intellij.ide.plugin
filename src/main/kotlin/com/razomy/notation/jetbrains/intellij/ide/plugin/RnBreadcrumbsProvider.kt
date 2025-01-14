package com.razomy.notation.jetbrains.intellij.ide.plugin

import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider;

internal class RnBreadcrumbsProvider : BreadcrumbsProvider {

    override fun getLanguages(): Array<Language> {
        return arrayOf(RnLanguage)
    }

    override fun acceptElement(element: PsiElement): Boolean {
        return element is RnProperty
    }

    override fun acceptStickyElement(e: PsiElement): Boolean {
        return acceptElement(e)
    }

    override fun getElementInfo(element: PsiElement): String {
        val name = (element as RnProperty).getName() ?: ""
        return substringWithDots(name, 7)
    }
}