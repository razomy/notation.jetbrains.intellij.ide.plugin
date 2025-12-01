package com.razomy.notation.jetbrains.intellij.ide.plugin.lexer.v2

class NotationLexeme : Lexeme, MultilineLexeme, BoundY, WithChildren<NotationLexeme>, NodeBound, WithValue,
    WithDeep, WithOffset {
    override var type: LexemeType
    override var offset: Int = 0
    override var start_y: Int = 0
    override var finish_y: Int = 0
    override var start_x: Int = 0
    override var finish_x: Int = 0
    override var children: MutableList<NotationLexeme> = mutableListOf()
    override var deep: Int = 0
    override var value: String = ""
    override var node_finish_x: Int = 0
    override var node_finish_y: Int = 0

    constructor(type: LexemeType) {
        this.type = type
    }
}

const val assign_char = ":"
const val separate_char = ","
const val break_char = ";"

val reserved_value_lexemes = listOf(assign_char, separate_char, break_char, new_line_char.toString())

fun assign_p(ctx: NotationCtx) = any_of(ctx, listOf(assign_char), LexemeType.assign)
fun separate_p(ctx: NotationCtx) = any_of(ctx, listOf(separate_char), LexemeType.separate)
fun break_p(ctx: NotationCtx) = any_of(ctx, listOf(break_char), LexemeType.break_)
fun value_p(ctx: NotationCtx) = try_before_any_of(ctx, reserved_value_lexemes, LexemeType.value_)

interface WithExecute<T> {
    var execute: (T) -> Unit
}

fun execute_pipes_logic(ctx: NotationCtx) {
    while (true) {
        //TODO: research this if
        if (ctx.offset == ctx.string.length) {
            break
        }
        if (ctx.parent_deeps.size === 0) {
            break
        }

        if (ctx.exceptions.size !== 0) {
            break
        }

        val anyMatched =
            try_multiline_value_p(ctx) ||
                    value_p(ctx) ||
                    assign_p(ctx) ||
                    separate_p(ctx) ||
                    break_p(ctx) ||
                    new_line_p(ctx)

        if (!anyMatched) break
    }
    tail_p(ctx)
}
