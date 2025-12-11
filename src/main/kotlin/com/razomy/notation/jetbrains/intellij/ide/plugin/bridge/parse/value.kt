package com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.parse

import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.ctx.WithString
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.WithLexemeType
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.WithOffset

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
