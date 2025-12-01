package com.razomy.notation.jetbrains.intellij.ide.plugin.lexer.v2

typealias KvArray = MutableList<Any?>

fun ak(): KvArray = mutableListOf()
fun k(key: String, value: Any?): KvArray = mutableListOf(key, value)

interface WithKv {
    var kv: KvArray
    var kv_last: String?
    var kv_deep: Int
}

interface ParentKv {
    var parent_kv: MutableList<WithKv>
}

fun <T : ParentKv> kv_create_ctx(ctx: T): T {
    val root = object : WithKv {
        override var kv = ak()
        override var kv_last: String? = null
        override var kv_deep = -1
    }
    ctx.parent_kv = mutableListOf(root)
    return ctx
}

fun <T, L> kv_create_lexeme(ctx: T, lexeme: L): L
        where T : WithExceptions, T : WithOffset, T : WithString, T : WithLexemes, T : ParentKv,
              L : Lexeme, L : WithOffset, L : WithDeep {

    fun try_assign_value_parent(): Boolean {
        val node = ctx.parent_kv.at(-1)!!
        val parent = ctx.parent_kv.at(-2)

        val is_assign_value_parent = parent != null && node.kv.size == 1

        if (is_assign_value_parent && parent != null) {
            val lastPair = parent.kv.last() as KvArray
            lastPair[1] = node.kv_last
            return true
        }
        return false
    }

    fun assign() {
        val parent = ctx.parent_kv.last()
        if (parent.kv_last == null) {
            ctx.exceptions.add(ArgumentException("invalid parent", mapOf("lexeme" to lexeme, "parent" to parent)))
        } else {
            val la = parent.kv.last() as KvArray
            la[1] = ak()

            val last = object : WithKv {
                override var kv = la[1] as KvArray
                override var kv_last: String? = null
                override var kv_deep = lexeme.deep - 1
            }
            ctx.parent_kv.add(last)
        }
    }

    fun value() {
        val valueStr = get_lexeme_value(ctx, lexeme)
        val parent = ctx.parent_kv.last()
        parent.kv.add(k(valueStr, ""))
        parent.kv_last = valueStr
    }

    when (lexeme.type) {
        LexemeType.value_ -> value()
        LexemeType.tail_value -> {
            value()
            try_assign_value_parent()
        }

        LexemeType.multiline_value -> value()
        LexemeType.assign -> assign()
        LexemeType.new_line_assign -> assign()
        LexemeType.new_line_break -> {
            val parent_index = ctx.parent_kv.indexOfFirst { it.kv_deep == lexeme.deep }
            if (parent_index == -1) {
                val parent = ctx.parent_kv.lastOrNull()
                ctx.exceptions.add(ArgumentException("invalid parent", mapOf("lexeme" to lexeme, "parent" to parent)))
            } else {
                try_assign_value_parent()
                val toRemove = ctx.parent_kv.size - parent_index
                for (i in 0 until toRemove) ctx.parent_kv.removeAt(ctx.parent_kv.lastIndex)
            }
        }

        LexemeType.break_ -> {
            if (ctx.parent_kv.isEmpty()) {
                val parent = ctx.parent_kv.lastOrNull()
                ctx.exceptions.add(ArgumentException("invalid parent", mapOf("lexeme" to lexeme, "parent" to parent)))
            } else {
                try_assign_value_parent()
                ctx.parent_kv.removeAt(ctx.parent_kv.lastIndex)
            }
        }

        LexemeType.separate, LexemeType.new_line_separate -> {
        }

        else -> throw UnknownTypeArgumentException(lexeme.type)
    }

    return lexeme
}
