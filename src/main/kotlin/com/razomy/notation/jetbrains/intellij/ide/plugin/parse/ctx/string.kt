package com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx

import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.create_ctx_deep
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.create_ctx_exceptions
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.create_ctx_lexeme
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.create_ctx_lexemes
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.create_ctx_offset
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.create_ctx_prev_offset
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.create_ctx_prev_spaces
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.create_ctx_tail
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.create_lexeme_deep
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.create_lexeme_deep_new_line
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.create_lexeme_offset
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.create_lexeme_tail
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.create_lexeme_value
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.create_lexemes

fun create_notation_ctx(string: String): NotationCtx {
    val ctx = NotationCtx(string)

    ctx.create_ctx_pipes = listOf(
        { c -> create_ctx_exceptions(c) },
        { c -> create_ctx_offset(c) },
        { c -> create_ctx_prev_offset(c) },
        { c -> create_ctx_prev_spaces(c) },
        { c -> create_ctx_lexeme(c) },
        { c -> create_ctx_lexemes(c) },
        { c -> create_ctx_tail(c) },
        { c -> create_ctx_deep(c) },
    )

    ctx.create_lexeme_pipes = listOf(
        { c, l -> create_lexeme_offset(c, l) },
        { c, l -> create_lexeme_value(c, l) },
        { c, l -> create_lexemes(c, l) },
        { c, l -> create_lexeme_tail(c, l) },
        { c, l -> create_lexeme_deep(c, l) },
        { c, l -> create_lexeme_deep_new_line(c, l) },

        )

    ctx.execute = { c -> execute_pipes(c) }

    return create_ctx(ctx)
}

fun string_to_lexeme_tree(str: String): NotationCtx {
        val ctx = create_notation_ctx(str)
    try {
        ctx.execute(ctx)
//        if (ctx.exceptions.isNotEmpty()) {
//            MultipleException(ctx.exceptions)
//        }
        return ctx
    } catch (e: Exception) {
        throw e;
    }
    return ctx
}