package com.razomy.notation.idea.plugin;

import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull

class RnElementType(@NotNull @NonNls debugName: String) : IElementType(debugName, RnLanguage)
