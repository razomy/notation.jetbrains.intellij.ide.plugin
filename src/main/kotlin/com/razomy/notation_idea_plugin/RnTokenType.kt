package com.razomy.notation_idea_plugin;

import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull

class RnTokenType(@NotNull @NonNls debugName: String) : IElementType(debugName, RnLanguage.INSTANCE) {

    override fun toString(): String {
        return "RnTokenType." + super.toString()
    }
}
