package org.razomy.notation.parser.lexeme

import org.razomy.notation.parser.lexeme.*
import org.razomy.notation.parser.ctx.*
import org.razomy.notation.parser.parse.*
const val assign_char = ':'
const val separate_char = ','
const val break_char = ';'

val reserved_value_lexemes = listOf(
    assign_char, separate_char, break_char,
    new_line_char
)

fun assign_p(ctx: NotationCtx) = any_of(ctx, listOf(assign_char), LexemeType.assign)
fun separate_p(ctx: NotationCtx) =
    any_of(ctx, listOf(separate_char), LexemeType.separate)

fun break_p(ctx: NotationCtx) = any_of(ctx, listOf(break_char), LexemeType.break_)
fun value_p(ctx: NotationCtx) =
    try_before_any_of(ctx, reserved_value_lexemes, LexemeType.value_)

interface WithExecute<T> {
    var execute: (T) -> Unit
}


fun iterate_pipe(ctx: NotationCtx): Boolean {
    try {

        if (
            try_multiline_value_p(ctx) ||
            value_p(ctx) ||
            assign_p(ctx) ||
            separate_p(ctx) ||
            break_p(ctx) ||
            new_line_p(ctx)
        ) {
            return true
        }

        tail_p(ctx)
        return false
    } catch (e: Exception) {
        error_p(ctx)
        return false
    }
}

fun execute_pipes(ctx: NotationCtx): Boolean {
    while (true) {
        if (iterate_pipe(ctx)) {
            continue
        }
        break
    }
    return false
}
