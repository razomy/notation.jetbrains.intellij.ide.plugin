package com.razomy.notation.jetbrains.intellij.ide.plugin.lexer.v2

fun strings_any_index(str: String, chars: List<String>, offset: Int): Int {
    var minIndex = -1
    for (char in chars) {
        val idx = str.indexOf(char, offset)
        if (idx != -1 && (minIndex == -1 || idx < minIndex)) {
            minIndex = idx
        }
    }
    return minIndex
}

interface WithTail {
    var tail: String
}

fun <T> try_end(ctx: T): Boolean
        where T : WithCreateLexemePipes, T : WithLexemes, T : WithString, T : WithTail, T : WithOffset {
    if (is_end(ctx)) {
        return true
    }
    return false
}

fun <T> any_of(ctx: T, chars: List<String>, type: LexemeType): Boolean
        where T : WithCreateLexemePipes, T : WithLexemes, T : WithString, T : WithTail, T : WithOffset {
    if (offset_safe_check(ctx) && chars.contains(curr_char(ctx).toString())) {
        ctx.offset += 1
        create_lexeme(ctx, NotationLexeme(type))
        return true
    }
    return false
}

fun <T> offset_safe_check(ctx: T): Boolean where T : WithString, T : WithOffset {
    return ctx.offset < ctx.string.length
}

fun <T> try_before_any_of(ctx: T, chars: List<String>, type: LexemeType): Boolean
        where T : WithCreateLexemePipes, T : WithLexemes, T : WithString, T : WithTail, T : WithOffset {
    val match = strings_any_index(ctx.string, chars, ctx.offset)
    if (match != -1 && match - ctx.offset > 0) {
        ctx.offset = match
        create_lexeme(ctx, NotationLexeme(type))
        return true
    }
    return false
}