package com.razomy.notation.jetbrains.intellij.ide.plugin.project

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.ProjectViewSettings
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.ProjectTreeStructure
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry

open class RnProjectTreeStructure(
    private val myProject: Project,
    private val id: String
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
}