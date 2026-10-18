package org.razomy.notation.parser.parse

import org.razomy.notation.parser.ctx.ArgumentException
import org.razomy.notation.parser.ctx.Exception
import org.razomy.notation.parser.ctx.NotationLexeme
import org.razomy.notation.parser.ctx.WithString
import org.razomy.notation.parser.lexeme.LexemeType
import org.razomy.notation.parser.lexeme.WithCreateLexemePipes
import org.razomy.notation.parser.lexeme.WithLexeme
import org.razomy.notation.parser.lexeme.WithOffset

interface WithExceptions {
    var exceptions: MutableList<org.razomy.notation.parser.ctx.Exception>
    var exception_fn: (org.razomy.notation.parser.ctx.Exception) -> Unit
}

open class InvalidNotation(message: String) : org.razomy.notation.parser.ctx.ArgumentException(message)

fun <T : WithExceptions> throwE(ctx: T, e: org.razomy.notation.parser.ctx.Exception): T {
    throw e
}

fun <T : WithExceptions> create_ctx_exceptions(ctx: T): T {
    ctx.exceptions = mutableListOf()
    ctx.exception_fn = { e -> throwE(ctx, e) }
    return ctx
}

fun <Ctx> error_p(ctx: Ctx): Boolean
        where Ctx : org.razomy.notation.parser.lexeme.WithCreateLexemePipes<Ctx, org.razomy.notation.parser.ctx.NotationLexeme>,
              Ctx : org.razomy.notation.parser.lexeme.WithOffset,
              Ctx : org.razomy.notation.parser.lexeme.WithLexeme<org.razomy.notation.parser.ctx.NotationLexeme>,
              Ctx : org.razomy.notation.parser.ctx.WithString {


    // TODO: build proper flow
    ctx.offset = ctx.string.length
    ctx.lexeme.type = _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.error
    var offset = ctx.lexeme.offset
    ctx.create_lexeme_fn(ctx,
        _root_ide_package_.org.razomy.notation.parser.ctx.NotationLexeme(_root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.error)
    )
    ctx.lexeme.offset = offset
    return true
}