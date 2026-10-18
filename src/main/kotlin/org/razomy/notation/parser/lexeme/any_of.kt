package org.razomy.notation.parser.lexeme

import org.razomy.notation.parser.ctx.NotationLexeme
import org.razomy.notation.parser.ctx.WithString

fun <T> any_of(ctx: T, chars: List<Char>, type: LexemeType): Boolean
        where T : WithCreateLexemePipes<T, org.razomy.notation.parser.ctx.NotationLexeme>, T : org.razomy.notation.parser.ctx.WithString, T : WithOffset {
    if (chars.contains(curr_char(ctx))) {
        ctx.offset += 1
        ctx.create_lexeme_fn(ctx, _root_ide_package_.org.razomy.notation.parser.ctx.NotationLexeme(type))
        return true
    }
    return false
}


fun strings_any_index(str: String, chars: List<Char>, offset: Int): Int {
    var minIndex = -1
    for (char in chars) {
        val idx = str.indexOf(char, offset)
        if (idx != -1 && (minIndex == -1 || idx < minIndex)) {
            minIndex = idx
        }
    }
    return minIndex
}


fun <T> try_before_any_of(ctx: T, chars: List<Char>, type: LexemeType): Boolean
        where T : WithCreateLexemePipes<T, org.razomy.notation.parser.ctx.NotationLexeme>, T : org.razomy.notation.parser.ctx.WithString, T : WithOffset {
    val match = strings_any_index(ctx.string, chars, ctx.offset)
    if (match != -1 && match - ctx.offset > 0) {
        ctx.offset = match
        ctx.create_lexeme_fn(ctx, _root_ide_package_.org.razomy.notation.parser.ctx.NotationLexeme(type))
        return true
    }
    return false
}