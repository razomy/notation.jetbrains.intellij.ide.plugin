// This is a generated file. Not intended for manual editing.
package com.razomy.notation.jetbrains.intellij.ide.plugin;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.razomy.notation.jetbrains.intellij.ide.plugin.impl.*;

public interface RnTypes {

  IElementType INLINE_PROPERTY = new RnElementType("INLINE_PROPERTY");
  IElementType PROPERTY = new RnElementType("PROPERTY");

  IElementType ASSIGN = new RnTokenType("ASSIGN");
  IElementType BREAK = new RnTokenType("BREAK");
  IElementType CHILD_DEEP = new RnTokenType("CHILD_DEEP");
  IElementType COMMENT = new RnTokenType("COMMENT");
  IElementType EMPTY_LINE = new RnTokenType("EMPTY_LINE");
  IElementType END = new RnTokenType("END");
  IElementType ERROR = new RnTokenType("ERROR");
  IElementType KEY = new RnTokenType("KEY");
  IElementType VALUE = new RnTokenType("VALUE");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == INLINE_PROPERTY) {
        return new RnInlinePropertyImpl(node);
      }
      else if (type == PROPERTY) {
        return new RnPropertyImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
