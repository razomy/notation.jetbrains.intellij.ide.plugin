package org.razomy.notation.parser.lexeme

import org.razomy.notation.parser.ctx.WithString


interface WithOffset {
    var offset: Int
}

fun <T : WithOffset> create_ctx_offset(ctx: T): T {
    ctx.offset = 0
    return ctx
}


fun <T> next_char(ctx: T): Char where T : org.razomy.notation.parser.ctx.WithString, T : WithOffset {
    return ctx.string[ctx.offset + 1]
}

fun <T> prev_char(ctx: T): Char where T : org.razomy.notation.parser.ctx.WithString, T : WithOffset {
    return ctx.string[ctx.offset - 1]
}

fun <T> curr_char(ctx: T): Char where T : org.razomy.notation.parser.ctx.WithString, T : WithOffset {
    return ctx.string[ctx.offset]
}

fun <T> is_end(ctx: T): Boolean where T : org.razomy.notation.parser.ctx.WithString, T : WithOffset {
    return ctx.offset == ctx.string.length
}