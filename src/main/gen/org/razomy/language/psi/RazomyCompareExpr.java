// This is a generated file. Not intended for manual editing.
package org.razomy.language.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface RazomyCompareExpr extends RazomyExpr {

  @NotNull
  List<RazomyCompareOp> getCompareOpList();

  @NotNull
  List<RazomyExpr> getExprList();

}
