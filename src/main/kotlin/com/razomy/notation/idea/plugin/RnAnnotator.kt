package com.razomy.notation.idea.plugin

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.*

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.containers.toArray

import java.util.ArrayList;

internal class RnAnnotator : Annotator {
    override fun annotate( element: PsiElement, holder: AnnotationHolder) {
        // Ensure the PSI Element is an expression
        if (element !is PsiLiteralExpression) {
            return
        }

        // Ensure the PSI element contains a string that starts with the prefix and separator
        val value = if (element.value is String) element.value as String? else null
        if (value == null || !value.startsWith(  SIMPLE_SEPARATOR_STR)) {
            return
        }

        // Define the text ranges (start is inclusive, end is exclusive)
        // "simple:key"
        //  01234567890
        val prefixRange: TextRange = TextRange.from(element.getTextRange().startOffset, 1)
        val separatorRange: TextRange = TextRange.from(prefixRange.getEndOffset(), SIMPLE_SEPARATOR_STR.length)
        val keyRange: TextRange = TextRange(separatorRange.getEndOffset(), element.getTextRange().endOffset - 1)

        // highlight "simple" prefix and ":" separator
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(prefixRange).textAttributes(DefaultLanguageHighlighterColors.KEYWORD).create()
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(separatorRange).textAttributes(RnSyntaxHighlighter.SEPARATOR).create()


        // Get the list of properties for given key
        val key = value.substring(SIMPLE_SEPARATOR_STR.length)
        val properties: List<RnProperty> = RnUtil.findProperties(element.getProject(), key)
        if (properties.isEmpty()) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Unresolved property")
                .range(keyRange)
                .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL) // ** Tutorial step 19. - Add a quick fix for the string containing possible properties
                .withFix(RnCreatePropertyQuickFix(key))
                .create()
        } else {
            // Found at least one property, force the text attributes to Simple syntax value character
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(keyRange).textAttributes(RnSyntaxHighlighter.VALUE).create()
        }
    }

    companion object {
        // Define strings for the Simple language prefix - used for annotations, line markers, etc.
        const val SIMPLE_SEPARATOR_STR: String = "."
    }
}