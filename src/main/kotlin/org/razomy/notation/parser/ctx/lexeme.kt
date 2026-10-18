package org.razomy.notation.parser.ctx

import org.razomy.notation.parser.lexeme.LexemeType
import org.razomy.notation.parser.lexeme.MultilineLexeme
import org.razomy.notation.parser.lexeme.WithDeep
import org.razomy.notation.parser.lexeme.WithLexemeType
import org.razomy.notation.parser.lexeme.WithOffset
import org.razomy.notation.parser.parse.WithValue

class NotationLexeme : org.razomy.notation.parser.lexeme.WithLexemeType,
    org.razomy.notation.parser.lexeme.MultilineLexeme, org.razomy.notation.parser.parse.WithValue,
    org.razomy.notation.parser.lexeme.WithDeep, org.razomy.notation.parser.lexeme.WithOffset {
    override var type: org.razomy.notation.parser.lexeme.LexemeType
    override var offset: Int = 0
    override var deep: Int = 0
    override var value: String = ""

    constructor(type: org.razomy.notation.parser.lexeme.LexemeType) {
        this.type = type
    }
}
