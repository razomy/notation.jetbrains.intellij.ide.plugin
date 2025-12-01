package com.razomy.notation.jetbrains.intellij.ide.plugin.lexer.v2

typealias RecursiveDict = MutableMap<String, Any>

interface WithRecursiveDict {
    var recursive: RecursiveDict
    var recursive_last: RecursiveDict?
    var recursive_deep: Int
}

interface ParentRecursiveDict {
    var parent_recursive: MutableList<WithRecursiveDict>
}

fun <T : ParentRecursiveDict> create_ctx_parent_recursive(ctx: T): T {
    val root = object : WithRecursiveDict {
        override var recursive = mutableMapOf<String, Any>()
        override var recursive_last: RecursiveDict? = null
        override var recursive_deep = -1
    }
    ctx.parent_recursive = mutableListOf(root)
    return ctx
}

fun <T, L> create_lexeme_parent_recursive(ctx: T, lexeme: L): L
        where T : WithExceptions, T : WithOffset, T : WithString, T : WithLexemes, T : ParentRecursiveDict,
              L : Lexeme, L : WithOffset, L : WithDeep {

    val parent = ctx.parent_recursive.last()
    val valueStr = get_lexeme_value(ctx, lexeme) // mock try_unescape

    when (lexeme.type) {
        LexemeType.value_, LexemeType.tail_value, LexemeType.multiline_value -> {
            val newDict = mutableMapOf<String, Any>()
            parent.recursive[valueStr] = newDict
            parent.recursive_last = newDict
        }

        LexemeType.assign, LexemeType.new_line_assign -> {
            if (parent.recursive_last == null) {
                ctx.exceptions.add(ArgumentException("invalid parent", mapOf("lexeme" to lexeme, "parent" to parent)))
            } else {
                val last = object : WithRecursiveDict {
                    override var recursive = parent.recursive_last!!
                    override var recursive_last: RecursiveDict? = null
                    override var recursive_deep = lexeme.deep - 1
                }
                ctx.parent_recursive.add(last)
            }
        }

        LexemeType.new_line_break -> {
            val parent_index = ctx.parent_recursive.indexOfFirst { it.recursive_deep == lexeme.deep }
            if (parent_index == -1) {
                ctx.exceptions.add(ArgumentException("invalid parent", mapOf("lexeme" to lexeme, "parent" to parent)))
            } else {
                val toRemove = ctx.parent_recursive.size - parent_index
                for (i in 0 until toRemove) ctx.parent_recursive.removeAt(ctx.parent_recursive.lastIndex)
            }
        }

        LexemeType.break_ -> {
            if (ctx.parent_recursive.isEmpty()) {
                ctx.exceptions.add(ArgumentException("invalid parent", mapOf("lexeme" to lexeme, "parent" to parent)))
            } else {
                ctx.parent_recursive.removeAt(ctx.parent_recursive.lastIndex)
            }
        }

        LexemeType.separate, LexemeType.new_line_separate -> {}
        else -> throw UnknownTypeArgumentException(lexeme.type)
    }

    return lexeme
}