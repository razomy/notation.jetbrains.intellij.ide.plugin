package com.razomy.notation.jetbrains.intellij.ide.plugin

import com.intellij.lang.Commenter
import org.jetbrains.annotations.Nullable

class RnCommenter : Commenter {
    @Nullable
    override fun getLineCommentPrefix(): String {
        return "#"
    }

    @Nullable
    override fun getBlockCommentPrefix(): String {
        return ""
    }

    @Nullable
    override fun getBlockCommentSuffix(): String? {
        return null
    }

    @Nullable
    override fun getCommentedBlockCommentPrefix(): String? {
        return null
    }

    @Nullable
    override fun getCommentedBlockCommentSuffix(): String? {
        return null
    }
}