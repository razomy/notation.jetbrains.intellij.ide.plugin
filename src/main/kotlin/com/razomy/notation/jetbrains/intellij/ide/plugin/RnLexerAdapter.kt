package com.razomy.notation.jetbrains.intellij.ide.plugin

import com.intellij.lexer.FlexAdapter

class RnLexerAdapter : FlexAdapter(RnLexer(null))