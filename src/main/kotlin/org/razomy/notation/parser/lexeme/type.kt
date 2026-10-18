package org.razomy.notation.parser.lexeme

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
}

interface WithLexemeType {
    var type: LexemeType
}