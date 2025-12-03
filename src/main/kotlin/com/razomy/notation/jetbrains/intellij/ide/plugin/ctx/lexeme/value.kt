package com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme

import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.WithString

interface WithValue {
    var value: String
}

fun <T, L> get_lexeme_value(ctx: T, lexeme: L): String
        where T : WithOffset, T : WithString, L : WithLexemeType, L : WithOffset {
    return ctx.string.substring(lexeme.offset, ctx.offset)
}

fun <T, L> create_lexeme_value(ctx: T, lexeme: L): L
        where T : WithOffset, T : WithString, L : WithLexemeType, L : WithOffset, L : WithValue {
    lexeme.value = get_lexeme_value(ctx, lexeme)
    return lexeme
}
