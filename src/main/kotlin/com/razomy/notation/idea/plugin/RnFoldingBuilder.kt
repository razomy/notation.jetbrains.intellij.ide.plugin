package com.razomy.notation.idea.plugin

import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange


class RnFoldingBuilder : FoldingBuilder {
    override fun buildFoldRegions(root: ASTNode, document: Document): Array<FoldingDescriptor> {
        val descriptors: MutableList<FoldingDescriptor> = ArrayList()

        fun add(nodeFrom: ASTNode, nodeTo: ASTNode) {
            val text = nodeFrom.text.substring(0, Math.min(120, nodeFrom.text.length))
            val start = nodeFrom.startOffset
            val end = nodeTo.textRange.endOffset - 1
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

//        var lastNode: ASTNode? = null
//
//
//
//        fun processNode(node: ASTNode, descriptors: MutableList<FoldingDescriptor>) {
//            if (lastNode == null && node.text != "\n") {
//                lastNode = node
//            } else if (node == lastNode) {
//                // do nothing
//            } else if (node.textRange.endOffset == 0){
//                // do nothing
//            }else if (lastNode != null && node.text == "\n") {
//                add(lastNode!!, node.treePrev)
//                lastNode = null
//            }
//
//            for (child in node.getChildren(null)) {
//                processNode(child, descriptors)
//            }
//        }
//
//        for (child in root.getChildren(null)) {
//            if (child == root.firstChildNode) {
//                lastNode = child
//                continue
//            };
//            processNode(child, descriptors)
//        }
//
//        if (lastNode != null && lastNode != root.lastChildNode) {
//            add(lastNode!!, root.lastChildNode)
//        }

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