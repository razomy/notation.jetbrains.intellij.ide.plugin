package com.razomy.notation.jetbrains.intellij.ide.plugin.parse.ctx

import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.LexemeType
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.MultilineLexeme
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.WithDeep
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.WithLexemeType
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.WithOffset
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.lexeme.WithValue

class NotationLexeme : WithLexemeType, MultilineLexeme, WithValue,
    WithDeep, WithOffset {
    override var type: LexemeType
    override var offset: Int = 0
    override var deep: Int = 0
    override var value: String = ""

    constructor(type: LexemeType) {
        this.type = type
    }
}
