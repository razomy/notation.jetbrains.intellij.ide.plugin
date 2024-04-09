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


COMMENT=("#")[^\R\n]* // not END
WHITE_SPACE=[\ \t\f]
END=[\R\n]
KEY_CHARACTER=[^\ \t\f\R\n] // not WHITE_SPACE and not END
SEPARATOR=[ ]
FIRST_VALUE_CHARACTER=[^\ \t\f\R\n] // not WHITE_SPACE and not END
VALUE=[^\R\n] // not END


%state WAITING_VALUE, WAITING_SEPARATOR, WAITING_VALUE_END, WAITING_CHILD_DEEP
%%

<YYINITIAL> ({WHITE_SPACE}*){COMMENT}{END}                 { yybegin(YYINITIAL); return RnTypes.COMMENT; }
<YYINITIAL> {KEY_CHARACTER}+                               { yybegin(WAITING_SEPARATOR); return RnTypes.KEY; }

<WAITING_SEPARATOR> {SEPARATOR}                            { yybegin(WAITING_VALUE); return RnTypes.SEPARATOR; }
<WAITING_SEPARATOR> {END}({WHITE_SPACE}+)                  { yybegin(WAITING_CHILD_DEEP); return RnTypes.DEEP; }
<WAITING_SEPARATOR> {END}                                  { yybegin(YYINITIAL); return RnTypes.END; }

<WAITING_CHILD_DEEP> {COMMENT}{END}({WHITE_SPACE}+)        { yybegin(WAITING_CHILD_DEEP); return RnTypes.COMMENT; }
<WAITING_CHILD_DEEP> {KEY_CHARACTER}+                      { yybegin(WAITING_SEPARATOR); return RnTypes.KEY; }

<WAITING_VALUE> {FIRST_VALUE_CHARACTER}({VALUE}*)          { yybegin(WAITING_VALUE_END); return RnTypes.VALUE; }

<WAITING_VALUE_END> {END}({WHITE_SPACE}+)                  { yybegin(WAITING_CHILD_DEEP); return RnTypes.DEEP; }
<WAITING_VALUE_END> {END}                                  { yybegin(YYINITIAL); return RnTypes.END; }

[^]                                                        { return RnTypes.ERROR; }
