package com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx

import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.LexemePipe
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.ParentValueRecursiveDict
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.SpaceInfo
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.WithCreateLexemePipes
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.WithExceptions
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.WithLexeme
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.WithLexemeType
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.WithLexemes
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.WithOffset
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.WithPrevDeep
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.WithPrevOffset
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.WithPrevSpaces
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.WithTail
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.WithValueRecursiveDict

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
    WithLexeme<NotationLexeme>,
    WithLexemes<NotationLexeme>,
    WithOffset,
    WithString,
    WithTail,
    WithExceptions,
    WithPrevOffset,
    WithPrevDeep,
    WithPrevSpaces,
    ParentValueRecursiveDict,
    WithExecute<NotationCtx> {
    override var create_ctx_pipes: List<(NotationCtx) -> NotationCtx> = listOf()
    override var create_lexeme_pipes: List<LexemePipe<NotationCtx, NotationLexeme>> = listOf()
    override lateinit var create_lexeme_fn: LexemePipe<NotationCtx, NotationLexeme>

    override var lexemes: MutableList<NotationLexeme> = mutableListOf()
    override var offset: Int = 0
    override var tail: String = ""
    override var exceptions: MutableList<Exception> = mutableListOf()
    override lateinit var exception_fn: (Exception) -> Unit
    override var prev_offset: Int = 0
    override var prev_deep: Int = 0
    override var prev_spaces: MutableList<SpaceInfo> = mutableListOf()
    override var execute: (NotationCtx) -> Unit = {}
    override lateinit var lexeme: NotationLexeme
    override lateinit var parent_value_recursive: MutableList<WithValueRecursiveDict>
}
