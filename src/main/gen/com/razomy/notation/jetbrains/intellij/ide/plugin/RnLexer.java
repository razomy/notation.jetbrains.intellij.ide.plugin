// Generated by JFlex 1.9.2 http://jflex.de/  (tweaked for IntelliJ platform)
// source: rn.flex

package com.razomy.notation.jetbrains.intellij.ide.plugin;

import com.intellij.psi.tree.IElementType;
import java.util.Stack;

class RnLexer extends RnLexerBridge {

  /** This character denotes the end of file */
  public static final int YYEOF = -1;

  /** initial size of the lookahead buffer */
  private static final int ZZ_BUFFERSIZE = 16384;

  /** lexical states */
  public static final int YYINITIAL = 0;
  public static final int WAITING_KEY = 2;
  public static final int WAITING_ASSIGN_OR_CHILD_OR_END = 4;
  public static final int WAITING_VALUE = 6;
  public static final int WAITING_CHILD_OR_END = 8;
  public static final int CHILD_WAITING_KEY = 10;
  public static final int WAITING_END_LINE = 12;
  public static final int CHILD_WAITING_ASSIGN_OR_CHILD_OR_END = 14;
  public static final int CHILD_WAITING_VALUE = 16;
  public static final int CHILD_WAITING_VALUE_CHILD_OR_END = 18;
  public static final int __CHILD_WAITING_VALUE_CHILD_OR_END = 20;
  public static final int CHILD_WAITING_CHILD_OR_END = 22;
  public static final int __CHILD_WAITING_CHILD_OR_END = 24;

  /**
   * ZZ_LEXSTATE[l] is the state in the DFA for the lexical state l
   * ZZ_LEXSTATE[l+1] is the state in the DFA for the lexical state l
   *                  at the beginning of a line
   * l is of the form l = 2*k, k a non negative integer
   */
  private static final int ZZ_LEXSTATE[] = {
     0,  0,  1,  1,  2,  2,  3,  3,  4,  4,  5,  5,  6,  6,  7,  7, 
     8,  8,  9,  9, 10, 10, 11, 11, 12, 12
  };

  /**
   * Top-level table for translating characters to character classes
   */
  private static final int [] ZZ_CMAP_TOP = zzUnpackcmap_top();

  private static final String ZZ_CMAP_TOP_PACKED_0 =
    "\1\0\37\u0100\1\u0200\267\u0100\10\u0300\u1020\u0100";

