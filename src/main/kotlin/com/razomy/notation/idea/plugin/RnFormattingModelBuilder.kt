package com.razomy.notation.idea.plugin

import com.intellij.formatting.*
import com.intellij.psi.codeStyle.CodeStyleSettings
import org.jetbrains.annotations.NotNull


internal class RnFormattingModelBuilder : FormattingModelBuilder {
    @NotNull
    override fun createModel(@NotNull formattingContext: FormattingContext): FormattingModel {
        val codeStyleSettings = formattingContext.codeStyleSettings
        return FormattingModelProvider
                .createFormattingModelForPsiFile(formattingContext.containingFile,
                        RnBlock(formattingContext.node,
                                Wrap.createWrap(WrapType.NONE, false),
                                Alignment.createAlignment(),
                                createSpaceBuilder(codeStyleSettings)),
                        codeStyleSettings)
    }

}

private fun createSpaceBuilder(settings: CodeStyleSettings): SpacingBuilder {

    return SpacingBuilder(settings, RnLanguage)
            .after(RnTypes.SEPARATOR)
            .none()
}