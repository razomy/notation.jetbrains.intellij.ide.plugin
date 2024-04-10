package com.razomy.notation.idea.plugin;

import com.intellij.psi.tree.IElementType;

%%

%class RnLexer
%extends RnLexerBridge
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}


COMMENT=("#")[^\r\n]* // not END
WHITE_SPACE=[\ \t\f]
END=[\r\n]
KEY_CHARACTER=[^\ \t\f\r\n] // not WHITE_SPACE and not END
SEPARATOR=[ ]
FIRST_VALUE_CHARACTER=[^\ \t\f\r\n] // not WHITE_SPACE and not END
VALUE=[^\r\n] // not END


%state WAITING_VALUE, WAITING_SEPARATOR, WAITING_VALUE_END, WAITING_CHILD_DEEP
%%

<YYINITIAL> {COMMENT}{END}                                 { yybegin(YYINITIAL); return RnTypes.COMMENT; }
<YYINITIAL> {KEY_CHARACTER}+                               { yybegin(WAITING_SEPARATOR); return RnTypes.KEY; }
<YYINITIAL> {END}                                          { yybegin(YYINITIAL); return RnTypes.EMPTY_LINE; }

<WAITING_SEPARATOR> {SEPARATOR}                            { yybegin(WAITING_VALUE); return RnTypes.SEPARATOR; }
<WAITING_SEPARATOR> {END}({WHITE_SPACE}+)                  { yybegin(WAITING_CHILD_DEEP); return tryDeep(yytext().length()); }
<WAITING_SEPARATOR> {END}                                  { yybegin(YYINITIAL); deep=0; return RnTypes.END; }

<WAITING_CHILD_DEEP> {COMMENT}{END}({WHITE_SPACE}+)        { yybegin(WAITING_CHILD_DEEP); deep=0; return RnTypes.COMMENT; }
<WAITING_CHILD_DEEP> {KEY_CHARACTER}+                      { yybegin(WAITING_SEPARATOR); return RnTypes.KEY; }

<WAITING_VALUE> {FIRST_VALUE_CHARACTER}({VALUE}*)          { yybegin(WAITING_VALUE_END); return RnTypes.VALUE; }

<WAITING_VALUE_END> {END}({WHITE_SPACE}+)                  { yybegin(WAITING_CHILD_DEEP); return tryDeep(yytext().length()); }
<WAITING_VALUE_END> {END}                                  { yybegin(YYINITIAL); deep=0; return RnTypes.END; }

[^]                                                        { return RnTypes.ERROR; }
