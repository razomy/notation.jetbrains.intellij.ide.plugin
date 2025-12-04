package com.razomy.notation.jetbrains.intellij.ide.plugin.parse.bridge

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.LexemeType
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx.NotationCtx
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx.NotationLexeme
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx.UnknownTypeArgumentException
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx.create_notation_ctx
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx.pop
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx.push
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

    var state_: Int = 0
    private val pendingTokens = ArrayDeque<IElementType>()
    private val indentLevelStack = ArrayDeque<Int>()

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.endOffset = endOffset
        this.ctx = create_notation_ctx(buffer.toString())
        this.state_ = initialState
        advance()
    }

    override fun getState(): Int = state_
    override fun getTokenType(): IElementType? = curTokenType
    override fun getTokenStart(): Int = this.ctx.lexeme.offset
    override fun getTokenEnd(): Int = this.ctx.offset

    override fun advance() {
        if (this.ctx.offset >= this.endOffset) {
            curTokenType = null
            return
        }

        if (pendingTokens.isNotEmpty()) {
            val t = pendingTokens.removeFirst()
            if (t == RnTypes.INDENT) {
                state_ += 1
            } else {
                state_ -= 1
            }
            this.ctx.lexeme.offset = this.ctx.offset
            curTokenType = t
            return
        }


        this.ctx.execute(this.ctx)
        curTokenType = map_token(this.ctx.lexeme.type)


        if (this.ctx.lexeme.type == LexemeType.assign) {
            indentLevelStack.push(this.ctx.lexeme.deep)
            pendingTokens.add(RnTypes.INDENT)
        }

        if (this.ctx.lexeme.type == LexemeType.new_line_assign) {
            indentLevelStack.push(this.ctx.lexeme.deep)
            pendingTokens.add(RnTypes.INDENT)
        }

        if (this.ctx.lexeme.type == LexemeType.break_) {
            if (indentLevelStack.isEmpty()) {
                pendingTokens.add(RnTypes.ERROR)
            } else {
                indentLevelStack.pop()
                pendingTokens.add(RnTypes.DEDENT)
            }
        }

        if (this.ctx.lexeme.type == LexemeType.new_line_break) {
            while (indentLevelStack.isNotEmpty() && indentLevelStack.last() >= this.ctx.lexeme.deep) {
                indentLevelStack.pop()
                pendingTokens.add(RnTypes.DEDENT)
            }
        }
    }

    override fun getBufferSequence(): CharSequence = this.ctx.string
    override fun getBufferEnd(): Int = this.endOffset
}