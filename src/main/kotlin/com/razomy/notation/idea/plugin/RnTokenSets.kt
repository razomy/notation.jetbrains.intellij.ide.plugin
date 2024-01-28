package com.razomy.notation.idea.plugin

import com.intellij.psi.tree.TokenSet

interface RnTokenSets {
    companion object {
        val IDENTIFIERS: TokenSet = TokenSet.create(RnTypes.KEY)

        val COMMENTS: TokenSet = TokenSet.create(RnTypes.COMMENT)
    }
}