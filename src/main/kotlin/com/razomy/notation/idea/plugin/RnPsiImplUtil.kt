package com.razomy.notation.idea.plugin

object RnPsiImplUtil {
    fun getKey(element: RnProperty): String? {
        val keyNode = element.node.findChildByType(RnTypes.KEY)
        return keyNode?.text
    }

    fun getValue(element: RnProperty): String? {
        val valueNode = element.node.findChildByType(RnTypes.VALUE)
        return valueNode?.text
    }
}
