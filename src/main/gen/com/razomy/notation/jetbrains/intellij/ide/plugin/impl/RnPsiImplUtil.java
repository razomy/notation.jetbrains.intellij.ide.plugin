package com.razomy.notation.jetbrains.intellij.ide.plugin.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.razomy.notation.jetbrains.intellij.ide.plugin.RnElementFactory;
import com.razomy.notation.jetbrains.intellij.ide.plugin.RnProperty;
import com.razomy.notation.jetbrains.intellij.ide.plugin.RnTypes;

public class RnPsiImplUtil {
    public static String getKey(RnProperty element) {
        ASTNode keyNode = element.getNode().findChildByType(RnTypes.KEY);
        return keyNode != null ? keyNode.getText() : null;
    }

    public static String getName(RnProperty element) {
        return getKey(element);
    }

    public static String getValue(RnProperty element) {
        ASTNode valueNode = element.getNode().findChildByType(RnTypes.VALUE);
        return valueNode != null ? valueNode.getText() : null;
    }

    public static PsiElement setName(RnProperty element, String newName) {
        ASTNode keyNode = element.getNode().findChildByType(RnTypes.KEY);
        if (keyNode != null) {
            RnProperty property = RnElementFactory.INSTANCE.createProperty(element.getProject(), newName);
            ASTNode newKeyNode = property.getFirstChild().getNode();
            element.getNode().replaceChild(keyNode, newKeyNode);
        }
        return element;
    }

    public static PsiElement getNameIdentifier(RnProperty element) {
        ASTNode keyNode = element.getNode().findChildByType(RnTypes.KEY);
        return keyNode != null ? keyNode.getPsi() : null;
    }
}

