package com.razomy.notation.jetbrains.intellij.ide.plugin.ctx

import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme.LexemePipe
import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme.LexemeType
import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme.SpaceInfo
import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme.WithCreateLexemePipes
import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme.WithExceptions
import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme.WithLexeme
import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme.WithLexemeType
import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme.WithLexemes
import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme.WithOffset
import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme.WithPrevDeep
import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme.WithPrevOffset
import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme.WithPrevSpaces
import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme.WithTail

interface WithCreateCtxPipes<T> {
    var create_ctx_pipes: List<(T) -> T>
}

fun <T : WithCreateCtxPipes<T>> create_ctx(ctx: T): T {
    return ctx.create_ctx_pipes.fold(ctx) { acc, pipe -> pipe(acc) }
}


class NotationCtx(
    override var string: String
) :
    WithCreateCtxPipes<NotationCtx>,
    WithCreateLexemePipes<NotationCtx, NotationLexeme>,
    WithLexeme,
    WithLexemes,
    WithOffset,
    WithString,
    WithTail,
    WithExceptions,
    WithPrevOffset,
    WithPrevDeep,
    WithPrevSpaces,
    WithExecute<NotationCtx> {
    override var create_ctx_pipes: List<(NotationCtx) -> NotationCtx> = listOf()
    override var create_lexeme_pipes: List<LexemePipe<NotationCtx, NotationLexeme>> = listOf()
    override lateinit var create_lexeme_fn: LexemePipe<NotationCtx, NotationLexeme>

    override var lexemes: MutableList<WithLexemeType> = mutableListOf()
    override var offset: Int = 0
    override var tail: String = ""
    override var exceptions: MutableList<Exception> = mutableListOf()
    override lateinit var exception_fn: (Exception) -> Unit
    override var prev_offset: Int = 0
    override var prev_deep: Int = 0
    override var prev_spaces: MutableList<SpaceInfo> = mutableListOf()
    override var execute: (NotationCtx) -> Unit = {}
    override lateinit var lexeme: WithLexemeType
}
