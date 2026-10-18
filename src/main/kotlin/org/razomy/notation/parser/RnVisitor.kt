// This is a generated file. Not intended for manual editing.
package org.razomy.notation.parser

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.razomy.notation.RnElement

class RnVisitor : com.intellij.psi.PsiElementVisitor() {
    fun visitProperty(o: RnProperty) {
        visitElement(o)
    }

    fun visitValue(o: RnValue) {
        visitElement(o)
    }

    fun visitElement(o: RnElement) {
        visitPsiElement(o)
    }

    fun visitPsiElement(o: PsiElement) {
        visitElement(o)
    }
}
