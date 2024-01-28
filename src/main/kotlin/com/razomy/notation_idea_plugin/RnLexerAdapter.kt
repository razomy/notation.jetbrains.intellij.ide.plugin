package com.razomy.notation_idea_plugin

import com.intellij.lexer.FlexAdapter

class RnLexerAdapter : FlexAdapter(RnLexer(null))