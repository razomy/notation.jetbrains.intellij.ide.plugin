package com.razomy.notation.jetbrains.intellij.ide.plugin.ctx

import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme.LexemeType
import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme.MultilineLexeme
import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme.WithDeep
import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme.WithLexemeType
import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme.WithOffset
import com.razomy.notation.jetbrains.intellij.ide.plugin.ctx.lexeme.WithValue

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
