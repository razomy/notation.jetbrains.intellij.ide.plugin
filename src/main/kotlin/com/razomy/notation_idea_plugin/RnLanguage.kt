package com.razomy.notation_idea_plugin;

import com.intellij.lang.Language

class RnLanguage private constructor() : Language("Rn") {

    companion object {
        val INSTANCE: RnLanguage = RnLanguage()
    }
}
