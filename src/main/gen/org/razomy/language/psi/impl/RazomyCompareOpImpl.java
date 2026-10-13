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

public class RazomyCompareOpImpl extends ASTWrapperPsiElement implements RazomyCompareOp {

  public RazomyCompareOpImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull RazomyVisitor visitor) {
    visitor.visitCompareOp(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RazomyVisitor) accept((RazomyVisitor)visitor);
    else super.accept(visitor);
  }

}
