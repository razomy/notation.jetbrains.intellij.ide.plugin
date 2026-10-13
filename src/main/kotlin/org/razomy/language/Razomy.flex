%%

// ВАЖНО: Только горизонтальные пробелы и табы! (без \n)
WHITE_SPACE=[ \t\r]+
CRLF=\R
LINE_COMMENT="//".*

IDENTIFIER=[a-zA-Z_][a-zA-Z0-9_]*
NUMBER=[0-9]+(\.[0-9]+)?
STRING=\"[^\"]*\"|'[^']*'
OP=[&|\+\-\*/\!\?@\#\$%\^~=]+

%%
<YYINITIAL> {
  {WHITE_SPACE}        { return WHITE_SPACE; }
  {CRLF}               { return CRLF2; }
  {LINE_COMMENT}       { return LINE_COMMENT; }

  "{"                  { return LBRACE; }
  "}"                  { return RBRACE; }
  "("                  { return LPAREN; }
  ")"                  { return RPAREN; }
  "<"                  { return LANGLE; }
  ">"                  { return RANGLE; }
  "["                  { return LBRACKET; }
  "]"                  { return RBRACKET; }
  "."                  { return DOT; }
  ";"                  { return SEMICOLON; }

  {IDENTIFIER}         { return IDENTIFIER; }
  {NUMBER}             { return NUMBER; }
  {STRING}             { return STRING; }
  {OP}                 { return OP; }
}

[^] { return BAD_CHARACTER; }