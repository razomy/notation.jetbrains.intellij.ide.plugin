// This is a generated file. Not intended for manual editing.
package org.razomy.language.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static org.razomy.language.psi.RazomyTypes.*;
import org.razomy.language.psi.*;

public class RazomyShiftExprImpl extends RazomyExprImpl implements RazomyShiftExpr {

  public RazomyShiftExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull RazomyVisitor visitor) {
    visitor.visitShiftExpr(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RazomyVisitor) accept((RazomyVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<RazomyExpr> getExprList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, RazomyExpr.class);
  }

  @Override
  @NotNull
  public List<RazomyShiftOp> getShiftOpList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, RazomyShiftOp.class);
  }

}
