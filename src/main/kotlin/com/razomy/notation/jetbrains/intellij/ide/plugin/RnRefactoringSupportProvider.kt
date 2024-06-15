package com.razomy.notation.jetbrains.intellij.ide.plugin

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