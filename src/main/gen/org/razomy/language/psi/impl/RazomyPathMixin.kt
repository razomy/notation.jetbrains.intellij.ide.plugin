package org.razomy.language.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

abstract class RazomyPathMixin(node: ASTNode) : ASTWrapperPsiElement(node), PsiNameIdentifierOwner {

    override fun getNameIdentifier(): PsiElement? {
        // Имя узла - это его первый токен (идентификатор или Default)
        return firstChild
    }

    override fun getName(): String? {
        return text
    }

    override fun setName(name: String): PsiElement {
        // Этот метод нужен для работы Shift+F6 (Rename).
        // Пока оставляем заглушку, чтобы IDE не ругалась.
        return this
    }
}