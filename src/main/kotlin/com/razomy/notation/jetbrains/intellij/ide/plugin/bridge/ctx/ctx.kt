package com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.ctx

import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.LexemePipe
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.parse.ParentValueRecursiveDict
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.parse.SpaceInfo
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.WithCreateLexemePipes
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.WithExecute
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.parse.WithExceptions
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.WithLexeme
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.parse.WithLexemes
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.WithOffset
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.WithPrevDeep
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.WithPrevOffset
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.parse.WithPrevSpaces
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.parse.WithTail
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.parse.WithValue
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.parse.WithValueRecursiveDict

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
    WithValue,
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
    override var value: String = ""
}
