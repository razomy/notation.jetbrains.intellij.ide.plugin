package com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme

import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx.NotationLexeme
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx.WithString

interface WithTail {
    var tail: String
}

fun <T, L> create_lexeme_tail(ctx: T, lexeme: L): L
        where T : WithTail, T : WithOffset, T : WithString {
    ctx.tail = ctx.string.substring(ctx.offset)
    return lexeme
}

fun <T> create_ctx_tail(ctx: T): T where T : WithTail, T : WithString {
    ctx.tail = ctx.string
    return ctx
}

fun <T> tail_p(ctx: T): Boolean
        where T : WithCreateLexemePipes<T, NotationLexeme>, T : WithString, T : WithTail, T : WithOffset {
    if (is_end(ctx)) {
        return true
    }
    ctx.offset = ctx.string.length
    ctx.create_lexeme_fn(ctx, NotationLexeme(LexemeType.tail_value))
    return true
}