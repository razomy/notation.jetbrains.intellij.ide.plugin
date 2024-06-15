 package com.razomy.notation.jetbrains.intellij.ide.plugin

import com.google.common.collect.Lists
import java.util.*;
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil


 import com.intellij.codeInsight.lookup.LookupElement
 import com.intellij.codeInsight.lookup.LookupElementBuilder
 import com.intellij.psi.*

 import com.intellij.openapi.project.Project;
 import com.intellij.openapi.util.TextRange;

 import java.util.ArrayList;


object RnUtil {
    /**
     * Searches the entire project for Rn language files with instances of the Rn property with the given key.
     *
     * @param project current project
     * @param key     to check
     * @return matching properties
     */
    fun findProperties(project: Project, key: String): List<RnProperty> {
        val result: MutableList<RnProperty> = ArrayList<RnProperty>()
        val virtualFiles =
                FileTypeIndex.getFiles(RnFileType, GlobalSearchScope.allScope(project))
        for (virtualFile in virtualFiles) {
            val RnFile: RnFile? = PsiManager.getInstance(project).findFile(virtualFile!!) as RnFile?
            if (RnFile != null) {
                val properties: Array<out RnProperty>? = PsiTreeUtil.getChildrenOfType(RnFile, RnProperty::class.java)
                if (properties != null) {
                    for (property in properties) {
                        if (key == property.text) {
                            result.add(property)
                        }
                    }
                }
            }
        }
        return result
    }

    fun findProperties(project: Project): List<RnProperty> {
        val result: List<RnProperty> = ArrayList<RnProperty>()
        val virtualFiles =
                FileTypeIndex.getFiles(RnFileType, GlobalSearchScope.allScope(project))
        for (virtualFile in virtualFiles) {
            val RnFile: RnFile? = PsiManager.getInstance(project).findFile(virtualFile!!) as RnFile?
            if (RnFile != null) {
                val properties: Array<out RnProperty>? = PsiTreeUtil.getChildrenOfType(RnFile, RnProperty::class.java)
                if (properties != null) {
                    Collections.addAll(result.toMutableList(), properties)
                }
            }
        }
        return result
    }

    /**
     * Attempts to collect any comment elements above the Rn key/value pair.
     */
    fun findDocumentationComment(property: RnProperty): String {
        val result: MutableList<String> = LinkedList()
        var element: PsiElement = property.prevSibling
        while (element is PsiComment || element is PsiWhiteSpace) {
            if (element is PsiComment) {
                val commentText = element.getText().replaceFirst("[!# ]+".toRegex(), "")
                result.add(commentText)
            }
            element = element.prevSibling
        }
        return StringUtil.join(Lists.reverse(result), "\n ")
    }
}