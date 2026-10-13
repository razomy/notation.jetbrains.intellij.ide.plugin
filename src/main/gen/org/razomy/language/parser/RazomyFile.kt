package org.razomy.language.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import org.razomy.language.RazomyFileType
import org.razomy.language.RazomyLanguage

class RazomyFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, RazomyLanguage.INSTANCE) {
    override fun getFileType(): FileType = RazomyFileType.INSTANCE
    override fun toString(): String = "Razomy File"
}