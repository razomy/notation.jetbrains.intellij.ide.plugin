// This is a generated file. Not intended for manual editing.
package org.razomy.notation.parser

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.razomy.notation.parser.ctx.UnknownTypeArgumentException
import org.razomy.notation.parser.RnPropertyImpl
import org.razomy.notation.parser.RnValueImpl
import org.razomy.notation.parser.lexeme.LexemeType

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

fun lexeme_to_element(type: org.razomy.notation.parser.lexeme.LexemeType): IElementType {
    return when (type) {
        _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.new_line_assign -> RnTypes.NEW_LINE_ASSIGN
        _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.multiline_value -> RnTypes.MULTILINE_VALUE
        _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.new_line_break -> RnTypes.NEW_LINE_BREAK
        _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.separate -> RnTypes.SEPARATE
        _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.value_ -> RnTypes.VALUE_
        _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.tail_value -> RnTypes.TAIL_VALUE
        _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.assign -> RnTypes.ASSIGN
        _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.break_ -> RnTypes.BREAK_
        _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.new_line_separate -> RnTypes.NEW_LINE_SEPARATE
        _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.not_closed_error_multiline_value -> RnTypes.NOT_CLOSED_ERROR_MULTILINE_VALUE
        _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.error -> RnTypes.ERROR
        null -> throw _root_ide_package_.org.razomy.notation.parser.ctx.UnknownTypeArgumentException("Unknown lexeme type ${type}")
    }
}

fun element_to_lexeme(type: IElementType): org.razomy.notation.parser.lexeme.LexemeType {
    return when (type) {
        RnTypes.NEW_LINE_ASSIGN -> _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.new_line_assign
        RnTypes.MULTILINE_VALUE -> _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.multiline_value
        RnTypes.NEW_LINE_BREAK -> _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.new_line_break
        RnTypes.SEPARATE -> _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.separate
        RnTypes.VALUE_ -> _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.value_
        RnTypes.TAIL_VALUE -> _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.tail_value
        RnTypes.ASSIGN -> _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.assign
        RnTypes.BREAK_ -> _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.break_
        RnTypes.NEW_LINE_SEPARATE -> _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.new_line_separate
        RnTypes.NOT_CLOSED_ERROR_MULTILINE_VALUE -> _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.not_closed_error_multiline_value
        RnTypes.ERROR -> _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.error
        // other
        RnTypes.INDENT -> _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.error
        RnTypes.PROPERTY -> _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.error
        RnTypes.DEDENT -> _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.error
        RnTypes.STRING -> _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.value_
        RnTypes.KEY -> _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.value_
        else -> throw IllegalArgumentException("Unknown element type ${type}")
    }
}
