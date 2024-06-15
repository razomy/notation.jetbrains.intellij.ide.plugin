package com.razomy.notation.jetbrains.intellij.ide.plugin

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.tree.TokenSet

internal class RnFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner? {
        return DefaultWordsScanner(
            RnLexerAdapter(),
            RnTokenSets.IDENTIFIERS,
            RnTokenSets.COMMENTS,
            TokenSet.EMPTY
        )
    }

    override fun canFindUsagesFor( psiElement: PsiElement): Boolean {
        return psiElement is PsiNamedElement
    }


    override fun getHelpId( psiElement: PsiElement): String? {
        return null
    }

    
    override fun getType( element: PsiElement): String {
        if (element is RnProperty) {
            return "Rn property"
        }
        return ""
    }

    
    override fun getDescriptiveName( element: PsiElement): String {
        if (element is RnProperty) {
            return (element as RnProperty).getKey()
        }
        return ""
    }

    
    override fun getNodeText( element: PsiElement, useFullName: Boolean): String {
        if (element is RnProperty) {
            return (element as RnProperty).getKey() +
                    RnAnnotator.SIMPLE_SEPARATOR_STR +
                    (element as RnProperty).getValue()
        }
        return ""
    }
}