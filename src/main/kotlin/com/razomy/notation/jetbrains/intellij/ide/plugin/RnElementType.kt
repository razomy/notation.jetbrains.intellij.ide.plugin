package com.razomy.notation.jetbrains.intellij.ide.plugin;

import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull

class RnElementType(@NotNull @NonNls debugName: String) : IElementType(debugName, RnLanguage)
