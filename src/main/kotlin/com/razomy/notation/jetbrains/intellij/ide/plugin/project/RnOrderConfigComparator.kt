package com.razomy.notation.jetbrains.intellij.ide.plugin.project

import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile

class RnOrderConfigComparator(
    private val project: Project?,
    private val paneId: String?
) : Comparator<NodeDescriptor<*>> {

    private fun rootConfigOrder(): List<String> {
        val baseDir = project?.guessProjectDir()
        return RnConfigCache.getConfig(baseDir).order
    }

    private fun getSortIndex(fileName: String, directoryVf: VirtualFile?): Int {
        val localOrder = RnConfigCache.getConfig(directoryVf).order
        val rootOrder = rootConfigOrder()

        if (localOrder.isEmpty() && rootOrder.isEmpty()) return Int.MAX_VALUE

        // Normalize the node's filename the exact same way as the config
        val cleanFileName = RnConfigCache.normalizeOrderName(fileName)

        // 1. Check local directory's config.rn first
        val localIndex = localOrder.indexOf(cleanFileName)
        if (localIndex >= 0) return localIndex

        // 2. Fallback to root project's config.rn
        val rootIndex = rootOrder.indexOf(cleanFileName)
        if (rootIndex >= 0) return localOrder.size + rootIndex

        return Int.MAX_VALUE
    }

    override fun compare(descriptor1: NodeDescriptor<*>?, descriptor2: NodeDescriptor<*>?): Int {
        if (descriptor1 === descriptor2) return 0
        if (descriptor1 == null) return -1
        if (descriptor2 == null) return 1

        val node1 = descriptor1 as? ProjectViewNode<*>
        val node2 = descriptor2 as? ProjectViewNode<*>

        // Prefer virtualFile.name over node.name to ensure accurate file extension matching
        val name1 = node1?.virtualFile?.name ?: node1?.name ?: return -1
        val name2 = node2?.virtualFile?.name ?: node2?.name ?: return 1

        val directory1 = node1?.virtualFile?.parent
        val directory2 = node2?.virtualFile?.parent

        val index1 = getSortIndex(name1, directory1)
        val index2 = getSortIndex(name2, directory2)

        val indexComparison = index1.compareTo(index2)
        return if (indexComparison != 0) {
            indexComparison
        } else {
            StringUtil.naturalCompare(name1, name2)
        }
    }
}