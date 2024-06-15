package com.razomy.notation.jetbrains.intellij.ide.plugin;

import com.intellij.lang.Language

object RnLanguage : Language("RN") {
    private fun readResolve(): Any = RnLanguage
}
