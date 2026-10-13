// This is a generated file. Not intended for manual editing.
package org.razomy.language.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static org.razomy.language.psi.RazomyTypes.*;
import static org.razomy.language.parser.RazomyParserUtil.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class RazomyParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    r = parse_root_(t, b);
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    return parse_root_(t, b, 0);
  }

  static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return razomyFile(b, l + 1);
  }

  /* ********************************************************** */
  // LANGLE (CRLF2 | SEMICOLON | statement)* RANGLE
  public static boolean angle_group(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "angle_group")) return false;
    if (!nextTokenIs(b, LANGLE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LANGLE);
    r = r && angle_group_1(b, l + 1);
    r = r && consumeToken(b, RANGLE);
    exit_section_(b, m, ANGLE_GROUP, r);
    return r;
  }

  // (CRLF2 | SEMICOLON | statement)*
  private static boolean angle_group_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "angle_group_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!angle_group_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "angle_group_1", c)) break;
    }
    return true;
  }

  // CRLF2 | SEMICOLON | statement
  private static boolean angle_group_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "angle_group_1_0")) return false;
    boolean r;
    r = consumeToken(b, CRLF2);
    if (!r) r = consumeToken(b, SEMICOLON);
    if (!r) r = statement(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // LBRACE (CRLF2 | SEMICOLON | statement)* RBRACE
  public static boolean brace_group(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "brace_group")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACE);
    r = r && brace_group_1(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, m, BRACE_GROUP, r);
    return r;
  }

  // (CRLF2 | SEMICOLON | statement)*
  private static boolean brace_group_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "brace_group_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!brace_group_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "brace_group_1", c)) break;
    }
    return true;
  }

  // CRLF2 | SEMICOLON | statement
  private static boolean brace_group_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "brace_group_1_0")) return false;
    boolean r;
    r = consumeToken(b, CRLF2);
    if (!r) r = consumeToken(b, SEMICOLON);
    if (!r) r = statement(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // LBRACKET (CRLF2 | SEMICOLON | statement)* RBRACKET
  public static boolean bracket_group(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bracket_group")) return false;
    if (!nextTokenIs(b, LBRACKET)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACKET);
    r = r && bracket_group_1(b, l + 1);
    r = r && consumeToken(b, RBRACKET);
    exit_section_(b, m, BRACKET_GROUP, r);
    return r;
  }

  // (CRLF2 | SEMICOLON | statement)*
  private static boolean bracket_group_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bracket_group_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!bracket_group_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "bracket_group_1", c)) break;
    }
    return true;
  }

  // CRLF2 | SEMICOLON | statement
  private static boolean bracket_group_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bracket_group_1_0")) return false;
    boolean r;
    r = consumeToken(b, CRLF2);
    if (!r) r = consumeToken(b, SEMICOLON);
    if (!r) r = statement(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // brace_group | paren_group | angle_group | bracket_group
  public static boolean group(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "group")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, GROUP, "<group>");
    r = brace_group(b, l + 1);
    if (!r) r = paren_group(b, l + 1);
    if (!r) r = angle_group(b, l + 1);
    if (!r) r = bracket_group(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // LPAREN (CRLF2 | SEMICOLON | statement)* RPAREN
  public static boolean paren_group(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "paren_group")) return false;
    if (!nextTokenIs(b, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAREN);
    r = r && paren_group_1(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, PAREN_GROUP, r);
    return r;
  }

  // (CRLF2 | SEMICOLON | statement)*
  private static boolean paren_group_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "paren_group_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!paren_group_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "paren_group_1", c)) break;
    }
    return true;
  }

  // CRLF2 | SEMICOLON | statement
  private static boolean paren_group_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "paren_group_1_0")) return false;
    boolean r;
    r = consumeToken(b, CRLF2);
    if (!r) r = consumeToken(b, SEMICOLON);
    if (!r) r = statement(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // IDENTIFIER (DOT IDENTIFIER)*
  public static boolean path(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "path")) return false;
    if (!nextTokenIs(b, IDENTIFIER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IDENTIFIER);
    r = r && path_1(b, l + 1);
    exit_section_(b, m, PATH, r);
    return r;
  }

  // (DOT IDENTIFIER)*
  private static boolean path_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "path_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!path_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "path_1", c)) break;
    }
    return true;
  }

  // DOT IDENTIFIER
  private static boolean path_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "path_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, DOT, IDENTIFIER);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // (CRLF2 | SEMICOLON | statement)*
  static boolean razomyFile(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "razomyFile")) return false;
    while (true) {
      int c = current_position_(b);
      if (!razomyFile_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "razomyFile", c)) break;
    }
    return true;
  }

  // CRLF2 | SEMICOLON | statement
  private static boolean razomyFile_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "razomyFile_0")) return false;
    boolean r;
    r = consumeToken(b, CRLF2);
    if (!r) r = consumeToken(b, SEMICOLON);
    if (!r) r = statement(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // token_item+
  public static boolean statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, STATEMENT, "<statement>");
    r = token_item(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!token_item(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "statement", c)) break;
    }
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // path | STRING | NUMBER | OP | group
  static boolean token_item(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "token_item")) return false;
    boolean r;
    r = path(b, l + 1);
    if (!r) r = consumeToken(b, STRING);
    if (!r) r = consumeToken(b, NUMBER);
    if (!r) r = consumeToken(b, OP);
    if (!r) r = group(b, l + 1);
    return r;
  }

}
