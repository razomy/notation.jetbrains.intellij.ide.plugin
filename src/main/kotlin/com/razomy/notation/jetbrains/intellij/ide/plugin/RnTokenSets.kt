package com.razomy.notation.jetbrains.intellij.ide.plugin

import com.intellij.psi.tree.TokenSet
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.bridge.RnTypes

interface RnTokenSets {
    companion object {
        val IDENTIFIERS: TokenSet = TokenSet.create(RnTypes.VALUE_)

        val COMMENTS: TokenSet = TokenSet.create(RnTypes.COMMENT)
    }
}