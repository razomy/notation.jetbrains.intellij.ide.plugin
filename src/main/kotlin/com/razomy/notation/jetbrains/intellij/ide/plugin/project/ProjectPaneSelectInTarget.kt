package com.razomy.notation.jetbrains.intellij.ide.plugin.project

import com.intellij.ide.SelectInContext
import com.intellij.ide.SelectInManager
import com.intellij.ide.StandardTargetWeights
import com.intellij.ide.impl.ProjectViewSelectInTarget
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project

class ProjectPaneSelectInTarget(project: Project?, protected val id: String) : ProjectViewSelectInTarget(project),
    DumbAware {
    override fun toString(): String {
        return SelectInManager.getProject()
    }

    override fun isSubIdSelectable(subId: String, context: SelectInContext): Boolean {
        return canSelect(context)
    }

    override fun getMinorViewId(): String {
        return id;
    }

    override fun getWeight(): Float {
        return StandardTargetWeights.PROJECT_WEIGHT
    }
}