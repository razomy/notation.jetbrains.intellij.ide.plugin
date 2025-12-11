// This is a generated file. Not intended for manual editing.
package com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.util.PsiTreeUtil
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.RnProperty
import com.razomy.notation.jetbrains.intellij.ide.plugin.editor.RnPsiImplUtil.createLiteralTextEscaper
import com.razomy.notation.jetbrains.intellij.ide.plugin.editor.RnPsiImplUtil.getKey
import com.razomy.notation.jetbrains.intellij.ide.plugin.editor.RnPsiImplUtil.getName
import com.razomy.notation.jetbrains.intellij.ide.plugin.editor.RnPsiImplUtil.getNameIdentifier
import com.razomy.notation.jetbrains.intellij.ide.plugin.editor.RnPsiImplUtil.getPresentation
import com.razomy.notation.jetbrains.intellij.ide.plugin.editor.RnPsiImplUtil.getValue
import com.razomy.notation.jetbrains.intellij.ide.plugin.editor.RnPsiImplUtil.isValidHost
import com.razomy.notation.jetbrains.intellij.ide.plugin.editor.RnPsiImplUtil.setName
import com.razomy.notation.jetbrains.intellij.ide.plugin.editor.RnPsiImplUtil.updateText
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.RnValue
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.RnVisitor

class RnPropertyImpl(node: ASTNode) : ASTWrapperPsiElement(node), RnProperty {
    fun accept(visitor: RnVisitor) {
        visitor.visitProperty(this)
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is RnVisitor) accept(visitor)
        else super.accept(visitor)
    }

    override val valueList: MutableList<RnValue?>
        get() = PsiTreeUtil.getChildrenOfTypeAsList<RnValue?>(this, RnValue::class.java)

    override fun getKey(): String {
        return getKey(this)
    }

    override fun getValue(): String? {
        return getValue(this)
    }

    override fun getName(): String {
        return getName(this)
    }

    override fun setName(newName: String): PsiElement {
        return setName(this, newName)
    }

    override fun getNameIdentifier(): PsiElement? {
        return getNameIdentifier(this)
    }

    override fun getPresentation(): ItemPresentation {
        return getPresentation(this)
    }

    override fun isValidHost(): Boolean {
        return isValidHost(this)
    }

    override fun updateText(text: String): PsiLanguageInjectionHost {
        return updateText(this, text)
    }

    override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost?> {
        return createLiteralTextEscaper(this)
    }
}
