// This is a generated file. Not intended for manual editing.
package com.razomy.notation.jetbrains.intellij.ide.plugin;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.navigation.ItemPresentation;

public interface RnProperty extends RnElement {

  @NotNull
  List<RnInlineProperty> getInlinePropertyList();

  @NotNull
  List<RnProperty> getPropertyList();

  String getKey();

  String getValue();

  String getName();

  PsiElement setName(String newName);

  PsiElement getNameIdentifier();

  ItemPresentation getPresentation();

}
