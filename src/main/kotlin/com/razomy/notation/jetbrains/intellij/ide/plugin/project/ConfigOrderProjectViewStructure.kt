package com.razomy.notation.jetbrains.intellij.ide.plugin.project

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiNamedElement
import com.intellij.ide.util.treeView.AbstractTreeNode

fun getRepresentableNameForUserObject(userObject: Any): String? {
    return when (userObject) {
        is PsiNamedElement -> userObject.name
        is AbstractTreeNode<*> -> {
            val value = userObject.value
            if (value is PsiNamedElement) value.name else userObject.name
        }

        else -> null
    }
}

class ConfigOrderProjectViewStructure(project: Project, id: String) :
    ProjectViewPaneTreeStructure(project, id) {
    override fun getChildElements(parentElementUserObject: Any): Array<Any> {
        val childrenUserObjects = super.getChildElements(parentElementUserObject)

        val parentVirtualFile = when (parentElementUserObject) {
            is PsiDirectory -> parentElementUserObject.virtualFile
            is AbstractTreeNode<*> -> {
                val value = parentElementUserObject.value
                if (value is PsiDirectory) value.virtualFile else null
            }
            else -> null
        }

        if (parentVirtualFile != null && parentVirtualFile.isDirectory) {
            val configScope = loadFileLines(parentVirtualFile, "config.scope.rn")
            if (!configScope.isEmpty()) {
                val filteredChildrenUserObjects = childrenUserObjects.filter { childUserObject ->
                    val childName = getRepresentableNameForUserObject(childUserObject)
                    childName != null && configScope.contains(childName)
                }
                return filteredChildrenUserObjects.toTypedArray()
            }
        }
        return childrenUserObjects
    }
}