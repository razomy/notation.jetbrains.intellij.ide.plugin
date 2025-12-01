package com.razomy.notation.jetbrains.intellij.ide.plugin.lexer.v2


interface WithValueRecursiveDict {
    var value_recursive: ValueRecursiveDictOrValue<String> // Any
    var value_recursive_last: String?
    var value_recursive_deep: Int
    var increment: Int
}

interface ParentValueRecursiveDict {
    var parent_value_recursive: MutableList<WithValueRecursiveDict>
}

fun <T : ParentValueRecursiveDict> create_ctx_parent_value_recursive(ctx: T): T {
    val root = object : WithValueRecursiveDict {
        override var value_recursive: Any = ValueRecursiveDict()
        override var value_recursive_last: String? = null
        override var value_recursive_deep = -1
        override var increment = 0
    }
    ctx.parent_value_recursive = mutableListOf(root)
    return ctx
}

fun <T : ParentValueRecursiveDict> get_path(ctx: T): String {
    return ctx.parent_value_recursive.mapNotNull { it.value_recursive_last }.joinToString(".")
}

fun <T, L> create_lexeme_parent_value_recursive(ctx: T, lexeme: L): L
        where T : WithExceptions, T : WithOffset, T : WithString, T : WithLexemes, T : ParentValueRecursiveDict,
              L : Lexeme, L : WithOffset, L : WithDeep {

    fun try_assign_value_parent(): Boolean {
        val node = ctx.parent_value_recursive.at(-1)!!
        val parent = ctx.parent_value_recursive.at(-2)

        // TS: Object.keys(node.value_recursive).length === 1
        val nodeMap = node.value_recursive as? HashMap<*, *>
        val is_assign_value_parent = parent != null && nodeMap != null && nodeMap.size == 1

        if (is_assign_value_parent && parent != null && parent.value_recursive is HashMap<*, *>) {
            val pMap = parent.value_recursive as HashMap<String, Any>
            if (parent.value_recursive_last != null && node.value_recursive_last != null) {
                pMap[parent.value_recursive_last!!] = node.value_recursive_last!!
                return true
            }
        }
        return false
    }

    fun assign() {
        val parent = ctx.parent_value_recursive.last()
        if (parent.value_recursive_last == null) {
            ctx.exceptions.add(
                ArgumentException(
                    "invalid parent",
                    mapOf("lexeme" to lexeme, "parent" to parent, "path" to get_path(ctx))
                )
            )
        } else {
            val pMap = parent.value_recursive as? HashMap<String, Any>
            if (pMap != null && pMap.containsKey(parent.value_recursive_last)) {
                ctx.exceptions.add(
                    ArgumentException(
                        "already exists assign",
                        mapOf("lexeme" to lexeme, "value" to parent.value_recursive_last, "path" to get_path(ctx))
                    )
                )
                parent.increment++
            }

            val newDict = ValueRecursiveDict()
            if (pMap != null && parent.value_recursive_last != null) {
                pMap[parent.value_recursive_last!!] = newDict
            }

            val last = object : WithValueRecursiveDict {
                override var value_recursive: Any = newDict
                override var value_recursive_last: String? = null
                override var value_recursive_deep = lexeme.deep - 1
                override var increment = 0
            }
            ctx.parent_value_recursive.add(last)
        }
    }

    fun value() {
        var valueStr = get_lexeme_value(ctx, lexeme)
        val parent = ctx.parent_value_recursive.last()
        val pMap = parent.value_recursive as? HashMap<String, Any>

        if (pMap != null && pMap.containsKey(valueStr)) {
            ctx.exceptions.add(
                ArgumentException(
                    "already exists value",
                    mapOf("lexeme" to lexeme, "value" to valueStr, "path" to get_path(ctx))
                )
            )
            valueStr += parent.increment++
        }
        if (pMap != null) {
            pMap[valueStr] = ""
            parent.value_recursive_last = valueStr
        }
    }

    when (lexeme.type) {
        LexemeType.value_ -> value()
        LexemeType.tail_value -> {
            value()
            try_assign_value_parent()
        }

        LexemeType.multiline_value -> value()
        LexemeType.assign, LexemeType.new_line_assign -> assign()
        LexemeType.new_line_break -> {
            val parent_index = ctx.parent_value_recursive.indexOfFirst { it.value_recursive_deep == lexeme.deep }
            if (parent_index == -1) {
                ctx.exceptions.add(
                    ArgumentException(
                        "invalid parent",
                        mapOf("lexeme" to lexeme, "path" to get_path(ctx))
                    )
                )
            } else {
                try_assign_value_parent()
                val toRemove = ctx.parent_value_recursive.size - parent_index
                for (i in 0 until toRemove) ctx.parent_value_recursive.removeAt(ctx.parent_value_recursive.lastIndex)
            }
        }

        LexemeType.break_ -> {
            if (ctx.parent_value_recursive.isEmpty()) {
                ctx.exceptions.add(
                    ArgumentException(
                        "invalid parent",
                        mapOf("lexeme" to lexeme, "path" to get_path(ctx))
                    )
                )
            } else {
                try_assign_value_parent()
                ctx.parent_value_recursive.removeAt(ctx.parent_value_recursive.lastIndex)
            }
        }

        LexemeType.separate, LexemeType.new_line_separate -> {}
        else -> throw UnknownTypeArgumentException(mapOf("lexeme" to lexeme, "path" to get_path(ctx)))
    }
    return lexeme
}
