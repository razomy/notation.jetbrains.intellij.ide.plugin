package org.razomy.language;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import org.razomy.language.psi.RazomyTypes;
import static com.intellij.psi.TokenType.BAD_CHARACTER;

%%

%class RazomyLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType

WHITE_SPACE=[ \t\r]+
CRLF=\R
LINE_COMMENT="//".*

IDENTIFIER=[a-zA-Z_][a-zA-Z0-9_]*
NUMBER=[0-9]+(\.[0-9]+)?
STRING=\"[^\"]*\"|'[^']*'
OP=[&|\+\-\*/\!\?@\#\$%\^~=]+

%%
<YYINITIAL> {
  {WHITE_SPACE}        { return com.intellij.psi.TokenType.WHITE_SPACE; }
  {CRLF}               { return RazomyTypes.CRLF2; }
  {LINE_COMMENT}       { return RazomyTypes.LINE_COMMENT; }

  "{"                  { return RazomyTypes.LBRACE; }
  "}"                  { return RazomyTypes.RBRACE; }
  "("                  { return RazomyTypes.LPAREN; }
  ")"                  { return RazomyTypes.RPAREN; }
  "<"                  { return RazomyTypes.LANGLE; }
  ">"                  { return RazomyTypes.RANGLE; }
  "["                  { return RazomyTypes.LBRACKET; }
  "]"                  { return RazomyTypes.RBRACKET; }
  "."                  { return RazomyTypes.DOT; }
  ";"                  { return RazomyTypes.SEMICOLON; }

  "match"              { return RazomyTypes.MATCH_KW; }
  "instanceof"         { return RazomyTypes.INSTANCEOF_KW; }
  "const"              { return RazomyTypes.CONST_KW; }

  {IDENTIFIER}         { return RazomyTypes.IDENTIFIER; }
  {NUMBER}             { return RazomyTypes.NUMBER; }
  {STRING}             { return RazomyTypes.STRING; }
  {OP}                 { return RazomyTypes.OP; }
}

[^] { return BAD_CHARACTER; }