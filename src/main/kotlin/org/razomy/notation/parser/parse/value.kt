package org.razomy.notation.parser.parse

import org.razomy.notation.parser.ctx.WithString
import org.razomy.notation.parser.lexeme.WithLexemeType
import org.razomy.notation.parser.lexeme.WithOffset

interface WithValue {
    var value: String
}

fun <T, L> get_lexeme_value(ctx: T, lexeme: L): String
        where T : org.razomy.notation.parser.lexeme.WithOffset, T : org.razomy.notation.parser.ctx.WithString, L : org.razomy.notation.parser.lexeme.WithLexemeType, L : org.razomy.notation.parser.lexeme.WithOffset {
    return ctx.string.substring(lexeme.offset, ctx.offset)
}

fun <T, L> create_lexeme_value(ctx: T, lexeme: L): L
        where T : org.razomy.notation.parser.lexeme.WithOffset, T : org.razomy.notation.parser.ctx.WithString, L : org.razomy.notation.parser.lexeme.WithLexemeType, L : org.razomy.notation.parser.lexeme.WithOffset, L : WithValue {
    lexeme.value = get_lexeme_value(ctx, lexeme)
    return lexeme
}
