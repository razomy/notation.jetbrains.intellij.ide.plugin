package org.razomy.notation

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.spellchecker.inspections.PlainTextSplitter
import com.intellij.spellchecker.tokenizer.TokenConsumer
import com.intellij.spellchecker.tokenizer.Tokenizer
import org.razomy.notation.parser.RnTypes

class RnCustomTokenizer : Tokenizer<PsiElement>() {
    override fun tokenize(element: PsiElement, consumer: TokenConsumer) {
        var child = element.firstChild

        while (child != null) {
            if (isValidToken(child)) {
                consumer.consumeToken(child, PlainTextSplitter())
            }

            child = child.nextSibling
        }
    }

    private fun isValidToken(element: PsiElement): Boolean {
        val elementType = element.node.elementType

        return TokenSet.create(
                RnTypes.COMMENT,
                RnTypes.STRING,
                RnTypes.KEY,
                RnTypes.VALUE_,
                RnTypes.MULTILINE_VALUE,
                RnTypes.TAIL_VALUE,
        ).contains(elementType)
    }
}