package org.razomy.notation.parser.parse

import org.razomy.notation.parser.lexeme.*
import org.razomy.notation.parser.ctx.*
import org.razomy.notation.parser.parse.*
interface WithLexemes<T : WithLexemeType> {
    var lexemes: MutableList<T>
}

fun <T, L> create_lexemes(
    ctx: T,
    lexeme: L
): L where T : WithLexemes<L>, L : WithLexemeType {
    ctx.lexemes.add(lexeme)
    return lexeme
}

fun <T : WithLexemes<NotationLexeme>> create_ctx_lexemes(ctx: T): T {
    ctx.lexemes = mutableListOf()
    return ctx
}
