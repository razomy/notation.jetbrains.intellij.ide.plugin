package com.razomy.notation.jetbrains.intellij.ide.plugin.bridge

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.LexemeType
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.ctx.NotationCtx
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.ctx.UnknownTypeArgumentException
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.string_to_lexeme_ctx
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.ctx.pop
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.ctx.push
import com.intellij.openapi.diagnostic.Logger

class RnLexer : LexerBase() {
    lateinit var ctx: NotationCtx
    var curTokenType: IElementType? = null
    var endOffset = 0

    var deep_: Int = 0
    var state_: Int = 0
    private val pendingTokens = ArrayDeque<IElementType>()
    private val indentLevelStack = ArrayDeque<Int>()

    companion object {
        private val LOG = Logger.getInstance(RnLexer::class.java)
    }

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.endOffset = endOffset
        this.ctx = string_to_lexeme_ctx(buffer.toString())
        this.ctx.prev_offset = startOffset
        this.ctx.offset = startOffset
        this.state_ = initialState
        this.deep_ =initialState
//        LOG.warn("startOffset " +  startOffset)
//        LOG.warn("endOffset " +  endOffset)
        advance()
    }

    override fun getState(): Int = state_
    override fun getTokenType(): IElementType? = curTokenType
    override fun getTokenStart(): Int = this.ctx.lexeme.offset
    override fun getTokenEnd(): Int = this.ctx.offset

    override fun advance() {
//        LOG.warn("this.ctx.lexeme.offset " +  this.ctx.lexeme.offset)
//        LOG.warn("this.ctx.offset " +  this.ctx.offset)
        if (this.ctx.offset >= this.endOffset) {
            curTokenType = null
            state_ = 0
            return
        }

        if (pendingTokens.isNotEmpty()) {
            val t = pendingTokens.removeFirst()
            if (t == RnTypes.INDENT) {
                deep_ += 1
                state_ = deep_
            } else {
                deep_ -= 1
                state_ = deep_
            }
            this.ctx.lexeme.offset = this.ctx.offset
            curTokenType = t
            return
        } else {
            state_ = 0
        }

        this.ctx.execute(this.ctx)
        curTokenType = lexeme_to_element(this.ctx.lexeme.type)


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
            while (indentLevelStack.isNotEmpty() && indentLevelStack.last() > this.ctx.lexeme.deep) {
                indentLevelStack.pop()
                pendingTokens.add(RnTypes.DEDENT)
            }
        }
    }

    override fun getBufferSequence(): CharSequence = this.ctx.string
    override fun getBufferEnd(): Int = this.endOffset
}