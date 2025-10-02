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
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiLanguageInjectionHost;

public class RnValueImpl extends ASTWrapperPsiElement implements RnValue {

  public RnValueImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull RnVisitor visitor) {
    visitor.visitValue(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RnVisitor) accept((RnVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  public @NotNull String getKey() {
    return RnPsiImplUtil.getKey(this);
  }

  @Override
  public @Nullable String getValue() {
    return RnPsiImplUtil.getValue(this);
  }

  @Override
  public @NotNull String getName() {
    return RnPsiImplUtil.getName(this);
  }

  @Override
  public @NotNull PsiElement setName(@NotNull String newName) {
    return RnPsiImplUtil.setName(this, newName);
  }

  @Override
  public @Nullable PsiElement getNameIdentifier() {
    return RnPsiImplUtil.getNameIdentifier(this);
  }

  @Override
  public @NotNull ItemPresentation getPresentation() {
    return RnPsiImplUtil.getPresentation(this);
  }

  @Override
  public boolean isValidHost() {
    return RnPsiImplUtil.isValidHost(this);
  }

  @Override
  public @NotNull PsiLanguageInjectionHost updateText(@NotNull String text) {
    return RnPsiImplUtil.updateText(this, text);
  }

  @Override
  public @NotNull LiteralTextEscaper<? extends @Nullable PsiLanguageInjectionHost> createLiteralTextEscaper() {
    return RnPsiImplUtil.createLiteralTextEscaper(this);
  }

}
