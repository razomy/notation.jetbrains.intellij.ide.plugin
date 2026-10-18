package org.razomy.notation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.razomy.notation.parser.RnTypes

class RnAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element.elementType == RnTypes.KEY) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .textAttributes(RnSyntaxHighlighter.RN_KEY)
                .create()
        } else if (element.elementType == RnTypes.PROPERTY && element.text.startsWith("#")) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .textAttributes(RnSyntaxHighlighter.RN_COMMENT)
                .create()
        } else if (element.elementType == RnTypes.STRING && element.text.startsWith("#")) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .textAttributes(RnSyntaxHighlighter.RN_COMMENT)
                .create()
        }
    }
}