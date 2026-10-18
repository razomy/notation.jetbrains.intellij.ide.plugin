package org.razomy.notation

import com.intellij.navigation.ItemPresentation
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import org.razomy.notation.parser.RnTypes
import javax.swing.Icon

object RnPsiImplUtil {
    @JvmStatic
    fun getKey(element: RnElement): String {
        val keyNode = element.node.findChildByType(RnTypes.VALUE_)
        return if (keyNode != null) keyNode.text else ""
    }

    @JvmStatic
    fun getName(element: RnElement): String {
        return getKey(element)
    }

    @JvmStatic
    fun setName(element: RnElement, newName: String): PsiElement {
        val keyNode = element.node.findChildByType(RnTypes.VALUE_)
        if (keyNode != null) {
            val property = RnElementFactory.createProperty(element.project, newName)
            val newKeyNode = property.firstChild.node
            element.node.replaceChild(keyNode, newKeyNode)
        }
        return element
    }

    @JvmStatic
    fun getValue(element: RnElement): String? {
        val valueNode = element.node.findChildByType(RnTypes.VALUE_)
        return if (valueNode != null) valueNode.text else null
    }

    @JvmStatic
    fun getNameIdentifier(element: RnElement): PsiElement? {
        val keyNode = element.node.findChildByType(RnTypes.VALUE_)
        return if (keyNode != null) keyNode.psi else null
    }

    @JvmStatic
    fun getPresentation(element: RnElement): ItemPresentation {
        return object : ItemPresentation {
            override fun getPresentableText(): String {
                return element.getKey()
            }

            override fun getLocationString(): String? {
                val containingFile = element.containingFile
                return if (containingFile == null) null else containingFile.name
            }

            override fun getIcon(unused: Boolean): Icon? {
                return element.getIcon(0)
            }
        }
    }

    @JvmStatic
    fun isValidHost(element: RnElement): Boolean {
        return element.isValid
    }

    @JvmStatic
    fun updateText(element: RnElement, text: String): PsiLanguageInjectionHost {
        if (element.lastChild != null) {
            element.lastChild.delete()
        }
        element.node.addLeaf(RnTypes.VALUE_, text, null)
        return element
    }

    @JvmStatic
    fun createLiteralTextEscaper(element: RnElement): LiteralTextEscaper<out PsiLanguageInjectionHost?> {
        return RnLiteralTextEscaper(element)
    }
}

