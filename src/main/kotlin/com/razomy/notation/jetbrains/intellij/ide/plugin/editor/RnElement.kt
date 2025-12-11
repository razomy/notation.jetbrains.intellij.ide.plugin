package com.razomy.notation.jetbrains.intellij.ide.plugin.editor

import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiNameIdentifierOwner


interface RnElement : PsiNameIdentifierOwner, PsiLanguageInjectionHost {
    fun getKey(): String

    fun getValue(): String?
}
