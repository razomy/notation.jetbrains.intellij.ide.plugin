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
  // (COMMENT END)|property|ERROR
  static boolean item_(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "item_")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = item__0(b, l + 1);
    if (!r) r = property(b, l + 1);
    if (!r) r = consumeToken(b, ERROR);
    exit_section_(b, m, null, r);
    return r;
  }

  // COMMENT END
  private static boolean item__0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "item__0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, COMMENT, END);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // (KEY END) | (KEY SEPARATOR VALUE END)| (KEY DEEP property) | (KEY SEPARATOR VALUE DEEP property)
  public static boolean property(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "property")) return false;
    if (!nextTokenIs(b, KEY)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = property_0(b, l + 1);
    if (!r) r = property_1(b, l + 1);
    if (!r) r = property_2(b, l + 1);
    if (!r) r = property_3(b, l + 1);
    exit_section_(b, m, PROPERTY, r);
    return r;
  }

  // KEY END
  private static boolean property_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "property_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, KEY, END);
    exit_section_(b, m, null, r);
    return r;
  }

  // KEY SEPARATOR VALUE END
  private static boolean property_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "property_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, KEY, SEPARATOR, VALUE, END);
    exit_section_(b, m, null, r);
    return r;
  }

  // KEY DEEP property
  private static boolean property_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "property_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, KEY, DEEP);
    r = r && property(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // KEY SEPARATOR VALUE DEEP property
  private static boolean property_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "property_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, KEY, SEPARATOR, VALUE, DEEP);
    r = r && property(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

}
