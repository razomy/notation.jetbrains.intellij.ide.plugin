package com.razomy.notation.jetbrains.intellij.ide.plugin.lexer

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType
import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme.LexemeType
import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.NotationCtx
import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.NotationLexeme
import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.string_to_lexeme_tree


class Ctx {
    var buffer: CharSequence = ""
    var startOffset = 0
    var ctx: NotationCtx? = null
    var endOffset = 0
    var curOffset = 0
    var curTokenType: IElementType? = null
    var curTokenStart = 0
    var curTokenEnd = 0
}

class StringToLexeme(var ctx: Ctx) {
    fun advance(): Int {
        if (this.ctx.ctx == null) {
            //TODO: performance issue
            this.ctx.ctx = string_to_lexeme_tree(this.ctx.buffer.toString())
        }

        var offsetLocal = 0
        var lexeme: NotationLexeme? = null;

        if (ctx.ctx?.exceptions?.isNotEmpty() == true) {
            // TODO: create error handler and auto resolve error
            this.ctx.curTokenType = RnTypes.ERROR;
            this.ctx.curOffset = this.ctx.endOffset;
            return this.ctx.curOffset
        }

        for (lexeme_ in ctx.ctx?.lexemes!!) {
            lexeme = lexeme_ as NotationLexeme;
            offsetLocal += lexeme.value.length;
            if (offsetLocal > ctx.curOffset) {
                break
            }
        }

        this.ctx.curOffset = offsetLocal;

        val message = when (lexeme?.type) {
            LexemeType.new_line_assign -> RnTypes.CHILD_DEEP
            LexemeType.comment -> RnTypes.COMMENT
            LexemeType.multiline_value -> RnTypes.VALUE
            LexemeType.new_line_break -> RnTypes.BREAK
            LexemeType.separate -> RnTypes.BREAK
            LexemeType.value_ -> RnTypes.VALUE
            LexemeType.tail_value -> RnTypes.VALUE
            LexemeType.assign -> RnTypes.ASSIGN
            LexemeType.break_ -> RnTypes.BREAK
            LexemeType.new_line_separate -> RnTypes.BREAK
            LexemeType.not_closed_error_multiline_value -> RnTypes.ERROR
            LexemeType.error -> RnTypes.ERROR
            null -> RnTypes.ERROR
        }

        this.ctx.curTokenType = message;
        return this.ctx.curOffset
    }
}

class RnLexer : LexerBase() {
    private var ctx: Ctx = Ctx()
    private var stringToLexeme: StringToLexeme = StringToLexeme(ctx)

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.ctx.buffer = buffer
        this.ctx.startOffset = startOffset
        this.ctx.endOffset = endOffset
        this.ctx.curOffset = startOffset
        advance() // Initialize first token
    }

    override fun getState(): Int = 0 // We aren't using complex states for this simple example

    override fun getTokenType(): IElementType? = this.ctx.curTokenType

    override fun getTokenStart(): Int = this.ctx.curTokenStart

    override fun getTokenEnd(): Int = this.ctx.curTokenEnd

    override fun advance() {
        if (this.ctx.curOffset >= this.ctx.endOffset) {
            this.ctx.curTokenType = null
            return
        }

        this.ctx.curTokenStart = this.ctx.curOffset
        val newOffset = this.stringToLexeme.advance();
        this.ctx.curTokenEnd = newOffset

    }

    override fun getBufferSequence(): CharSequence = this.ctx.buffer
    override fun getBufferEnd(): Int = this.ctx.endOffset
}