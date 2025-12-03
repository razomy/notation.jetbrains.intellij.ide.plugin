package com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme

import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx.NotationLexeme
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx.WithString
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx.last

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
        where T : WithPrevDeep, T : WithCreateLexemePipes<T,NotationLexeme>, T : WithPrevSpaces, T : WithString, T : WithOffset {
    if (!new_lines_list.contains(curr_char(ctx))) {
        return false
    }
    ctx.offset += 1

    val spaces_offset = next_margin_index(ctx.string, " ", ctx.offset)
    val spaces = spaces_offset - ctx.offset
    val last_spaces = ctx.prev_spaces.last()
    ctx.offset = spaces_offset // update ctx offset

    if (spaces == last_spaces.spaces) {
        if (ctx.prev_deep > last_spaces.deep) {
            ctx.create_lexeme_fn(ctx, NotationLexeme(LexemeType.new_line_break))
        } else {
            ctx.create_lexeme_fn(ctx, NotationLexeme(LexemeType.new_line_separate))
        }
    } else if (spaces > last_spaces.spaces) {
        ctx.prev_spaces.add(SpaceInfo(spaces = spaces, deep = ctx.prev_deep + 1))
        ctx.create_lexeme_fn(ctx, NotationLexeme(LexemeType.new_line_assign))
    } else if (spaces < last_spaces.spaces) {
        val level_index = ctx.prev_spaces.indexOfFirst { it.spaces > spaces }
        if (level_index != -1) {
            val toRemove = ctx.prev_spaces.size - level_index
            for (i in 0 until toRemove) ctx.prev_spaces.removeAt(ctx.prev_spaces.lastIndex)
        }
        ctx.create_lexeme_fn(ctx, NotationLexeme(LexemeType.new_line_break))
    }
    return true
}

fun <T, L> create_lexeme_deep_new_line(ctx: T, lexeme: L): L
        where T : WithString, T : WithPrevSpaces, T : WithOffset, T : WithPrevDeep, L : WithDeep, L : WithLexemeType {
    if (lexeme.type == LexemeType.new_line_separate) {
// no-op
    } else if (lexeme.type == LexemeType.new_line_assign) {
        ctx.prev_deep = ctx.prev_spaces.last().deep
    } else if (lexeme.type == LexemeType.new_line_break) {
        ctx.prev_deep = ctx.prev_spaces.last().deep
    }
    lexeme.deep = ctx.prev_deep
    return lexeme
}
