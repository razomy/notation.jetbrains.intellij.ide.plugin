// This is a generated file. Not intended for manual editing.
package org.razomy.language.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface RazomyCompareExpr extends PsiElement {

  @NotNull
  List<RazomyAddExpr> getAddExprList();

  @NotNull
  List<RazomyCompareOp> getCompareOpList();

}
