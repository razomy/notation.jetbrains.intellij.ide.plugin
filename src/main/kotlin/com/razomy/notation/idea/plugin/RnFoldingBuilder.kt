package com.razomy.notation.idea.plugin

import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import kotlin.math.min


class RnFoldingBuilder : FoldingBuilder {
    override fun buildFoldRegions(root: ASTNode, document: Document): Array<FoldingDescriptor> {
        val descriptors: MutableList<FoldingDescriptor> = ArrayList()

        fun add(nodeFrom: ASTNode, nodeTo: ASTNode) {
            val text = nodeFrom.text.substring(0, min(120, nodeFrom.text.length))
            val start = nodeFrom.startOffset
            val end = nodeTo.textRange.endOffset
            if (end <= start) {
                return
            }
            val range = TextRange(start, end)

            val descriptor = FoldingDescriptor(
                    nodeFrom,
                    range,
                    null,
                    text
            )

            descriptors.add(descriptor)
        }

        fun processNode(node: ASTNode) {
            if (node.elementType == RnTypes.PROPERTY) {
                add(node, node.lastChildNode)
            }

            for (child in node.getChildren(null)) {
                processNode(child)
            }
        }

        processNode(root)

        return descriptors.toTypedArray<FoldingDescriptor>()
    }


    override fun getPlaceholderText(node: ASTNode): String {
        return "..."
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        return false
    }
}