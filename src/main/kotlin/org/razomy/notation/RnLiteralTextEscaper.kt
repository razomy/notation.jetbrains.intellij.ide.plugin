package org.razomy.notation

import com.intellij.openapi.util.TextRange
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiLanguageInjectionHost

class RnLiteralTextEscaper(host: PsiLanguageInjectionHost) :
    LiteralTextEscaper<PsiLanguageInjectionHost?>(host) {
    override fun decode(rangeInsideHost: TextRange, outChars: StringBuilder): Boolean {
        outChars.append(myHost!!.getText(), rangeInsideHost.getStartOffset(), rangeInsideHost.getEndOffset())
        return true
    }

    override fun getOffsetInHost(offsetInDecoded: Int, rangeInsideHost: TextRange): Int {
        var offset = offsetInDecoded + rangeInsideHost.startOffset
        if (offset > rangeInsideHost.endOffset) {
            offset = rangeInsideHost.endOffset
        }
        return offset
    }

    override fun isOneLine(): Boolean {
        val value = myHost!!.getText()
        return value != null && value.indexOf('\n') < 0
    }
}
