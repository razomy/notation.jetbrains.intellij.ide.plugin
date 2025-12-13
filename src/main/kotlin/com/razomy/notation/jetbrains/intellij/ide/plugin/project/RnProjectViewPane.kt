package com.razomy.notation.jetbrains.intellij.ide.plugin.project

import com.intellij.ide.SelectInTarget
import com.intellij.ide.projectView.impl.ProjectAbstractTreeStructureBase
import com.intellij.ide.projectView.impl.ProjectViewPane
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.project.Project
import java.util.Comparator

class RnProjectViewPane(project: Project) : ProjectViewPane(project) {
    var ID = "RnProject"
    override fun getTitle(): String {
        return ID;
    }

    override fun getWeight(): Int {
        return 8;
    }

    override fun getId(): String {
        return ID;
    }

    override fun createComparator(): Comparator<NodeDescriptor<*>> {
        return RnOrderConfigComparator(myProject, ID)
    }

    override fun createSelectInTarget(): SelectInTarget {
        return RnProjectViewSelectInTarget(myProject, ID)
    }

    override fun createStructure(): ProjectAbstractTreeStructureBase {
        return ConfigOrderProjectViewStructure(this.myProject, ID)
    }
}