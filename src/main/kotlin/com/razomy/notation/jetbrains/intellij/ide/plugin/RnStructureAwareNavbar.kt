package com.razomy.notation.jetbrains.intellij.ide.plugin

import com.intellij.icons.AllIcons
import com.intellij.ide.navigationToolbar.StructureAwareNavBarModelExtension
import com.intellij.lang.Language
import javax.swing.Icon


internal class RnStructureAwareNavbar() : StructureAwareNavBarModelExtension() {
    override val language: Language = RnLanguage
    override fun getPresentableText(`object`: Any): String? {
        var name = "";
        if (`object` is RnFile) {
            name = `object`.getName()
        }
        if (`object` is RnProperty) {
            name = `object`.getName() ?: ""
        }

        name = name.substring(0, Math.min(name.length, 6))

        if (name == "") {
            return null
        }

        return name;
    }


    override fun getIcon(`object`: Any): Icon? {
        if (`object` is RnProperty) {
            return AllIcons.Nodes.Property
        }

        return null
    }
}