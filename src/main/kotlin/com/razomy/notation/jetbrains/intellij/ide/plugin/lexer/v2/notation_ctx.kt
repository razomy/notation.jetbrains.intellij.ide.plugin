package com.razomy.notation.jetbrains.intellij.ide.plugin.lexer.v2

class NotationCtx(override var string: String) :
    WithCreateCtxPipes<NotationCtx>,
    WithCreateLexemePipes,
    WithLexemes,
    WithOffset,
    WithString,
    WithTail,
    WithExceptions,
    WithPrevOffset,
    WithPrevDeep,
    WithPrevSpaces,
    WithPrevStartXy,
    WithPrevParentDeeps<NotationLexeme>,
    ParentKv,
    ParentRecursiveDict,
    ParentValueRecursiveDict,
    WithExecute<NotationCtx> {
    override var create_ctx_pipes: List<(NotationCtx) -> NotationCtx> = listOf()
    override var create_lexeme_pipes: List<LexemePipe<Lexeme>> = listOf()

    override var lexemes: MutableList<Lexeme> = mutableListOf()
    override var offset: Int = 0
    override var tail: String = ""
    override var exceptions: MutableList<Exception> = mutableListOf()
    override var prev_offset: Int = 0
    override var prev_deep: Int = 0
    override var prev_spaces: MutableList<SpaceInfo> = mutableListOf()
    override var prev_x_offset: Int = 0
    override var prev_start_y: Int = 0

    // Initializing complex structures (usually handled by create_ctx pipes, but fields need init)
    override var parent_deeps: MutableList<NotationLexeme> = mutableListOf()
    override var parent_kv: MutableList<WithKv> = mutableListOf()
    override var parent_recursive: MutableList<WithRecursiveDict> = mutableListOf()
    override var parent_value_recursive: MutableList<WithValueRecursiveDict> = mutableListOf()

    override var execute: (NotationCtx) -> Unit = {}
}

fun create_notation_ctx(string: String): NotationCtx {
    val ctx = NotationCtx(string)

    ctx.create_ctx_pipes = listOf(
        { c -> create_ctx_exceptions(c) },
        { c -> create_ctx_offset(c) },
        { c -> create_ctx_prev_offset(c) },
        { c -> create_ctx_prev_spaces(c) },
        { c -> create_ctx_lexeme(c) },
        { c -> create_ctx_tail(c) },
        { c -> create_ctx_deep(c) },
        { c ->
            val root = NotationLexeme(LexemeType.root)
            root.deep = -1
            c.parent_deeps = mutableListOf(root)
            c
        },
        { c -> create_ctx_prev_bound(c) },
        { c -> create_ctx_parent_recursive(c) },
        { c -> create_ctx_parent_value_recursive(c) }
    )

    ctx.create_lexeme_pipes = listOf(
        { c, l -> create_lexeme_offset(c as NotationCtx, l as NotationLexeme) },
        { c, l -> create_lexeme_value(c as NotationCtx, l as NotationLexeme) },
        { c, l -> create_lexeme_tail(c as NotationCtx, l as NotationLexeme) },
        { c, l -> create_lexeme_deep(c as NotationCtx, l as NotationLexeme) },
        { c, l -> create_lexeme_deep_new_line(c as NotationCtx, l as NotationLexeme) },
        { c, l -> create_lexeme_bound(c as NotationCtx, l as NotationLexeme) },
        { c, l -> create_lexeme_tree(c as NotationCtx, l as NotationLexeme) },
        { c, l -> create_lexeme_tree_bound(c as NotationCtx, l as NotationLexeme) },
        { c, l -> create_lexeme_parent_recursive(c as NotationCtx, l as NotationLexeme) },
        { c, l -> create_lexeme_parent_value_recursive(c as NotationCtx, l as NotationLexeme) }
    )

    ctx.execute = { c -> execute_pipes_logic(c) }

    return create_ctx(ctx)
}

fun string_to_lexeme_tree(str: String): NotationCtx {
    try {
        val ctx = create_notation_ctx(str)
        ctx.execute(ctx)
        if (ctx.exceptions.isNotEmpty()) {
            MultipleException(ctx.exceptions)
        }
        return ctx
    } catch (e: Exception) {
        throw e;
    }
}