package com.razomy.notation.jetbrains.intellij.ide.plugin.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile

class ScopeDefinition(val name: String, val patterns: List<String>) {

    private data class CompiledPattern(
        val regex: Regex,
        val prefix: String,
        val root: String
    )

    private val matchesAll = patterns.isEmpty() || patterns.contains("*")

    private val compiledPatterns: List<CompiledPattern> = if (matchesAll) {
        emptyList()
    } else {
        patterns.map { pattern ->
            CompiledPattern(
                regex = pattern.replace(".", "\\.").replace("*", ".*").toRegex(),
                prefix = pattern.replace("*", ""),
                root = pattern.replace("/*", "")
            )
        }
    }

    fun accepts(file: VirtualFile?, project: Project): Boolean {
        if (matchesAll) return true
        if (file == null) return false

        val baseDir = project.guessProjectDir() ?: return false
        val relativePath = VfsUtilCore.getRelativePath(file, baseDir) ?: return false

        return compiledPatterns.any { cp ->
            relativePath.matches(cp.regex) ||
                    relativePath.startsWith(cp.prefix) ||
                    cp.root.startsWith(relativePath)
        }
    }

    override fun toString(): String = name
}

object ScopeLoader {
    private val ALL_FILES_SCOPE = ScopeDefinition("All Files", listOf("*"))

    fun loadScopes(project: Project): List<ScopeDefinition> {
        val baseDir = project.guessProjectDir() ?: return listOf(ALL_FILES_SCOPE)
        val parsedScopes = RnConfigCache.getConfig(baseDir).scopes
        return listOf(ALL_FILES_SCOPE) + parsedScopes
    }
}
