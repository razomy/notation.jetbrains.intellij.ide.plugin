package com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.parse

import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.ctx.ArgumentException
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.ctx.Exception
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.ctx.NotationLexeme
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.ctx.WithString
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.LexemeType
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.WithCreateLexemePipes
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.WithLexeme
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.WithOffset

interface WithExceptions {
    var exceptions: MutableList<Exception>
    var exception_fn: (Exception) -> Unit
}

open class InvalidNotation(message: String) : ArgumentException(message)

fun <T : WithExceptions> throwE(ctx: T, e: Exception): T {
    throw e
}

fun <T : WithExceptions> create_ctx_exceptions(ctx: T): T {
    ctx.exceptions = mutableListOf()
    ctx.exception_fn = { e -> throwE(ctx, e) }
    return ctx
}

fun <Ctx> error_p(ctx: Ctx): Boolean
        where Ctx : WithCreateLexemePipes<Ctx, NotationLexeme>,
              Ctx : WithOffset,
              Ctx : WithLexeme<NotationLexeme>,
              Ctx : WithString {


    // TODO: build proper flow
    ctx.offset = ctx.string.length
    ctx.lexeme.type = LexemeType.error
    var offset = ctx.lexeme.offset
    ctx.create_lexeme_fn(ctx, NotationLexeme(LexemeType.error))
    ctx.lexeme.offset = offset
    return true
}