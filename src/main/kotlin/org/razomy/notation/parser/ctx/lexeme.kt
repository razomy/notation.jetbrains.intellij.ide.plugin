package org.razomy.notation.parser.ctx

import org.razomy.notation.parser.lexeme.*
import org.razomy.notation.parser.parse.WithValue

class NotationLexeme : WithLexemeType,
    MultilineLexeme, WithValue,
    WithDeep, WithOffset {
    override var type: LexemeType
    override var offset: Int = 0
    override var deep: Int = 0
    override var value: String = ""

    constructor(type: LexemeType) {
        this.type = type
    }
}
