////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

/*
 * Written by Jeff Dyer
 * Copyright (c) 1998-2003 Mountain View Compiler Company
 * All rights reserved.
 */

package macromedia.asc.parser;
import java.util.concurrent.*;
import macromedia.asc.util.*;
import macromedia.asc.embedding.ErrorConstants;

import java.io.InputStream;
import static macromedia.asc.parser.Tokens.*;
import static macromedia.asc.parser.States.*;
import static macromedia.asc.parser.CharacterClasses.*;
import static macromedia.asc.embedding.avmplus.Features.*;

/**
 * Partitions input character stream into tokens.
 *
 * @author Jeff Dyer
 */
public final class Scanner implements ErrorConstants
{
    private static final boolean debug = false;
    private static int token_count = 0;

    private static final int slashdiv_context = 0x1;
    private static final int slashregexp_context = 0x2;

    /*
     * Scanner maintains a notion of a single current token 
     * (It used to keep an array of all pseudo-terminal tokens...)
     * This means that the notion of 'tokenClass' as opposed to tokenID (an index to the pseudo-terminal array)
     * can also go away.
     * We should also upgrade a Token to contain source seekpos,line,column information, so we can throw away the line table,
     * which would mean we also dont need to buffer source after scanning, since we could reread it if an error/warning/info
     * line print was requested.
     */
    
    private Token currentToken;
    private int currentTokenId;
    
    private IntList slash_context = new IntList();  // slashdiv_context or slashregexp_context
    private boolean isFirstTokenOnLine;
    private boolean save_comments;
    private Context ctx;
    public InputBuffer input;
   
    private static final ConcurrentHashMap<String,Integer> reservedWord;
    
    static {
    	reservedWord = new ConcurrentHashMap<String,Integer>(64);
    	reservedWord.put("as",AS_TOKEN); // ??? predicated on HAS_ASOPERATOR
    	reservedWord.put("break",BREAK_TOKEN);
    	reservedWord.put("case",CASE_TOKEN);
    	reservedWord.put("catch",CATCH_TOKEN);
    	reservedWord.put("class",CLASS_TOKEN);
    	reservedWord.put("const",CONST_TOKEN);
    	reservedWord.put("continue",CONTINUE_TOKEN);
    	reservedWord.put("default",DEFAULT_TOKEN);
    	reservedWord.put("delete",DELETE_TOKEN);
    	reservedWord.put("do",DO_TOKEN);
    	reservedWord.put("else",ELSE_TOKEN);
    	reservedWord.put("extends",EXTENDS_TOKEN);
    	reservedWord.put("false",FALSE_TOKEN);
    	reservedWord.put("finally",FINALLY_TOKEN);
    	reservedWord.put("for",FOR_TOKEN);
    	reservedWord.put("function",FUNCTION_TOKEN);
    	reservedWord.put("get",GET_TOKEN);
    	reservedWord.put("if",IF_TOKEN);
    	reservedWord.put("implements",IMPLEMENTS_TOKEN);
    	reservedWord.put("import",IMPORT_TOKEN);
    	reservedWord.put("in",IN_TOKEN);
    	reservedWord.put("include",INCLUDE_TOKEN);  
    	reservedWord.put("instanceof",INSTANCEOF_TOKEN);
    	reservedWord.put("interface",INTERFACE_TOKEN);
    	reservedWord.put("is",IS_TOKEN); //??? predicated on HAS_ISOPERATOR
    	reservedWord.put("namespace",NAMESPACE_TOKEN);
    	reservedWord.put("new",NEW_TOKEN);
    	reservedWord.put("null",NULL_TOKEN);
    	reservedWord.put("package",PACKAGE_TOKEN);
    	reservedWord.put("private",PRIVATE_TOKEN);
    	reservedWord.put("protected",PROTECTED_TOKEN);
    	reservedWord.put("public",PUBLIC_TOKEN);
    	reservedWord.put("return",RETURN_TOKEN);
    	reservedWord.put("set",SET_TOKEN);
    	reservedWord.put("super",SUPER_TOKEN);
    	reservedWord.put("switch",SWITCH_TOKEN);
    	reservedWord.put("this",THIS_TOKEN);
    	reservedWord.put("throw",THROW_TOKEN);
    	reservedWord.put("true",TRUE_TOKEN);
    	reservedWord.put("try",TRY_TOKEN);
    	reservedWord.put("typeof",TYPEOF_TOKEN);
    	reservedWord.put("use",USE_TOKEN);
    	reservedWord.put("var",VAR_TOKEN);
    	reservedWord.put("void",VOID_TOKEN);
    	reservedWord.put("while",WHILE_TOKEN);
    	reservedWord.put("with",WITH_TOKEN);
    }
    
    /*
     * Scanner constructors.
     */

    private void init(Context cx, boolean save_comments)
    {
        ctx = cx;
        state = start_state;
        level = 0;
        slash_context.add(slashregexp_context);
        states = new IntList();
        levels = new IntList();
        slashcontexts = new ObjectList<IntList>();
        this.save_comments = save_comments;
        
        currentToken = new Token(0,"");
        currentTokenId = 0;
    }

    
    public Scanner(Context cx, InputStream in, String encoding, String origin){this(cx,in,encoding,origin,true);}
    public Scanner(Context cx, InputStream in, String encoding, String origin, boolean save_comments)
    {
        init(cx,save_comments);
        this.input = new InputBuffer(in, encoding, origin);
        cx.input = this.input;
    }
    
