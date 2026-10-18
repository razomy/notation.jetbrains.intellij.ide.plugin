package org.razomy.notation

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement
import org.razomy.notation.parser.RnProperty

internal class RnRefactoringSupportProvider : RefactoringSupportProvider() {
    override fun isMemberInplaceRenameAvailable(
        elementToRename: PsiElement,
        context: PsiElement?
    ): Boolean {
        return (elementToRename is RnProperty)
    }
}