package org.razomy.notation.parser.lexeme

import org.razomy.notation.parser.ctx.NotationLexeme

interface WithLexeme<T:WithLexemeType> {
    var lexeme: T
}

typealias LexemePipe<T, L> = (ctx: T, lexeme: L) -> L

interface WithCreateLexemePipes<T, L> {
    val create_lexeme_pipes: List<LexemePipe<T, L>>
    var create_lexeme_fn: LexemePipe<T, L>
}


fun <T, L> create_lexeme(
    ctx: T,
    lexeme: L
): L where T : WithLexeme<L>, T : WithCreateLexemePipes<T, L>, L : WithLexemeType {
    ctx.lexeme = lexeme

    val lexemePiped = ctx.create_lexeme_pipes.fold(lexeme) { acc, pipe ->
        pipe(ctx, acc)
    }

    return lexemePiped
}

fun <T, L> create_ctx_lexeme(ctx: T): T
        where T : WithLexeme<L>, T : WithCreateLexemePipes<T, L>, L : WithLexemeType {

    ctx.lexeme = _root_ide_package_.org.razomy.notation.parser.ctx.NotationLexeme(LexemeType.error) as L
    ctx.create_lexeme_fn = ::create_lexeme
    return ctx
}