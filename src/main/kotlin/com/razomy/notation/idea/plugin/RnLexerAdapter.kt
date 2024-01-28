package com.razomy.notation.idea.plugin

import com.intellij.lexer.FlexAdapter

class RnLexerAdapter : FlexAdapter(RnLexer(null))