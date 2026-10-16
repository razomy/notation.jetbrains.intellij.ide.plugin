// This is a generated file. Not intended for manual editing.
package org.razomy.language.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static org.razomy.language.psi.RazomyTypes.*;
import static org.razomy.language.RazomyParserUtil.*;
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
    b = adapt_builder_(t, b, this, EXTENDS_SETS_);
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

  public static final TokenSet[] EXTENDS_SETS_ = new TokenSet[] {
    create_token_set_(ADD_EXPR, COMPARE_EXPR, EXPR, FLOW_BLOCK,
      FORM_BLOCK, LITERAL, LOGIC_EXPR, MERGE_EXPR,
      MUL_EXPR, PATH, PIPELINE_EXPR, POSTFIX_EXPR,
      POW_EXPR, SHIFT_EXPR, SLOT, SUBTRACT_EXPR,
      UNARY_EXPR, UNION_EXPR),
  };

  /* ********************************************************** */
  // mul_expr ( (CRLF2)* add_op (CRLF2)* mul_expr )*
  public static boolean add_expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "add_expr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, ADD_EXPR, "<add expr>");
    r = mul_expr(b, l + 1);
    r = r && add_expr_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ( (CRLF2)* add_op (CRLF2)* mul_expr )*
  private static boolean add_expr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "add_expr_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!add_expr_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "add_expr_1", c)) break;
    }
    return true;
  }

  // (CRLF2)* add_op (CRLF2)* mul_expr
  private static boolean add_expr_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "add_expr_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = add_expr_1_0_0(b, l + 1);
    r = r && add_op(b, l + 1);
    r = r && add_expr_1_0_2(b, l + 1);
    r = r && mul_expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (CRLF2)*
  private static boolean add_expr_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "add_expr_1_0_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CRLF2)) break;
      if (!empty_element_parsed_guard_(b, "add_expr_1_0_0", c)) break;
    }
    return true;
  }

  // (CRLF2)*
  private static boolean add_expr_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "add_expr_1_0_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CRLF2)) break;
      if (!empty_element_parsed_guard_(b, "add_expr_1_0_2", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // PLUS | MINUS
  public static boolean add_op(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "add_op")) return false;
    if (!nextTokenIs(b, "<add op>", MINUS, PLUS)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ADD_OP, "<add op>");
    r = consumeToken(b, PLUS);
    if (!r) r = consumeToken(b, MINUS);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // shift_expr ( (CRLF2)* compare_op (CRLF2)* shift_expr )*
  public static boolean compare_expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "compare_expr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, COMPARE_EXPR, "<compare expr>");
    r = shift_expr(b, l + 1);
    r = r && compare_expr_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ( (CRLF2)* compare_op (CRLF2)* shift_expr )*
  private static boolean compare_expr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "compare_expr_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!compare_expr_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "compare_expr_1", c)) break;
    }
    return true;
  }

  // (CRLF2)* compare_op (CRLF2)* shift_expr
  private static boolean compare_expr_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "compare_expr_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = compare_expr_1_0_0(b, l + 1);
    r = r && compare_op(b, l + 1);
    r = r && compare_expr_1_0_2(b, l + 1);
    r = r && shift_expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (CRLF2)*
  private static boolean compare_expr_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "compare_expr_1_0_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CRLF2)) break;
      if (!empty_element_parsed_guard_(b, "compare_expr_1_0_0", c)) break;
    }
    return true;
  }

  // (CRLF2)*
  private static boolean compare_expr_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "compare_expr_1_0_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CRLF2)) break;
      if (!empty_element_parsed_guard_(b, "compare_expr_1_0_2", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // EQ_EQ | NOT_EQ | LANGLE | RANGLE | LTE | GTE
  public static boolean compare_op(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "compare_op")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, COMPARE_OP, "<compare op>");
    r = consumeToken(b, EQ_EQ);
    if (!r) r = consumeToken(b, NOT_EQ);
    if (!r) r = consumeToken(b, LANGLE);
    if (!r) r = consumeToken(b, RANGLE);
    if (!r) r = consumeToken(b, LTE);
    if (!r) r = consumeToken(b, GTE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // expr expr*
  public static boolean declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "declaration")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, DECLARATION, "<declaration>");
    r = expr(b, l + 1);
    p = r; // pin = 1
    r = r && declaration_1(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // expr*
  private static boolean declaration_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "declaration_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expr(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "declaration_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // pipeline_expr
  public static boolean expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, EXPR, "<expr>");
    r = pipeline_expr(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // LBRACKET (CRLF2 | SEMICOLON | COMMA | flow_item)* RBRACKET
  public static boolean flow_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "flow_block")) return false;
    if (!nextTokenIs(b, LBRACKET)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACKET);
    r = r && flow_block_1(b, l + 1);
    r = r && consumeToken(b, RBRACKET);
    exit_section_(b, m, FLOW_BLOCK, r);
    return r;
  }

  // (CRLF2 | SEMICOLON | COMMA | flow_item)*
  private static boolean flow_block_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "flow_block_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!flow_block_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "flow_block_1", c)) break;
    }
    return true;
  }

  // CRLF2 | SEMICOLON | COMMA | flow_item
  private static boolean flow_block_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "flow_block_1_0")) return false;
    boolean r;
    r = consumeToken(b, CRLF2);
    if (!r) r = consumeToken(b, SEMICOLON);
    if (!r) r = consumeToken(b, COMMA);
    if (!r) r = flow_item(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // expr expr*
  public static boolean flow_item(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "flow_item")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, FLOW_ITEM, "<flow item>");
    r = expr(b, l + 1);
    p = r; // pin = 1
    r = r && flow_item_1(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // expr*
  private static boolean flow_item_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "flow_item_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expr(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "flow_item_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // LBRACE (CRLF2 | SEMICOLON | COMMA | form_item)* RBRACE
  public static boolean form_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "form_block")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACE);
    r = r && form_block_1(b, l + 1);
    r = r && consumeToken(b, RBRACE);
    exit_section_(b, m, FORM_BLOCK, r);
    return r;
  }

  // (CRLF2 | SEMICOLON | COMMA | form_item)*
  private static boolean form_block_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "form_block_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!form_block_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "form_block_1", c)) break;
    }
    return true;
  }

  // CRLF2 | SEMICOLON | COMMA | form_item
  private static boolean form_block_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "form_block_1_0")) return false;
    boolean r;
    r = consumeToken(b, CRLF2);
    if (!r) r = consumeToken(b, SEMICOLON);
    if (!r) r = consumeToken(b, COMMA);
    if (!r) r = form_item(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // expr expr*
  public static boolean form_item(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "form_item")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, FORM_ITEM, "<form item>");
    r = expr(b, l + 1);
    p = r; // pin = 1
    r = r && form_item_1(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // expr*
  private static boolean form_item_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "form_item_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expr(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "form_item_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // NUMBER | STRING | TRUE | FALSE
  public static boolean literal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "literal")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, LITERAL, "<literal>");
    r = consumeToken(b, NUMBER);
    if (!r) r = consumeToken(b, STRING);
    if (!r) r = consumeToken(b, TRUE);
    if (!r) r = consumeToken(b, FALSE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // compare_expr ( (CRLF2)* logic_op (CRLF2)* compare_expr )*
  public static boolean logic_expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logic_expr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, LOGIC_EXPR, "<logic expr>");
    r = compare_expr(b, l + 1);
    r = r && logic_expr_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ( (CRLF2)* logic_op (CRLF2)* compare_expr )*
  private static boolean logic_expr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logic_expr_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!logic_expr_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "logic_expr_1", c)) break;
    }
    return true;
  }

  // (CRLF2)* logic_op (CRLF2)* compare_expr
  private static boolean logic_expr_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logic_expr_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = logic_expr_1_0_0(b, l + 1);
    r = r && logic_op(b, l + 1);
    r = r && logic_expr_1_0_2(b, l + 1);
    r = r && compare_expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (CRLF2)*
  private static boolean logic_expr_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logic_expr_1_0_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CRLF2)) break;
      if (!empty_element_parsed_guard_(b, "logic_expr_1_0_0", c)) break;
    }
    return true;
  }

  // (CRLF2)*
  private static boolean logic_expr_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logic_expr_1_0_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CRLF2)) break;
      if (!empty_element_parsed_guard_(b, "logic_expr_1_0_2", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // AND_AND | OR_OR
  public static boolean logic_op(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logic_op")) return false;
    if (!nextTokenIs(b, "<logic op>", AND_AND, OR_OR)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, LOGIC_OP, "<logic op>");
    r = consumeToken(b, AND_AND);
    if (!r) r = consumeToken(b, OR_OR);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // subtract_expr ( (CRLF2)* AMP (CRLF2)* subtract_expr )*
  public static boolean merge_expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "merge_expr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, MERGE_EXPR, "<merge expr>");
    r = subtract_expr(b, l + 1);
    r = r && merge_expr_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ( (CRLF2)* AMP (CRLF2)* subtract_expr )*
  private static boolean merge_expr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "merge_expr_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!merge_expr_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "merge_expr_1", c)) break;
    }
    return true;
  }

  // (CRLF2)* AMP (CRLF2)* subtract_expr
  private static boolean merge_expr_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "merge_expr_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = merge_expr_1_0_0(b, l + 1);
    r = r && consumeToken(b, AMP);
    r = r && merge_expr_1_0_2(b, l + 1);
    r = r && subtract_expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (CRLF2)*
  private static boolean merge_expr_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "merge_expr_1_0_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CRLF2)) break;
      if (!empty_element_parsed_guard_(b, "merge_expr_1_0_0", c)) break;
    }
    return true;
  }

  // (CRLF2)*
  private static boolean merge_expr_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "merge_expr_1_0_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CRLF2)) break;
      if (!empty_element_parsed_guard_(b, "merge_expr_1_0_2", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // pow_expr ( (CRLF2)* mul_op (CRLF2)* pow_expr )*
  public static boolean mul_expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "mul_expr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, MUL_EXPR, "<mul expr>");
    r = pow_expr(b, l + 1);
    r = r && mul_expr_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ( (CRLF2)* mul_op (CRLF2)* pow_expr )*
  private static boolean mul_expr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "mul_expr_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!mul_expr_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "mul_expr_1", c)) break;
    }
    return true;
  }

  // (CRLF2)* mul_op (CRLF2)* pow_expr
  private static boolean mul_expr_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "mul_expr_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = mul_expr_1_0_0(b, l + 1);
    r = r && mul_op(b, l + 1);
    r = r && mul_expr_1_0_2(b, l + 1);
    r = r && pow_expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (CRLF2)*
  private static boolean mul_expr_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "mul_expr_1_0_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CRLF2)) break;
      if (!empty_element_parsed_guard_(b, "mul_expr_1_0_0", c)) break;
    }
    return true;
  }

  // (CRLF2)*
  private static boolean mul_expr_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "mul_expr_1_0_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CRLF2)) break;
      if (!empty_element_parsed_guard_(b, "mul_expr_1_0_2", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // MUL | MOD
  public static boolean mul_op(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "mul_op")) return false;
    if (!nextTokenIs(b, "<mul op>", MOD, MUL)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, MUL_OP, "<mul op>");
    r = consumeToken(b, MUL);
    if (!r) r = consumeToken(b, MOD);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // path_root (DOT path_node)*
  public static boolean path(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "path")) return false;
    if (!nextTokenIs(b, "<path>", IDENTIFIER, UNDERSCORE)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PATH, "<path>");
    r = path_root(b, l + 1);
    r = r && path_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (DOT path_node)*
  private static boolean path_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "path_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!path_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "path_1", c)) break;
    }
    return true;
  }

  // DOT path_node
  private static boolean path_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "path_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DOT);
    r = r && path_node(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // IDENTIFIER | NUMBER | (MINUS NUMBER)
  static boolean path_node(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "path_node")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, IDENTIFIER);
    if (!r) r = consumeToken(b, NUMBER);
    if (!r) r = path_node_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // MINUS NUMBER
  private static boolean path_node_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "path_node_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, MINUS, NUMBER);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // IDENTIFIER | UNDERSCORE
  static boolean path_root(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "path_root")) return false;
    if (!nextTokenIs(b, "", IDENTIFIER, UNDERSCORE)) return false;
    boolean r;
    r = consumeToken(b, IDENTIFIER);
    if (!r) r = consumeToken(b, UNDERSCORE);
    return r;
  }

  /* ********************************************************** */
  // union_expr ( (CRLF2)* pipeline_op (CRLF2)* union_expr )*
  public static boolean pipeline_expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pipeline_expr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, PIPELINE_EXPR, "<pipeline expr>");
    r = union_expr(b, l + 1);
    r = r && pipeline_expr_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ( (CRLF2)* pipeline_op (CRLF2)* union_expr )*
  private static boolean pipeline_expr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pipeline_expr_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!pipeline_expr_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "pipeline_expr_1", c)) break;
    }
    return true;
  }

  // (CRLF2)* pipeline_op (CRLF2)* union_expr
  private static boolean pipeline_expr_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pipeline_expr_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = pipeline_expr_1_0_0(b, l + 1);
    r = r && pipeline_op(b, l + 1);
    r = r && pipeline_expr_1_0_2(b, l + 1);
    r = r && union_expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (CRLF2)*
  private static boolean pipeline_expr_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pipeline_expr_1_0_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CRLF2)) break;
      if (!empty_element_parsed_guard_(b, "pipeline_expr_1_0_0", c)) break;
    }
    return true;
  }

  // (CRLF2)*
  private static boolean pipeline_expr_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pipeline_expr_1_0_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CRLF2)) break;
      if (!empty_element_parsed_guard_(b, "pipeline_expr_1_0_2", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // DOT_GT | DOT_AMP | DOT_PIPE | DOT_QM | DOT_RANGE | PIPE_DOT
  public static boolean pipeline_op(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pipeline_op")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PIPELINE_OP, "<pipeline op>");
    r = consumeToken(b, DOT_GT);
    if (!r) r = consumeToken(b, DOT_AMP);
    if (!r) r = consumeToken(b, DOT_PIPE);
    if (!r) r = consumeToken(b, DOT_QM);
    if (!r) r = consumeToken(b, DOT_RANGE);
    if (!r) r = consumeToken(b, PIPE_DOT);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // primary_expr postfix_suffix*
  public static boolean postfix_expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "postfix_expr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, POSTFIX_EXPR, "<postfix expr>");
    r = primary_expr(b, l + 1);
    r = r && postfix_expr_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // postfix_suffix*
  private static boolean postfix_expr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "postfix_expr_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!postfix_suffix(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "postfix_expr_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // slot | form_block | flow_block
  static boolean postfix_suffix(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "postfix_suffix")) return false;
    boolean r;
    r = slot(b, l + 1);
    if (!r) r = form_block(b, l + 1);
    if (!r) r = flow_block(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // unary_expr ( (CRLF2)* POW (CRLF2)* unary_expr )*
  public static boolean pow_expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pow_expr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, POW_EXPR, "<pow expr>");
    r = unary_expr(b, l + 1);
    r = r && pow_expr_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ( (CRLF2)* POW (CRLF2)* unary_expr )*
  private static boolean pow_expr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pow_expr_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!pow_expr_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "pow_expr_1", c)) break;
    }
    return true;
  }

  // (CRLF2)* POW (CRLF2)* unary_expr
  private static boolean pow_expr_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pow_expr_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = pow_expr_1_0_0(b, l + 1);
    r = r && consumeToken(b, POW);
    r = r && pow_expr_1_0_2(b, l + 1);
    r = r && unary_expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (CRLF2)*
  private static boolean pow_expr_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pow_expr_1_0_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CRLF2)) break;
      if (!empty_element_parsed_guard_(b, "pow_expr_1_0_0", c)) break;
    }
    return true;
  }

  // (CRLF2)*
  private static boolean pow_expr_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "pow_expr_1_0_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CRLF2)) break;
      if (!empty_element_parsed_guard_(b, "pow_expr_1_0_2", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // literal
  //                        | form_block
  //                        | flow_block
  //                        | slot
  //                        | path
  static boolean primary_expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "primary_expr")) return false;
    boolean r;
    r = literal(b, l + 1);
    if (!r) r = form_block(b, l + 1);
    if (!r) r = flow_block(b, l + 1);
    if (!r) r = slot(b, l + 1);
    if (!r) r = path(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // (CRLF2 | SEMICOLON | declaration)*
  static boolean razomyFile(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "razomyFile")) return false;
    while (true) {
      int c = current_position_(b);
      if (!razomyFile_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "razomyFile", c)) break;
    }
    return true;
  }

  // CRLF2 | SEMICOLON | declaration
  private static boolean razomyFile_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "razomyFile_0")) return false;
    boolean r;
    r = consumeToken(b, CRLF2);
    if (!r) r = consumeToken(b, SEMICOLON);
    if (!r) r = declaration(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // add_expr ( (CRLF2)* shift_op (CRLF2)* add_expr )*
  public static boolean shift_expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "shift_expr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, SHIFT_EXPR, "<shift expr>");
    r = add_expr(b, l + 1);
    r = r && shift_expr_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ( (CRLF2)* shift_op (CRLF2)* add_expr )*
  private static boolean shift_expr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "shift_expr_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!shift_expr_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "shift_expr_1", c)) break;
    }
    return true;
  }

  // (CRLF2)* shift_op (CRLF2)* add_expr
  private static boolean shift_expr_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "shift_expr_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = shift_expr_1_0_0(b, l + 1);
    r = r && shift_op(b, l + 1);
    r = r && shift_expr_1_0_2(b, l + 1);
    r = r && add_expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (CRLF2)*
  private static boolean shift_expr_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "shift_expr_1_0_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CRLF2)) break;
      if (!empty_element_parsed_guard_(b, "shift_expr_1_0_0", c)) break;
    }
    return true;
  }

  // (CRLF2)*
  private static boolean shift_expr_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "shift_expr_1_0_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CRLF2)) break;
      if (!empty_element_parsed_guard_(b, "shift_expr_1_0_2", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // SHIFT_L | SHIFT_R
  public static boolean shift_op(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "shift_op")) return false;
    if (!nextTokenIs(b, "<shift op>", SHIFT_L, SHIFT_R)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, SHIFT_OP, "<shift op>");
    r = consumeToken(b, SHIFT_L);
    if (!r) r = consumeToken(b, SHIFT_R);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // LPAREN (CRLF2 | SEMICOLON | COMMA | slot_item)* RPAREN
  public static boolean slot(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slot")) return false;
    if (!nextTokenIs(b, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAREN);
    r = r && slot_1(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, SLOT, r);
    return r;
  }

  // (CRLF2 | SEMICOLON | COMMA | slot_item)*
  private static boolean slot_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slot_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!slot_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "slot_1", c)) break;
    }
    return true;
  }

  // CRLF2 | SEMICOLON | COMMA | slot_item
  private static boolean slot_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slot_1_0")) return false;
    boolean r;
    r = consumeToken(b, CRLF2);
    if (!r) r = consumeToken(b, SEMICOLON);
    if (!r) r = consumeToken(b, COMMA);
    if (!r) r = slot_item(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // expr expr*
  public static boolean slot_item(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slot_item")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, SLOT_ITEM, "<slot item>");
    r = expr(b, l + 1);
    p = r; // pin = 1
    r = r && slot_item_1(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // expr*
  private static boolean slot_item_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "slot_item_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expr(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "slot_item_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // logic_expr ( (CRLF2)* SLASH (CRLF2)* logic_expr )*
  public static boolean subtract_expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subtract_expr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, SUBTRACT_EXPR, "<subtract expr>");
    r = logic_expr(b, l + 1);
    r = r && subtract_expr_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ( (CRLF2)* SLASH (CRLF2)* logic_expr )*
  private static boolean subtract_expr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subtract_expr_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!subtract_expr_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "subtract_expr_1", c)) break;
    }
    return true;
  }

  // (CRLF2)* SLASH (CRLF2)* logic_expr
  private static boolean subtract_expr_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subtract_expr_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = subtract_expr_1_0_0(b, l + 1);
    r = r && consumeToken(b, SLASH);
    r = r && subtract_expr_1_0_2(b, l + 1);
    r = r && logic_expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (CRLF2)*
  private static boolean subtract_expr_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subtract_expr_1_0_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CRLF2)) break;
      if (!empty_element_parsed_guard_(b, "subtract_expr_1_0_0", c)) break;
    }
    return true;
  }

  // (CRLF2)*
  private static boolean subtract_expr_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subtract_expr_1_0_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CRLF2)) break;
      if (!empty_element_parsed_guard_(b, "subtract_expr_1_0_2", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // unary_op+ postfix_expr | postfix_expr
  public static boolean unary_expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unary_expr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, UNARY_EXPR, "<unary expr>");
    r = unary_expr_0(b, l + 1);
    if (!r) r = postfix_expr(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // unary_op+ postfix_expr
  private static boolean unary_expr_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unary_expr_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = unary_expr_0_0(b, l + 1);
    r = r && postfix_expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // unary_op+
  private static boolean unary_expr_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unary_expr_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = unary_op(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!unary_op(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "unary_expr_0_0", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // BANG | MINUS
  public static boolean unary_op(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unary_op")) return false;
    if (!nextTokenIs(b, "<unary op>", BANG, MINUS)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, UNARY_OP, "<unary op>");
    r = consumeToken(b, BANG);
    if (!r) r = consumeToken(b, MINUS);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // merge_expr ( (CRLF2)* PIPE (CRLF2)* merge_expr )*
  public static boolean union_expr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "union_expr")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, UNION_EXPR, "<union expr>");
    r = merge_expr(b, l + 1);
    r = r && union_expr_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ( (CRLF2)* PIPE (CRLF2)* merge_expr )*
  private static boolean union_expr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "union_expr_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!union_expr_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "union_expr_1", c)) break;
    }
    return true;
  }

  // (CRLF2)* PIPE (CRLF2)* merge_expr
  private static boolean union_expr_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "union_expr_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = union_expr_1_0_0(b, l + 1);
    r = r && consumeToken(b, PIPE);
    r = r && union_expr_1_0_2(b, l + 1);
    r = r && merge_expr(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (CRLF2)*
  private static boolean union_expr_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "union_expr_1_0_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CRLF2)) break;
      if (!empty_element_parsed_guard_(b, "union_expr_1_0_0", c)) break;
    }
    return true;
  }

  // (CRLF2)*
  private static boolean union_expr_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "union_expr_1_0_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CRLF2)) break;
      if (!empty_element_parsed_guard_(b, "union_expr_1_0_2", c)) break;
    }
    return true;
  }

}
