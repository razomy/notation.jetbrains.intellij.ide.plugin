// This is a generated file. Not intended for manual editing.
package org.razomy.language.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import org.razomy.language.RazomyElementType;
import org.razomy.language.RazomyTokenType;
import org.razomy.language.psi.impl.*;

public interface RazomyTypes {

  IElementType ANGLE_GROUP = new RazomyElementType("ANGLE_GROUP");
  IElementType BRACE_GROUP = new RazomyElementType("BRACE_GROUP");
  IElementType BRACKET_GROUP = new RazomyElementType("BRACKET_GROUP");
  IElementType GROUP = new RazomyElementType("GROUP");
  IElementType PAREN_GROUP = new RazomyElementType("PAREN_GROUP");
  IElementType PATH = new RazomyElementType("PATH");
  IElementType STATEMENT = new RazomyElementType("STATEMENT");

  IElementType CONST_KW = new RazomyTokenType("const");
  IElementType CRLF2 = new RazomyTokenType("CRLF2");
  IElementType DOT = new RazomyTokenType(".");
  IElementType IDENTIFIER = new RazomyTokenType("IDENTIFIER");
  IElementType INSTANCEOF_KW = new RazomyTokenType("instanceof");
  IElementType LANGLE = new RazomyTokenType("<");
  IElementType LBRACE = new RazomyTokenType("{");
  IElementType LBRACKET = new RazomyTokenType("[");
  IElementType LINE_COMMENT = new RazomyTokenType("LINE_COMMENT");
  IElementType LPAREN = new RazomyTokenType("(");
  IElementType MATCH_KW = new RazomyTokenType("match");
  IElementType NUMBER = new RazomyTokenType("NUMBER");
  IElementType OP = new RazomyTokenType("OP");
  IElementType RANGLE = new RazomyTokenType(">");
  IElementType RBRACE = new RazomyTokenType("}");
  IElementType RBRACKET = new RazomyTokenType("]");
  IElementType RPAREN = new RazomyTokenType(")");
  IElementType SEMICOLON = new RazomyTokenType(";");
  IElementType STRING = new RazomyTokenType("STRING");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == ANGLE_GROUP) {
        return new RazomyAngleGroupImpl(node);
      }
      else if (type == BRACE_GROUP) {
        return new RazomyBraceGroupImpl(node);
      }
      else if (type == BRACKET_GROUP) {
        return new RazomyBracketGroupImpl(node);
      }
      else if (type == GROUP) {
        return new RazomyGroupImpl(node);
      }
      else if (type == PAREN_GROUP) {
        return new RazomyParenGroupImpl(node);
      }
      else if (type == PATH) {
        return new RazomyPathImpl(node);
      }
      else if (type == STATEMENT) {
        return new RazomyStatementImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
