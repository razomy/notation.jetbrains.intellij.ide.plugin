package com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme

interface WithLexeme {
    var lexeme: WithLexemeType
}

typealias LexemePipe<T, L> = (ctx: T, lexeme: L) -> L

interface WithCreateLexemePipes<T, L> {
    val create_lexeme_pipes: List<LexemePipe<T, L>>
    var create_lexeme_fn: LexemePipe<T, L>
}


fun <T, L> create_lexeme(
    ctx: T,
    lexeme: L
): L where T : WithLexeme, T : WithCreateLexemePipes<T, L>, L : WithLexemeType {
    ctx.lexeme = lexeme

    val lexemePiped = ctx.create_lexeme_pipes.fold(lexeme) { acc, pipe ->
        pipe(ctx, acc)
    }

    return lexemePiped
}

fun <T, L> create_ctx_lexeme(ctx: T): T
        where T : WithLexeme, T : WithCreateLexemePipes<T, L>, L : WithLexemeType {

// TODO:   ctx.lexeme = null
    ctx.create_lexeme_fn = ::create_lexeme
    return ctx
}