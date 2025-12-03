// This is a generated file. Not intended for manual editing.
package com.razomy.notation.jetbrains.intellij.ide.plugin.parse.bridge

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.razomy.notation.jetbrains.intellij.ide.plugin.RnElement

class RnVisitor : PsiElementVisitor() {
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
