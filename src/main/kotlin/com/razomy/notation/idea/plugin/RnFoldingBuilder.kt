package com.razomy.notation.idea.plugin

import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange


class RnFoldingBuilder : FoldingBuilder {
    override fun buildFoldRegions(node: ASTNode, document: Document): Array<FoldingDescriptor> {
        val descriptors: MutableList<FoldingDescriptor> = ArrayList()

        var lastNode: ASTNode? = null

        fun add(nodeFrom: ASTNode, nodeTo: ASTNode) {
            if (nodeTo.textRange.endOffset == 0) {
                return
            }
            descriptors.add(FoldingDescriptor(
                    nodeFrom,
                    TextRange(nodeFrom.startOffset, nodeTo.textRange.endOffset - 1),
                    null,
                    nodeFrom.text.substring(0, Math.min(100, nodeFrom.text.length))))
        }


        fun processNode(node: ASTNode, descriptors: MutableList<FoldingDescriptor>) {
            if (node.treePrev != null) {
                if (node.treePrev.text.endsWith('\n')) {
                    if (lastNode is ASTNode) {
                        add(lastNode!!, node.treePrev)
                    }
                    lastNode = node
                }
            }

            for (child in node.getChildren(null)) {
                processNode(child, descriptors)
            }
        }

        processNode(node, descriptors)

        if (lastNode != null) {
            add(lastNode!!, node)
        }

        return descriptors.toTypedArray<FoldingDescriptor>()
    }


    override fun getPlaceholderText(node: ASTNode): String? {
        // Return the text that will be displayed in the editor as a placeholder for the folded region
        // You can customize this based on your needs
        return "..."
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        // Return true if the folding region should be collapsed by default
        // You can customize this based on your needs
        return false
    }
}