package org.razomy.notation

import com.intellij.lang.Language

object RnLanguage : Language("RN") {
    private fun readResolve(): Any = RnLanguage
}
