package com.razomy.notation.jetbrains.intellij.ide.plugin

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.formatter.common.AbstractBlock
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

class RnBlock(
    @NotNull node: ASTNode, @Nullable wrap: Wrap?, @Nullable alignment: Alignment?,
    private val spacingBuilder: SpacingBuilder
) : AbstractBlock(node, wrap, alignment) {

    private var deep: Int = 0;

    override fun buildChildren(): List<Block> {
        val blocks: MutableList<Block> = ArrayList()
        var child: ASTNode? = myNode.firstChildNode;
        var deep = 0;
        while (child != null) {
            if (child.elementType == RnTypes.CHILD_DEEP) {
                deep += 1;
            }
            if (child.elementType == RnTypes.END) {
                deep = 0;
            }
            if (child.elementType !== RnTypes.END
                || child.elementType !== RnTypes.CHILD_DEEP
                || child.elementType !== RnTypes.EMPTY_LINE
            ) {
                val block = RnBlock(
                    child,
                    Wrap.createWrap(WrapType.NONE, false),
                    Alignment.createAlignment(),
                    spacingBuilder
                )
                block.deep = deep;
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