// This is a generated file. Not intended for manual editing.
package com.razomy.notation.jetbrains.intellij.ide.plugin.bridge

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.ctx.UnknownTypeArgumentException
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.impl.RnPropertyImpl
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.impl.RnValueImpl
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.LexemeType

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

fun lexeme_to_element(type: LexemeType): IElementType {
    return when (type) {
        LexemeType.new_line_assign -> RnTypes.NEW_LINE_ASSIGN
        LexemeType.multiline_value -> RnTypes.MULTILINE_VALUE
        LexemeType.new_line_break -> RnTypes.NEW_LINE_BREAK
        LexemeType.separate -> RnTypes.SEPARATE
        LexemeType.value_ -> RnTypes.VALUE_
        LexemeType.tail_value -> RnTypes.TAIL_VALUE
        LexemeType.assign -> RnTypes.ASSIGN
        LexemeType.break_ -> RnTypes.BREAK_
        LexemeType.new_line_separate -> RnTypes.NEW_LINE_SEPARATE
        LexemeType.not_closed_error_multiline_value -> RnTypes.NOT_CLOSED_ERROR_MULTILINE_VALUE
        LexemeType.error -> RnTypes.ERROR
        null -> throw UnknownTypeArgumentException("Unknown lexeme type ${type}")
    }
}

fun element_to_lexeme(type: IElementType): LexemeType {
    return when (type) {
        RnTypes.NEW_LINE_ASSIGN -> LexemeType.new_line_assign
        RnTypes.MULTILINE_VALUE -> LexemeType.multiline_value
        RnTypes.NEW_LINE_BREAK -> LexemeType.new_line_break
        RnTypes.SEPARATE -> LexemeType.separate
        RnTypes.VALUE_ -> LexemeType.value_
        RnTypes.TAIL_VALUE -> LexemeType.tail_value
        RnTypes.ASSIGN -> LexemeType.assign
        RnTypes.BREAK_ -> LexemeType.break_
        RnTypes.NEW_LINE_SEPARATE -> LexemeType.new_line_separate
        RnTypes.NOT_CLOSED_ERROR_MULTILINE_VALUE -> LexemeType.not_closed_error_multiline_value
        RnTypes.ERROR -> LexemeType.error
        // other
        RnTypes.INDENT -> LexemeType.error
        RnTypes.PROPERTY -> LexemeType.error
        RnTypes.DEDENT -> LexemeType.error
        RnTypes.STRING -> LexemeType.value_
        RnTypes.KEY -> LexemeType.value_
        else -> throw IllegalArgumentException("Unknown element type ${type}")
    }
}
