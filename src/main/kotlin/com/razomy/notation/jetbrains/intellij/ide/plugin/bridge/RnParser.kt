package com.razomy.notation.jetbrains.intellij.ide.plugin.bridge

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.ctx.NotationCtx
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.ctx.NotationLexeme
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.parse.create_ctx_exceptions
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.parse.lexeme_to_vrd_ctx
import java.util.ArrayDeque

data class StackFrame(
    val marker: PsiBuilder.Marker,
)

class RnParser : PsiParser {

    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val rootMarker = builder.mark()
        val stack = ArrayDeque<StackFrame>()
        // TODO:refactor
        var freezeLexeme: NotationLexeme? = null

        fun lexeme_iterator(ctx: NotationCtx): NotationLexeme? {
            if (freezeLexeme != null) {
                freezeLexeme!!.deep = ctx.prev_deep
                return freezeLexeme
            }
            return null
        }

        var ctx = lexeme_to_vrd_ctx({ c -> lexeme_iterator(c) });
        fun tryUnfreeze() {
            if (freezeLexeme == null) return
            try {
                ctx.execute(ctx)
            } catch (e: Exception) {
                while (stack.isNotEmpty()) {
                    stack.removeLast().marker.done(RnTypes.PROPERTY)
                }
                val errorMarker = builder.mark()
                builder.advanceLexer()
                errorMarker.error("This token is invalid")
            }
            freezeLexeme = null
        }

        fun next() {
            if (freezeLexeme != null) {
                builder.advanceLexer()
                return
            }

            val type = builder.tokenType
            if (type != null) {
                freezeLexeme = NotationLexeme(element_to_lexeme(type))
                freezeLexeme!!.value = builder.tokenText ?: ""
                tryUnfreeze()
            }
            builder.advanceLexer()
        }


        while (!builder.eof()) {
            val type = builder.tokenType

            when (type) {
                RnTypes.ERROR -> {
                    tryUnfreeze()
                    while (stack.isNotEmpty()) {
                        stack.removeLast().marker.done(RnTypes.PROPERTY)
                    }
                    val errorMarker = builder.mark()
                    next()
                    errorMarker.error("This token is invalid")
                }

                RnTypes.ASSIGN, RnTypes.NEW_LINE_ASSIGN -> {
                    tryUnfreeze()
                    freezeLexeme = NotationLexeme(element_to_lexeme(type))
                    freezeLexeme!!.value = builder.tokenText ?: ""
                    next()
                }

                RnTypes.BREAK_, RnTypes.NEW_LINE_BREAK -> {
                    tryUnfreeze()
                    freezeLexeme = NotationLexeme(element_to_lexeme(type))
                    freezeLexeme!!.value = builder.tokenText ?: ""
                    next()
                }

                RnTypes.INDENT -> {
                    ctx.prev_deep += 1
                    next()
                }

                RnTypes.DEDENT -> {
                    next()
                    while (stack.isNotEmpty() && stack.size >= ctx.prev_deep) {
                        stack.removeLast().marker.done(RnTypes.PROPERTY)
                    }
                    ctx.prev_deep -= 1
                }

                RnTypes.VALUE_, RnTypes.TAIL_VALUE, RnTypes.MULTILINE_VALUE -> {
                    tryUnfreeze()
                    val marker = builder.mark()
                    val keyMarker = builder.mark()

                    next()
                    val nextType = builder.tokenType
                    if (nextType == RnTypes.ASSIGN || nextType == RnTypes.NEW_LINE_ASSIGN) {
                        keyMarker.done(RnTypes.KEY)
                        freezeLexeme = NotationLexeme(element_to_lexeme(nextType))
                        freezeLexeme!!.value = builder.tokenText ?: ""
                        next()
                        stack.addLast(StackFrame(marker))
                    } else {
                        keyMarker.done(RnTypes.STRING)
                        marker.drop()
                    }
                }

                else -> {
                    tryUnfreeze()
                    next()
                }
            }
        }

        while (stack.isNotEmpty()) {
            stack.removeLast().marker.done(RnTypes.PROPERTY)
        }

        rootMarker.done(root)
        return builder.treeBuilt
    }
}