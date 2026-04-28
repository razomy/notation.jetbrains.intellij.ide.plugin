package com.razomy.notation.jetbrains.intellij.ide.plugin.ai

import com.intellij.openapi.actionSystem.ActionPromoter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataContext

class InlineAiActionPromoter : ActionPromoter {

    // Этот метод вызывается IDE при конфликте шорткатов
    override fun promote(actions: List<AnAction>, context: DataContext): List<AnAction> {
        // Ищем наш плагин среди всех действий, привязанных к Option+Space
        val ourAction = actions.find { it is InlineAiAction } ?: return actions.toList()

        // Если наш плагин найден, перестраиваем список:
        val result = actions.toMutableList()
        result.remove(ourAction)
        result.add(0, ourAction) // Ставим НАШ плагин на 1-е место (наивысший приоритет)

        return result
    }
}