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
DOC_COMMENT="#"[^#]*"#"

IDENTIFIER=[a-zA-Z][a-zA-Z0-9_]*|_[a-zA-Z0-9_]+
NUMBER=[0-9]+(\.[0-9]+)?
STRING=\"([^\"\\]|\\.)*\"|\'([^\'\\]|\\.)*\'

%%
<YYINITIAL> {
  {WHITE_SPACE}        { return com.intellij.psi.TokenType.WHITE_SPACE; }
  {CRLF}               { return RazomyTypes.CRLF2; }
  {DOC_COMMENT}        { return RazomyTypes.DOC_COMMENT; }
  {LINE_COMMENT}       { return RazomyTypes.LINE_COMMENT; }

  // Скобки
  "{"                  { return RazomyTypes.LBRACE; }
  "}"                  { return RazomyTypes.RBRACE; }
  "["                  { return RazomyTypes.LBRACKET; }
  "]"                  { return RazomyTypes.RBRACKET; }
  "("                  { return RazomyTypes.LPAREN; }
  ")"                  { return RazomyTypes.RPAREN; }

  // Пайплайны
  ".>"                 { return RazomyTypes.DOT_GT; }
  ".&"                 { return RazomyTypes.DOT_AMP; }

  // Сравнения
  "=="                 { return RazomyTypes.EQ_EQ; }
  "!="                 { return RazomyTypes.NOT_EQ; }
  "<="                 { return RazomyTypes.LTE; }
  ">="                 { return RazomyTypes.GTE; }
  "<"                  { return RazomyTypes.LANGLE; }
  ">"                  { return RazomyTypes.RANGLE; }
  "="                  { return RazomyTypes.EQ; }

  // Алгебра типов и математика
  "&"                  { return RazomyTypes.AMP; }
  "|"                  { return RazomyTypes.PIPE; }
  "/"                  { return RazomyTypes.SLASH; }
  "+"                  { return RazomyTypes.PLUS; }
  "-"                  { return RazomyTypes.MINUS; }
  "*"                  { return RazomyTypes.MUL; }

  // Разделители
  ","                  { return RazomyTypes.COMMA; }
  "_"                  { return RazomyTypes.UNDERSCORE; }
  ";"                  { return RazomyTypes.SEMICOLON; }

  // Литералы
  "true"               { return RazomyTypes.TRUE; }
  "false"              { return RazomyTypes.FALSE; }

  {IDENTIFIER}         { return RazomyTypes.IDENTIFIER; }
  {NUMBER}             { return RazomyTypes.NUMBER; }
  {STRING}             { return RazomyTypes.STRING; }

  // ВАЖНО: Разделение точки доступа (User.Id) и точки пайплайна (A . B, или . перед переносом строки / скобкой)
  "."/([ \t\r\n\{\[\(]) { return RazomyTypes.PIPE_DOT; }
  "."                  { return RazomyTypes.DOT; }
}

[^] { return BAD_CHARACTER; }