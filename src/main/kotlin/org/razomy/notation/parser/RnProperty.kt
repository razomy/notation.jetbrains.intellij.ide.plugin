// This is a generated file. Not intended for manual editing.
package org.razomy.notation.parser

import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import org.razomy.notation.RnElement

interface RnProperty : RnElement {
    val valueList: MutableList<RnValue?>

    override fun getKey(): String

    override fun getValue(): String?

    override fun getName(): String

    override fun setName(newName: String): PsiElement

    override fun getNameIdentifier(): PsiElement?

    override fun isValidHost(): Boolean

    override fun updateText(text: String): PsiLanguageInjectionHost

    override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost?>
}
