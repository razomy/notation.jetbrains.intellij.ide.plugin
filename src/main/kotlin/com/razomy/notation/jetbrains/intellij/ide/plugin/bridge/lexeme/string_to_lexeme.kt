package com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme

import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.ctx.Exception
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.ctx.NotationCtx
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.ctx.create_ctx
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.parse.create_ctx_exceptions
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.parse.create_ctx_prev_spaces
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.parse.create_lexeme_deep_new_line

fun string_to_lexeme_ctx(string: String): NotationCtx {
    val ctx = NotationCtx(string)

    ctx.create_ctx_pipes = listOf(
        { c -> create_ctx_exceptions(c) },
        { c -> create_ctx_offset(c) },
        { c -> create_ctx_prev_offset(c) },
        { c -> create_ctx_prev_spaces(c) },
        { c -> create_ctx_lexeme(c) },
//        { c -> create_ctx_lexemes(c) },
//        { c -> create_ctx_tail(c) },
        { c -> create_ctx_deep(c) },
//        { c -> create_ctx_value_recursive_dict(c) },
    )

    ctx.create_lexeme_pipes = listOf(
        { c, l -> create_lexeme_offset(c, l) },
//        { c, l -> create_lexeme_value(c, l) },
//        { c, l -> create_lexemes(c, l) },
//        { c, l -> create_lexeme_tail(c, l) },
        { c, l -> create_lexeme_deep(c, l) },
        { c, l -> create_lexeme_deep_new_line(c, l) },
//        { c, l -> create_lexeme_value_recursive_dict(c, l) },

        )

    ctx.execute = { c -> iterate_pipe(c) }

    return create_ctx(ctx)
}
