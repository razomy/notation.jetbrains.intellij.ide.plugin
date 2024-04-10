package com.razomy.notation.idea.plugin;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;


abstract class RnLexerBridge implements FlexLexer {

    public int deep = 0;

    public IElementType tryDeep(int deep) {
        int lastDeep = this.deep;
        this.deep = deep;
        if (deep > lastDeep) {
            return RnTypes.CHILD_DEEP;
        }
//        TODO: loop
        return RnTypes.DEEP;
    }

}
