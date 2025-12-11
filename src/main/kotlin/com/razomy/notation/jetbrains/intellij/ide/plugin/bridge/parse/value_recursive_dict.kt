package com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.parse

import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.ctx.UnknownTypeArgumentException
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.ctx.WithString
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.ctx.at
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.LexemeType
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.WithDeep
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.WithLexemeType
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.WithOffset
import kotlin.collections.get

class ValueRecursiveDict : HashMap<String, Any>()
typealias ValueRecursiveDictOrValue = Any

interface WithValueRecursiveDict {
    var value_recursive: ValueRecursiveDictOrValue // Any
    var value_recursive_last: String?
    var value_recursive_deep: Int
    var increment: Int
}

interface ParentValueRecursiveDict {
    var parent_value_recursive: MutableList<WithValueRecursiveDict>
}

fun <T : ParentValueRecursiveDict> create_ctx_value_recursive_dict(ctx: T): T {
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


fun <T> try_assign_value_parent(ctx: T): Boolean
        where T : WithExceptions, T : WithOffset, T : WithString, T : ParentValueRecursiveDict {

    val node = ctx.parent_value_recursive.at(-1)!!
    val parent = ctx.parent_value_recursive.at(-2)
    val nodeValue = node.value_recursive as ValueRecursiveDict

    val is_assign_value_parent = parent != null && nodeValue.size == 1
    if (is_assign_value_parent) {
        val parentValue = parent.value_recursive as ValueRecursiveDict
        parentValue[parent.value_recursive_last!!] = node.value_recursive_last!!
        return true
    }
    return false
}

fun <T, L> assign(ctx: T, lexeme: L)
        where T : WithExceptions, T : WithOffset, T : WithString, T : ParentValueRecursiveDict,
              L : WithOffset, L : WithDeep, L : WithLexemeType {

    val parent = ctx.parent_value_recursive.last()
    if (parent.value_recursive_last == null || parent.value_recursive_last == "") {
        ctx.exception_fn(
            InvalidNotation(
                "parent must be defined before assign" + "path" + get_path(ctx)
            )
        )
    } else {
        val parentValue = parent.value_recursive as ValueRecursiveDict
        if (parentValue[parent.value_recursive_last] !== null && parentValue[parent.value_recursive_last] !== "") {
            ctx.exception_fn(
                InvalidNotation(
                    "already exists assign" + "path" + get_path(ctx)
                )
            )
            parent.value_recursive_last += parent.increment++
        }

        parentValue[parent.value_recursive_last!!] = ValueRecursiveDict();

        val last = object : WithValueRecursiveDict {
            override var value_recursive = parentValue[parent.value_recursive_last]!!
            override var value_recursive_last: String? = null
            override var value_recursive_deep = lexeme.deep - 1
            override var increment = 0
        }
        ctx.parent_value_recursive.add(last)
    }
}

fun <T, L> value(ctx: T, lexeme: L)
        where T : WithExceptions, T : WithOffset, T : WithString, T : ParentValueRecursiveDict,
              L : WithOffset, L : WithDeep, L : WithLexemeType, L : WithValue {

    var valueStr = lexeme.value
    val parent = ctx.parent_value_recursive.last()
    val parentValue = parent.value_recursive as ValueRecursiveDict

    if (parentValue.containsKey(valueStr)) {
        ctx.exception_fn(
            InvalidNotation(
                "already exists value" + "path" + get_path(ctx)
            )
        )
        valueStr += parent.increment++
    }

    parentValue[valueStr] = ""
    parent.value_recursive_last = valueStr
}

fun <T, L> create_lexeme_value_recursive_dict(ctx: T, lexeme: L): L
        where T : WithExceptions, T : WithOffset, T : WithString, T : ParentValueRecursiveDict,
              L : WithOffset, L : WithDeep, L : WithLexemeType, L : WithValue {


    when (lexeme.type) {
        LexemeType.value_ -> value(ctx, lexeme)
        LexemeType.tail_value -> {
            value(ctx, lexeme)
            try_assign_value_parent(ctx)
        }

        LexemeType.multiline_value -> value(ctx, lexeme)
        LexemeType.assign, LexemeType.new_line_assign -> assign(ctx, lexeme)
        LexemeType.new_line_break -> {
            val parent_index = ctx.parent_value_recursive.indexOfFirst { it.value_recursive_deep == lexeme.deep }
            if (parent_index == -1) {
                ctx.exception_fn(
                    InvalidNotation(
                        "invalid parent" + "path" + get_path(ctx)
                    )
                )
            } else {
                try_assign_value_parent(ctx)
                val toRemove = ctx.parent_value_recursive.size - parent_index
                for (i in 0 until toRemove) ctx.parent_value_recursive.removeAt(ctx.parent_value_recursive.lastIndex)
            }
        }

        LexemeType.break_ -> {
            if (ctx.parent_value_recursive.isEmpty()) {
                ctx.exception_fn(
                    InvalidNotation(
                        "invalid parent" + "path" + get_path(ctx)

                    )
                )
            } else {
                try_assign_value_parent(ctx)
                ctx.parent_value_recursive.removeAt(ctx.parent_value_recursive.lastIndex)
            }
        }

        LexemeType.error,
        LexemeType.separate,
        LexemeType.not_closed_error_multiline_value,
        LexemeType.new_line_separate -> {
        }

        else -> throw UnknownTypeArgumentException(mapOf("lexeme" to lexeme, "path" to get_path(ctx)))
    }
    return lexeme
}
