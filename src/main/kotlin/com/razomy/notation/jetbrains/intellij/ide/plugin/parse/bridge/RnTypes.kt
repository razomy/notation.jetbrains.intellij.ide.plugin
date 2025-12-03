// This is a generated file. Not intended for manual editing.
package com.razomy.notation.jetbrains.intellij.ide.plugin.parse.bridge

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.razomy.notation.jetbrains.intellij.ide.plugin.RnElementType
import com.razomy.notation.jetbrains.intellij.ide.plugin.RnTokenType
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.bridge.impl.RnPropertyImpl
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.bridge.impl.RnValueImpl

interface RnTypes {
    object Factory {
        fun createElement(node: ASTNode): PsiElement {
            val type = node.getElementType()
            if (type === PROPERTY) {
                return RnPropertyImpl(node)
            } else if (type === VALUE) {
                return RnValueImpl(node)
            }
            throw AssertionError("Unknown element type: " + type)
        }
    }

    companion object {
        val PROPERTY: IElementType = RnElementType("PROPERTY")
        val VALUE: IElementType = RnElementType("VALUE")

        val ASSIGN: IElementType = RnTokenType("ASSIGN")
        val BREAK: IElementType = RnTokenType("BREAK")
        val CHILD_DEEP: IElementType = RnTokenType("CHILD_DEEP")
        val COMMENT: IElementType = RnTokenType("COMMENT")
        val EMPTY_LINE: IElementType = RnTokenType("EMPTY_LINE")
        val END: IElementType = RnTokenType("END")
        val ERROR: IElementType = RnTokenType("ERROR")
        val KEY: IElementType = RnTokenType("KEY")
    }
}
