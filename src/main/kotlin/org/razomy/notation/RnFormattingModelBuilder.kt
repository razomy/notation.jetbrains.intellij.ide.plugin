package org.razomy.notation

import com.intellij.formatting.*
import com.intellij.psi.codeStyle.CodeStyleSettings
import org.jetbrains.annotations.NotNull

internal class RnFormattingModelBuilder : FormattingModelBuilder {
    @NotNull
    override fun createModel(@NotNull formattingContext: FormattingContext): FormattingModel {
        val codeStyleSettings = formattingContext.codeStyleSettings

        return FormattingModelProvider
            .createFormattingModelForPsiFile(
                formattingContext.containingFile,
                RnBlock(
                    formattingContext.node,
                    Wrap.createWrap(WrapType.NORMAL, false),
                    Alignment.createAlignment(),
                    Indent.getNoneIndent(), // Root has no indent
                    createSpaceBuilder(codeStyleSettings)
                ),
                codeStyleSettings
            )
    }
}

private fun createSpaceBuilder(settings: CodeStyleSettings): SpacingBuilder {
    return SpacingBuilder(settings, RnLanguage)
}