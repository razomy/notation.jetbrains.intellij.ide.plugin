package com.razomy.notation.idea.plugin;

import com.intellij.lang.Language

object RnLanguage : Language("RN") {
    private fun readResolve(): Any = RnLanguage
}