    public Scanner(Context cx, String in, String origin){this(cx,in,origin,true);}
    public Scanner(Context cx, String in, String origin, boolean save_comments)
    {
        init(cx,save_comments);
        this.input = new InputBuffer(in, origin);
        cx.input = this.input;
    }
    
    /**
     * This contructor is used by Flex direct AST generation.  It
     * allows Flex to pass in a specialized InputBuffer.
     */
    
    public Scanner(Context cx, InputBuffer input)
    {
        init(cx,true);
        this.input = input;
        cx.input = input;
    }

    /**
     * nextchar() --just fetch the next char 
     */

    private char nextchar()
    {
        return (char) input.nextchar();
    }
  
    /*
     * retract() --
     * Causes one character of input to be 'put back' onto the
     * que. [Test whether this works for comments and white space.]
     */

    public void retract()
    {
        input.retract();
    }

    /**
     * @return +1 from current char pos in InputBuffer
     */
    
    private int pos()
    {
        return input.textPos();
    }
    
    /**
     * set mark position
     */
    private void mark()
    {
        input.textMark();
    }
    
    /*
     * Various helper methods for managing and testing the
     * scanning context for slashes.
     */

    public void enterSlashDivContext()
    {
        slash_context.add(slashdiv_context);
    }

    public void exitSlashDivContext()
    {
        slash_context.removeLast();
    }

    public void enterSlashRegExpContext()
    {
        slash_context.add(slashregexp_context);
    }

    public void exitSlashRegExpContext()
    {
        slash_context.removeLast();
    }

    private boolean isSlashDivContext()
    {
        return slash_context.last() == slashdiv_context;
    }

    /*
     * Make an instance of the specified token class using the lexeme string.
     * Return the index of the token which is its identifier.
     */

    private int makeTokenInstance(int token_class, String lexeme)
    {
        token_count++;
        currentToken.set(token_class, lexeme);
        currentTokenId = token_count;
        return token_count;
    }
    
    /*
     * Get the class of a token instance.
     */

    public int getCurrentTokenClass(int token_id)
    {
        
        // if the token id is negative, it is a token_class.
        if (token_id < 0)
        {
            return token_id;
        }
        return currentToken.getTokenClass();
    }

    /*
     * Get the text of the current token
     */

    public String getCurrentTokenText(int token_id)
    {
        // if the token id is negative, it is a token_class.
        if (token_id < 0)
        {
            return Token.getTokenClassName(token_id);
        }

        if (token_id != currentTokenId)
        {
            error("Scanner internal: token id does not match current token id");   
        }
        return currentToken.getTokenText();
    }

    /*
     * Get text of literal string token as well as info about whether it was single quoted or not
     */
    
    public String getCurrentStringTokenText( int token_id, boolean[] is_single_quoted )
    {
        // if the token id is negative, it is a token_class. ???if we know its a string, why are we doing this???
        if( token_id < 0 )
        {
            is_single_quoted[0] = false;
            return Token.getTokenClassName(token_id);
        }

        // otherwise, get tokenSourceText (which includes string delimiters)
        String fulltext = currentToken.getTokenSource();
        is_single_quoted[0] = (fulltext.charAt(0) == '\'' ? true : false);
        String enclosedText = fulltext.substring(1, fulltext.length() - 1);
        
        return enclosedText;
    }

    /*
     * Record an error.
     */

    private void error(int kind, String arg)
    {
        StringBuilder out = new StringBuilder();

        String origin = this.input.origin;
        
        int errPos = input.positionOfMark();    // note use of source adjusted position
        int ln  = input.getLnNum(errPos);
        int col = input.getColPos(errPos);

        String msg = (ContextStatics.useVerboseErrors ? "[Compiler] Error #" + kind + ": " : "") + ctx.errorString(kind);
        
        if(debug) 
        {
            msg = "[Scanner] " + msg;
        }
        
        int nextLoc = Context.replaceStringArg(out, msg, 0, arg);
        if (nextLoc != -1) // append msg remainder after replacement point, if any
            out.append(msg.substring(nextLoc, msg.length()));

        ctx.localizedError(origin,ln,col,out.toString(),input.getLineText(errPos), kind);
        skiperror(kind);
    }

    private void error(String msg)
    {
        ctx.internalError(msg);
        error(kError_Lexical_General, msg);
    }

    private void error(int kind)
    {
        error(kind, "");
    }

    /*
     * skip ahead after an error is detected. this simply goes until the next
     * whitespace or end of input.
     */

    private void skiperror()
    {
        skiperror(kError_Lexical_General);
    }

    private void skiperror(int kind)
    {
        //Debugger::trace("skipping error\n");
        switch (kind)
        {
            case kError_Lexical_General:
                return;

            case kError_Lexical_LineTerminatorInSingleQuotedStringLiteral:
            case kError_Lexical_LineTerminatorInDoubleQuotedStringLiteral:
                while (true)
                {
                    char nc = nextchar();
                    if (nc == '\'' || nc == 0)
                    {
                        return;
                    }
                }

            case kError_Lexical_SyntaxError:
            default:
                while (true)
                {
                    char nc = nextchar();
                    if (nc == ';' || nc == '\n' || nc == '\r' || nc == 0)
                    {
                        return;
                    }
                }
        }
    }

    /*
     *
     *
     */

    public boolean followsLineTerminator()
    {
        if (debug)
        {
            System.out.println("isFirstTokenOnLine = " + isFirstTokenOnLine);
        }
        return isFirstTokenOnLine;
    }

