package com.razomy.notation.jetbrains.intellij.ide.plugin

import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import kotlin.math.min

fun countTrailingSpacesWithNewLines(str: String): Int {
    var count = 0
    for (i in str.lastIndex downTo 0) {
        if (str[i] == ' ' || str[i] == '\n') {
            count++
        } else {
            break
        }
    }
    return count
}

fun countTrailingSpaces(str: String): Int {
    var count = 0
    for (i in str.lastIndex downTo 0) {
        if (str[i] == ' ') {
            count++
        } else {
            break
        }
    }
    return count
}


class RnFoldingBuilder : FoldingBuilder {
    override fun buildFoldRegions(root: ASTNode, document: Document): Array<FoldingDescriptor> {
        val descriptors: MutableList<FoldingDescriptor> = ArrayList()

        fun add(propertyNode: ASTNode) {
            val trailingSpaces = countTrailingSpacesWithNewLines(propertyNode.text);
            val removed_last_line_spaces = propertyNode.textRange.endOffset - trailingSpaces;
            val range = TextRange(propertyNode.textRange.startOffset, removed_last_line_spaces)

            var prevtext = propertyNode.treePrev?.text;
            if (prevtext == null) {
                prevtext = ""
            }

            val leadingSpaces = countTrailingSpaces(prevtext);
            val from_text_in_range_120_with_ellipces = propertyNode.text.substring(
                0,
                min(120 - 3 - leadingSpaces, propertyNode.text.length)
            ) + "..."
            val placeholderText = from_text_in_range_120_with_ellipces

            val descriptor = FoldingDescriptor(
                propertyNode,
                range,
                null,
                placeholderText
            )

            descriptors.add(descriptor)
        }

        fun processNode(node: ASTNode) {
            if (node.elementType == RnTypes.PROPERTY) {
                if (node.textRange.length == 0) {
                    return
                }

                add(node)
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