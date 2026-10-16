// This is a generated file. Not intended for manual editing.
package org.razomy.language.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;

public class RazomyVisitor extends PsiElementVisitor {

  public void visitAddExpr(@NotNull RazomyAddExpr o) {
    visitExpr(o);
  }

  public void visitAddOp(@NotNull RazomyAddOp o) {
    visitPsiElement(o);
  }

  public void visitCompareExpr(@NotNull RazomyCompareExpr o) {
    visitExpr(o);
  }

  public void visitCompareOp(@NotNull RazomyCompareOp o) {
    visitPsiElement(o);
  }

  public void visitDeclaration(@NotNull RazomyDeclaration o) {
    visitPsiElement(o);
  }

  public void visitExpr(@NotNull RazomyExpr o) {
    visitPsiElement(o);
  }

  public void visitFlowBlock(@NotNull RazomyFlowBlock o) {
    visitExpr(o);
  }

  public void visitFlowItem(@NotNull RazomyFlowItem o) {
    visitPsiElement(o);
  }

  public void visitFormBlock(@NotNull RazomyFormBlock o) {
    visitExpr(o);
  }

  public void visitFormItem(@NotNull RazomyFormItem o) {
    visitPsiElement(o);
  }

  public void visitLiteral(@NotNull RazomyLiteral o) {
    visitExpr(o);
  }

  public void visitLogicExpr(@NotNull RazomyLogicExpr o) {
    visitExpr(o);
  }

  public void visitLogicOp(@NotNull RazomyLogicOp o) {
    visitPsiElement(o);
  }

  public void visitMergeExpr(@NotNull RazomyMergeExpr o) {
    visitExpr(o);
  }

  public void visitMulExpr(@NotNull RazomyMulExpr o) {
    visitExpr(o);
  }

  public void visitMulOp(@NotNull RazomyMulOp o) {
    visitPsiElement(o);
  }

  public void visitPath(@NotNull RazomyPath o) {
    visitExpr(o);
    // visitPsiNameIdentifierOwner(o);
  }

  public void visitPipelineExpr(@NotNull RazomyPipelineExpr o) {
    visitExpr(o);
  }

  public void visitPipelineOp(@NotNull RazomyPipelineOp o) {
    visitPsiElement(o);
  }

  public void visitPostfixExpr(@NotNull RazomyPostfixExpr o) {
    visitExpr(o);
  }

  public void visitPowExpr(@NotNull RazomyPowExpr o) {
    visitExpr(o);
  }

  public void visitShiftExpr(@NotNull RazomyShiftExpr o) {
    visitExpr(o);
  }

  public void visitShiftOp(@NotNull RazomyShiftOp o) {
    visitPsiElement(o);
  }

  public void visitSlot(@NotNull RazomySlot o) {
    visitExpr(o);
  }

  public void visitSlotItem(@NotNull RazomySlotItem o) {
    visitPsiElement(o);
  }

  public void visitSubtractExpr(@NotNull RazomySubtractExpr o) {
    visitExpr(o);
  }

  public void visitUnaryExpr(@NotNull RazomyUnaryExpr o) {
    visitExpr(o);
  }

  public void visitUnaryOp(@NotNull RazomyUnaryOp o) {
    visitPsiElement(o);
  }

  public void visitUnionExpr(@NotNull RazomyUnionExpr o) {
    visitExpr(o);
  }

  public void visitPsiElement(@NotNull PsiElement o) {
    visitElement(o);
  }

}
