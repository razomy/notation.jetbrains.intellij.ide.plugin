package com.razomy.notation.jetbrains.intellij.ide.plugin

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.formatter.common.AbstractBlock
import com.razomy.notation.jetbrains.intellij.ide.plugin.parse.bridge.RnTypes
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

class RnBlock(
    @NotNull node: ASTNode, @Nullable wrap: Wrap?, @Nullable alignment: Alignment?,
    private val spacingBuilder: SpacingBuilder
) : AbstractBlock(node, wrap, alignment) {

    override fun buildChildren(): List<Block> {
        val blocks: MutableList<Block> = ArrayList()
        var child: ASTNode? = myNode.firstChildNode;

        while (child != null) {
            if (child.elementType == RnTypes.PROPERTY) {

                val block = RnBlock(
                    child,
                    Wrap.createWrap(WrapType.NONE, false),
                    Alignment.createAlignment(),
                    spacingBuilder
                )
                blocks.add(block)
            }
            child = child.treeNext
        }
        return blocks
    }

    override fun getIndent(): Indent? {
        return Indent.getNoneIndent()
    }

    override fun getSpacing(child1: Block?, child2: Block): Spacing? {
        return spacingBuilder.getSpacing(this, child1, child2)
    }

    override fun isLeaf(): Boolean {
        return myNode.firstChildNode == null
    }
}