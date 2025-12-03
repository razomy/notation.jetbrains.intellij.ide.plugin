package com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme

interface WithLexemes {
    var lexemes: MutableList<WithLexemeType>
}

fun <T, L> create_lexemes(ctx: T, lexeme: L): L where T : WithLexemes, L : WithLexemeType {
    ctx.lexemes.add(lexeme)
    return lexeme
}

fun <T : WithLexemes> create_ctx_lexemes(ctx: T): T {
    ctx.lexemes = mutableListOf()
    return ctx
}
