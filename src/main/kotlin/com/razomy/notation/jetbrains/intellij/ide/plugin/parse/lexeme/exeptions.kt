package com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme

import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx.ArgumentException
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx.Exception
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx.NotationLexeme
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx.WithString

interface WithExceptions {
    var exceptions: MutableList<Exception>
    var exception_fn: (Exception) -> Unit
}

open class InvalidNotation(message: String) : ArgumentException(message)


fun <T : WithExceptions> create_ctx_exceptions(ctx: T): T {
    ctx.exceptions = mutableListOf()
    ctx.exception_fn = { e -> ctx.exceptions.add(e) }
    return ctx
}

fun <Ctx, L : WithLexemeType> create_lexeme_error(ctx: Ctx, lexeme: L): L
        where Ctx : WithCreateLexemePipes<Ctx, NotationLexeme>,
              Ctx : WithExceptions,
              Ctx : WithOffset,
              Ctx : WithString {

    if (ctx.exceptions.isNotEmpty()) {
        ctx.offset = ctx.string.length
        ctx.create_lexeme_fn(ctx, NotationLexeme(LexemeType.error))
    }

    return lexeme
}