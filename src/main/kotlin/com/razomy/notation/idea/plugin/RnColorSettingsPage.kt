package com.razomy.notation.idea.plugin

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import org.jetbrains.annotations.Nullable
import javax.swing.Icon

internal class RnColorSettingsPage : ColorSettingsPage {
    @Nullable
    override fun getIcon(): Icon {
        return RnIcons.FILE
    }

    override fun getHighlighter(): SyntaxHighlighter {
        return RnSyntaxHighlighter()
    }

    override fun getDemoText(): String {
        return """# You are reading the ".properties" entry.
! The exclamation mark can also mark text as comments.
website = https://en.wikipedia.org/
language = English
# The backslash below tells the application to continue reading
# the value onto the next line.
message = Welcome to \
          Wikipedia!
# Add spaces to the key
key\ with\ spaces = This is the value that could be looked up with the key "key with spaces".
# Unicode
tab : \u0009"""
    }

    @Nullable
    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? {
        return null
    }

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> {
        return descriptors
    }

    override fun getColorDescriptors(): Array<ColorDescriptor> {
        return ColorDescriptor.EMPTY_ARRAY
    }

    override fun getDisplayName(): String {
        return "Razomy Notation"
    }

    private val descriptors = arrayOf(AttributesDescriptor("Key", RnSyntaxHighlighter.KEY),
            AttributesDescriptor("Separator", RnSyntaxHighlighter.SEPARATOR),
            AttributesDescriptor("Value", RnSyntaxHighlighter.VALUE),
            AttributesDescriptor("Bad value", RnSyntaxHighlighter.BAD_CHARACTER)
    )
}