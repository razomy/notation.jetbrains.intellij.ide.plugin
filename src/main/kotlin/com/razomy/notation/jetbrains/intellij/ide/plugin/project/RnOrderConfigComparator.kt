package com.razomy.notation.jetbrains.intellij.ide.plugin.project

import com.intellij.ide.impl.ProjectUtil
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile


fun loadFileLines(directoryVf: VirtualFile?, filename: String): List<String> {
    val scopeFile = directoryVf?.findChild(filename)
    if (scopeFile != null && scopeFile.exists() && !scopeFile.isDirectory) {
        var strings = String(scopeFile.contentsToByteArray(), scopeFile.charset)
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { !it.startsWith("#") }
        var fromIndex = strings.indexOfFirst { it == "order" }
        return strings.subList(fromIndex, strings.size)
    } else {
        return emptyList()
    }
}

class RnOrderConfigComparator(private val project: Project?, private val paneId: String?) :
    Comparator<NodeDescriptor<*>> {
    private fun rootConfigOrder(): List<String> {
        val baseDir: VirtualFile? = project?.guessProjectDir()
        return loadFileLines(baseDir, "config.rn")
    }

    private fun getSortIndex(fileName: String, directoryVf: VirtualFile?): Int {
        val configOrder = loadFileLines(directoryVf, "config.rn")
        var order = configOrder + rootConfigOrder()
        return order.indexOfFirst({
            it.replace("^\\.+".toRegex(), "") == fileName
                .replace(".rn$".toRegex(), "")
                .replace(".rl$".toRegex(), "")
        }).takeIf { it >= 0 } ?: Int.MAX_VALUE
    }

    override fun compare(descriptor1: NodeDescriptor<*>?, descriptor2: NodeDescriptor<*>?): Int {
        descriptor1 ?: return -1
        descriptor2 ?: return 1

        val node1 = descriptor1 as? ProjectViewNode<*>
        val node2 = descriptor2 as? ProjectViewNode<*>
        val name1 = node1?.name ?: return -1
        val name2 = node2?.name ?: return 1
        val directory1 = node1.virtualFile?.parent
        val directory2 = node2.virtualFile?.parent

        val index1 = getSortIndex(name1, directory1)
        val index2 = getSortIndex(name2, directory2)

        return index1.compareTo(index2).takeIf { it != 0 } ?: StringUtil.naturalCompare(name1, name2)
    }
}