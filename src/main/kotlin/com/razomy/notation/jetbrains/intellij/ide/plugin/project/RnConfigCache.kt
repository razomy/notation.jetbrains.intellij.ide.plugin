package com.razomy.notation.jetbrains.intellij.ide.plugin.project

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import java.util.concurrent.ConcurrentHashMap

data class RnConfig(
    val order: List<String> = emptyList(),
    val scopes: List<ScopeDefinition> = emptyList()
)

object RnConfigCache {
    private val LOG = thisLogger()
    private const val CONFIG_FILE_NAME = "config.rn"
    private val LEADING_DOTS_REGEX = "^\\.+".toRegex()
    private val KNOWN_EXTENSIONS_REGEX = "\\.(rn|rl|rnt)$".toRegex()

    private data class CacheEntry(val modStamp: Long, val config: RnConfig)
    private val cache = ConcurrentHashMap<VirtualFile, CacheEntry>()

    fun getConfig(directoryVf: VirtualFile?): RnConfig {
        if (directoryVf == null) return RnConfig()
        val configFile = directoryVf.findChild(CONFIG_FILE_NAME)

        if (configFile == null || !configFile.exists() || configFile.isDirectory) {
            cache.remove(directoryVf)
            return RnConfig()
        }

        val currentStamp = configFile.modificationStamp
        val cached = cache[directoryVf]
        if (cached != null && cached.modStamp == currentStamp) {
            return cached.config
        }

        val parsed = parse(configFile)
        cache[directoryVf] = CacheEntry(currentStamp, parsed)
        return parsed
    }

    /**
     * Normalizes filenames so both "start.rn" and "start" match cleanly,
     * while preserving extensions like "external.java" vs "external.py".
     */
    fun normalizeOrderName(name: String): String {
        return name
            .replace(LEADING_DOTS_REGEX, "")
            .replace(KNOWN_EXTENSIONS_REGEX, "")
    }

    private fun parse(file: VirtualFile): RnConfig {
        val orderList = mutableListOf<String>()
        val scopesMap = linkedMapOf<String, MutableList<String>>()

        var currentSection = ""
        var currentScopeName: String? = null

        try {
            for (rawLine in file.readText().lineSequence()) {
                val trimmed = rawLine.trim()
                // Skip empty lines and comments (like "# comment.rnt")
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

                val indent = rawLine.takeWhile { it == ' ' }.length

                // Top-level section (e.g., "order", "scopes")
                if (indent == 0) {
                    currentSection = trimmed
                    currentScopeName = null
                    continue
                }

                when (currentSection) {
                    "order" -> {
                        // Store both exact name and normalized name (without .rn/.rl/.rnt)
                        orderList.add(normalizeOrderName(trimmed))
                    }
                    "scopes" -> {
                        if (indent == 1) {
                            currentScopeName = trimmed
                            scopesMap.putIfAbsent(currentScopeName, mutableListOf())
                        } else if (indent >= 2 && currentScopeName != null) {
                            scopesMap[currentScopeName]?.add(trimmed)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOG.warn("Failed to parse ${file.path}", e)
        }

        val scopes = scopesMap.map { (name, patterns) -> ScopeDefinition(name, patterns) }
        return RnConfig(orderList, scopes)
    }
}
