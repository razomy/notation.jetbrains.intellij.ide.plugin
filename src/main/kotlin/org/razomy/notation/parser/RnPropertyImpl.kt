// This is a generated file. Not intended for manual editing.
package org.razomy.notation.parser

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.util.PsiTreeUtil
import org.razomy.notation.parser.RnProperty
import org.razomy.notation.RnPsiImplUtil.createLiteralTextEscaper
import org.razomy.notation.RnPsiImplUtil.getKey
import org.razomy.notation.RnPsiImplUtil.getName
import org.razomy.notation.RnPsiImplUtil.getNameIdentifier
import org.razomy.notation.RnPsiImplUtil.getPresentation
import org.razomy.notation.RnPsiImplUtil.getValue
import org.razomy.notation.RnPsiImplUtil.isValidHost
import org.razomy.notation.RnPsiImplUtil.setName
import org.razomy.notation.RnPsiImplUtil.updateText
import org.razomy.notation.parser.RnValue
import org.razomy.notation.parser.RnVisitor

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
