// This is a generated file. Not intended for manual editing.
package com.razomy.notation.jetbrains.intellij.ide.plugin;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.navigation.ItemPresentation;

public interface RnProperty extends RnElement {

  @NotNull
  List<RnValue> getValueList();

  @NotNull String getKey();

  @Nullable String getValue();

  @NotNull String getName();

  @NotNull PsiElement setName(@NotNull String newName);

  @Nullable PsiElement getNameIdentifier();

  @NotNull ItemPresentation getPresentation();

  PsiElement getNameIdentifier();

  ItemPresentation getPresentation();

}
