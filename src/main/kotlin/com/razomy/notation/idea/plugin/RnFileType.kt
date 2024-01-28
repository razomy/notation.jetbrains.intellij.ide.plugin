package com.razomy.notation.idea.plugin;

import com.intellij.openapi.fileTypes.LanguageFileType

object RnFileType : LanguageFileType(RnLanguage) {
    override fun getName() = "Rn"
    override fun getDescription() = "Razomy notation file"
    override fun getDefaultExtension() = "rn"
    override fun getIcon() = RnIcons.FILE
}
