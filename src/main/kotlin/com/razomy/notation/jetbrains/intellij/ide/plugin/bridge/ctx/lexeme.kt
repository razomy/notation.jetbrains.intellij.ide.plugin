package com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.ctx

import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.LexemeType
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.MultilineLexeme
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.WithDeep
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.WithLexemeType
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.lexeme.WithOffset
import com.razomy.notation.jetbrains.intellij.ide.plugin.bridge.parse.WithValue

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
