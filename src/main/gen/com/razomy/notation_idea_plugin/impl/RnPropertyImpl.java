// This is a generated file. Not intended for manual editing.
package com.razomy.notation_idea_plugin.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.razomy.notation_idea_plugin.RnTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.razomy.notation_idea_plugin.*;

public class RnPropertyImpl extends ASTWrapperPsiElement implements RnProperty {

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

}
