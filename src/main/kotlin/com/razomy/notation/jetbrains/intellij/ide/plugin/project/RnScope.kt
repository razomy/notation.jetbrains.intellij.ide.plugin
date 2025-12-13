package com.razomy.notation.jetbrains.intellij.ide.plugin.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import com.intellij.openapi.vfs.readText
import com.intellij.openapi.vfs.resolveFromRootOrRelative
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

class ScopeDefinition(public var name: String?, public var patterns: MutableList<String>) {
    fun accepts(file: VirtualFile?, project: Project): Boolean {
        // "All" fallback
        if (patterns.isEmpty() || patterns.contains("*")) return true

        val baseDir = project.guessProjectDir()
        if (baseDir == null || file == null) return false
        val relativePath = VfsUtilCore.getRelativePath(file, baseDir)
        if (relativePath == null) return false

        for (pattern in patterns) {
            // Check if file matches pattern
            val regex = pattern.replace(".", "\\.").replace("*", ".*")
            if (relativePath.matches(regex.toRegex()) || relativePath.startsWith(pattern.replace("*", ""))) {
                return true
            }
            // Check if directory is an ancestor of the pattern (so tree can open)
            val patternRoot = pattern.replace("/*", "")
            if (patternRoot.startsWith(relativePath)) return true
        }
        return false
    }

    override fun toString(): String {
        return name!!
    }
}

object ScopeLoader {
    fun loadScopes(project: Project): MutableList<ScopeDefinition?> {
        val scopes: MutableList<ScopeDefinition?> = ArrayList<ScopeDefinition?>()
        scopes.add(ScopeDefinition("All Files", mutableListOf<String>("*")))

        val basePath = project.guessProjectDir()
        if (basePath == null) return scopes

        val scopeFile = basePath.findFile("config.rn") // The config file
        if (scopeFile?.exists() == null) return scopes

        try {
            val content: String = scopeFile.readText()
            val scope: String = content.substringAfter("scopes", "")
            // Split by double newlines (regex covers \n\n or \r\n\r\n)
            val blocks = scope.split("\n (?=[a-z])".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            for (block in blocks) {
                if (block.isBlank()) continue


                // Split block into lines
                val lines: MutableList<String?> =
                    Arrays.stream(block.split("\\R".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                        .map({ obj: String? -> obj!!.trim { it <= ' ' } })
                        .filter({ s -> !s.isEmpty() })
                        .collect(Collectors.toList())

                if (!lines.isEmpty()) {
                    val name = lines.get(0) // First line is Name
                    val patterns = lines.subList(1, lines.size) // Rest are paths
                    scopes.add(ScopeDefinition(name, patterns as MutableList<String>))
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return scopes
    }
}