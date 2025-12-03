package com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme

import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.WithString


interface WithOffset {
    var offset: Int
}

fun <T : WithOffset> create_ctx_offset(ctx: T): T {
    ctx.offset = 0
    return ctx
}


fun <T> next_char(ctx: T): Char where T : WithString, T : WithOffset {
    return ctx.string[ctx.offset + 1]
}

fun <T> prev_char(ctx: T): Char where T : WithString, T : WithOffset {
    return ctx.string[ctx.offset - 1]
}

fun <T> curr_char(ctx: T): Char where T : WithString, T : WithOffset {
    return ctx.string[ctx.offset]
}

fun <T> is_end(ctx: T): Boolean where T : WithString, T : WithOffset {
    return ctx.offset == ctx.string.length
}