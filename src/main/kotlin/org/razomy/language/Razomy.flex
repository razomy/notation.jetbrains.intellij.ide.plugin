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

WHITE_SPACE=[ \t\f\r]+
CRLF=\R
LINE_COMMENT="//"[^\r\n]*
DOC_COMMENT="#"[^#]*"#"

IDENTIFIER=[a-zA-Z][a-zA-Z0-9_]*|_[a-zA-Z0-9_]+
HEX_NUMBER=0x[0-9a-fA-F]+
INT_NUMBER=[0-9e]+
FLOAT_NUMBER=[0-9e]+\.[0-9e]+
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

  // Пайплайны (Морфизмы)
  ".>"                 { return RazomyTypes.DOT_GT; }
  ".&"                 { return RazomyTypes.DOT_AMP; }
  ".|"                 { return RazomyTypes.DOT_PIPE; }
  ".?"                 { return RazomyTypes.DOT_QM; }
  ".."                 { return RazomyTypes.DOT_RANGE; }

  // Сравнения, логика, сдвиги, степень
  "=="                 { return RazomyTypes.EQ_EQ; }
  "!="                 { return RazomyTypes.NOT_EQ; }
  "<="                 { return RazomyTypes.LTE; }
  ">="                 { return RazomyTypes.GTE; }
  "&&"                 { return RazomyTypes.AND_AND; }
  "||"                 { return RazomyTypes.OR_OR; }
  "**"                 { return RazomyTypes.POW; }
  "<<"                 { return RazomyTypes.SHIFT_L; }
  ">>"                 { return RazomyTypes.SHIFT_R; }
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
  "%"                  { return RazomyTypes.MOD; }
  "!"                  { return RazomyTypes.BANG; }
  "?"                  { return RazomyTypes.QM; }

  // Разделители
  ","                  { return RazomyTypes.COMMA; }
  ":"                  { return RazomyTypes.COLON; }
  "_"                  { return RazomyTypes.UNDERSCORE; }
  ";"                  { return RazomyTypes.SEMICOLON; }

  // Ключевые слова-литералы
  "true"               { return RazomyTypes.TRUE; }
  "false"              { return RazomyTypes.FALSE; }

  // Идентификаторы и числа (FLOAT требует цифр после точки, чтобы 1..10 не ломался)
  {IDENTIFIER}         { return RazomyTypes.IDENTIFIER; }
  {HEX_NUMBER}         { return RazomyTypes.NUMBER; }
  {FLOAT_NUMBER}       { return RazomyTypes.NUMBER; }
  {INT_NUMBER}         { return RazomyTypes.NUMBER; }
  {STRING}             { return RazomyTypes.STRING; }

  // ВАЖНО: Разделение точки доступа (User.Id) и точки пайплайна (A . B, или . перед переносом строки / скобкой)
  "."/([ \t\r\n\{\[\(]) { return RazomyTypes.PIPE_DOT; }
  "."                  { return RazomyTypes.DOT; }
}

[^] { return BAD_CHARACTER; }