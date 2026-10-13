// This is a generated file. Not intended for manual editing.
package org.razomy.language.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface RazomyPipelineExpr extends RazomyExpr {

  @NotNull
  List<RazomyExpr> getExprList();

  @NotNull
  List<RazomyPipelineOp> getPipelineOpList();

}
