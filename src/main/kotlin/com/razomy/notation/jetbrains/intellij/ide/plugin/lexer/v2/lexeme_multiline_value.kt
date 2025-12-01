package com.razomy.notation.jetbrains.intellij.ide.plugin.lexer.v2


const val multiline_char = '`'

interface MultilineLexeme : Lexeme

interface MultilineWithLexemes<T> : WithLexemes {
}

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
        where T : WithCreateLexemePipes, T : WithString, T : WithOffset, T : WithLexemes {
    if (is_open(ctx)) {
        ctx.offset += 1
        return true
    } else {
        return false
    }
}

fun <T> try_finish_multiline_pipe(ctx: T): Boolean
        where T : WithCreateLexemePipes, T : WithString, T : WithOffset, T : WithLexemes {
    if (is_close(ctx)) {
        ctx.offset += 1
        create_lexeme(ctx, NotationLexeme(LexemeType.multiline_value))

        return true
    } else {
        return false
    }
}

fun <T> finish_error_multiline_pipe(ctx: T): Boolean
        where T : WithCreateLexemePipes, T : WithString, T : WithOffset, T : WithLexemes {
    create_lexeme(ctx, NotationLexeme(LexemeType.not_closed_error_multiline_value))
    return true
}

fun <T> try_multiline_value_p(ctx: T): Boolean
        where T : WithCreateLexemePipes, T : WithString, T : WithOffset, T : WithLexemes {
    return is_start_multiline_pipe(ctx) &&
            (try_finish_multiline_pipe(ctx) || finish_error_multiline_pipe(ctx))
}