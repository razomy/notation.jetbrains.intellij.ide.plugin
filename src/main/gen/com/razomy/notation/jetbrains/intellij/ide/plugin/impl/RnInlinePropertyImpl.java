// This is a generated file. Not intended for manual editing.
package com.razomy.notation.jetbrains.intellij.ide.plugin.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.razomy.notation.jetbrains.intellij.ide.plugin.RnTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.razomy.notation.jetbrains.intellij.ide.plugin.*;

public class RnInlinePropertyImpl extends ASTWrapperPsiElement implements RnInlineProperty {

  public RnInlinePropertyImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull RnVisitor visitor) {
    visitor.visitInlineProperty(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RnVisitor) accept((RnVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<RnInlineProperty> getInlinePropertyList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, RnInlineProperty.class);
  }

  @Override
  @Nullable
  public RnProperty getProperty() {
    return findChildByClass(RnProperty.class);
  }

}
