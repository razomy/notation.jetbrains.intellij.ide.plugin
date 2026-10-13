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

public class RazomyPrimaryExprImpl extends RazomyExprImpl implements RazomyPrimaryExpr {

  public RazomyPrimaryExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull RazomyVisitor visitor) {
    visitor.visitPrimaryExpr(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RazomyVisitor) accept((RazomyVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public RazomyFlowBlock getFlowBlock() {
    return findChildByClass(RazomyFlowBlock.class);
  }

  @Override
  @Nullable
  public RazomyFormBlock getFormBlock() {
    return findChildByClass(RazomyFormBlock.class);
  }

  @Override
  @Nullable
  public RazomyLiteral getLiteral() {
    return findChildByClass(RazomyLiteral.class);
  }

  @Override
  @Nullable
  public RazomyPath getPath() {
    return findChildByClass(RazomyPath.class);
  }

  @Override
  @Nullable
  public RazomySlot getSlot() {
    return findChildByClass(RazomySlot.class);
  }

  @Override
  @Nullable
  public RazomyTypedFlowBlock getTypedFlowBlock() {
    return findChildByClass(RazomyTypedFlowBlock.class);
  }

  @Override
  @Nullable
  public RazomyTypedFormBlock getTypedFormBlock() {
    return findChildByClass(RazomyTypedFormBlock.class);
  }

}
