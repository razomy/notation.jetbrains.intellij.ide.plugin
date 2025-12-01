package com.razomy.notation.jetbrains.intellij.ide.plugin.lexer.v2

fun <T> create_lexeme_tail(ctx: T, lexeme: Lexeme): Lexeme
        where T : WithTail, T : WithOffset, T : WithString {
    ctx.tail = ctx.string.substring(ctx.offset)
    return lexeme
}

fun <T> create_ctx_tail(ctx: T): T where T : WithTail, T : WithString {
    ctx.tail = ctx.string
    return ctx
}

fun <T> tail_p(ctx: T): Boolean
        where T : WithCreateLexemePipes, T : WithLexemes, T : WithString, T : WithTail, T : WithOffset {
    if (is_end(ctx)) {
        return true
    }
    ctx.offset = ctx.string.length
    create_lexeme(ctx, NotationLexeme(LexemeType.tail_value))
    return true
}