    /*
     *
     *
     */

    public int state;
    public int level;

    public IntList states;
    public IntList levels;
    public ObjectList<IntList> slashcontexts;

    public void pushState()
    {
        states.add(state);
        levels.add(level);
        IntList temp = new IntList(slash_context);
        slashcontexts.add(temp);
        state = start_state;
        level = 0;
        slash_context.clear();
        enterSlashRegExpContext();
    }

    public void popState()
    {
        exitSlashRegExpContext();  // only necessary to do the following assert
        if (slash_context.size() != 0)
        {
            assert(false); // throw "internal error";
        }
        state = states.removeLast();
        level = levels.removeLast();
        slash_context = slashcontexts.removeLast();
    }

    private StringBuilder getDocTextBuffer(String doctagname)
    {
        StringBuilder doctextbuf = new StringBuilder();
        doctextbuf.append("<").append(doctagname).append("><![CDATA[");
        return doctextbuf;
    }

    public void clearUnusedBuffers() {
        input.clearUnusedBuffers();
        input = null;
    } 
    
    /*
     * 
     * 
     */

    public int nexttoken(boolean resetState)
    {
        String doctagname = "description";
        StringBuilder doctextbuf = null;
        int startofxml = pos();
        StringBuilder blockcommentbuf = null;
        char regexp_flags =0; // used to track option flags encountered in a regexp expression.  Initialized in regexp_state
        boolean maybe_reserved = false;
        char c = 0;

        if (resetState)
        {
            isFirstTokenOnLine = false;
        }
        
        while (true)
        {
            if (debug)
            {
                System.out.println("state = " + state + ", next = " + pos());
            }

            switch (state)
            {
                case start_state:
                    {
                        c = nextchar();
                        mark();
                        
                        switch (c)
                        {
                        case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g': case 'h': case 'i': case 'j':
                        case 'k': case 'l': case 'm': case 'n': case 'o': case 'p': case 'q': case 'r': case 's': case 't':
                        case 'u': case 'v': case 'w': case 'x': case 'y': case 'z': 
                            maybe_reserved = true;
                        case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': case 'G': case 'H': case 'I': case 'J':
                        case 'K': case 'L': case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R': case 'S': case 'T':
                        case 'U': case 'V': case 'W': case 'X': case 'Y': case 'Z': 
                        case '_': case '$':
                            state = A_state;
                            continue;
                            
                        case 0xffef: // could not have worked...case 0xffffffef: // ??? not in Character type range ???
                            if (nextchar()==0xffffffbb &&
                                nextchar()==0xffffffbf)
                            {
                                // ISSUE: set encoding scheme to utf-8, and implement support for utf8
                                state = start_state;
                            }
                            else 
                            {
                                state = error_state;
                            }
                            continue;
                                
                            case '@':
                                return AMPERSAND_TOKEN;
                              
                            case '\'':
                            case '\"':
                            {
                                char startquote = (char) c;
                                boolean needs_escape = false;

                                while ( (c=nextchar()) != startquote )
                                {         
                                    if ( c == '\\' )
                                    {
                                        needs_escape = true;
                                        c = nextchar();

                                        // special case: escaped eol strips crlf or lf
                                         
                                        if ( c  == '\r' )
                                            c = nextchar();
                                        if ( c == '\n' )
                                            continue;
                                    }
                                    else if ( c == '\r' || c == '\n' )
                                    {
                                        if ( startquote == '\'' )
                                            error(kError_Lexical_LineTerminatorInSingleQuotedStringLiteral);
                                        else
                                            error(kError_Lexical_LineTerminatorInDoubleQuotedStringLiteral);
                                        break;
                                    }
                                    else if ( c == 0 )
                                    {
                                        error(kError_Lexical_EndOfStreamInStringLiteral);
                                        return EOS_TOKEN;
                                    }
                                }
                                return makeTokenInstance(STRINGLITERAL_TOKEN, input.copyReplaceStringEscapes(needs_escape));
                            }

                            case '-':   // tokens: -- -= -
                                switch (nextchar())
                                {
                                case '-':
                                    return MINUSMINUS_TOKEN;
                                case '=':
                                    return MINUSASSIGN_TOKEN;
                                default:
                                    retract();
                                return MINUS_TOKEN;
                                }

                            case '!':   // tokens: ! != !===
                                if (nextchar()=='=')
                                {
                                    if (nextchar()=='=')
                                        return STRICTNOTEQUALS_TOKEN;
                                    retract();
                                    return NOTEQUALS_TOKEN;
                                }   
                                retract();
                                return NOT_TOKEN;
                                
                            case '%':   // tokens: % %=
                                switch (nextchar())
                                {
                                case '=':
                                    return MODULUSASSIGN_TOKEN;
                                default:
                                    retract();
                                return MODULUS_TOKEN;
                                }

                            case '&':   // tokens: & &= && &&=
                                c = nextchar();
                                if (c=='=')
                                    return BITWISEANDASSIGN_TOKEN;
                                if (c=='&')
                                {
                                    if (nextchar()=='=')
                                        return LOGICALANDASSIGN_TOKEN;
                                    retract();
                                    return LOGICALAND_TOKEN;
                                }
                                retract();
                                return BITWISEAND_TOKEN;
                        
                            case '#':   // # is short for use
                                if (HAS_HASHPRAGMAS)
                                {
                                    return USE_TOKEN;
                                }
                                state = error_state;
                                continue;
                                
                            case '(':
                                return LEFTPAREN_TOKEN;
                                
                            case ')':
                                return RIGHTPAREN_TOKEN;
                                
                            case '*':   // tokens: *=  *
                                if (nextchar()=='=')
                                    return MULTASSIGN_TOKEN;
                                retract();
                                return MULT_TOKEN;

                            case ',':
                                return COMMA_TOKEN;
                                
                            case '.':
                                state = dot_state;
                                continue;
                                
                            case '/':
                                state = slash_state;
                                continue;

                            case ':':   // tokens: : ::
                                if (nextchar()==':')
                                {
                                    return DOUBLECOLON_TOKEN;
                                }
                                retract();
                                return COLON_TOKEN;
                             
                            case ';':
                                return SEMICOLON_TOKEN;
                                
                            case '?':
                                return QUESTIONMARK_TOKEN;
                                
                            case '[':
                                return LEFTBRACKET_TOKEN;
                                
                            case ']':
                                return RIGHTBRACKET_TOKEN;
                                
                            case '^':   // tokens: ^=  ^
                                if (nextchar()=='=')
                                        return BITWISEXORASSIGN_TOKEN;
                                retract();
                                return BITWISEXOR_TOKEN;
                                
                            case '{':
                                return LEFTBRACE_TOKEN;
                                
                            case '|':   // tokens: | |= || ||=
                                c = nextchar();
                                if (c=='=')
                                    return BITWISEORASSIGN_TOKEN;
                                if (c=='|')
                                {
                                    if (nextchar()=='=')
                                        return LOGICALORASSIGN_TOKEN;
                                    retract();
                                    return LOGICALOR_TOKEN;
                                }
                                retract();
                                return BITWISEOR_TOKEN;
                                
                            case '}':
                                return RIGHTBRACE_TOKEN;
                                
                            case '~':
                                return BITWISENOT_TOKEN;
                                
                            case '+':   // tokens: ++ += +
                                c = nextchar();
                                if (c=='+')
                                    return PLUSPLUS_TOKEN;
                                if (c=='=')
                                    return PLUSASSIGN_TOKEN;
                                retract();
                                return PLUS_TOKEN;
                                
                            case '<':
                                if( isSlashDivContext() )
                                {
                                    switch (nextchar())
                                    {
                                    case '<':   // tokens: << <<-                                            
                                        if (nextchar()=='=')
                                            return LEFTSHIFTASSIGN_TOKEN;
                                        retract();
                                        return LEFTSHIFT_TOKEN;

                                    case '=':
                                        return LESSTHANOREQUALS_TOKEN;

                                    case '/':  
                                        return XMLTAGSTARTEND_TOKEN;
                                    case '!': 
                                        state = xmlcommentorcdatastart_state; 
                                        continue;
                                    case '?': 
                                        state = xmlpi_state; 
                                        continue;                            
                                    }
                                }
                                else
                                {
                                    switch ( nextchar() )             
                                    {
                                    case '/':  
                                        return XMLTAGSTARTEND_TOKEN;
                                    case '!': 
                                        state = xmlcommentorcdatastart_state; 
                                        continue;
                                    case '?': 
                                        state = xmlpi_state; 
                                        continue;
                                    }                          
                                }
                                retract();  
                                return LESSTHAN_TOKEN;

                            case '=':   // tokens: === == =
                                if (nextchar()=='=')
                                {
                                    if (nextchar()=='=')
                                        return STRICTEQUALS_TOKEN;
                                    retract();
                                    return EQUALS_TOKEN;
                                }
                                retract();
                                return ASSIGN_TOKEN;
                                
                            case '>':   // tokens: > >= >> >>= >>> >>>=
                                state = start_state;
                                if( isSlashDivContext() )       
                                {
                                    switch ( nextchar() )          
                                    {
                                    case '>':
                                        switch (nextchar())
                                        {
                                        case '>':
                                            if (nextchar()=='=')
                                                return UNSIGNEDRIGHTSHIFTASSIGN_TOKEN;
                                            retract();
                                            return UNSIGNEDRIGHTSHIFT_TOKEN;
                                        case '=':
                                            return RIGHTSHIFTASSIGN_TOKEN;
                                        default:
                                            retract();
                                        return RIGHTSHIFT_TOKEN;
                                        }

                                    case '=': 
                                        return GREATERTHANOREQUALS_TOKEN;

                                    default:  
                                        retract();  
                                    }
                                }     
                                return GREATERTHAN_TOKEN;            
                                
                            case '0':
                                state = zero_state;
                                continue;
                                
                            case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
                                state = decimalinteger_state;
                                continue;
                                
                            case ' ': // ascii range white space
                            case '\t':
                            case 0x000b:
                            case 0x000c:
                            case 0x0085:    
                            case 0x00a0:
                                continue;

                            case '\n': // ascii line terminators.
                            case '\r':
                                isFirstTokenOnLine = true;
                                continue;
                                
                            case 0:
                                return EOS_TOKEN;
                                
                            default:
                                switch (input.nextcharClass((char)c,true))
                                {
                                case Lu: case Ll: case Lt: case Lm: case Lo: case Nl:
                                    maybe_reserved = false;
                                    state = A_state;
                                    continue;

                                case Zs:// unicode whitespace and control-characters
                                case Cc: 
                                case Cf:
                                    continue;

                                case Zl:// unicode line terminators 
                                case Zp:
                                    isFirstTokenOnLine = true;
                                    continue;

                                default:
                                    state = error_state;
                                continue;
                                }
                        }
                    }

                /*
                 * prefix: <letter>
                 */

                case A_state:
                {
                    boolean needs_escape = c == '\\';   // ??? really should only be true if the word started with a backslash
                
                    while ( true ){
                        c = nextchar();
                        if ( c >= 'a' && c <= 'z' )
                        {
                            continue;
                        }
                        if ( (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '$' || c == '_' ){
                            maybe_reserved = false;
                            continue;
                        }
                        if ( c <= 0x7f ) // in ascii range & mostly not a valid char
                        {
                            if ( c == '\\' )
                            {
                                needs_escape = true; // close enough, we just want to minimize rescans for unicode escapes
                            }
                            else {
                                retract();
                                break;
                            }
                        }

                        // else outside ascii range (or an escape sequence )
                        
                        switch (input.nextcharClass(c,false))
                        {
                        case Lu: case Ll: case Lt: case Lm: case Lo: case Nl: case Mn: case Mc: case Nd: case Pc:
                            maybe_reserved = false;
                            input.nextcharClass(c,true); // advance input cursor
                            continue;
                        }
                        
                        retract();
                        break;
                    }
                    
                    state = start_state;   
                    String s = input.copyReplaceUnicodeEscapes(needs_escape); 
                    if ( maybe_reserved )
                    {
                        Integer i = reservedWord.get(s); 
                        if ( i != null )
                            return (int) i;
                    }
                    return makeTokenInstance(IDENTIFIER_TOKEN,s);
                }
                
                /*
                 * prefix: 0
                 * accepts: 0x... | 0X... | 01... | 0... | 0
                 */

                case zero_state:
                    switch (nextchar())
                    {
                    case 'x':
                    case 'X':
                        switch(nextchar())
                        {
                        case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9': 
                        case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'A': case 'B': case 'C': case 'D': 
                        case 'E': case 'F':
                            state = hexinteger_state;
                            break;
                        default:
                            state = start_state;
                        error(kError_Lexical_General); 
                        }
                        continue;

                    case '.':
                        state = decimal_state;
                        continue;
                        
                    case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9': 
                        state = decimalinteger_state;
                        continue;
                    case 'E':
                    case 'e':
                        state = exponentstart_state;
                        continue;
                    case 'd':
                    case 'm':
                    case 'i':
                    case 'u':
                        if (!ctx.statics.es4_numerics)
                            retract();
                        state = start_state;
                        return makeTokenInstance(NUMBERLITERAL_TOKEN, input.copy());
                    default:
                        retract();
                    state = start_state;
                    return makeTokenInstance(NUMBERLITERAL_TOKEN, input.copy());
                    }

                    /*
                     * prefix: 0x<hex digits>
                     * accepts: 0x123f
                     */

                case hexinteger_state:
                    switch (nextchar())
                    {
                    case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9': 
                    case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'A': case 'B': case 'C': case 'D': 
                    case 'E': case 'F':
                        state = hexinteger_state;
                        continue;
                    case 'u':
                    case 'i':
                        if (!ctx.statics.es4_numerics)
                            retract();
                        state = start_state; 
                        return makeTokenInstance( NUMBERLITERAL_TOKEN, input.copy() );
                    default:  
                        retract();
                    state = start_state; 
                    return makeTokenInstance( NUMBERLITERAL_TOKEN, input.copy() );
                    }

                    /*
                     * prefix: .
                     * accepts: .123 | .
                     */

                case dot_state:
                    switch (nextchar())
                    {
                    case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9': 
                        state = decimal_state;
                        continue;

                    case '.':
                        state = start_state;
                        if (nextchar()=='.')
                            return TRIPLEDOT_TOKEN;
                        retract();
                        return DOUBLEDOT_TOKEN;

                    case '<':
                        state = start_state;
                        return DOTLESSTHAN_TOKEN;

                    default:
                        retract();
                    state = start_state;
                    return DOT_TOKEN;
                    }

                    /*
                     * prefix: N
                     * accepts: 0.123 | 1.23 | 123 | 1e23 | 1e-23
                     */

                case decimalinteger_state:
                    switch (nextchar())
                    {
                    case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9': 
                        state = decimalinteger_state;
                        continue;
                    case '.':
                        state = decimal_state;
                        continue;
                    case 'd':
                    case 'm':
                    case 'u':
                    case 'i':
                        if (!ctx.statics.es4_numerics)
                            retract();
                        state = start_state;
                        return makeTokenInstance(NUMBERLITERAL_TOKEN, input.copy());
                    case 'E':
                    case 'e':
                        state = exponentstart_state;
                        continue;
                    default:
                        retract();
                    state = start_state;
                    return makeTokenInstance(NUMBERLITERAL_TOKEN, input.copy());
                    }

                    /*
                     * prefix: N.
                     * accepts: 0.1 | 1e23 | 1e-23
                     */

                case decimal_state:
                    switch (nextchar())
                    {
                    case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9': 
                        state = decimal_state;
                        continue;
                    case 'd':
                    case 'm':
                        if (!ctx.statics.es4_numerics)
                            retract();
                        state = start_state;
                        return makeTokenInstance(NUMBERLITERAL_TOKEN, input.copy());
                    case 'E':
                    case 'e':
                        state = exponentstart_state;
                        continue;
                    default:
                        retract();
                    state = start_state;
                    return makeTokenInstance(NUMBERLITERAL_TOKEN, input.copy());
                    }

                    /*
                     * prefix: ..e
                     * accepts: ..eN | ..e+N | ..e-N
                     */

                case exponentstart_state:
                    switch (nextchar())
                    {
                    case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9': 
                    case '+':
                    case '-':
                        state = exponent_state;
                        continue;
                    default:
                        error(kError_Lexical_General);
                    state = start_state;
                    continue;
                    // Issue: needs specific error here.
                    }

                    /*
                     * prefix: ..e
                     * accepts: ..eN | ..e+N | ..e-N
                     */

                case exponent_state:
                    switch (nextchar())
                    {
                    case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9': 
                        state = exponent_state;
                        continue;
                    case 'd':
                    case 'm':
                        if (!ctx.statics.es4_numerics)
                            retract();
                        state = start_state;
                        return makeTokenInstance(NUMBERLITERAL_TOKEN, input.copy());
                    default:
                        retract();
                    state = start_state;
                    return makeTokenInstance(NUMBERLITERAL_TOKEN, input.copy());
                    }

                    /*
                     * prefix: /
                     */

                case slash_state:
                {
                    c = nextchar();
              
                    switch (c)
                    {   
                    case '/': // line comment
                        state = start_state;
                        line_comment: 
                            while ( (c=nextchar()) != 0)
                            {
                                if ( c == '\r' || c == '\n' )
                                {
                                    isFirstTokenOnLine = true;
                                    if (save_comments == false)
                                    {
                                        break line_comment;
                                    }
                                    retract(); // don't include newline in line comment. (Sec 7.3)
                                    return makeTokenInstance( SLASHSLASHCOMMENT_TOKEN, input.copyReplaceUnicodeEscapes() );
                                }
                            }
                        continue;

                    case '*':
                        if (save_comments == false)
                        {
                            block_comment:
                                while ( (c=nextchar()) != 0)
                                {
                                    if ( c == '\r' || c == '\n' )
                                        isFirstTokenOnLine = true;

                                    if (c == '*')
                                    {
                                        c = nextchar();
                                        if (c == '/' )
                                        {
                                            break block_comment;
                                        }
                                        retract();
                                    }   
                                }
                        state = start_state;
                        }
                        else {
                            if (blockcommentbuf == null) 
                                blockcommentbuf = new StringBuilder();
                            blockcommentbuf.append("/*");
                            state = blockcommentstart_state;
                        }
                        continue;

                    default:
                        if (isSlashDivContext())
                        {
                            /*
                             * tokens: /> /= /
                             */

                            state = start_state; 
                            if (c=='>')
                                return XMLTAGENDEND_TOKEN;
                            if (c=='=')
                                return DIVASSIGN_TOKEN;
                            retract();
                            return DIV_TOKEN;
                        }
                    state = slashregexp_state;
                    retract(); // since we didn't use the current character for this decision.
                    continue;
                    }
                }

                /*
                     * tokens: /<regexpbody>/<regexpflags>
                     */

                case slashregexp_state:
                    switch (nextchar())
                    {
                    case '\\': 
                        nextchar(); 
                        continue;
                    case '/':
                        regexp_flags = 0;
                        state = regexp_state;
                        continue;
                    case 0:
                    case '\n':
                    case '\r':
                        error(kError_Lexical_General);
                        state = start_state;
                        continue;
                    default:
                        state = slashregexp_state;
                    continue;
                    }

                /*
                * tokens: g | i | m | s | x  .  Note that s and x are custom extentions to match perl's functionality
                *   Also note we handle this via an array of boolean flags intead of state change logic.
                *   (5,1) + (5,2) + (5,3) + (5,4) + (5,5) is just too many states to handle this via state logic
                */

                case regexp_state:
                    c = nextchar();
                    switch ( c )
                    {
                    case 'g': 
                        if ((regexp_flags & 0x01) == 0)
                        {
                            regexp_flags |= 0x01;
                            continue;
                        }
                        error(kError_Lexical_General); 
                        state = start_state; 
                        continue;

                    case 'i': 
                        if ((regexp_flags & 0x02) == 0)
                        {
                            regexp_flags |= 0x02;
                            continue;
                        }
                        error(kError_Lexical_General); 
                        state = start_state; 
                        continue;

                    case 'm': 
                        if ((regexp_flags & 0x04) == 0)
                        {
                            regexp_flags |= 0x04;
                            continue;
                        }
                        error(kError_Lexical_General); 
                        state = start_state; 
                        continue;

                    case 's':
                        if ((regexp_flags & 0x08) == 0)
                        {
                            regexp_flags |= 0x08;
                            continue;
                        }
                        error(kError_Lexical_General); 
                        state = start_state; 
                        continue;

                    case 'x':
                        if ((regexp_flags & 0x10) == 0)
                        {
                            regexp_flags |= 0x10;
                            continue;
                        }
                        error(kError_Lexical_General); 
                        state = start_state; 
                        continue;

                    default: 
                        if (Character.isJavaIdentifierPart(c))
                        {
                            error(kError_Lexical_General); 
                            state = start_state; 
                            continue; 
                        }
                    retract(); 
                    state = start_state; 
                    return makeTokenInstance( REGEXPLITERAL_TOKEN, input.copyReplaceUnicodeEscapes());
                    }

                /*
                 * prefix: <!
                 */
                    
                case xmlcommentorcdatastart_state:
                    switch ( nextchar() )        
                    {
                    case '[':  
                        if (nextchar()=='C' &&
                            nextchar()=='D' &&
                            nextchar()=='A' &&
                            nextchar()=='T' &&
                            nextchar()=='A' &&
                            nextchar()=='[')
                        {
                            state = xmlcdata_state; 
                            continue;
                        }
                        break; // error

                    case '-':  
                        if (nextchar()=='-')
                        {
                            state = xmlcomment_state; 
                            continue;
                        }
                    }    
                    error(kError_Lexical_General); 
                    state = start_state; 
                    continue;

                case xmlcdata_state:
                    switch ( nextchar() )         
                    {
                    case ']':
                        if (nextchar()==']' && nextchar()=='>')
                        {
                            state = start_state;
                            return makeTokenInstance(XMLMARKUP_TOKEN,input.substringReplaceUnicodeEscapes(startofxml,pos()));
                        }
                        continue;

                    case 0:   
                        error(kError_Lexical_General); 
                        state = start_state;
                    }
                    continue;

                case xmlcomment_state:
                    while ( (c=nextchar()) != '-' && c != 0 )
                        ;
                    
                    if (c=='-' && nextchar() != '-')
                    {
                        continue;
                    }
                    
                    // got -- if next is > ok else error
                    
                    if ( nextchar()=='>')
                    {
                        state = start_state;
                        return makeTokenInstance(XMLMARKUP_TOKEN,input.substringReplaceUnicodeEscapes(startofxml,pos())); 
                    }
                    
                    error(kError_Lexical_General); 
                    state = start_state;
                    continue;

                case xmlpi_state:
                    while ( (c=nextchar()) != '?' && c != 0 )
                        ;
                    
                    if (c=='?' && nextchar() == '>')
                    {
                        state = start_state;
                        return makeTokenInstance(XMLMARKUP_TOKEN,input.substringReplaceUnicodeEscapes(startofxml,pos()));  
                    }

                    if (nextchar()==0)
                    {
                        error(kError_Lexical_General); 
                        state = start_state;   
                    }
                    continue;

                case xmltext_state:
                { 
                    switch(nextchar())
                    {
                    case '<': case '{':  
                    {
                        retract();
                        String xmltext = input.substringReplaceUnicodeEscapes(startofxml,pos());
                        if( xmltext != null )
                        {
                            state = start_state;
                            return makeTokenInstance(XMLTEXT_TOKEN,xmltext);
                        }
                        else  // if there is no leading text, then just return punctuation token to avoid empty text tokens
                        {
                            switch(nextchar()) 
                            {
                            case '<': 
                                switch( nextchar() )
                                {
                                case '/': state = start_state; return XMLTAGSTARTEND_TOKEN;
                                case '!': state = xmlcommentorcdatastart_state; continue;
                                case '?': state = xmlpi_state; continue;
                                default: retract(); state = start_state; return LESSTHAN_TOKEN;
                                }
                            case '{': 
                                state = start_state; 
                                return LEFTBRACE_TOKEN;
                            }
                        }
                    }
                    case 0:   
                        state = start_state; 
                        return EOS_TOKEN;
                    }
                    continue;
                }

                case xmlliteral_state:
                    switch (nextchar())
                    {
                    case '{':  // return XMLPART_TOKEN
                        return makeTokenInstance(XMLPART_TOKEN, input.substringReplaceUnicodeEscapes(startofxml, pos()-1));

                    case '<':
                        switch (nextchar())
                        {
                        case '/':
                            --level;
                            nextchar();
                            mark();
                            retract();
                            state = endxmlname_state;
                            continue;

                        default:
                            ++level;
                        state = xmlliteral_state;
                        continue;
                        }

                    case '/':
                        if (nextchar()=='>')
                        {
                            --level;
                            if (level == 0)
                            {
                                state = start_state;
                                return makeTokenInstance(XMLLITERAL_TOKEN, input.substringReplaceUnicodeEscapes(startofxml, pos()+1)); 
                            }
                        }
                        continue;

                    case 0:
                        retract();
                        error(kError_Lexical_NoMatchingTag);
                        state = start_state;
                        continue;

                    default:
                        continue;
                    }

                case endxmlname_state:
                    c = nextchar();
                    if (Character.isJavaIdentifierPart(c)||c==':')
                    {
                        continue;
                    }
                    
                    switch(c)
                    {
                    case '{':  // return XMLPART_TOKEN
                    {
                        String xmltext = input.substringReplaceUnicodeEscapes(startofxml, pos()-1);
                        return makeTokenInstance(XMLPART_TOKEN, xmltext);
                    }
                    case '>':
                        retract();
                        nextchar();
                        if (level == 0)
                        {
                            String xmltext = input.substringReplaceUnicodeEscapes(startofxml, pos()+1);
                            state = start_state;
                            return makeTokenInstance(XMLLITERAL_TOKEN, xmltext);
                        }
                        state = xmlliteral_state;
                        continue;

                    default:
                        state = xmlliteral_state;
                        continue;
                    }

               /*
                * prefix: /*
                */

                case blockcommentstart_state:
                {
                    c = nextchar();
                    blockcommentbuf.append(c);
                    switch ( c )
                    {
                    case '*':
                        if ( nextchar() == '/' ){
                            state = start_state;
                            return makeTokenInstance( BLOCKCOMMENT_TOKEN, new String());
                        }
                        retract(); 
                        state = doccomment_state; 
                        continue;
                        
                    case 0:    
                        error(kError_BlockCommentNotTerminated); 
                        state = start_state; 
                        continue;
                        
                    case '\n': 
                    case '\r':
                        isFirstTokenOnLine = true; 
                    default:
                        state = blockcomment_state;
                        continue;
                    }
                }

                /*
                 * prefix: /**
                 */

                case doccomment_state:
                {
                    c = nextchar();
                    blockcommentbuf.append(c);
                    switch ( c )
                    {
                    case '*':  
                        state = doccommentstar_state; 
                        continue;
                    
                    case '@':
                        if (doctextbuf == null) 
                            doctextbuf = getDocTextBuffer(doctagname);
                        if( doctagname.length() > 0 ) 
                        { 
                            doctextbuf.append("]]></").append(doctagname).append(">"); 
                        }
                        doctagname = "";
                        state = doccommenttag_state; 
                        continue;
                        
                    case '\r': 
                    case '\n': 
                        isFirstTokenOnLine = true;
                        if (doctextbuf == null) 
                            doctextbuf = getDocTextBuffer(doctagname);
                        doctextbuf.append('\n');
                        state = doccomment_state; 
                        continue;
                    
                    case 0:    
                        error(kError_BlockCommentNotTerminated); 
                        state = start_state; 
                        continue;
                        
                    default:
                        if (doctextbuf == null) 
                            doctextbuf = getDocTextBuffer(doctagname);
                        doctextbuf.append((char)(c)); 
                        state = doccomment_state; 
                        continue;
                    }
                }

                case doccommentstar_state:
                {
                    c = nextchar();
                    blockcommentbuf.append(c);
                    switch ( c )                    
                    {
                    case '/':
                    {
                        if (doctextbuf == null) 
                            doctextbuf = getDocTextBuffer(doctagname);
                        if( doctagname.length() > 0 ) 
                        { 
                            doctextbuf.append("]]></").append(doctagname).append(">"); 
                        }
                        String doctext = doctextbuf.toString(); // ??? does this needs escape conversion ???
                        state = start_state; 
                        return makeTokenInstance(DOCCOMMENT_TOKEN,doctext);
                    }

                    case '*':  
                        state = doccommentstar_state; 
                        continue;
                    
                    case 0:    
                        error(kError_BlockCommentNotTerminated); 
                        state = start_state; 
                        continue;
                    
                    default:   
                        state = doccomment_state; 
                        continue;
                    // if not a slash, then keep looking for an end comment.
                    }
                }

                /*
                * prefix: @
                */

                case doccommenttag_state:
                {
                    c = nextchar();
                    switch ( c )
                    {
                    case '*':  
                        state = doccommentstar_state; 
                        continue;

                    case ' ': case '\t': case '\r': case '\n': 
                    {
                        if (doctextbuf == null) 
                            doctextbuf = getDocTextBuffer(doctagname);

                        // skip extra whitespace --fixes bug on tag text parsing 
                        // --but really, the problem is not here, it's in whatever reads asdoc output..
                        // --So if that gets fixed, feel free to delete the following.

                        while ( (c=nextchar()) == ' ' || c == '\t' )
                            ;
                        retract();

                        if( doctagname.length() > 0 ) 
                        { 
                            doctextbuf.append("\n<").append(doctagname).append("><![CDATA["); 
                        }
                        state = doccomment_state; 
                        continue;
                    }

                    case 0:    
                        error(kError_BlockCommentNotTerminated); 
                        state = start_state; 
                        continue;

                    default:   
                        doctagname += (char)(c); 
                    continue;
                    }
                }

                /*
                 * prefix: /**
                 */

                case doccommentvalue_state:
                    switch ( nextchar() )
                    {
                    case '*':  
                        state = doccommentstar_state; 
                        continue;

                    case '@':  
                        state = doccommenttag_state; 
                        continue;

                    case 0:    
                        error(kError_BlockCommentNotTerminated); 
                        state = start_state; 
                        continue;

                    default:   
                        state = doccomment_state; 
                    continue;
                    }

                /*
                * prefix: /*
                */

                case blockcomment_state:
                {
                    c = nextchar();
                    blockcommentbuf.append(c);
                    switch ( c )                    
                    {
                    case '*':  state = blockcommentstar_state; continue;
                    case '\r': case '\n': isFirstTokenOnLine = true; 
                    state = blockcomment_state; continue;
                    case 0:    error(kError_BlockCommentNotTerminated); state = start_state; continue;
                    default:   state = blockcomment_state; continue;
                    }
                }

                case blockcommentstar_state:
                {
                    c = nextchar();
                    blockcommentbuf.append(c);
                    switch ( c )
                    {
                    case '/':  
                    {
                        state = start_state;
                        String blocktext = blockcommentbuf.toString(); // ??? needs escape conversion
                        return makeTokenInstance( BLOCKCOMMENT_TOKEN, blocktext );
                    }
                    case '*':  state = blockcommentstar_state; continue;
                    case 0:    error(kError_BlockCommentNotTerminated); state = start_state; continue;
                    default:   state = blockcomment_state; continue;
                    // if not a slash, then keep looking for an end comment.
                    }
                }

                /*
                 * skip error
                 */

                case error_state:
                    error(kError_Lexical_General);
                    skiperror();
                    state = start_state;
                    continue;

                default:
                    error("invalid scanner state");
                    state = start_state;
                    return EOS_TOKEN;
            }
        }
    }
}
