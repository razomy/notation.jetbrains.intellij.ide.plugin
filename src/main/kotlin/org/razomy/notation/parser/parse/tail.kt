package org.razomy.notation.parser.parse

import org.razomy.notation.parser.ctx.NotationLexeme
import org.razomy.notation.parser.ctx.WithString
import org.razomy.notation.parser.lexeme.LexemeType
import org.razomy.notation.parser.lexeme.WithCreateLexemePipes
import org.razomy.notation.parser.lexeme.WithOffset
import org.razomy.notation.parser.lexeme.is_end

interface WithTail {
    var tail: String
}

fun <T, L> create_lexeme_tail(ctx: T, lexeme: L): L
        where T : WithTail, T : org.razomy.notation.parser.lexeme.WithOffset, T : org.razomy.notation.parser.ctx.WithString {
    ctx.tail = ctx.string.substring(ctx.offset)
    return lexeme
}

fun <T> create_ctx_tail(ctx: T): T where T : WithTail, T : org.razomy.notation.parser.ctx.WithString {
    ctx.tail = ctx.string
    return ctx
}

fun <T> tail_p(ctx: T): Boolean
        where T : org.razomy.notation.parser.lexeme.WithCreateLexemePipes<T, org.razomy.notation.parser.ctx.NotationLexeme>, T : org.razomy.notation.parser.ctx.WithString, T : WithTail, T : org.razomy.notation.parser.lexeme.WithOffset {
    if (_root_ide_package_.org.razomy.notation.parser.lexeme.is_end(ctx)) {
        return true
    }
    ctx.offset = ctx.string.length
    ctx.create_lexeme_fn(ctx,
        _root_ide_package_.org.razomy.notation.parser.ctx.NotationLexeme(_root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.tail_value)
    )
    return true
}