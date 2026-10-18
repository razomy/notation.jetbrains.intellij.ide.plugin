package org.razomy.notation.parser.parse

import org.razomy.notation.parser.ctx.NotationLexeme
import org.razomy.notation.parser.lexeme.WithLexemeType

interface WithLexemes<T: org.razomy.notation.parser.lexeme.WithLexemeType> {
    var lexemes: MutableList<T>
}

fun <T, L> create_lexemes(ctx: T, lexeme: L): L where T : WithLexemes<L>, L : org.razomy.notation.parser.lexeme.WithLexemeType {
    ctx.lexemes.add(lexeme)
    return lexeme
}

fun <T : WithLexemes<org.razomy.notation.parser.ctx.NotationLexeme>> create_ctx_lexemes(ctx: T): T {
    ctx.lexemes = mutableListOf()
    return ctx
}
