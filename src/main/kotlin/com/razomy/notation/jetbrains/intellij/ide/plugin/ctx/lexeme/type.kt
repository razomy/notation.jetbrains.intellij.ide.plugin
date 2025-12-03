package com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme

enum class LexemeType(val value: String) {

    value_("value"),
    tail_value("tail_value"),
    multiline_value("multiline_value"),
    assign("assign"),
    error("error"),
    new_line_assign("new_line_assign"),
    new_line_break("new_line_break"),
    break_("break"),
    separate("separate"),
    new_line_separate("new_line_separate"),
    not_closed_error_multiline_value("not_closed_error_multiline_value"),
    //TODO?
    comment("comment"),
}

interface WithLexemeType {
    var type: LexemeType
}