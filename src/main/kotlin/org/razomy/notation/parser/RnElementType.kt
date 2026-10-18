package org.razomy.notation.parser

import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull
import org.razomy.notation.RnLanguage

class RnElementType(@NotNull @NonNls debugName: String) : com.intellij.psi.tree.IElementType(debugName, RnLanguage)