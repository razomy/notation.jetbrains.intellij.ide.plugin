// This is a generated file. Not intended for manual editing.
package org.razomy.language.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface RazomyPrimaryExpr extends PsiElement {

  @Nullable
  RazomyCallExpr getCallExpr();

  @Nullable
  RazomyFlowBlock getFlowBlock();

  @Nullable
  RazomyFormBlock getFormBlock();

  @Nullable
  RazomyLiteral getLiteral();

  @Nullable
  RazomyPath getPath();

  @Nullable
  RazomySlot getSlot();

  @Nullable
  RazomyTypedFlowBlock getTypedFlowBlock();

  @Nullable
  RazomyTypedFormBlock getTypedFormBlock();

}
