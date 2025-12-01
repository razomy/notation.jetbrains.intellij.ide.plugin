package com.razomy.notation.jetbrains.intellij.ide.plugin.lexer.v2

interface WithValue {
    var value: String
}

interface WithPrevOffset {
    var prev_offset: Int
}

fun <T, L> get_lexeme_value(ctx: T, lexeme: L): String
        where T : WithOffset, T : WithString, L : Lexeme, L : WithOffset {
    return ctx.string.substring(lexeme.offset, ctx.offset)
}

fun <T, L> create_lexeme_value(ctx: T, lexeme: L): L
        where T : WithOffset, T : WithString, T : WithPrevOffset, L : Lexeme, L : WithOffset, L : WithValue {
    lexeme.value = get_lexeme_value(ctx, lexeme)
    return lexeme
}
