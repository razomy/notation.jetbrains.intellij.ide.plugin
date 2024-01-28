// This is a generated file. Not intended for manual editing.
package com.razomy.notation_idea_plugin;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.razomy.notation_idea_plugin.impl.*;

public interface RnTypes {

  IElementType PROPERTY = new RnElementType("PROPERTY");

  IElementType COMMENT = new RnTokenType("COMMENT");
  IElementType CRLF = new RnTokenType("CRLF");
  IElementType KEY = new RnTokenType("KEY");
  IElementType SEPARATOR = new RnTokenType("SEPARATOR");
  IElementType VALUE = new RnTokenType("VALUE");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == PROPERTY) {
        return new RnPropertyImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
