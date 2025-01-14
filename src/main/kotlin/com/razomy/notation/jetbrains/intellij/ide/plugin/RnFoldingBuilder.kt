package com.razomy.notation.jetbrains.intellij.ide.plugin

import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import kotlin.math.min

fun substringWithDots(input: String, maxLength: Int): String {
    if (input.length <= maxLength) return input

    var f_part: Int
    var s_part: Int
    var t_part: Int

    val remainder = maxLength % 3
    when (remainder) {
        0 -> {
            // 120 + 40 / 40 / 40
            val base = maxLength / 3
            f_part = base
            s_part = base
            t_part = base
        }

        1 -> {
            // 118 - 39.3 - 39 - 39  = 1
            val base = maxLength / 3
            f_part = base + 1
            s_part = base
            t_part = base
        }

        2 -> {
            // 119 - 39.6 - 39 - 39 = 2
            val base = maxLength / 3
            f_part = base + 2
            s_part = base
            t_part = base
        }

        else -> throw IllegalStateException("Unexpected remainder: $remainder")
    }

    s_part -= 1

    val start = input.substring(0, f_part + s_part)
    val end = input.substring(input.length - t_part)

    return "$start…$end"
}

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
            val from_text_in_range_120_with_ellipces = substringWithDots(propertyNode.text, 120 - leadingSpaces)
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