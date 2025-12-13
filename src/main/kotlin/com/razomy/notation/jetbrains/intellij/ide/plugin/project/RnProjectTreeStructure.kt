package com.razomy.notation.jetbrains.intellij.ide.plugin.project

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.ProjectViewSettings
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.ProjectTreeStructure
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement

open class RnProjectTreeStructure(
    private val myProject: Project,
    private val id: String,
    public var currentScope: ScopeDefinition
) : ProjectTreeStructure(myProject, id), ProjectViewSettings {

    override fun createRoot(project: Project, settings: ViewSettings): AbstractTreeNode<*> {
        return ProjectViewProjectNode(project, settings)
    }

    override fun isShowExcludedFiles(): Boolean {
        return ProjectView.getInstance(myProject).isShowExcludedFiles(id)
    }

    override fun isShowLibraryContents(): Boolean {
        return true
    }

    override fun isUseFileNestingRules(): Boolean {
        return ProjectView.getInstance(myProject).isUseFileNestingRules(id)
    }

    override fun isToBuildChildrenInBackground(element: Any): Boolean {
        return Registry.`is`("ide.projectView.ProjectViewPaneTreeStructure.BuildChildrenInBackground")
    }

    override fun getChildElements(parentElementUserObject: Any): Array<out Any?> {
        // Get standard children from the default project structure
        val childrenUserObjects = super.getChildElements(parentElementUserObject)
        val filteredChildren: MutableList<AbstractTreeNode<*>?> = ArrayList<AbstractTreeNode<*>?>()

        for (node in childrenUserObjects) {
            if (shouldShow(node as AbstractTreeNode<*>?)) {
                filteredChildren.add(node)
            }
        }
        return filteredChildren.toTypedArray()
    }

    private fun shouldShow(node: AbstractTreeNode<*>?): Boolean {

        if (node is PsiFileNode) return currentScope.accepts(node.getVirtualFile(), myProject)
        // Always show the generic logic (modules, external libs, etc) or filter them out if you want
        if (node !is PsiDirectoryNode) return true

        val dirNode = node
        val value: PsiElement? = dirNode.getValue()

        if (value != null && value.getContainingFile() != null) {
            return currentScope.accepts(value.getContainingFile().getVirtualFile(), myProject)
        } else if (dirNode.getVirtualFile() != null) {
            return currentScope.accepts(dirNode.getVirtualFile(), myProject)
        }

        return true
    }
}