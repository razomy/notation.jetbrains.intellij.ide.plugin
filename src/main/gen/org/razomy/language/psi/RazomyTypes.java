// This is a generated file. Not intended for manual editing.
package org.razomy.language.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import org.razomy.language.psi.impl.*;

public interface RazomyTypes {

  IElementType ADD_EXPR = new RazomyElementType("ADD_EXPR");
  IElementType ADD_OP = new RazomyElementType("ADD_OP");
  IElementType COMPARE_EXPR = new RazomyElementType("COMPARE_EXPR");
  IElementType COMPARE_OP = new RazomyElementType("COMPARE_OP");
  IElementType DECLARATION = new RazomyElementType("DECLARATION");
  IElementType EXPR = new RazomyElementType("EXPR");
  IElementType FLOW_BLOCK = new RazomyElementType("FLOW_BLOCK");
  IElementType FLOW_ITEM = new RazomyElementType("FLOW_ITEM");
  IElementType FORM_BLOCK = new RazomyElementType("FORM_BLOCK");
  IElementType FORM_ITEM = new RazomyElementType("FORM_ITEM");
  IElementType LITERAL = new RazomyElementType("LITERAL");
  IElementType LOGIC_EXPR = new RazomyElementType("LOGIC_EXPR");
  IElementType LOGIC_OP = new RazomyElementType("LOGIC_OP");
  IElementType MERGE_EXPR = new RazomyElementType("MERGE_EXPR");
  IElementType MUL_EXPR = new RazomyElementType("MUL_EXPR");
  IElementType MUL_OP = new RazomyElementType("MUL_OP");
  IElementType PATH = new RazomyElementType("PATH");
  IElementType PIPELINE_EXPR = new RazomyElementType("PIPELINE_EXPR");
  IElementType PIPELINE_OP = new RazomyElementType("PIPELINE_OP");
  IElementType POSTFIX_EXPR = new RazomyElementType("POSTFIX_EXPR");
  IElementType POW_EXPR = new RazomyElementType("POW_EXPR");
  IElementType SHIFT_EXPR = new RazomyElementType("SHIFT_EXPR");
  IElementType SHIFT_OP = new RazomyElementType("SHIFT_OP");
  IElementType SLOT = new RazomyElementType("SLOT");
  IElementType SLOT_ITEM = new RazomyElementType("SLOT_ITEM");
  IElementType SUBTRACT_EXPR = new RazomyElementType("SUBTRACT_EXPR");
  IElementType UNARY_EXPR = new RazomyElementType("UNARY_EXPR");
  IElementType UNARY_OP = new RazomyElementType("UNARY_OP");
  IElementType UNION_EXPR = new RazomyElementType("UNION_EXPR");

