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

// Обычный ID начинается с буквы. Если начинается с '_', то это ID только если за '_' идут еще символы.
IDENTIFIER=[a-zA-Z][a-zA-Z0-9_]*|_[a-zA-Z0-9_]+
NUMBER=[0-9]+(\.[0-9]+)?
STRING=\"([^\"\\]|\\.)*\"|\'([^\'\\]|\\.)*\'

%%
<YYINITIAL> {
  {WHITE_SPACE}        { return com.intellij.psi.TokenType.WHITE_SPACE; }
  {CRLF}               { return RazomyTypes.CRLF2; }
  {LINE_COMMENT}       { return RazomyTypes.LINE_COMMENT; }

  // Скобки
  "{"                  { return RazomyTypes.LBRACE; }
  "}"                  { return RazomyTypes.RBRACE; }
  "["                  { return RazomyTypes.LBRACKET; }
  "]"                  { return RazomyTypes.RBRACKET; }
  "("                  { return RazomyTypes.LPAREN; }
  ")"                  { return RazomyTypes.RPAREN; }

  // Пайплайны и миграции
  ".>"                 { return RazomyTypes.DOT_GT; }
  ".&"                 { return RazomyTypes.DOT_AMP; }
  "->"                 { return RazomyTypes.MIGRATION; }
  "."                  { return RazomyTypes.DOT; }

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

  // Разделители и спецсимволы
  ","                  { return RazomyTypes.COMMA; }
  "_"                  { return RazomyTypes.UNDERSCORE; }
  ";"                  { return RazomyTypes.SEMICOLON; }

  // Литералы
  "true"               { return RazomyTypes.TRUE; }
  "false"              { return RazomyTypes.FALSE; }

  {IDENTIFIER}         { return RazomyTypes.IDENTIFIER; }
  {NUMBER}             { return RazomyTypes.NUMBER; }
  {STRING}             { return RazomyTypes.STRING; }
}

[^] { return BAD_CHARACTER; }