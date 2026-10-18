package org.razomy.notation.parser.lexeme
import org.razomy.notation.parser.lexeme.*
import org.razomy.notation.parser.ctx.*
import org.razomy.notation.parser.parse.*

interface WithPrevDeep {
    var prev_deep: Int
}

interface WithDeep {
    var deep: Int
}

fun <T : WithPrevDeep> create_ctx_deep(ctx: T): T {
    ctx.prev_deep = 0
    return ctx
}

fun <T, L> create_lexeme_deep(ctx: T, lexeme: L): L
        where T : WithOffset, T : WithPrevDeep, L : WithDeep, L : WithLexemeType, T : WithExceptions {
    if (lexeme.type == LexemeType.assign) {
        ctx.prev_deep = ctx.prev_deep + 1
    } else if (lexeme.type == LexemeType.break_) {
        if (ctx.prev_deep == 0) {
            ctx.exception_fn(InvalidNotation("break out of context"))
        }
        ctx.prev_deep = ctx.prev_deep - 1
    }
    lexeme.deep = ctx.prev_deep
    return lexeme
}