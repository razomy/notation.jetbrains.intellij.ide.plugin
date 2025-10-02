// This is a generated file. Not intended for manual editing.
package com.razomy.notation.jetbrains.intellij.ide.plugin;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiLanguageInjectionHost;

public interface RnValue extends RnElement {

  @NotNull String getKey();

  @Nullable String getValue();

  @NotNull String getName();

  @NotNull PsiElement setName(@NotNull String newName);

  @Nullable PsiElement getNameIdentifier();

  @NotNull ItemPresentation getPresentation();

  boolean isValidHost();

  @NotNull PsiLanguageInjectionHost updateText(@NotNull String text);

  @NotNull LiteralTextEscaper<? extends @Nullable PsiLanguageInjectionHost> createLiteralTextEscaper();

}