  IElementType AMP = new RazomyTokenType("&");
  IElementType AND_AND = new RazomyTokenType("&&");
  IElementType BANG = new RazomyTokenType("!");
  IElementType COLON = new RazomyTokenType(":");
  IElementType COMMA = new RazomyTokenType(",");
  IElementType CRLF2 = new RazomyTokenType("CRLF2");
  IElementType DOC_COMMENT = new RazomyTokenType("DOC_COMMENT");
  IElementType DOT = new RazomyTokenType(".");
  IElementType DOT_AMP = new RazomyTokenType(".&");
  IElementType DOT_GT = new RazomyTokenType(".>");
  IElementType DOT_PIPE = new RazomyTokenType(".|");
  IElementType DOT_QM = new RazomyTokenType(".?");
  IElementType DOT_RANGE = new RazomyTokenType("..");
  IElementType EQ = new RazomyTokenType("=");
  IElementType EQ_EQ = new RazomyTokenType("==");
  IElementType FALSE = new RazomyTokenType("false");
  IElementType GTE = new RazomyTokenType(">=");
  IElementType IDENTIFIER = new RazomyTokenType("IDENTIFIER");
  IElementType LANGLE = new RazomyTokenType("<");
  IElementType LBRACE = new RazomyTokenType("{");
  IElementType LBRACKET = new RazomyTokenType("[");
  IElementType LINE_COMMENT = new RazomyTokenType("LINE_COMMENT");
  IElementType LPAREN = new RazomyTokenType("(");
  IElementType LTE = new RazomyTokenType("<=");
  IElementType MINUS = new RazomyTokenType("-");
  IElementType MOD = new RazomyTokenType("%");
  IElementType MUL = new RazomyTokenType("*");
  IElementType NOT_EQ = new RazomyTokenType("!=");
  IElementType NUMBER = new RazomyTokenType("NUMBER");
  IElementType OR_OR = new RazomyTokenType("||");
  IElementType PIPE = new RazomyTokenType("|");
  IElementType PIPE_DOT = new RazomyTokenType("PIPE_DOT");
  IElementType PLUS = new RazomyTokenType("+");
  IElementType POW = new RazomyTokenType("**");
  IElementType QM = new RazomyTokenType("?");
  IElementType RANGLE = new RazomyTokenType(">");
  IElementType RBRACE = new RazomyTokenType("}");
  IElementType RBRACKET = new RazomyTokenType("]");
  IElementType RPAREN = new RazomyTokenType(")");
  IElementType SEMICOLON = new RazomyTokenType(";");
  IElementType SHIFT_L = new RazomyTokenType("<<");
  IElementType SHIFT_R = new RazomyTokenType(">>");
  IElementType SLASH = new RazomyTokenType("/");
  IElementType STRING = new RazomyTokenType("STRING");
  IElementType TRUE = new RazomyTokenType("true");
  IElementType UNDERSCORE = new RazomyTokenType("_");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == ADD_EXPR) {
        return new RazomyAddExprImpl(node);
      }
      else if (type == ADD_OP) {
        return new RazomyAddOpImpl(node);
      }
      else if (type == COMPARE_EXPR) {
        return new RazomyCompareExprImpl(node);
      }
      else if (type == COMPARE_OP) {
        return new RazomyCompareOpImpl(node);
      }
      else if (type == DECLARATION) {
        return new RazomyDeclarationImpl(node);
      }
      else if (type == FLOW_BLOCK) {
        return new RazomyFlowBlockImpl(node);
      }
      else if (type == FLOW_ITEM) {
        return new RazomyFlowItemImpl(node);
      }
      else if (type == FORM_BLOCK) {
        return new RazomyFormBlockImpl(node);
      }
      else if (type == FORM_ITEM) {
        return new RazomyFormItemImpl(node);
      }
      else if (type == LITERAL) {
        return new RazomyLiteralImpl(node);
      }
      else if (type == LOGIC_EXPR) {
        return new RazomyLogicExprImpl(node);
      }
      else if (type == LOGIC_OP) {
        return new RazomyLogicOpImpl(node);
      }
      else if (type == MERGE_EXPR) {
        return new RazomyMergeExprImpl(node);
      }
      else if (type == MUL_EXPR) {
        return new RazomyMulExprImpl(node);
      }
      else if (type == MUL_OP) {
        return new RazomyMulOpImpl(node);
      }
      else if (type == PATH) {
        return new RazomyPathImpl(node);
      }
      else if (type == PIPELINE_EXPR) {
        return new RazomyPipelineExprImpl(node);
      }
      else if (type == PIPELINE_OP) {
        return new RazomyPipelineOpImpl(node);
      }
      else if (type == POSTFIX_EXPR) {
        return new RazomyPostfixExprImpl(node);
      }
      else if (type == POW_EXPR) {
        return new RazomyPowExprImpl(node);
      }
      else if (type == SHIFT_EXPR) {
        return new RazomyShiftExprImpl(node);
      }
      else if (type == SHIFT_OP) {
        return new RazomyShiftOpImpl(node);
      }
      else if (type == SLOT) {
        return new RazomySlotImpl(node);
      }
      else if (type == SLOT_ITEM) {
        return new RazomySlotItemImpl(node);
      }
      else if (type == SUBTRACT_EXPR) {
        return new RazomySubtractExprImpl(node);
      }
      else if (type == UNARY_EXPR) {
        return new RazomyUnaryExprImpl(node);
      }
      else if (type == UNARY_OP) {
        return new RazomyUnaryOpImpl(node);
      }
      else if (type == UNION_EXPR) {
        return new RazomyUnionExprImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
