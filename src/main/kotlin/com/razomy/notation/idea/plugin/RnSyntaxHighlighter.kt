package com.razomy.notation.idea.plugin;

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NotNull


class RnSyntaxHighlighter : SyntaxHighlighterBase() {
    @NotNull
    override fun getHighlightingLexer(): Lexer {
        return RnLexerAdapter()
    }

    @NotNull
    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey?> {
        if (tokenType == RnTypes.SEPARATOR) {
            return SEPARATOR_KEYS
        }
        if (tokenType == RnTypes.KEY) {
            return KEY_KEYS
        }
        if (tokenType == RnTypes.VALUE) {
            return VALUE_KEYS
        }
        if (tokenType == RnTypes.COMMENT) {
            return COMMENT_KEYS
        }
        if (tokenType == RnTypes.SPACE) {
            return SPACE_KEYS
        }
        if (tokenType == RnTypes.ERROR) {
            return ERROR_KEYS
        }
        return EMPTY_KEYS
    }

    companion object {
        val SEPARATOR: TextAttributesKey = createTextAttributesKey("RN_SEPARATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        val KEY: TextAttributesKey = createTextAttributesKey("RN_KEY", DefaultLanguageHighlighterColors.KEYWORD)
        val VALUE: TextAttributesKey = createTextAttributesKey("RN_VALUE", DefaultLanguageHighlighterColors.STRING)
        val COMMENT: TextAttributesKey = createTextAttributesKey("RN_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        val ERROR: TextAttributesKey = createTextAttributesKey("RN_ERROR", HighlighterColors.BAD_CHARACTER)
        val SPACE: TextAttributesKey = createTextAttributesKey("RN_SPACE", HighlighterColors.TEXT)


        private val SPACE_KEYS = arrayOf<TextAttributesKey?>(SPACE)
        private val ERROR_KEYS = arrayOf<TextAttributesKey?>(ERROR)
        private val SEPARATOR_KEYS = arrayOf<TextAttributesKey?>(SEPARATOR)
        private val KEY_KEYS = arrayOf<TextAttributesKey?>(KEY)
        private val VALUE_KEYS = arrayOf<TextAttributesKey?>(VALUE)
        private val COMMENT_KEYS = arrayOf<TextAttributesKey?>(COMMENT)
        private val EMPTY_KEYS = arrayOfNulls<TextAttributesKey>(0)
    }
}


