package com.razomy.notation.jetbrains.intellij.ide.plugin.lexer.v2


interface NodeBound {
    var node_finish_x: Int
    var node_finish_y: Int
}

fun <T, L> create_lexeme_tree_bound(ctx: T, lexeme: L): L
        where T : WithLexemes, T : WithPrevParentDeeps<L>,
              L : WithOffset, L : BoundY, L : WithValue, L : Lexeme, L : NodeBound, L : WithChildren<NotationLexeme> {

    lexeme.node_finish_x = lexeme.finish_x
    lexeme.node_finish_y = lexeme.finish_y

    for (i in (ctx.parent_deeps.size - 1) downTo 1) {
        val p = ctx.parent_deeps[i] as? NodeBound
        p?.node_finish_x = lexeme.node_finish_x
        p?.node_finish_y = lexeme.node_finish_y
    }
    return lexeme
}
