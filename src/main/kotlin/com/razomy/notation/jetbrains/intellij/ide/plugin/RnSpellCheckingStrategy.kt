package com.razomy.notation.jetbrains.intellij.ide.plugin;

import com.intellij.psi.PsiElement
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy
import com.intellij.spellchecker.tokenizer.Tokenizer
import org.jetbrains.annotations.NotNull


class RnSpellCheckingStrategy : SpellcheckingStrategy() {
    @NotNull
    override fun getTokenizer(element: PsiElement): Tokenizer<PsiElement> {
        return RnCustomTokenizer()
    }
}