package org.razomy.notation.parser.parse

import org.razomy.notation.parser.ctx.NotationLexeme
import org.razomy.notation.parser.ctx.WithString
import org.razomy.notation.parser.ctx.last
import org.razomy.notation.parser.lexeme.LexemeType
import org.razomy.notation.parser.lexeme.WithCreateLexemePipes
import org.razomy.notation.parser.lexeme.WithDeep
import org.razomy.notation.parser.lexeme.WithLexemeType
import org.razomy.notation.parser.lexeme.WithOffset
import org.razomy.notation.parser.lexeme.WithPrevDeep
import org.razomy.notation.parser.lexeme.curr_char
import org.razomy.notation.parser.ctx.last

data class SpaceInfo(val spaces: Int, val deep: Int)

interface WithPrevSpaces {
    var prev_spaces: MutableList<SpaceInfo>
}

fun <T : WithPrevSpaces> create_ctx_prev_spaces(ctx: T): T {
    ctx.prev_spaces = mutableListOf(SpaceInfo(0, 0))
    return ctx
}

val new_line_char = '\n'
val new_lines_list = listOf('\n')

fun next_margin_index(str: String, charStr: String, offset: Int): Int {
    var current = offset
    while (current < str.length && str[current].toString() == charStr) {
        current++
    }
    return current
}

fun <T> new_line_p(ctx: T): Boolean
        where T : org.razomy.notation.parser.lexeme.WithPrevDeep, T : org.razomy.notation.parser.lexeme.WithCreateLexemePipes<T, org.razomy.notation.parser.ctx.NotationLexeme>, T : WithPrevSpaces, T : org.razomy.notation.parser.ctx.WithString, T : org.razomy.notation.parser.lexeme.WithOffset {
    if (!new_lines_list.contains(_root_ide_package_.org.razomy.notation.parser.lexeme.curr_char(ctx))) {
        return false
    }
    ctx.offset += 1

    val spaces_offset = next_margin_index(ctx.string, " ", ctx.offset)
    val spaces = spaces_offset - ctx.offset
    val last_spaces = ctx.prev_spaces.last()
    ctx.offset = spaces_offset // update ctx offset

    if (spaces == last_spaces.spaces) {
        if (ctx.prev_deep > last_spaces.deep) {
            ctx.create_lexeme_fn(ctx,
                _root_ide_package_.org.razomy.notation.parser.ctx.NotationLexeme(_root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.new_line_break)
            )
        } else {
            ctx.create_lexeme_fn(ctx,
                _root_ide_package_.org.razomy.notation.parser.ctx.NotationLexeme(_root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.new_line_separate)
            )
        }
    } else if (spaces > last_spaces.spaces) {
        ctx.prev_spaces.add(SpaceInfo(spaces = spaces, deep = ctx.prev_deep + 1))
        ctx.create_lexeme_fn(ctx,
            _root_ide_package_.org.razomy.notation.parser.ctx.NotationLexeme(_root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.new_line_assign)
        )
    } else if (spaces < last_spaces.spaces) {
        val level_index = ctx.prev_spaces.indexOfFirst { it.spaces > spaces }
        if (level_index != -1) {
            val toRemove = ctx.prev_spaces.size - level_index
            for (i in 0 until toRemove) ctx.prev_spaces.removeAt(ctx.prev_spaces.lastIndex)
        }
        ctx.create_lexeme_fn(ctx,
            _root_ide_package_.org.razomy.notation.parser.ctx.NotationLexeme(_root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.new_line_break)
        )
    }
    return true
}

fun <T, L> create_lexeme_deep_new_line(ctx: T, lexeme: L): L
        where T : org.razomy.notation.parser.ctx.WithString, T : WithPrevSpaces, T : org.razomy.notation.parser.lexeme.WithOffset, T : org.razomy.notation.parser.lexeme.WithPrevDeep, L : org.razomy.notation.parser.lexeme.WithDeep, L : org.razomy.notation.parser.lexeme.WithLexemeType {
    if (lexeme.type == _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.new_line_separate) {
// no-op
    } else if (lexeme.type == _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.new_line_assign) {
        ctx.prev_deep = ctx.prev_spaces.last().deep
    } else if (lexeme.type == _root_ide_package_.org.razomy.notation.parser.lexeme.LexemeType.new_line_break) {
        ctx.prev_deep = ctx.prev_spaces.last().deep
    }
    lexeme.deep = ctx.prev_deep
    return lexeme
}
