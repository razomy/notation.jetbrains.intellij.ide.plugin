// This is a generated file. Not intended for manual editing.
package org.razomy.language.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;

public class RazomyVisitor extends PsiElementVisitor {

  public void visitAngleGroup(@NotNull RazomyAngleGroup o) {
    visitPsiElement(o);
  }

  public void visitBraceGroup(@NotNull RazomyBraceGroup o) {
    visitPsiElement(o);
  }

  public void visitBracketGroup(@NotNull RazomyBracketGroup o) {
    visitPsiElement(o);
  }

  public void visitGroup(@NotNull RazomyGroup o) {
    visitPsiElement(o);
  }

  public void visitParenGroup(@NotNull RazomyParenGroup o) {
    visitPsiElement(o);
  }

  public void visitPath(@NotNull RazomyPath o) {
    visitPsiNameIdentifierOwner(o);
  }

  public void visitStatement(@NotNull RazomyStatement o) {
    visitPsiElement(o);
  }

  public void visitPsiNameIdentifierOwner(@NotNull PsiNameIdentifierOwner o) {
    visitElement(o);
  }

  public void visitPsiElement(@NotNull PsiElement o) {
    visitElement(o);
  }

}
