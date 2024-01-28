package com.razomy.notation.idea.plugin;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;

%%

%class RnLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

CRLF=\R
WHITE_SPACE=[\ \n\t\f]
FIRST_VALUE_CHARACTER=[^\ \n\f\\]
VALUE_CHARACTER=[^\n\f\\]
END_OF_LINE_COMMENT=("#"|"!")[^\r\n]*
SEPARATOR=[:= ]
KEY_CHARACTER=[^:=\ \n\t\f\\] | "\\ "

%state WAITING_VALUE, WAITING_SPACE

%%

<YYINITIAL> {END_OF_LINE_COMMENT}                           { yybegin(YYINITIAL); return RnTypes.COMMENT; }
<YYINITIAL> {KEY_CHARACTER}+                                { yybegin(WAITING_SPACE); return RnTypes.KEY; }
<YYINITIAL> ({CRLF}|{WHITE_SPACE})+                         { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }


<WAITING_SPACE> {SEPARATOR}+                                { yybegin(WAITING_VALUE); return RnTypes.SEPARATOR; }


<WAITING_VALUE> {END_OF_LINE_COMMENT}                       { yybegin(WAITING_VALUE); return RnTypes.COMMENT; }

<WAITING_VALUE> {WHITE_SPACE}*{CRLF}{VALUE_CHARACTER}*      { return TokenType.BAD_CHARACTER; }
<WAITING_VALUE> {WHITE_SPACE}+                              { return TokenType.BAD_CHARACTER; }

<WAITING_VALUE> {FIRST_VALUE_CHARACTER}{VALUE_CHARACTER}*   { yybegin(YYINITIAL); return RnTypes.VALUE; }

<WAITING_VALUE> [^]                                         { return TokenType.BAD_CHARACTER; }

[^]                                                         { return TokenType.BAD_CHARACTER; }
