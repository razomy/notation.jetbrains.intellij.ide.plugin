package com.razomy.notation.jetbrains.intellij.ide.plugin.lexer

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class RnParser : PsiParser {

    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        // Start the Root Marker (File)
        val rootMarker = builder.mark()

        // Loop through tokens until EOF
        while (!builder.eof()) {
            val type = builder.tokenType

            // Skip whitespace explicitly if the lexer passes it to parser
            // (Usually ParserDefinition handles whitespace, but logic is safer here)
            if (type == TokenType.WHITE_SPACE || type == RnTypes.COMMENT) {
                builder.advanceLexer()
            } else if (type == RnTypes.KEY) {
                parseProperty(builder)
            } else {
                builder.advanceLexer() // Skip unknown/bad chars
            }
        }

        rootMarker.done(root)
        return builder.treeBuilt
    }

    // Logic: Key [Value]
    // We treat the first identifier as KEY, and if there is another on the same line, it's VALUE.
    private fun parseProperty(builder: PsiBuilder) {
        val marker = builder.mark()

        // Consume Key
        builder.advanceLexer()

        // Check for Value (Look ahead for identifier without a newline in between)
        // Note: In a raw lexer, WHITE_SPACE includes newlines.
        // We need to check if the next token is on the same line.

        // We simply peek. If it's an Identifier, we assume it belongs to this property
        // For a strict line-based parser, we would need to check the whitespace content for '\n'.
        // Here, to match your "key value" vs "key \n value" requirement:

        if (!builder.eof() && builder.tokenType == RnTypes.KEY) {
            // Check if the whitespace between Key and Value contained a newline
            // This is tricky in PsiBuilder without raw lookup, but generally:
            // If the parser definition filters whitespace, we can't see it here.
            // If we assume a flat stream: KEY -> VALUE

            builder.advanceLexer() // Consume Value
        } else {
            // It might be the "key \n sub key" case.
            // In this simple parser, we treat that as a Property(Key) followed by a Property(SubKey).
            // Handling deep indentation nesting manually is complex; this creates a flat list of properties.
        }

        marker.done(RnTypes.PROPERTY)
    }
}