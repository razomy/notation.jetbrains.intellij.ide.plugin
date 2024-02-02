package com.razomy.notation.idea.plugin;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

%%

%class RnLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

CRLF=\R|\n
WHITE_SPACE=[\ \t\f]
FIRST_VALUE_CHARACTER=[^\ \n\f\\]
VALUE_CHARACTER=[^\n\f\\#] | "\\#"
END_OF_LINE_COMMENT=("#")[^\r\n]*
SEPARATOR=[ ]
KEY_CHARACTER=[^\ \n\t\f\\] | "\\ "

%state WAITING_VALUE, WAITING_SEPARATOR, WAITING_DEEP

%%

<YYINITIAL> {WHITE_SPACE}                                   { yybegin(YYINITIAL); return RnTypes.ERROR; }
<YYINITIAL> {END_OF_LINE_COMMENT}{CRLF}?({WHITE_SPACE}*)                           { yybegin(YYINITIAL); return RnTypes.COMMENT; }
<YYINITIAL> {CRLF}                                          { yybegin(YYINITIAL); return RnTypes.DEEP; }
<YYINITIAL> {KEY_CHARACTER}+                                { yybegin(WAITING_SEPARATOR); return RnTypes.KEY; }


<WAITING_SEPARATOR> ({WHITE_SPACE}+){CRLF}                  { yybegin(YYINITIAL); return RnTypes.ERROR; }
<WAITING_SEPARATOR> {SEPARATOR}                             { yybegin(WAITING_VALUE); return RnTypes.SEPARATOR; }
<WAITING_SEPARATOR> {CRLF}({WHITE_SPACE}+)                  { yybegin(YYINITIAL); return RnTypes.DEEP; }
<WAITING_SEPARATOR> {CRLF}                                  { yybegin(YYINITIAL); return RnTypes.DEEP; }


<WAITING_VALUE> {END_OF_LINE_COMMENT}{CRLF}?({WHITE_SPACE}*)                       { yybegin(WAITING_VALUE); return RnTypes.COMMENT; }

<WAITING_VALUE> {WHITE_SPACE}*{CRLF}                        { yybegin(YYINITIAL); return RnTypes.ERROR; }
<WAITING_VALUE> {WHITE_SPACE}+                              { yybegin(WAITING_VALUE); return RnTypes.SPACE; }

<WAITING_VALUE> {FIRST_VALUE_CHARACTER}{VALUE_CHARACTER}*   { yybegin(WAITING_DEEP); return RnTypes.VALUE; }

<WAITING_DEEP> {END_OF_LINE_COMMENT}{CRLF}?({WHITE_SPACE}*)                     { yybegin(WAITING_DEEP); return RnTypes.COMMENT; }
<WAITING_DEEP> {CRLF}({WHITE_SPACE}+)                       { yybegin(YYINITIAL); return RnTypes.DEEP; }
<WAITING_DEEP> {CRLF}                                       { yybegin(YYINITIAL); return RnTypes.DEEP; }

[^]                                                         { return RnTypes.ERROR; }
