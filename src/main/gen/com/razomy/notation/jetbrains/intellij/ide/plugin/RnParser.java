// This is a generated file. Not intended for manual editing.
package com.razomy.notation.jetbrains.intellij.ide.plugin;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.razomy.notation.jetbrains.intellij.ide.plugin.RnTypes.*;
import static com.razomy.notation.jetbrains.intellij.ide.plugin.RnParserUtil.*;
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
  // (VALUE CHILD_DEEP INLINE_PROPERTY+)
  //              | (VALUE ASSIGN INLINE_PROPERTY+ BREAK)
  //              | (VALUE ASSIGN INLINE_PROPERTY+)
  //              | (VALUE BREAK)
  //              | VALUE
  //              | PROPERTY
  public static boolean INLINE_PROPERTY(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "INLINE_PROPERTY")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, INLINE_PROPERTY, "<inline property>");
    r = INLINE_PROPERTY_0(b, l + 1);
    if (!r) r = INLINE_PROPERTY_1(b, l + 1);
    if (!r) r = INLINE_PROPERTY_2(b, l + 1);
    if (!r) r = INLINE_PROPERTY_3(b, l + 1);
    if (!r) r = consumeToken(b, VALUE);
    if (!r) r = PROPERTY(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // VALUE CHILD_DEEP INLINE_PROPERTY+
  private static boolean INLINE_PROPERTY_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "INLINE_PROPERTY_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, VALUE, CHILD_DEEP);
    r = r && INLINE_PROPERTY_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // INLINE_PROPERTY+
  private static boolean INLINE_PROPERTY_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "INLINE_PROPERTY_0_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = INLINE_PROPERTY(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!INLINE_PROPERTY(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "INLINE_PROPERTY_0_2", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // VALUE ASSIGN INLINE_PROPERTY+ BREAK
  private static boolean INLINE_PROPERTY_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "INLINE_PROPERTY_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, VALUE, ASSIGN);
    r = r && INLINE_PROPERTY_1_2(b, l + 1);
    r = r && consumeToken(b, BREAK);
    exit_section_(b, m, null, r);
    return r;
  }

  // INLINE_PROPERTY+
  private static boolean INLINE_PROPERTY_1_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "INLINE_PROPERTY_1_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = INLINE_PROPERTY(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!INLINE_PROPERTY(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "INLINE_PROPERTY_1_2", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // VALUE ASSIGN INLINE_PROPERTY+
  private static boolean INLINE_PROPERTY_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "INLINE_PROPERTY_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, VALUE, ASSIGN);
    r = r && INLINE_PROPERTY_2_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // INLINE_PROPERTY+
  private static boolean INLINE_PROPERTY_2_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "INLINE_PROPERTY_2_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = INLINE_PROPERTY(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!INLINE_PROPERTY(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "INLINE_PROPERTY_2_2", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // VALUE BREAK
  private static boolean INLINE_PROPERTY_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "INLINE_PROPERTY_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, VALUE, BREAK);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // (KEY CHILD_DEEP PROPERTY+ END)
  //              | (VALUE CHILD_DEEP PROPERTY+ END)
  //              | (KEY END)
  //              | (KEY ASSIGN INLINE_PROPERTY+ END)
  //              | (COMMENT)
  public static boolean PROPERTY(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PROPERTY")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PROPERTY, "<property>");
    r = PROPERTY_0(b, l + 1);
    if (!r) r = PROPERTY_1(b, l + 1);
    if (!r) r = PROPERTY_2(b, l + 1);
    if (!r) r = PROPERTY_3(b, l + 1);
    if (!r) r = consumeToken(b, COMMENT);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // KEY CHILD_DEEP PROPERTY+ END
  private static boolean PROPERTY_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PROPERTY_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, KEY, CHILD_DEEP);
    r = r && PROPERTY_0_2(b, l + 1);
    r = r && consumeToken(b, END);
    exit_section_(b, m, null, r);
    return r;
  }

  // PROPERTY+
  private static boolean PROPERTY_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PROPERTY_0_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = PROPERTY(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!PROPERTY(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "PROPERTY_0_2", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // VALUE CHILD_DEEP PROPERTY+ END
  private static boolean PROPERTY_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PROPERTY_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, VALUE, CHILD_DEEP);
    r = r && PROPERTY_1_2(b, l + 1);
    r = r && consumeToken(b, END);
    exit_section_(b, m, null, r);
    return r;
  }

  // PROPERTY+
  private static boolean PROPERTY_1_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PROPERTY_1_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = PROPERTY(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!PROPERTY(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "PROPERTY_1_2", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // KEY END
  private static boolean PROPERTY_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PROPERTY_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, KEY, END);
    exit_section_(b, m, null, r);
    return r;
  }

  // KEY ASSIGN INLINE_PROPERTY+ END
  private static boolean PROPERTY_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PROPERTY_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, KEY, ASSIGN);
    r = r && PROPERTY_3_2(b, l + 1);
    r = r && consumeToken(b, END);
    exit_section_(b, m, null, r);
    return r;
  }

  // INLINE_PROPERTY+
  private static boolean PROPERTY_3_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PROPERTY_3_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = INLINE_PROPERTY(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!INLINE_PROPERTY(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "PROPERTY_3_2", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // START*
  static boolean RnFile(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RnFile")) return false;
    while (true) {
      int c = current_position_(b);
      if (!START(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "RnFile", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // COMMENT
  //                     | (PROPERTY EMPTY_LINE)
  //                     | ERROR
  static boolean START(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "START")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMENT);
    if (!r) r = START_1(b, l + 1);
    if (!r) r = consumeToken(b, ERROR);
    exit_section_(b, m, null, r);
    return r;
  }

  // PROPERTY EMPTY_LINE
  private static boolean START_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "START_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = PROPERTY(b, l + 1);
    r = r && consumeToken(b, EMPTY_LINE);
    exit_section_(b, m, null, r);
    return r;
  }

}
