package org.razomy.language.psi

import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull
import org.razomy.language.RazomyLanguage

class RazomyTokenType(@NotNull @NonNls debugName: String) : IElementType(debugName, RazomyLanguage.INSTANCE)
class RazomyElementType(@NotNull @NonNls debugName: String) : IElementType(debugName, RazomyLanguage.INSTANCE)
