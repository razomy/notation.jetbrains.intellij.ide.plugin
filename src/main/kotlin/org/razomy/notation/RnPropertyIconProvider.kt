package org.razomy.notation

import com.intellij.ide.IconProvider
import com.intellij.psi.PsiElement
import org.razomy.notation.parser.RnProperty
import javax.swing.Icon

internal class RnPropertyIconProvider : IconProvider() {
    override fun getIcon(element: PsiElement, flags: Int): Icon? {
        return if (element is RnProperty) RnIcons.FILE else null
    }
}