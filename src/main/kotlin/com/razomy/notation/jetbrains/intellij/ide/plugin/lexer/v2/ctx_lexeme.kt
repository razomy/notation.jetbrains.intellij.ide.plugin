package com.razomy.notation.jetbrains.intellij.ide.plugin.lexer.v2

enum class LexemeType(val value: String) {
    root("root"),
    value_("value"),
    tail_value("tail_value"),
    multiline_value("multiline_value"),
    assign("assign"),
    new_line_assign("new_line_assign"),
    new_line_break("new_line_break"),
    break_("break"),
    separate("separate"),
    new_line_separate("new_line_separate"),
    not_closed_error_multiline_value("not_closed_error_multiline_value")
}

interface Lexeme {
    var type: LexemeType
}

interface WithLexemes {
    var lexemes: MutableList<Lexeme>
}

typealias LexemePipe<T> = (Any, T) -> T

interface WithCreateLexemePipes {
    var create_lexeme_pipes: List<LexemePipe<Lexeme>>
}

fun <T : Lexeme> create_lexeme(ctx: WithCreateLexemePipes, lexeme: T) {
    val ctxWithLexemes = ctx as? WithLexemes ?: throw ArgumentException("Ctx must implement WithLexemes")

    val lexeme_pipped = ctx.create_lexeme_pipes.fold(lexeme) { acc, f ->
        f(ctx, acc) as T
    }
    ctxWithLexemes.lexemes.add(lexeme_pipped)
}

fun <T : WithLexemes> create_ctx_lexeme(ctx: T): T {
    ctx.lexemes = mutableListOf()
    return ctx
}