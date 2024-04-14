package com.razomy.notation.idea.plugin

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement

internal class RnRefactoringSupportProvider : RefactoringSupportProvider() {
    override fun isMemberInplaceRenameAvailable(
        elementToRename: PsiElement,
        context: PsiElement?
    ): Boolean {
        return (elementToRename is RnProperty)
    }
}