package com.razomy.notation.idea.plugin

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory

object RnElementFactory {
    fun createProperty(project: Project?, name: String): RnProperty {
        val file: RnFile = createFile(project, name)
        return file.firstChild as RnProperty
    }

    fun createFile(project: Project?, text: CharSequence): RnFile {
        val name = "dummy.rn"
        return PsiFileFactory.getInstance(project).createFileFromText(name, RnFileType, text) as RnFile
    }

    fun createProperty(project: Project?, name: String, value: String): RnProperty {
        val file: RnFile = createFile(project, "$name = $value")
        return file.firstChild as RnProperty
    }

    fun createCRLF(project: Project?): PsiElement {
        val file: RnFile = createFile(project, "\n")
        return file.firstChild
    }
}