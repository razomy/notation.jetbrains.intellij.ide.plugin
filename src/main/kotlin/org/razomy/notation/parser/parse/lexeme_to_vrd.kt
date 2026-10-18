package org.razomy.notation.parser.parse

import org.razomy.notation.parser.ctx.NotationCtx
import org.razomy.notation.parser.ctx.NotationLexeme
import org.razomy.notation.parser.ctx.create_ctx
import org.razomy.notation.parser.lexeme.create_ctx_deep
import org.razomy.notation.parser.lexeme.create_ctx_lexeme

fun lexeme_to_vrd_ctx(lexeme_iterator: (c: org.razomy.notation.parser.ctx.NotationCtx) -> org.razomy.notation.parser.ctx.NotationLexeme?): org.razomy.notation.parser.ctx.NotationCtx {
    val ctx = _root_ide_package_.org.razomy.notation.parser.ctx.NotationCtx("")

    ctx.create_ctx_pipes = listOf(
        { c -> create_ctx_exceptions(c) },
        { c -> _root_ide_package_.org.razomy.notation.parser.lexeme.create_ctx_lexeme(c) },
        { c -> _root_ide_package_.org.razomy.notation.parser.lexeme.create_ctx_deep(c) },
        { c -> create_ctx_value_recursive_dict(c) },
    )

    ctx.execute = { c ->
        val next = lexeme_iterator(c)
        if (next != null) {
            create_lexeme_value_recursive_dict(c, next)
        }
    }

    return _root_ide_package_.org.razomy.notation.parser.ctx.create_ctx(ctx)
}
