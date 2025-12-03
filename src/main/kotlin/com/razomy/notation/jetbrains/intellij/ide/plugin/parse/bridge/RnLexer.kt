package com.razomy.notation.jetbrains.intellij.ide.plugin.parse.bridge

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.LexemeType
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx.NotationCtx
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx.NotationLexeme
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx.UnknownTypeArgumentException
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx.create_notation_ctx
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx.string_to_lexeme_tree

fun map_token(type: LexemeType): IElementType {
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

class RnLexer : LexerBase() {
    lateinit var ctx: NotationCtx
    var curTokenType: IElementType? = null
    var endOffset = 0

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.endOffset = endOffset
        this.ctx = create_notation_ctx(buffer.toString())
        advance()
    }

    override fun getState(): Int = 0
    override fun getTokenType(): IElementType? = curTokenType

    override fun getTokenStart(): Int = this.ctx.lexeme.offset

    override fun getTokenEnd(): Int = this.ctx.offset

    override fun advance() {
        if (this.ctx.offset >= this.endOffset) {
            curTokenType = null
            return
        }
        this.ctx.execute(this.ctx)
        curTokenType = map_token(this.ctx.lexeme.type)
    }

    override fun getBufferSequence(): CharSequence = this.ctx.string
    override fun getBufferEnd(): Int = this.endOffset
}