  private static int [] zzUnpackcmap_top() {
    int [] result = new int[4352];
    int offset = 0;
    offset = zzUnpackcmap_top(ZZ_CMAP_TOP_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackcmap_top(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /**
   * Second-level tables for translating characters to character classes
   */
  private static final int [] ZZ_CMAP_BLOCKS = zzUnpackcmap_blocks();

  private static final String ZZ_CMAP_BLOCKS_PACKED_0 =
    "\11\0\1\1\1\2\1\3\1\4\1\2\22\0\1\1"+
    "\2\0\1\5\26\0\1\6\1\7\40\0\1\10\3\0"+
    "\1\11\44\0\1\3\u01a2\0\2\3\326\0\u0100\3";

  private static int [] zzUnpackcmap_blocks() {
    int [] result = new int[1024];
    int offset = 0;
    offset = zzUnpackcmap_blocks(ZZ_CMAP_BLOCKS_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackcmap_blocks(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }

  /**
   * Translates DFA states to action switch labels.
   */
  private static final int [] ZZ_ACTION = zzUnpackAction();

  private static final String ZZ_ACTION_PACKED_0 =
    "\15\0\1\1\1\2\4\3\1\4\1\5\1\6\2\7"+
    "\4\10\1\11\1\12\1\13\1\14\1\15\1\14\1\16"+
    "\1\17\1\16\1\12\1\20\1\0\1\3\1\0\1\3"+
    "\1\21\1\7\1\0\1\7\1\22\1\0\1\10\1\0"+
    "\1\10\1\23\1\14\1\0\1\14\1\24\1\0\1\3"+
    "\1\0\1\7\1\0\1\10\1\0\1\14\1\3\1\7"+
    "\1\10\1\14";

  private static int [] zzUnpackAction() {
    int [] result = new int[69];
    int offset = 0;
    offset = zzUnpackAction(ZZ_ACTION_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAction(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /**
   * Translates a state to a row index in the transition table
   */
  private static final int [] ZZ_ROWMAP = zzUnpackRowMap();

  private static final String ZZ_ROWMAP_PACKED_0 =
    "\0\0\0\12\0\24\0\36\0\50\0\62\0\74\0\106"+
    "\0\120\0\132\0\144\0\156\0\170\0\202\0\202\0\214"+
    "\0\226\0\240\0\252\0\264\0\202\0\202\0\276\0\310"+
    "\0\322\0\334\0\346\0\360\0\202\0\372\0\202\0\u0104"+
    "\0\202\0\u010e\0\372\0\u0118\0\202\0\202\0\202\0\u0122"+
    "\0\u012c\0\u0136\0\u0140\0\264\0\u014a\0\u0154\0\u015e\0\u0168"+
    "\0\u0172\0\u017c\0\u0186\0\u0190\0\372\0\u019a\0\u01a4\0\u01ae"+
    "\0\u0118\0\u01b8\0\202\0\u01c2\0\202\0\u01cc\0\202\0\u01d6"+
    "\0\202\0\u0136\0\u0154\0\u0186\0\u01a4";

  private static int [] zzUnpackRowMap() {
    int [] result = new int[69];
    int offset = 0;
    offset = zzUnpackRowMap(ZZ_ROWMAP_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackRowMap(String packed, int offset, int [] result) {
    int i = 0;  /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length() - 1;
    while (i < l) {
      int high = packed.charAt(i++) << 16;
      result[j++] = high | packed.charAt(i++);
    }
    return j;
  }

  /**
   * The transition table of the DFA
   */
  private static final int [] ZZ_TRANS = zzUnpacktrans();

  private static final String ZZ_TRANS_PACKED_0 =
    "\2\16\3\17\5\16\2\20\1\17\2\20\1\21\2\17"+
    "\1\22\1\23\2\17\1\24\3\17\1\25\1\26\2\17"+
    "\2\27\1\17\3\27\1\17\1\26\1\27\1\30\12\17"+
    "\2\31\1\17\2\31\1\32\2\17\1\33\1\34\2\17"+
    "\1\35\11\17\1\36\3\17\1\37\3\17\2\40\1\17"+
    "\3\40\1\17\1\41\1\40\1\42\2\17\1\43\3\17"+
    "\1\37\1\41\4\17\1\44\11\17\1\45\11\17\1\46"+
    "\7\17\12\0\2\20\1\0\3\20\2\0\1\22\1\20"+
    "\2\21\1\47\3\21\2\50\1\51\1\21\2\20\1\0"+
    "\5\20\1\22\1\20\2\23\1\52\3\23\2\52\1\53"+
    "\1\20\1\0\1\54\2\0\1\54\5\0\2\27\1\0"+
    "\3\27\2\0\1\55\1\27\2\30\1\56\3\30\2\56"+
    "\1\57\1\27\2\31\1\0\3\31\2\0\1\33\1\31"+
    "\2\32\1\60\3\32\2\61\1\62\1\32\2\31\1\0"+
    "\5\31\1\33\1\31\2\34\1\63\3\34\2\63\1\64"+
    "\1\31\1\0\1\65\2\0\1\65\5\0\2\40\1\0"+
    "\3\40\2\0\1\66\1\40\2\42\1\67\3\42\2\67"+
    "\1\70\1\40\1\0\1\71\2\0\1\71\5\0\2\50"+
    "\1\47\7\50\2\21\1\47\5\21\1\51\1\21\10\52"+
    "\1\72\1\73\2\23\1\52\5\23\1\53\1\23\2\27"+
    "\1\0\5\27\1\55\1\27\10\56\1\74\1\75\2\30"+
    "\1\56\5\30\1\57\1\30\1\0\1\60\2\0\1\60"+
    "\5\0\2\61\1\60\7\61\2\32\1\60\5\32\1\62"+
    "\1\32\10\63\1\76\1\77\2\34\1\63\5\34\1\64"+
    "\1\34\2\40\1\0\5\40\1\66\1\40\10\67\1\100"+
    "\1\101\2\42\1\67\5\42\1\70\1\42\10\52\1\72"+
    "\1\102\10\56\1\74\1\103\10\63\1\76\1\104\10\67"+
    "\1\100\1\105";

  private static int [] zzUnpacktrans() {
    int [] result = new int[480];
    int offset = 0;
    offset = zzUnpacktrans(ZZ_TRANS_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpacktrans(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      value--;
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /* error codes */
  private static final int ZZ_UNKNOWN_ERROR = 0;
  private static final int ZZ_NO_MATCH = 1;
  private static final int ZZ_PUSHBACK_2BIG = 2;

  /* error messages for the codes above */
  private static final String[] ZZ_ERROR_MSG = {
    "Unknown internal scanner error",
    "Error: could not match input",
    "Error: pushback value was too large"
  };

  /**
   * ZZ_ATTRIBUTE[aState] contains the attributes of state {@code aState}
   */
  private static final int [] ZZ_ATTRIBUTE = zzUnpackAttribute();

  private static final String ZZ_ATTRIBUTE_PACKED_0 =
    "\15\0\2\11\5\1\2\11\6\1\1\11\1\1\1\11"+
    "\1\1\1\11\3\1\3\11\1\0\1\1\1\0\3\1"+
    "\1\0\2\1\1\0\1\1\1\0\3\1\1\0\2\1"+
    "\1\0\1\11\1\0\1\11\1\0\1\11\1\0\1\11"+
    "\4\1";

  private static int [] zzUnpackAttribute() {
    int [] result = new int[69];
    int offset = 0;
    offset = zzUnpackAttribute(ZZ_ATTRIBUTE_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAttribute(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }

  /** the input device */
  private java.io.Reader zzReader;

  /** the current state of the DFA */
  private int zzState;

  /** the current lexical state */
  private int zzLexicalState = YYINITIAL;

  /** this buffer contains the current text to be matched and is
      the source of the yytext() string */
  private CharSequence zzBuffer = "";

  /** the textposition at the last accepting state */
  private int zzMarkedPos;

  /** the current text position in the buffer */
  private int zzCurrentPos;

  /** startRead marks the beginning of the yytext() string in the buffer */
  private int zzStartRead;

  /** endRead marks the last character in the buffer, that has been read
      from input */
  private int zzEndRead;

  /** zzAtEOF == true <=> the scanner is at the EOF */
  private boolean zzAtEOF;

  /** Number of newlines encountered up to the start of the matched text. */
  @SuppressWarnings("unused")
  private int yyline;

  /** Number of characters from the last newline up to the start of the matched text. */
  @SuppressWarnings("unused")
  protected int yycolumn;

  /** Number of characters up to the start of the matched text. */
  @SuppressWarnings("unused")
  private long yychar;

  /** Whether the scanner is currently at the beginning of a line. */
  @SuppressWarnings("unused")
  private boolean zzAtBOL = true;

  /** Whether the user-EOF-code has already been executed. */
  private boolean zzEOFDone;

  /* user code: */
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


  /**
   * Creates a new scanner
   *
   * @param   in  the java.io.Reader to read input from.
   */
  RnLexer(java.io.Reader in) {
    this.zzReader = in;
  }


  /** Returns the maximum size of the scanner buffer, which limits the size of tokens. */
  private int zzMaxBufferLen() {
    return Integer.MAX_VALUE;
  }

  /**  Whether the scanner buffer can grow to accommodate a larger token. */
  private boolean zzCanGrow() {
    return true;
  }

  /**
   * Translates raw input code points to DFA table row
   */
  private static int zzCMap(int input) {
    int offset = input & 255;
    return offset == input ? ZZ_CMAP_BLOCKS[offset] : ZZ_CMAP_BLOCKS[ZZ_CMAP_TOP[input >> 8] | offset];
  }

  public final int getTokenStart() {
    return zzStartRead;
  }

  public final int getTokenEnd() {
    return getTokenStart() + yylength();
  }

  public void reset(CharSequence buffer, int start, int end, int initialState) {
    zzBuffer = buffer;
    zzCurrentPos = zzMarkedPos = zzStartRead = start;
    zzAtEOF  = false;
    zzAtBOL = true;
    zzEndRead = end;
    yybegin(initialState);
  }

  /**
   * Refills the input buffer.
   *
   * @return      {@code false}, iff there was new input.
   *
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  private boolean zzRefill() throws java.io.IOException {
    return true;
  }


  /**
   * Returns the current lexical state.
   */
  public final int yystate() {
    return zzLexicalState;
  }


  /**
   * Enters a new lexical state
   *
   * @param newState the new lexical state
   */
  public final void yybegin(int newState) {
    zzLexicalState = newState;
  }


  /**
   * Returns the text matched by the current regular expression.
   */
  public final CharSequence yytext() {
    return zzBuffer.subSequence(zzStartRead, zzMarkedPos);
  }


  /**
   * Returns the character at position {@code pos} from the
   * matched text.
   *
   * It is equivalent to yytext().charAt(pos), but faster
   *
   * @param pos the position of the character to fetch.
   *            A value from 0 to yylength()-1.
   *
   * @return the character at position pos
   */
  public final char yycharat(int pos) {
    return zzBuffer.charAt(zzStartRead+pos);
  }


  /**
   * Returns the length of the matched text region.
   */
  public final int yylength() {
    return zzMarkedPos-zzStartRead;
  }


  /**
   * Reports an error that occurred while scanning.
   *
   * In a wellformed scanner (no or only correct usage of
   * yypushback(int) and a match-all fallback rule) this method
   * will only be called with things that "Can't Possibly Happen".
   * If this method is called, something is seriously wrong
   * (e.g. a JFlex bug producing a faulty scanner etc.).
   *
   * Usual syntax/scanner level error handling should be done
   * in error fallback rules.
   *
   * @param   errorCode  the code of the errormessage to display
   */
  private void zzScanError(int errorCode) {
    String message;
    try {
      message = ZZ_ERROR_MSG[errorCode];
    }
    catch (ArrayIndexOutOfBoundsException e) {
      message = ZZ_ERROR_MSG[ZZ_UNKNOWN_ERROR];
    }

    throw new Error(message);
  }


  /**
   * Pushes the specified amount of characters back into the input stream.
   *
   * They will be read again by then next call of the scanning method
   *
   * @param number  the number of characters to be read again.
   *                This number must not be greater than yylength()!
   */
  public void yypushback(int number)  {
    if ( number > yylength() )
      zzScanError(ZZ_PUSHBACK_2BIG);

    zzMarkedPos -= number;
  }


  /**
   * Contains user EOF-code, which will be executed exactly once,
   * when the end of file is reached
   */
  private void zzDoEOF() {
    if (!zzEOFDone) {
      zzEOFDone = true;
    
    }
  }


  /**
   * Resumes scanning until the next regular expression is matched,
   * the end of input is encountered or an I/O-Error occurs.
   *
   * @return      the next token
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  public IElementType advance() throws java.io.IOException
  {
    int zzInput;
    int zzAction;

    // cached fields:
    int zzCurrentPosL;
    int zzMarkedPosL;
    int zzEndReadL = zzEndRead;
    CharSequence zzBufferL = zzBuffer;

    int [] zzTransL = ZZ_TRANS;
    int [] zzRowMapL = ZZ_ROWMAP;
    int [] zzAttrL = ZZ_ATTRIBUTE;

    while (true) {
      zzMarkedPosL = zzMarkedPos;

      zzAction = -1;

      zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;

      zzState = ZZ_LEXSTATE[zzLexicalState];

      // set up zzAction for empty match case:
      int zzAttributes = zzAttrL[zzState];
      if ( (zzAttributes & 1) == 1 ) {
        zzAction = zzState;
      }


      zzForAction: {
        while (true) {

          if (zzCurrentPosL < zzEndReadL) {
            zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL);
            zzCurrentPosL += Character.charCount(zzInput);
          }
          else if (zzAtEOF) {
            zzInput = YYEOF;
            break zzForAction;
          }
          else {
            // store back cached positions
            zzCurrentPos  = zzCurrentPosL;
            zzMarkedPos   = zzMarkedPosL;
            boolean eof = zzRefill();
            // get translated positions and possibly new buffer
            zzCurrentPosL  = zzCurrentPos;
            zzMarkedPosL   = zzMarkedPos;
            zzBufferL      = zzBuffer;
            zzEndReadL     = zzEndRead;
            if (eof) {
              zzInput = YYEOF;
              break zzForAction;
            }
            else {
              zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL);
              zzCurrentPosL += Character.charCount(zzInput);
            }
          }
          int zzNext = zzTransL[ zzRowMapL[zzState] + zzCMap(zzInput) ];
          if (zzNext == -1) break zzForAction;
          zzState = zzNext;

          zzAttributes = zzAttrL[zzState];
          if ( (zzAttributes & 1) == 1 ) {
            zzAction = zzState;
            zzMarkedPosL = zzCurrentPosL;
            if ( (zzAttributes & 8) == 8 ) break zzForAction;
          }

        }
      }

      // store back cached position
      zzMarkedPos = zzMarkedPosL;

      if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
        zzAtEOF = true;
            zzDoEOF();
        return null;
      }
      else {
        switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {
          case 1:
            { yypushback(1); yybegin(WAITING_KEY);
            }
          // fall through
          case 21: break;
          case 2:
            { return RnTypes.ERROR;
            }
          // fall through
          case 22: break;
          case 3:
            { yybegin(WAITING_ASSIGN_OR_CHILD_OR_END); return RnTypes.KEY;
            }
          // fall through
          case 23: break;
          case 4:
            { yypushback(1); yybegin(WAITING_END_LINE); return RnTypes.END;
            }
          // fall through
          case 24: break;
          case 5:
            { yybegin(WAITING_VALUE); return RnTypes.ASSIGN;
            }
          // fall through
          case 25: break;
          case 6:
            { yybegin(WAITING_VALUE); return RnTypes.BREAK;
            }
          // fall through
          case 26: break;
          case 7:
            { yybegin(WAITING_ASSIGN_OR_CHILD_OR_END); return RnTypes.VALUE;
            }
          // fall through
          case 27: break;
          case 8:
            { yybegin(CHILD_WAITING_ASSIGN_OR_CHILD_OR_END); return RnTypes.KEY;
            }
          // fall through
          case 28: break;
          case 9:
            { yybegin(YYINITIAL); return RnTypes.EMPTY_LINE;
            }
          // fall through
          case 29: break;
          case 10:
            { return waiting_child_end(CHILD_WAITING_CHILD_OR_END, WAITING_END_LINE);
            }
          // fall through
          case 30: break;
          case 11:
            { yybegin(CHILD_WAITING_VALUE); return RnTypes.ASSIGN;
            }
          // fall through
          case 31: break;
          case 12:
            { yybegin(CHILD_WAITING_VALUE_CHILD_OR_END); return RnTypes.VALUE;
            }
          // fall through
          case 32: break;
          case 13:
            { yybegin(CHILD_WAITING_VALUE); return RnTypes.BREAK;
            }
          // fall through
          case 33: break;
          case 14:
            { return waiting_child_end(__CHILD_WAITING_CHILD_OR_END, WAITING_END_LINE);
            }
          // fall through
          case 34: break;
          case 15:
            { return waiting_child_end(CHILD_WAITING_VALUE_CHILD_OR_END, WAITING_END_LINE);
            }
          // fall through
          case 35: break;
          case 16:
            { yybegin(YYINITIAL); return RnTypes.COMMENT;
            }
          // fall through
          case 36: break;
          case 17:
            { return waiting_child(CHILD_WAITING_KEY);
            }
          // fall through
          case 37: break;
          case 18:
            { yybegin(CHILD_WAITING_KEY); return RnTypes.COMMENT;
            }
          // fall through
          case 38: break;
          case 19:
            { return waiting_child_or_end(__CHILD_WAITING_VALUE_CHILD_OR_END, CHILD_WAITING_KEY);
            }
          // fall through
          case 39: break;
          case 20:
            { return waiting_child_or_end(CHILD_WAITING_VALUE_CHILD_OR_END, CHILD_WAITING_KEY);
            }
          // fall through
          case 40: break;
          default:
            zzScanError(ZZ_NO_MATCH);
          }
      }
    }
  }


}
