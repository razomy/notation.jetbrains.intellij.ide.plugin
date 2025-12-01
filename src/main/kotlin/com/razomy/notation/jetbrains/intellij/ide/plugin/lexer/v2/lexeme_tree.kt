package com.razomy.notation.jetbrains.intellij.ide.plugin.lexer.v2

interface WithChildren<T> : WithDeep {
    var children: MutableList<T>
}

interface WithPrevParentDeeps<T> {
    var parent_deeps: MutableList<T>
}

fun <T : WithPrevParentDeeps<C>, C> create_ctx_prev_tree(ctx: T): T
        where C : WithChildren<C> {

    var root = NotationLexeme(LexemeType.root) as C;
    root.children = ArrayList<C>();
    root.deep = -1;
    ctx.parent_deeps = ArrayList<C>();
    ctx.parent_deeps.add(root)
    return ctx
}

fun <T, L> create_lexeme_tree(ctx: T, lexeme: L): L
        where T : WithExceptions, T : WithLexemes, T : WithPrevParentDeeps<L>,
              L : WithChildren<L>, L : WithValue, L : Lexeme, L : WithDeep {
    val parent = ctx.parent_deeps.last()

    when (lexeme.type) {
        LexemeType.value_, LexemeType.tail_value, LexemeType.multiline_value -> {
            lexeme.children = mutableListOf()
            parent.children.add(lexeme)
        }

        LexemeType.assign, LexemeType.new_line_assign -> {
            val lastChild = parent.children.lastOrNull()
            if (lastChild === null) {
                ctx.exceptions.add(ArgumentException("invalid parent", mapOf("lexeme" to lexeme, "parent" to parent)))
            } else {
                ctx.parent_deeps.add(lastChild)
            }
        }

        LexemeType.new_line_break -> {
            val parent_index = ctx.parent_deeps.indexOfFirst { it.deep == lexeme.deep }
            if (parent_index == -1) {
                ctx.exceptions.add(ArgumentException("invalid parent", mapOf("lexeme" to lexeme, "parent" to parent)))
            } else {
                val toRemove = ctx.parent_deeps.size - parent_index
                for (i in 0 until toRemove) ctx.parent_deeps.removeAt(ctx.parent_deeps.lastIndex)
            }
        }

        LexemeType.break_ -> {
            if (ctx.parent_deeps.isEmpty()) {
                ctx.exceptions.add(ArgumentException("invalid parent", mapOf("lexeme" to lexeme, "parent" to parent)))
            } else {
                ctx.parent_deeps.removeAt(ctx.parent_deeps.lastIndex)
            }
        }

        LexemeType.separate, LexemeType.new_line_separate -> {}
        else -> throw UnknownTypeArgumentException(lexeme.type)
    }

    return lexeme
}
