package org.razomy.notation.parser.parse

import org.razomy.notation.parser.lexeme.*
import org.razomy.notation.parser.ctx.*
import org.razomy.notation.parser.parse.*

fun lexeme_to_vrd_ctx(lexeme_iterator: (c: NotationCtx) -> NotationLexeme?): NotationCtx {
    val ctx = NotationCtx("")

    ctx.create_ctx_pipes = listOf(
        { c -> create_ctx_exceptions(c) },
        { c -> create_ctx_lexeme(c) },
        { c -> create_ctx_deep(c) },
        { c -> create_ctx_value_recursive_dict(c) },
    )

    ctx.execute = { c ->
        val next = lexeme_iterator(c)
        if (next != null) {
            create_lexeme_value_recursive_dict(c, next)
        }
    }

    return create_ctx(ctx)
}
