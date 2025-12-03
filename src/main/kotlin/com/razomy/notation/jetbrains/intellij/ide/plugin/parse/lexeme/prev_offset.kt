package com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme

interface WithPrevOffset {
    var prev_offset: Int
}

fun <T : WithPrevOffset> create_ctx_prev_offset(ctx: T): T {
    ctx.prev_offset = 0
    return ctx
}

fun <T, L> create_lexeme_offset(ctx: T, lexeme: L): L
        where T : WithOffset, T : WithPrevOffset, L : WithLexemeType, L : WithOffset {
    lexeme.offset = ctx.prev_offset
    ctx.prev_offset = ctx.offset
    return lexeme
}