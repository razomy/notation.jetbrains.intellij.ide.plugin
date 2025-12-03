package com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme

import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx.NotationLexeme
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx.WithString


const val multiline_char = '`'

interface MultilineLexeme : WithLexemeType

fun <T> is_open(ctx: T): Boolean where T : WithString, T : WithOffset {
    return curr_char(ctx) == multiline_char
}

fun <T> is_close(ctx: T): Boolean where T : WithString, T : WithOffset {
    while (true) {
        val close = ctx.string.indexOf(multiline_char, ctx.offset)
        if (close == -1) {
            return false
        } else {
            ctx.offset = close
        }

        if (prev_char(ctx) == '\\') {
            ctx.offset += 1
            continue
        } else {
            return true
        }
    }
}

fun <T> is_start_multiline_pipe(ctx: T): Boolean
        where T : WithCreateLexemePipes<T,NotationLexeme>, T : WithString, T : WithOffset {
    if (is_open(ctx)) {
        ctx.offset += 1
        return true
    } else {
        return false
    }
}

fun <T> try_finish_multiline_pipe(ctx: T): Boolean
        where T : WithCreateLexemePipes<T,NotationLexeme>, T : WithString, T : WithOffset {
    if (is_close(ctx)) {
        ctx.offset += 1
        ctx.create_lexeme_fn(ctx, NotationLexeme(LexemeType.multiline_value))

        return true
    } else {
        return false
    }
}

fun <T> finish_error_multiline_pipe(ctx: T): Boolean
        where T : WithCreateLexemePipes<T,NotationLexeme>, T : WithString, T : WithOffset{
    ctx.create_lexeme_fn(ctx, NotationLexeme(LexemeType.not_closed_error_multiline_value))
    return true
}

fun <T> try_multiline_value_p(ctx: T): Boolean
        where T : WithCreateLexemePipes<T,NotationLexeme>, T : WithString, T : WithOffset {
    return is_start_multiline_pipe(ctx) &&
            (try_finish_multiline_pipe(ctx) || finish_error_multiline_pipe(ctx))
}