package com.razomy.notation.jetbrains.intellij.ide.plugin.lexer.v2

interface WithCreateCtxPipes<T> {
    var create_ctx_pipes: List<(T) -> T>
}

fun <T : WithCreateCtxPipes<T>> create_ctx(ctx: T): T {
    return ctx.create_ctx_pipes.fold(ctx) { acc, pipe -> pipe(acc) }
}