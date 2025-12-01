package com.razomy.notation.jetbrains.intellij.ide.plugin.lexer.v2

interface WithExceptions {
    var exceptions: MutableList<Exception>
}

fun <T : WithExceptions> create_ctx_exceptions(ctx: T): T {
    ctx.exceptions = mutableListOf()
    return ctx
}