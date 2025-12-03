package com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme

import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx.NotationLexeme

interface WithLexemes<T:WithLexemeType> {
    var lexemes: MutableList<T>
}

fun <T, L> create_lexemes(ctx: T, lexeme: L): L where T : WithLexemes<L>, L : WithLexemeType {
    ctx.lexemes.add(lexeme)
    return lexeme
}

fun <T : WithLexemes<NotationLexeme>> create_ctx_lexemes(ctx: T): T {
    ctx.lexemes = mutableListOf()
    return ctx
}
