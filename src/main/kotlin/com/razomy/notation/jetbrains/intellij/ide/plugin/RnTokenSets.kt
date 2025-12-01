package com.razomy.notation.jetbrains.intellij.ide.plugin

import com.intellij.psi.tree.TokenSet
import com.razomy.notation.jetbrains.intellij.ide.plugin.lexer.RnTypes

interface RnTokenSets {
    companion object {
        val IDENTIFIERS: TokenSet = TokenSet.create(RnTypes.KEY)

        val COMMENTS: TokenSet = TokenSet.create(RnTypes.COMMENT)
    }
}