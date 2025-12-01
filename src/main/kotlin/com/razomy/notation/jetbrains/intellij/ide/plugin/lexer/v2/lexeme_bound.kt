package com.razomy.notation.jetbrains.intellij.ide.plugin.lexer.v2


interface BoundY {
    var start_y: Int
    var finish_y: Int
    var start_x: Int
    var finish_x: Int
}

interface WithPrevStartXy {
    var prev_x_offset: Int
    var prev_start_y: Int
}

fun <T : WithPrevStartXy> create_ctx_prev_bound(ctx: T): T {
    ctx.prev_x_offset = 0
    ctx.prev_start_y = 0
    return ctx
}

fun string_count(str: String, char: String, start: Int, end: Int): Int {
    var count = 0
    var idx = start
    while (idx < end) {
        val found = str.indexOf(char, idx)
        if (found == -1 || found >= end) break
        count++
        idx = found + 1
    }
    return count
}

fun last_index(str: String, char: String, start: Int, end: Int): Int {
    val substr = str.substring(start, end)
    val idx = substr.lastIndexOf(char)
    return if (idx == -1) -1 else start + idx
}

fun <T, L> create_lexeme_bound(ctx: T, lexeme: L): L
        where T : WithLexemes, T : WithPrevStartXy, T : WithString, T : WithOffset,
              L : WithOffset, L : WithValue, L : BoundY, L : Lexeme {

    val new_line_str = "\n"

    lexeme.start_y = ctx.prev_start_y
    val new_line_count = string_count(ctx.string, new_line_str, lexeme.offset, ctx.offset)
    ctx.prev_start_y += new_line_count
    lexeme.finish_y = ctx.prev_start_y

    lexeme.start_x = lexeme.offset - ctx.prev_x_offset
    val x_last = last_index(ctx.string, new_line_str, lexeme.offset, ctx.offset)
    if (x_last != -1) {
        ctx.prev_x_offset = x_last + 1
    }

    val delta_from_end = ctx.offset - ctx.prev_x_offset
    lexeme.finish_x = delta_from_end

    return lexeme
}
