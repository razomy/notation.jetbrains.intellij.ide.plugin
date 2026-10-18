package org.razomy.language

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.FlexAdapter
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.razomy.language.parser.RazomyParser
import org.razomy.language.psi.RazomyFile
import org.razomy.language.psi.RazomyTypes

class RazomyParserDefinition : ParserDefinition {
    companion object {
        val WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE)
        val COMMENTS = TokenSet.create(RazomyTypes.LINE_COMMENT, RazomyTypes.DOC_COMMENT)
        val FILE = IFileElementType(RazomyLanguage.INSTANCE)
    }

    override fun createLexer(project: Project?) = FlexAdapter(RazomyLexer(null))
    override fun createParser(project: Project?): PsiParser = RazomyParser()
    override fun getFileNodeType(): IFileElementType = FILE
    override fun getWhitespaceTokens(): TokenSet = WHITE_SPACES
    override fun getCommentTokens(): TokenSet = COMMENTS
    override fun getStringLiteralElements(): TokenSet = TokenSet.create(RazomyTypes.STRING)
    override fun createElement(node: ASTNode?): PsiElement = RazomyTypes.Factory.createElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = RazomyFile(viewProvider)
}

