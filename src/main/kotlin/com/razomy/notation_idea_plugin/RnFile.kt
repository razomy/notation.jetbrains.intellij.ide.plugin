package com.razomy.notation_idea_plugin

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

class RnFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, RnLanguage.INSTANCE) {
    override fun getFileType(): FileType {
        return RnFileType
    }

    override fun toString(): String {
        return "Rn File"
    }
}