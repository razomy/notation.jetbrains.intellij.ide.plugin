// This is a generated file. Not intended for manual editing.
package com.razomy.notation.idea.plugin;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface RnProperty extends RnElement {

  @NotNull
  List<RnProperty> getPropertyList();

  @Nullable
  RnValue getValue();

  String getKey();

  String getName();

  PsiElement setName(String newName);

  PsiElement getNameIdentifier();

}
