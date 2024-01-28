package com.razomy.notation_idea_plugin;

import com.intellij.openapi.fileTypes.LanguageFileType

object RnFileType : LanguageFileType(RnLanguage.INSTANCE) {
    override fun getName() = "Rn"
    override fun getDescription() = "Razomy notation file"
    override fun getDefaultExtension() = "rn"
    override fun getIcon() = RnIcons.FILE
}
