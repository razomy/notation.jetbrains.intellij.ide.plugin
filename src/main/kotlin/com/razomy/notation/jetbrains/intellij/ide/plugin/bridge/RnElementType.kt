package com.razomy.notation.jetbrains.intellij.ide.plugin.bridge

import com.intellij.psi.tree.IElementType
import com.razomy.notation.jetbrains.intellij.ide.plugin.editor.RnLanguage
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull

class RnElementType(@NotNull @NonNls debugName: String) : IElementType(debugName, RnLanguage)