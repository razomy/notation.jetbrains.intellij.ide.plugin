package com.razomy.notation.idea.plugin

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.spellchecker.inspections.PlainTextSplitter
import com.intellij.spellchecker.tokenizer.TokenConsumer
import com.intellij.spellchecker.tokenizer.Tokenizer

class RnCustomTokenizer : Tokenizer<PsiElement>() {
    override fun tokenize(element: PsiElement, consumer: TokenConsumer) {
        var child = element.firstChild

        while (child != null) {
            // Check if the child is a valid token for spelling checking
            if (isValidToken(child)) {
                // Add the token to the spellchecker
                consumer.consumeToken(child, PlainTextSplitter())
            }

            // Move to the next sibling
            child = child.nextSibling
        }
    }

    private fun isValidToken(element: PsiElement): Boolean {
        // You can customize this method to define which elements should be considered valid tokens
        val elementType = element.node.elementType

        // Example: consider identifiers, strings, and comments as valid tokens
        return TokenSet.create(
                RnTypes.COMMENT,
                RnTypes.KEY,
                RnTypes.VALUE,
        ).contains(elementType)
    }
}