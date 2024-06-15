package com.razomy.notation.jetbrains.intellij.ide.plugin;

import com.intellij.openapi.fileTypes.LanguageFileType

object RnFileType : LanguageFileType(RnLanguage) {
    override fun getName() = "RN"
    override fun getDescription() = "Razomy notation file"
    override fun getDefaultExtension() = "rn"
    override fun getIcon() = RnIcons.FILE
}
