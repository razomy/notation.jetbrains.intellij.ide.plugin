// This is a generated file. Not intended for manual editing.
package com.razomy.notation.jetbrains.intellij.ide.plugin.parse.bridge

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.bridge.RnElementType
import com.razomy.notation.jetbrains.intellij.ide.plugin.RnTokenType
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.bridge.impl.RnPropertyImpl
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.bridge.impl.RnValueImpl

interface RnTypes {
    object Factory {
        fun createElement(node: ASTNode): PsiElement {
            val type = node.getElementType()
            if (type === PROPERTY) {
                return RnPropertyImpl(node)
            } else   {
                return RnValueImpl(node)
            }
            throw AssertionError("Unknown element type: " + type)
        }
    }

    companion object {
        val NEW_LINE_ASSIGN: IElementType = RnElementType("NEW_LINE_ASSIGN")
        val COMMENT: IElementType = RnElementType("COMMENT")
        val MULTILINE_VALUE: IElementType = RnElementType("MULTILINE_VALUE")
        val NEW_LINE_BREAK: IElementType = RnElementType("NEW_LINE_BREAK")
        val SEPARATE: IElementType = RnElementType("SEPARATE")
        val VALUE_: IElementType = RnElementType("VALUE_")
        val TAIL_VALUE: IElementType = RnElementType("TAIL_VALUE")
        val ASSIGN: IElementType = RnElementType("ASSIGN")
        val BREAK_: IElementType = RnElementType("BREAK_")
        val NEW_LINE_SEPARATE: IElementType = RnElementType("NEW_LINE_SEPARATE")
        val NOT_CLOSED_ERROR_MULTILINE_VALUE: IElementType = RnElementType("NOT_CLOSED_ERROR_MULTILINE_VALUE")
        val ERROR: IElementType = RnElementType("ERROR")

        // parser
        val PROPERTY: IElementType = RnElementType("PROPERTY")
        val KEY: IElementType = RnElementType("KEY")
        val STRING: IElementType = RnElementType("STRING")
        val INDENT: IElementType = RnElementType("INDENT")
        val DEDENT: IElementType = RnElementType("DEDENT")
    }
}
