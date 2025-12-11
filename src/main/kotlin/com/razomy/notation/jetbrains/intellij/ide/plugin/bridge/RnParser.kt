package com.razomy.notation.jetbrains.intellij.ide.plugin.bridge

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType
import java.util.ArrayDeque

data class StackFrame(
    val marker: PsiBuilder.Marker,
)

class RnParser : PsiParser {

    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val rootMarker = builder.mark()
        val stack = ArrayDeque<StackFrame>()
        var deep = 0

        while (!builder.eof()) {
            val type = builder.tokenType

            when (type) {
                RnTypes.ERROR -> {
                    while (stack.isNotEmpty()) {
                        stack.removeLast().marker.done(RnTypes.PROPERTY)
                    }
                    val errorMarker = builder.mark()
                    builder.advanceLexer()
                    errorMarker.error("This token is invalid")
                }
                RnTypes.INDENT -> {
                    deep += 1
                    builder.advanceLexer()
                }

                RnTypes.DEDENT -> {
                    builder.advanceLexer()
                    while (stack.isNotEmpty() && stack.size >= deep) {
                        stack.removeLast().marker.done(RnTypes.PROPERTY)
                    }
                    deep -= 1
                }

                RnTypes.VALUE_, RnTypes.TAIL_VALUE, RnTypes.MULTILINE_VALUE -> {
                    val marker = builder.mark()
                    val keyMarker = builder.mark()

                    builder.advanceLexer()
                    val nextType = builder.tokenType
                    if (nextType == RnTypes.ASSIGN || nextType == RnTypes.NEW_LINE_ASSIGN) {
                        keyMarker.done(RnTypes.KEY)
                        builder.advanceLexer()
                        stack.addLast(StackFrame(marker))
                    } else {
                        keyMarker.done(RnTypes.STRING)
                        marker.drop()
                    }
                }

                else -> {
                    builder.advanceLexer()
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