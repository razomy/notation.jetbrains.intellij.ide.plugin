// This is a generated file. Not intended for manual editing.
package com.razomy.notation.jetbrains.intellij.ide.plugin;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiElement;

public class RnVisitor extends PsiElementVisitor {

  public void visitInlineProperty(@NotNull RnInlineProperty o) {
    visitPsiElement(o);
  }

  public void visitProperty(@NotNull RnProperty o) {
    visitElement(o);
  }

  public void visitElement(@NotNull RnElement o) {
    visitPsiElement(o);
  }

  public void visitPsiElement(@NotNull PsiElement o) {
    visitElement(o);
  }

}
