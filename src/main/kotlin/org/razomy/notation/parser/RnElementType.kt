package org.razomy.notation.parser

import com.intellij.psi.tree.IElementType
import org.razomy.notation.RnLanguage
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull

class RnElementType(@NotNull @NonNls debugName: String) : com.intellij.psi.tree.IElementType(debugName, RnLanguage)