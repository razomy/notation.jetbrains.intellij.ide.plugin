package com.razomy.notation.jetbrains.intellij.ide.plugin.editor

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.RnLexer
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.RnTypes
import org.jetbrains.annotations.NotNull

class RnSyntaxHighlighter : SyntaxHighlighterBase() {
    @NotNull
    override fun getHighlightingLexer(): Lexer {
        return RnLexer()
    }

    @NotNull
    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey?> {
        return when (tokenType) {
            RnTypes.NEW_LINE_ASSIGN,
            RnTypes.ASSIGN -> RN_ASSIGN_KEYS

            RnTypes.KEY -> RN_KEY_KEYS

            RnTypes.VALUE_,
            RnTypes.MULTILINE_VALUE,
            RnTypes.TAIL_VALUE,
            RnTypes.STRING -> RN_STRING_KEYS

            RnTypes.COMMENT -> RN_COMMENT_KEYS
            RnTypes.ERROR -> RN_ERROR_KEYS

            RnTypes.NEW_LINE_BREAK,
            RnTypes.BREAK_ -> RN_BREAK_KEYS

            RnTypes.NEW_LINE_SEPARATE,
            RnTypes.SEPARATE -> RN_DELIMITER_KEYS

            else -> EMPTY_KEYS
        }
    }

    companion object {
        val RN_ASSIGN: TextAttributesKey =
            createTextAttributesKey("RN_ASSIGN", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        val RN_ERROR: TextAttributesKey = createTextAttributesKey("RN_ERROR", HighlighterColors.BAD_CHARACTER)
        val RN_STRING: TextAttributesKey = createTextAttributesKey("RN_VALUE", DefaultLanguageHighlighterColors.STRING)
        val RN_BREAK: TextAttributesKey =
            createTextAttributesKey("RN_BREAK", DefaultLanguageHighlighterColors.SEMICOLON)
        val RN_DELIMITER: TextAttributesKey =
            createTextAttributesKey("RN_DELIMITER", DefaultLanguageHighlighterColors.COMMA)
        val RN_COMMENT: TextAttributesKey =
            createTextAttributesKey("RN_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        val RN_KEY: TextAttributesKey = createTextAttributesKey("RN_KEY", DefaultLanguageHighlighterColors.IDENTIFIER)
        private val RN_ASSIGN_KEYS = arrayOf<TextAttributesKey?>(RN_ASSIGN)
        private val RN_ERROR_KEYS = arrayOf<TextAttributesKey?>(RN_ERROR)
        private val RN_STRING_KEYS = arrayOf<TextAttributesKey?>(RN_STRING)
        private val RN_BREAK_KEYS = arrayOf<TextAttributesKey?>(RN_BREAK)
        private val RN_DELIMITER_KEYS = arrayOf<TextAttributesKey?>(RN_DELIMITER)
        private val RN_COMMENT_KEYS = arrayOf<TextAttributesKey?>(RN_COMMENT)
        private val RN_KEY_KEYS = arrayOf<TextAttributesKey?>(RN_KEY)
        private val EMPTY_KEYS = arrayOfNulls<TextAttributesKey>(0)
    }
}
