package com.razomy.notation.idea.plugin;

import com.intellij.psi.tree.IElementType;
import java.util.Stack;
%%

%class RnLexer
%extends RnLexerBridge
%unicode
%function advance
%type IElementType
%{



    public static int countMatchingChars(CharSequence text) {
        if (text.charAt(text.length() - 1) != ' ') {
            return 0;
        }

        int count = 0;
        for (int i = text.length() - 1; i >= 0; i--) {
            if (count == 0 || text.charAt(i) == text.charAt(text.length() - count - 1)) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }
    public int getSpaces() {
        int spaceCount = countMatchingChars(yytext());
 return spaceCount;
    }


    class Node {
        final int spaces;

        public Node(){
            this.spaces = getSpaces();
        }
    }

    private final Stack<Node> states = new Stack<Node>();

    private void pushState() {
        states.push(new Node());
    }

    private void popState() {
        Node state = states.pop();
    }

    public int deep = 0;
    public int deep_spaces = 0;

     private IElementType waiting_child(int CHILD_WAITING_KEY) {
        deep +=1;
        yybegin(CHILD_WAITING_KEY);
        return RnTypes.CHILD_DEEP;
        }

             private IElementType waiting_child_or_end(int CHILD_WAITING_KEY) {
                deep +=1;
                yybegin(CHILD_WAITING_KEY);
                return RnTypes.CHILD_DEEP;
                }



     private IElementType waiting_child_end(int NEXT, int WAITING_END_LINE) {
         if(deep>0){
            deep =deep-1;
            yypushback(1);
            yybegin(NEXT);
            return RnTypes.END;
         }

         yypushback(1);
         yybegin(WAITING_END_LINE);
         return RnTypes.END;
    }

%}
%eof{  return;
%eof}


COMMENT=("#")[^\r\n]* // not END
WHITE_SPACE=[\ \t\f]
END=[\r\n]
KEY_CHARACTER=[^\ \t\f\r\n] // not WHITE_SPACE and not END
SEPARATOR=[ ]
FIRST_VALUE_CHARACTER=[^\ \t\f\r\n] // not WHITE_SPACE and not END
VALUE=[^\r\n] // not END


%state WAITING_VALUE, CHILD_WAITING_CHILD_OR_END,__CHILD_WAITING_CHILD_OR_END,CHILD_WAITING_VALUE,CHILD_WAITING_SEPARATOR_OR_CHILD_OR_END,CHILD_WAITING_KEY,CHILD_WAITING_END,__CHILD_WAITING_END, CHILD_WAITING_END_NEXT, WAITING_KEY,WAITING_SEPARATOR_OR_CHILD_OR_END,WAITING_CHILD_OR_END, WAITING_KEY_END, WAITING_END_LINE, WAITING_SEPARATOR, WAITING_VALUE_END, WAITING_CHILD_DEEP
%%

<YYINITIAL> {
. { yypushback(1); yybegin(WAITING_KEY);}
}

<WAITING_KEY>{
{KEY_CHARACTER}+                               {yybegin(WAITING_SEPARATOR_OR_CHILD_OR_END); return RnTypes.KEY; }
}

<WAITING_END_LINE> {END}                                                   { yybegin(WAITING_KEY); return RnTypes.EMPTY_LINE; }



<WAITING_SEPARATOR_OR_CHILD_OR_END> {SEPARATOR}                            { yybegin(WAITING_VALUE); return RnTypes.SEPARATOR; }
<WAITING_SEPARATOR_OR_CHILD_OR_END> {END}{WHITE_SPACE}+                    { return waiting_child(CHILD_WAITING_KEY); }
<WAITING_SEPARATOR_OR_CHILD_OR_END> {END}                                  { yypushback(1); yybegin(WAITING_END_LINE); return RnTypes.END; }

<WAITING_VALUE> {FIRST_VALUE_CHARACTER}({VALUE}*)          { yybegin(WAITING_CHILD_OR_END); return RnTypes.VALUE; }

<WAITING_CHILD_OR_END> {END}{WHITE_SPACE}+                    { return waiting_child(CHILD_WAITING_KEY); }
<WAITING_CHILD_OR_END> {END}                                  { yypushback(1); yybegin(WAITING_END_LINE); return RnTypes.END; }




<CHILD_WAITING_KEY>{KEY_CHARACTER}+ {yybegin(CHILD_WAITING_SEPARATOR_OR_CHILD_OR_END); return RnTypes.KEY; }


<CHILD_WAITING_SEPARATOR_OR_CHILD_OR_END> {SEPARATOR}   { yybegin(CHILD_WAITING_VALUE); return RnTypes.SEPARATOR; }
<CHILD_WAITING_SEPARATOR_OR_CHILD_OR_END> {END}{WHITE_SPACE}+  { return waiting_child_or_end(CHILD_WAITING_KEY); }
<CHILD_WAITING_SEPARATOR_OR_CHILD_OR_END> {END}         { return waiting_child_end(__CHILD_WAITING_CHILD_OR_END, WAITING_END_LINE); }

<CHILD_WAITING_VALUE> {FIRST_VALUE_CHARACTER}({VALUE}*)          { yybegin(CHILD_WAITING_CHILD_OR_END); return RnTypes.VALUE; }

<CHILD_WAITING_CHILD_OR_END> {END}{WHITE_SPACE}+                    { return waiting_child_or_end(CHILD_WAITING_KEY); }

<CHILD_WAITING_CHILD_OR_END> {END}                                    { return waiting_child_end(__CHILD_WAITING_CHILD_OR_END, WAITING_END_LINE); }
<__CHILD_WAITING_CHILD_OR_END> {END}                                  { return waiting_child_end(CHILD_WAITING_CHILD_OR_END, WAITING_END_LINE); }


[^]                                                        {return RnTypes.ERROR; }
