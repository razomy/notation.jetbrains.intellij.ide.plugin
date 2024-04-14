// This is a generated file. Not intended for manual editing.
package com.razomy.notation.idea.plugin.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.razomy.notation.idea.plugin.RnTypes.*;
import com.razomy.notation.idea.plugin.*;

public class RnPropertyImpl extends RnElementImpl implements RnProperty {

  public RnPropertyImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull RnVisitor visitor) {
    visitor.visitProperty(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RnVisitor) accept((RnVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<RnProperty> getPropertyList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, RnProperty.class);
  }

  @Override
  @Nullable
  public RnValue getValue() {
    return findChildByClass(RnValue.class);
  }

  @Override
  public String getKey() {
    return RnPsiImplUtil.getKey(this);
  }

  @Override
  public String getName() {
    return RnPsiImplUtil.getName(this);
  }

  @Override
  public PsiElement setName(String newName) {
    return RnPsiImplUtil.setName(this, newName);
  }

  @Override
  public PsiElement getNameIdentifier() {
    return RnPsiImplUtil.getNameIdentifier(this);
  }

}
