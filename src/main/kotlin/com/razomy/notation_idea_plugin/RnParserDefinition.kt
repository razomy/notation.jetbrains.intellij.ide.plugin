package com.razomy.notation_idea_plugin;

import com.intellij.lang.*
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.annotations.NotNull


class RnParserDefinition : ParserDefinition {

    companion object {
        val FILE = IFileElementType(RnLanguage.INSTANCE)
    }

    @NotNull
    override fun createLexer(project: Project): Lexer {
        return RnLexerAdapter()
    }

    @NotNull
    override fun getCommentTokens(): TokenSet {
        return RnTokenSets.COMMENTS
    }

    @NotNull
    override fun getStringLiteralElements(): TokenSet {
        return TokenSet.EMPTY
    }

    @NotNull
    override fun createParser(project: Project): PsiParser {
        return RnParser()
    }

    @NotNull
    override fun getFileNodeType(): IFileElementType {
        return FILE
    }

    @NotNull
    override fun createFile(viewProvider: FileViewProvider): PsiFile {
        return RnFile(viewProvider)
    }

    @NotNull
    override fun createElement(node: ASTNode): PsiElement {
        return RnTypes.Factory.createElement(node)
    }

}
