package org.razomy.notation

import com.intellij.psi.tree.TokenSet
import org.razomy.notation.parser.RnTypes

interface RnTokenSets {
    companion object {
        val IDENTIFIERS: TokenSet = TokenSet.create(RnTypes.VALUE_)

        val COMMENTS: TokenSet = TokenSet.create(RnTypes.COMMENT)
    }
}