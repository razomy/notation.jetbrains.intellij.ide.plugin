// This is a generated file. Not intended for manual editing.
package com.razomy.notation.jetbrains.intellij.ide.plugin;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.razomy.notation.jetbrains.intellij.ide.plugin.RnTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class RnParser implements PsiParser, LightPsiParser {

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
    return RnFile(b, l + 1);
  }

  /* ********************************************************** */
  // start*
  static boolean RnFile(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RnFile")) return false;
    while (true) {
      int c = current_position_(b);
      if (!start(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "RnFile", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // (value CHILD_DEEP inline_property+)
  //              | (value ASSIGN inline_property+ BREAK)
  //              | (value ASSIGN inline_property+)
  //              | (value BREAK)
  //              | value
  //              | multiline_property
  static boolean inline_property(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inline_property")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = inline_property_0(b, l + 1);
    if (!r) r = inline_property_1(b, l + 1);
    if (!r) r = inline_property_2(b, l + 1);
    if (!r) r = inline_property_3(b, l + 1);
    if (!r) r = value(b, l + 1);
    if (!r) r = multiline_property(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // value CHILD_DEEP inline_property+
  private static boolean inline_property_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inline_property_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = value(b, l + 1);
    r = r && consumeToken(b, CHILD_DEEP);
    r = r && inline_property_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // inline_property+
  private static boolean inline_property_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inline_property_0_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = inline_property(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!inline_property(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "inline_property_0_2", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // value ASSIGN inline_property+ BREAK
  private static boolean inline_property_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inline_property_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = value(b, l + 1);
    r = r && consumeToken(b, ASSIGN);
    r = r && inline_property_1_2(b, l + 1);
    r = r && consumeToken(b, BREAK);
    exit_section_(b, m, null, r);
    return r;
  }

  // inline_property+
  private static boolean inline_property_1_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inline_property_1_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = inline_property(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!inline_property(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "inline_property_1_2", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // value ASSIGN inline_property+
  private static boolean inline_property_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inline_property_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = value(b, l + 1);
    r = r && consumeToken(b, ASSIGN);
    r = r && inline_property_2_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // inline_property+
  private static boolean inline_property_2_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inline_property_2_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = inline_property(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!inline_property(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "inline_property_2_2", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // value BREAK
  private static boolean inline_property_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inline_property_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = value(b, l + 1);
    r = r && consumeToken(b, BREAK);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // (KEY CHILD_DEEP multiline_property+ END)
  //              | (value CHILD_DEEP multiline_property+ END)
  //              | (KEY END)
  //              | (KEY ASSIGN inline_property+ END)
  //              | (COMMENT)
  static boolean multiline_property(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "multiline_property")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = multiline_property_0(b, l + 1);
    if (!r) r = multiline_property_1(b, l + 1);
    if (!r) r = multiline_property_2(b, l + 1);
    if (!r) r = multiline_property_3(b, l + 1);
    if (!r) r = consumeToken(b, COMMENT);
    exit_section_(b, m, null, r);
    return r;
  }

  // KEY CHILD_DEEP multiline_property+ END
  private static boolean multiline_property_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "multiline_property_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, KEY, CHILD_DEEP);
    r = r && multiline_property_0_2(b, l + 1);
    r = r && consumeToken(b, END);
    exit_section_(b, m, null, r);
    return r;
  }

  // multiline_property+
  private static boolean multiline_property_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "multiline_property_0_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = multiline_property(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!multiline_property(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "multiline_property_0_2", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // value CHILD_DEEP multiline_property+ END
  private static boolean multiline_property_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "multiline_property_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = value(b, l + 1);
    r = r && consumeToken(b, CHILD_DEEP);
    r = r && multiline_property_1_2(b, l + 1);
    r = r && consumeToken(b, END);
    exit_section_(b, m, null, r);
    return r;
  }

  // multiline_property+
  private static boolean multiline_property_1_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "multiline_property_1_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = multiline_property(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!multiline_property(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "multiline_property_1_2", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // KEY END
  private static boolean multiline_property_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "multiline_property_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, KEY, END);
    exit_section_(b, m, null, r);
    return r;
  }

  // KEY ASSIGN inline_property+ END
  private static boolean multiline_property_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "multiline_property_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, KEY, ASSIGN);
    r = r && multiline_property_3_2(b, l + 1);
    r = r && consumeToken(b, END);
    exit_section_(b, m, null, r);
    return r;
  }

  // inline_property+
  private static boolean multiline_property_3_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "multiline_property_3_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = inline_property(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!inline_property(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "multiline_property_3_2", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // inline_property | multiline_property
  public static boolean property(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "property")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PROPERTY, "<property>");
    r = inline_property(b, l + 1);
    if (!r) r = multiline_property(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // COMMENT
  //                     | (property EMPTY_LINE)
  //                     | ERROR
  static boolean start(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "start")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMENT);
    if (!r) r = start_1(b, l + 1);
    if (!r) r = consumeToken(b, ERROR);
    exit_section_(b, m, null, r);
    return r;
  }

  // property EMPTY_LINE
  private static boolean start_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "start_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = property(b, l + 1);
    r = r && consumeToken(b, EMPTY_LINE);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // VALUE
  public static boolean value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "value")) return false;
    if (!nextTokenIs(b, VALUE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, VALUE);
    exit_section_(b, m, VALUE, r);
    return r;
  }

}
