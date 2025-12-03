package com.razomy.notation.jetbrains.intellij.ide.plugin.parse.bridge

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType


class RnParser : PsiParser {

    fun next(builder: PsiBuilder) {
        val tokenType = builder.tokenType
        when (tokenType) {
            RnTypes.VALUE_,
            RnTypes.TAIL_VALUE,
            RnTypes.MULTILINE_VALUE -> {
                val nMarker = builder.mark()
                builder.advanceLexer()
                val next_ = builder.tokenType
                if (next_ == RnTypes.ASSIGN || next_ == RnTypes.NEW_LINE_ASSIGN) {
                    next(builder)
                    nMarker.done(RnTypes.PROPERTY)
                } else if (next_ == RnTypes.BREAK_) {
                    next(builder)
                    nMarker.done(RnTypes.PROPERTY)
                } else {
                    nMarker.done(tokenType)
                }
            }

            RnTypes.ASSIGN,
            RnTypes.NEW_LINE_ASSIGN -> {
                builder.advanceLexer()
                next(builder)
            }


            RnTypes.NEW_LINE_BREAK -> {
                builder.advanceLexer()
                next(builder)
            }

            RnTypes.BREAK_ -> {
                builder.advanceLexer()
                next(builder)
            }

            RnTypes.ERROR,
            RnTypes.NOT_CLOSED_ERROR_MULTILINE_VALUE,
            RnTypes.SEPARATE,
            RnTypes.NEW_LINE_SEPARATE -> {
            }

        }
    }

    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val rootMarker = builder.mark()

        while (!builder.eof()) {
            builder.advanceLexer()
            next(builder)
        }
        rootMarker.done(root)
        return builder.treeBuilt
    }
}
