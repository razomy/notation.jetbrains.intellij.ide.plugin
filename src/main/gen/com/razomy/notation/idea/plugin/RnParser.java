// This is a generated file. Not intended for manual editing.
package com.razomy.notation.idea.plugin;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.razomy.notation.idea.plugin.RnTypes.*;
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
  // KEY (SEPARATOR VALUE)? (END | CHILD_DEEP PROPERTY* | DEEP)
  public static boolean PROPERTY(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PROPERTY")) return false;
    if (!nextTokenIs(b, KEY)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KEY);
    r = r && PROPERTY_1(b, l + 1);
    r = r && PROPERTY_2(b, l + 1);
    exit_section_(b, m, PROPERTY, r);
    return r;
  }

  // (SEPARATOR VALUE)?
  private static boolean PROPERTY_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PROPERTY_1")) return false;
    PROPERTY_1_0(b, l + 1);
    return true;
  }

  // SEPARATOR VALUE
  private static boolean PROPERTY_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PROPERTY_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, SEPARATOR, VALUE);
    exit_section_(b, m, null, r);
    return r;
  }

  // END | CHILD_DEEP PROPERTY* | DEEP
  private static boolean PROPERTY_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PROPERTY_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, END);
    if (!r) r = PROPERTY_2_1(b, l + 1);
    if (!r) r = consumeToken(b, DEEP);
    exit_section_(b, m, null, r);
    return r;
  }

  // CHILD_DEEP PROPERTY*
  private static boolean PROPERTY_2_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PROPERTY_2_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CHILD_DEEP);
    r = r && PROPERTY_2_1_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // PROPERTY*
  private static boolean PROPERTY_2_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "PROPERTY_2_1_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!PROPERTY(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "PROPERTY_2_1_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // item_*
  static boolean RnFile(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "RnFile")) return false;
    while (true) {
      int c = current_position_(b);
      if (!item_(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "RnFile", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // COMMENT|PROPERTY|DEEP|EMPTY_LINE|ERROR
  static boolean item_(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "item_")) return false;
    boolean r;
    r = consumeToken(b, COMMENT);
    if (!r) r = PROPERTY(b, l + 1);
    if (!r) r = consumeToken(b, DEEP);
    if (!r) r = consumeToken(b, EMPTY_LINE);
    if (!r) r = consumeToken(b, ERROR);
    return r;
  }

}
