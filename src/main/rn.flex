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
    int last_index = text.length() - 1;
    var last_char = text.charAt(last_index);

    if (last_char != ' ') {
        return 0;
    }

    int count = 1;
    for (int i = last_index-1; i >= 0; i--) {
        if (text.charAt(i) == last_char) {
            count++;
        } else {
            break;
        }
    }
    return count;
}

class Node {
    final int spaces;

    public Node(){
        this.spaces = countMatchingChars(yytext());
    }
}

private final Stack<Node> states = new Stack<Node>();

private Node pushState() {
    Node node = new Node();
    states.push(node);
    return node;
}

private Node popState() {
    Node node = states.pop();
    return node;
}

private IElementType waiting_child(int CHILD_WAITING_KEY) {
    pushState();
    yybegin(CHILD_WAITING_KEY);
    return RnTypes.CHILD_DEEP;
}

private IElementType waiting_child_or_end(int __NEXT, int CHILD_WAITING_KEY) {
   if(states.isEmpty()){
        yybegin(CHILD_WAITING_KEY);
        return RnTypes.CHILD_DEEP;
   }

    Node prev_n = popState();
    Node curr_n = new Node();

    if (curr_n.spaces > prev_n.spaces){
        states.push(prev_n);
        states.push(curr_n);
        yybegin(CHILD_WAITING_KEY);
        return RnTypes.CHILD_DEEP;
    }

    if (curr_n.spaces == prev_n.spaces){
        states.push(curr_n);
        yybegin(CHILD_WAITING_KEY);
        return RnTypes.END;
    }

//    1 - {END}
    yypushback(curr_n.spaces+1);
    yybegin(__NEXT);
    return RnTypes.END;
}

private IElementType waiting_child_end(int __NEXT, int WAITING_END_LINE) {
    if(!states.isEmpty()){
        popState();
        yypushback(1);
        yybegin(__NEXT);
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


// ROOT
%state WAITING_KEY,
%state     WAITING_SEPARATOR_OR_CHILD_OR_END,
%state         WAITING_VALUE,
%state             WAITING_CHILD_OR_END,
%state         CHILD_WAITING_KEY,
%state WAITING_END_LINE
// CHILD
%state CHILD_WAITING_KEY
%state     CHILD_WAITING_SEPARATOR_OR_CHILD_OR_END
%state         CHILD_WAITING_VALUE
%state             CHILD_WAITING_VALUE_CHILD_OR_END, __CHILD_WAITING_VALUE_CHILD_OR_END
%state         CHILD_WAITING_CHILD_OR_END, __CHILD_WAITING_CHILD_OR_END,


%%
<YYINITIAL> . { yypushback(1); yybegin(WAITING_KEY);}


// ROOT

<WAITING_KEY> {COMMENT}{END}                                {yybegin(WAITING_KEY); return RnTypes.COMMENT; }
<WAITING_KEY> {KEY_CHARACTER}+                              {yybegin(WAITING_SEPARATOR_OR_CHILD_OR_END); return RnTypes.KEY; }


<WAITING_SEPARATOR_OR_CHILD_OR_END> {SEPARATOR}             { yybegin(WAITING_VALUE); return RnTypes.SEPARATOR; }
<WAITING_SEPARATOR_OR_CHILD_OR_END> {END}{WHITE_SPACE}+     { return waiting_child(CHILD_WAITING_KEY); }
<WAITING_SEPARATOR_OR_CHILD_OR_END> {END}                   { yypushback(1); yybegin(WAITING_END_LINE); return RnTypes.END; }

<WAITING_VALUE> {FIRST_VALUE_CHARACTER}({VALUE}*)           { yybegin(WAITING_CHILD_OR_END); return RnTypes.VALUE; }

<WAITING_CHILD_OR_END> {END}{WHITE_SPACE}+                  { return waiting_child(CHILD_WAITING_KEY); }
<WAITING_CHILD_OR_END> {END}                                { yypushback(1); yybegin(WAITING_END_LINE); return RnTypes.END; }

<WAITING_END_LINE> {END}                                    { yybegin(WAITING_KEY); return RnTypes.EMPTY_LINE; }

// CHILD

<CHILD_WAITING_KEY> {COMMENT}{END}{WHITE_SPACE}*               {yybegin(CHILD_WAITING_KEY); return RnTypes.COMMENT; }
<CHILD_WAITING_KEY>{KEY_CHARACTER}+                            {yybegin(CHILD_WAITING_SEPARATOR_OR_CHILD_OR_END); return RnTypes.KEY; }

<CHILD_WAITING_SEPARATOR_OR_CHILD_OR_END> {SEPARATOR}          { yybegin(CHILD_WAITING_VALUE); return RnTypes.SEPARATOR; }
<CHILD_WAITING_SEPARATOR_OR_CHILD_OR_END> {END}{WHITE_SPACE}+  { return waiting_child_or_end(__CHILD_WAITING_VALUE_CHILD_OR_END, CHILD_WAITING_KEY); }
<CHILD_WAITING_SEPARATOR_OR_CHILD_OR_END> {END}                { return waiting_child_end(CHILD_WAITING_CHILD_OR_END, WAITING_END_LINE); }

<CHILD_WAITING_VALUE> {FIRST_VALUE_CHARACTER}({VALUE}*)        { yybegin(CHILD_WAITING_VALUE_CHILD_OR_END); return RnTypes.VALUE; }
<CHILD_WAITING_VALUE_CHILD_OR_END> {END}{WHITE_SPACE}+         { return waiting_child_or_end(__CHILD_WAITING_VALUE_CHILD_OR_END, CHILD_WAITING_KEY); }
<CHILD_WAITING_VALUE_CHILD_OR_END> {END}                       { return waiting_child_end(__CHILD_WAITING_CHILD_OR_END, WAITING_END_LINE); }
<__CHILD_WAITING_VALUE_CHILD_OR_END> {END}{WHITE_SPACE}+       { return waiting_child_or_end(CHILD_WAITING_VALUE_CHILD_OR_END, CHILD_WAITING_KEY); }
<__CHILD_WAITING_VALUE_CHILD_OR_END> {END}                     { return waiting_child_end(CHILD_WAITING_VALUE_CHILD_OR_END, WAITING_END_LINE); }


<CHILD_WAITING_CHILD_OR_END> {END}                             { return waiting_child_end(__CHILD_WAITING_CHILD_OR_END, WAITING_END_LINE); }
<__CHILD_WAITING_CHILD_OR_END> {END}                           { return waiting_child_end(CHILD_WAITING_CHILD_OR_END, WAITING_END_LINE); }

[^] {return RnTypes.ERROR; }
