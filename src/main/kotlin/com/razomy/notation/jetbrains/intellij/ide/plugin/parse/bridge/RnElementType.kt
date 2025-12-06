package com.razomy.notation.jetbrains.intellij.ide.plugin.parse.bridge

import com.intellij.psi.tree.IElementType
import com.razomy.notation.jetbrains.intellij.ide.plugin.RnLanguage
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull

class RnElementType(@NotNull @NonNls debugName: String) : IElementType(debugName, RnLanguage)