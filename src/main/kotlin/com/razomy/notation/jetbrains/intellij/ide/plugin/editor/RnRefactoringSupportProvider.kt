package com.razomy.notation.jetbrains.intellij.ide.plugin.editor

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.RnProperty

internal class RnRefactoringSupportProvider : RefactoringSupportProvider() {
    override fun isMemberInplaceRenameAvailable(
        elementToRename: PsiElement,
        context: PsiElement?
    ): Boolean {
        return (elementToRename is RnProperty)
    }
}