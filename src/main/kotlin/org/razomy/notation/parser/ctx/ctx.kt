package org.razomy.notation.parser.ctx

import org.razomy.notation.parser.lexeme.LexemePipe
import org.razomy.notation.parser.parse.ParentValueRecursiveDict
import org.razomy.notation.parser.parse.SpaceInfo
import org.razomy.notation.parser.lexeme.WithCreateLexemePipes
import org.razomy.notation.parser.lexeme.WithExecute
import org.razomy.notation.parser.parse.WithExceptions
import org.razomy.notation.parser.lexeme.WithLexeme
import org.razomy.notation.parser.parse.WithLexemes
import org.razomy.notation.parser.lexeme.WithOffset
import org.razomy.notation.parser.lexeme.WithPrevDeep
import org.razomy.notation.parser.lexeme.WithPrevOffset
import org.razomy.notation.parser.parse.WithPrevSpaces
import org.razomy.notation.parser.parse.WithTail
import org.razomy.notation.parser.parse.WithValue
import org.razomy.notation.parser.parse.WithValueRecursiveDict

interface WithCreateCtxPipes<T> {
    var create_ctx_pipes: List<(T) -> T>
}

fun <T : WithCreateCtxPipes<T>> create_ctx(ctx: T): T {
    return ctx.create_ctx_pipes.fold(ctx) { acc, pipe -> pipe(acc) }
}


class NotationCtx(
    override var string: String
) :
    WithCreateCtxPipes<NotationCtx>,
    org.razomy.notation.parser.lexeme.WithCreateLexemePipes<NotationCtx, NotationLexeme>,
    org.razomy.notation.parser.lexeme.WithLexeme<NotationLexeme>,
    org.razomy.notation.parser.parse.WithLexemes<NotationLexeme>,
    org.razomy.notation.parser.lexeme.WithOffset,
    WithString,
    org.razomy.notation.parser.parse.WithValue,
    org.razomy.notation.parser.parse.WithTail,
    org.razomy.notation.parser.parse.WithExceptions,
    org.razomy.notation.parser.lexeme.WithPrevOffset,
    org.razomy.notation.parser.lexeme.WithPrevDeep,
    org.razomy.notation.parser.parse.WithPrevSpaces,
    org.razomy.notation.parser.parse.ParentValueRecursiveDict,
    org.razomy.notation.parser.lexeme.WithExecute<NotationCtx> {
    override var create_ctx_pipes: List<(NotationCtx) -> NotationCtx> = listOf()
    override var create_lexeme_pipes: List<org.razomy.notation.parser.lexeme.LexemePipe<NotationCtx, NotationLexeme>> = listOf()
    override lateinit var create_lexeme_fn: org.razomy.notation.parser.lexeme.LexemePipe<NotationCtx, NotationLexeme>

    override var lexemes: MutableList<NotationLexeme> = mutableListOf()
    override var offset: Int = 0
    override var tail: String = ""
    override var exceptions: MutableList<Exception> = mutableListOf()
    override lateinit var exception_fn: (Exception) -> Unit
    override var prev_offset: Int = 0
    override var prev_deep: Int = 0
    override var prev_spaces: MutableList<org.razomy.notation.parser.parse.SpaceInfo> = mutableListOf()
    override var execute: (NotationCtx) -> Unit = {}
    override lateinit var lexeme: NotationLexeme
    override lateinit var parent_value_recursive: MutableList<org.razomy.notation.parser.parse.WithValueRecursiveDict>
    override var value: String = ""
}
