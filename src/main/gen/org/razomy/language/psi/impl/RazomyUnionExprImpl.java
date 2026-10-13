// This is a generated file. Not intended for manual editing.
package org.razomy.language.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static org.razomy.language.psi.RazomyTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import org.razomy.language.psi.*;

public class RazomyUnionExprImpl extends ASTWrapperPsiElement implements RazomyUnionExpr {

  public RazomyUnionExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull RazomyVisitor visitor) {
    visitor.visitUnionExpr(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RazomyVisitor) accept((RazomyVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<RazomyMergeExpr> getMergeExprList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, RazomyMergeExpr.class);
  }

}
