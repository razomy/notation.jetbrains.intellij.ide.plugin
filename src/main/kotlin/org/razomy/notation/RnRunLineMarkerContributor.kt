package org.razomy.notation

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.razomy.notation.parser.RnTypes

class RnRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element.elementType != RnTypes.VALUE_ && element.elementType != RnTypes.MULTILINE_VALUE) return null

        if (element.text.startsWith("run ") || element.text.startsWith("`run ")) {
            val actions = ExecutorAction.getActions(Int.MAX_VALUE)
            return Info(
                AllIcons.RunConfigurations.TestState.Run,
                actions
            )
        }
        return null
    }
}
