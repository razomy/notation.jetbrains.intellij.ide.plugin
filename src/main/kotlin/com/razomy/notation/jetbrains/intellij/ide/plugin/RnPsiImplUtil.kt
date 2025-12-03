package com.razomy.notation.jetbrains.intellij.ide.plugin

import com.intellij.navigation.ItemPresentation
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.razomy.notation.jetbrains.intellij.ide.plugin.RnElementFactory.createProperty
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.bridge.RnTypes
import javax.swing.Icon

object RnPsiImplUtil {
    @JvmStatic
    fun getKey(element: RnElement): String {
        val keyNode = element.getNode().findChildByType(RnTypes.KEY)
        return if (keyNode != null) keyNode.getText() else ""
    }

    @JvmStatic
    fun getName(element: RnElement): String {
        return getKey(element)
    }

    @JvmStatic
    fun setName(element: RnElement, newName: String): PsiElement {
        val keyNode = element.getNode().findChildByType(RnTypes.KEY)
        if (keyNode != null) {
            val property = createProperty(element.getProject(), newName)
            val newKeyNode = property.getFirstChild().getNode()
            element.getNode().replaceChild(keyNode, newKeyNode)
        }
        return element
    }

    @JvmStatic
    fun getValue(element: RnElement): String? {
        val valueNode = element.getNode().findChildByType(RnTypes.VALUE)
        return if (valueNode != null) valueNode.getText() else null
    }

    @JvmStatic
    fun getNameIdentifier(element: RnElement): PsiElement? {
        val keyNode = element.getNode().findChildByType(RnTypes.KEY)
        return if (keyNode != null) keyNode.getPsi() else null
    }

    @JvmStatic
    fun getPresentation(element: RnElement): ItemPresentation {
        return object : ItemPresentation {
            override fun getPresentableText(): String? {
                return element.getKey()
            }

            override fun getLocationString(): String? {
                val containingFile = element.getContainingFile()
                return if (containingFile == null) null else containingFile.getName()
            }

            override fun getIcon(unused: Boolean): Icon? {
                return element.getIcon(0)
            }
        }
    }

    @JvmStatic
    fun isValidHost(element: RnElement): Boolean {
        return element.isValid()
    }

    @JvmStatic
    fun updateText(element: RnElement, text: String): PsiLanguageInjectionHost {
        if (element.lastChild != null) {
            element.lastChild.delete()
        }
        element.getNode().addLeaf(RnTypes.VALUE, text, null)
        return element
    }

    @JvmStatic
    fun createLiteralTextEscaper(element: RnElement): LiteralTextEscaper<out PsiLanguageInjectionHost?> {
        return RnLiteralTextEscaper(element)
    }
}

