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

    private static final int slashdiv_context = 0x1;
    private static final int slashregexp_context = 0x2;

    private ObjectList<Token> tokens;   // vector of token instances.
    private IntList slash_context = new IntList();  // slashdiv_context or slashregexp_context
    private boolean isFirstTokenOnLine;
    //private Writer err;
    private Context ctx;

    public InputBuffer input;

    /*
     * Scanner constructors.
     */

    private void init(Context cx)
    {
        ctx = cx;
        tokens = new ObjectList<Token>(200);
        state = start_state;
        level = 0;
        slash_context.add(slashregexp_context);
        states = new IntList();
        levels = new IntList();
        slashcontexts = new ObjectList<IntList>();
    }

    public Scanner(Context cx, InputStream in, String encoding, String origin)
    {
        init(cx);

        this.input = new InputBuffer(in, encoding, origin);
        cx.input = this.input;
    }

    public Scanner(Context cx, String in, String origin)
    {
        init(cx);

        this.input = new InputBuffer(in, origin);
        cx.input = this.input;
    }

    /**
     * This contructor is used by Flex direct AST generation.  It
     * allows Flex to pass in a specialized InputBuffer.
     */
    public Scanner(Context cx, InputBuffer input)
    {
        init(cx);
        this.input = input;
        cx.input = input;
    }

    /*
     * nextchar() --
     * Get the next character that has lexical significance. If get_unnormalized == false,
     * WhiteSpace, LineTerminators are normalized to various combinations
     * of ' ' and '\n'. Unicode format control character are not significant
     * in the lexical grammar and so are removed from the character stream.
     * If the end of the input stream is reached, then 0 is returned.
     * 
     * TODO: Unicode format control characters may be significant to string literals, and
     *  thus should be included when get_unnormalized=true.  That requires supporting two
     *  seperate colPos indicies in InputBuffer, however.  For now, get_unnormalized only
     *  avoids the normalization of whitespace, allowing us to use the same colPos for both
     *  the normalized and unnormalized buffers. 
     */

    public char nextchar()
    {
        return (char) input.nextchar(false);
    }
    public char nextchar(boolean get_unnormalized)
    {
        return (char) input.nextchar(get_unnormalized);
    }


    public String lexeme()
    {
        return input.copy(); // Copies text since last mark.
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

    public boolean isSlashDivContext()
    {
        return slash_context.last() == slashdiv_context;
    }

    public boolean isSlashRegexpContext()
    {
        return slash_context.last() == slashregexp_context;
    }

    /*
     * makeTokenInstance() --
     * Make an instance of the specified token class using the lexeme string.
     * Return the index of the token which is its identifier.
     */

    public int makeTokenInstance(int token_class, String lexeme)
    {
        tokens.add(new Token(token_class, lexeme));
        return tokens.size() - 1; /* return the tokenid */
    }

    /*
     * getTokenClass() --
     * Get the class of a token instance.
     */

    public int getTokenClass(int token_id)
    {

        // if the token id is negative, it is a token_class.
        if (token_id < 0)
        {
            return token_id;
        }

        // otherwise, get instance data from the instance vector.
        return tokens.get(token_id).getTokenClass();
    }

    /*
     * getTokenText() --
     * Get the text of a token instance.
     *
     */

    public String getTokenText(int token_id)
    {

        // if the token id is negative, it is a token_class.
        if (token_id < 0)
        {
            return Token.getTokenClassName(token_id);
        }

        // otherwise, get instance data from the instance vector.
        return tokens.get(token_id).getTokenText();
    }

    /*
     * getStringTokenText
     * Get text of literal string token as well as info about whether it was single quoted or not
     *
     */
    String getStringTokenText( int token_id, boolean[] is_single_quoted )
    {
        // if the token id is negative, it is a token_class.
        if( token_id < 0 )
        {
            is_single_quoted[0] = false;
            return Token.getTokenClassName(token_id);
        }

        // otherwise, get tokenSourceText (which includes string delimiters)
        String fulltext = tokens.get( token_id ).getTokenSource();
        is_single_quoted[0] = (fulltext.charAt(0) == '\'' ? true : false);
        String enclosedText = fulltext.substring(1, fulltext.length() - 1);
        
        return enclosedText;
    }

    /*
     * getLinePointer() --
     * Generate a string that contains a carat character at
     * a specified position.
     */

    public String getLinePointer()
    {
        return InputBuffer.getLinePointer(0);
    }

    /*
     * Record an error.
     */

    public void error(int kind, String arg, int tokenid)
    {
        StringBuffer out = new StringBuffer();

        String origin = this.input.origin;
        int ln  = this.input.markLn + 1;
        int col = this.input.markCol;

        String msg = (ContextStatics.useVerboseErrors ? "[Compiler] Error #" + kind + ": " : "") + ctx.errorString(kind);
        
        if(debug) 
        {
            msg = "[Scanner] " + msg;
        }
        
        int nextLoc = Context.replaceStringArg(out, msg, 0, arg);
        if (nextLoc != -1) // append msg remainder after replacement point, if any
            out.append(msg.substring(nextLoc, msg.length()));

        ctx.localizedError(origin,ln,col,out.toString(),input.getLineText(input.positionOfMark()), kind);
        skiperror(kind);
    }

    public void error(String msg)
    {
        ctx.internalError(msg);
        error(kError_Lexical_General, msg, ERROR_TOKEN);
    }

    public void error(int kind)
    {
        error(kind, "", ERROR_TOKEN);
    }

    /*
     * skip ahead after an error is detected. this simply goes until the next
     * whitespace or end of input.
     */

    public void skiperror()
    {
        skiperror(kError_Lexical_General);
    }

    public void skiperror(int kind)
    {
        //Debugger::trace("skipping error\n");
        switch (kind)
        {
            case kError_Lexical_General:
                //while ( true )
                //{
                //    char nc = nextchar();
                //    //Debugger::trace("nc " + nc);
                //    if( nc == ' ' ||
                //        nc == '\n' ||
                //        nc == 0 )
                //    {
                //        return;
                //    }
                //}
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
                    if (nc == ';' || nc == '\n' || nc == 0)
                    {
                        return;
                    }
                }
        }
    }

    /*
     *
     */

    public boolean test_skiperror()
    {
        return true;
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

    private StringBuffer getDocTextBuffer(String doctagname)
    {
        StringBuffer doctextbuf = new StringBuffer();
        doctextbuf.append("<").append(doctagname).append("><![CDATA[");
        return doctextbuf;
    }

    String getXMLText(int begin, int end)
    {
        int len = end-begin; 

        String xmltext = null;
        if( len > 0 )
        {
	        xmltext = input.source(begin,end);
        }
        return xmltext;
    }
    
    public int nexttoken(boolean resetState)
    {
        if (resetState)
        {
            isFirstTokenOnLine = false;
        }
        String xmltagname = null, doctagname = "description";
        StringBuffer doctextbuf = null;
        int startofxml = input.positionOfNext()+1;
        StringBuffer blockcommentbuf = null;
        StringBuffer stringliteralbuf = null;
        char regexp_flags =0; // used to track option flags encountered in a regexp expression.  Initialized in regexp_state

        while (true)
        {
            if (debug)
            {
                System.out.println("state = " + state + ", next = " + input.positionOfNext());
            }

            switch (state)
            {

                case start_state:
                    {
                        int c = nextchar();
                        input.mark();
                        switch (c)
                        {
                            case 0xffffffef:
                                state = utf8sig_state;
                                continue;
                            case '@':
                                state = start_state;
                                return AMPERSAND_TOKEN;
                            case '\'':
                                stringliteralbuf = new StringBuffer();
                                stringliteralbuf.append((char)c);
                                state = singlequote_state;
                                continue;
                            case '\"':
                                stringliteralbuf = new StringBuffer();
                                stringliteralbuf.append((char)c);
                                state = doublequote_state;
                                continue;
                            case '-':
                                state = minus_state;
                                continue;
                            case '!':
                                state = not_state;
                                continue;
                            case '%':
                                state = remainder_state;
                                continue;
                            case '&':
                                state = and_state;
                                continue;
                            case '#':
                                if (HAS_HASHPRAGMAS)
                                {
                                    return USE_TOKEN;
                                }
                                else
                                {
                                    state = error_state;
                                    continue;
                                }  // # is short for use
                            case '(':
                                state = start_state;
                                return LEFTPAREN_TOKEN;
                            case ')':
                                state = start_state;
                                return RIGHTPAREN_TOKEN;
                            case '*':
                                state = star_state;
                                continue;
                            case ',':
                                state = start_state;
                                return COMMA_TOKEN;
                            case '.':
                                state = dot_state;
                                continue;
                            case '/':
                                state = slash_state;
                                continue;
                            case ':':
                                state = colon_state;
                                continue;
                            case ';':
                                state = start_state;
                                return SEMICOLON_TOKEN;
                            case '?':
                                state = start_state;
                                return QUESTIONMARK_TOKEN;
                            case '[':
                                state = start_state;
                                return LEFTBRACKET_TOKEN;
                            case ']':
                                state = start_state;
                                return RIGHTBRACKET_TOKEN;
                            case '^':
                                state = bitwisexor_state;
                                continue;
                            case '{':
                                state = start_state;
                                return LEFTBRACE_TOKEN;
                            case '|':
                                state = or_state;
                                continue;
                            case '}':
                                state = start_state;
                                return RIGHTBRACE_TOKEN;
                            case '~':
                                state = start_state;
                                return BITWISENOT_TOKEN;
                            case '+':
                                state = plus_state;
                                continue;
                            case '<':
                                state = lessthan_state;
                                continue;
                            case '=':
                                state = equal_state;
                                continue;
                            case '>':
                                state = greaterthan_state;
                                continue;
                            case 'a':
                                state = a_state;
                                continue;
                            case 'b':
                                state = b_state;
                                continue;
                            case 'c':
                                state = c_state;
                                continue;
                            case 'd':
                                state = d_state;
                                continue;
                            case 'e':
                                state = e_state;
                                continue;
                            case 'f':
                                state = f_state;
                                continue;
                            case 'g':
                                state = g_state;
                                continue;
                            case 'i':
                                state = i_state;
                                continue;
                            case 'l':
                                state = l_state;
                                continue;
                            case 'n':
                                state = n_state;
                                continue;
                            case 'p':
                                state = p_state;
                                continue;
                            case 'r':
                                state = r_state;
                                continue;
                            case 's':
                                state = s_state;
                                continue;
                            case 't':
                                state = t_state;
                                continue;
                            case 'u':
                                state = u_state;
                                continue;
                            case 'v':
                                state = v_state;
                                continue;
                            case 'w':
                                state = w_state;
                                continue;
                            case 'x':
                                state = x_state;
                                continue;
                            case 'A':
                            case 'B':
                            case 'C':
                            case 'D':
                            case 'E':
                            case 'F':
                            case 'G':
                            case 'H':
                            case 'h':
                            case 'I':
                            case 'J':
                            case 'j':
                            case 'K':
                            case 'k':
                            case 'L':
                            case 'M':
                            case 'm':
                            case 'N':
                            case 'O':
                            case 'o':
                            case 'P':
                            case 'Q':
                            case 'q':
                            case 'R':
                            case 'S':
                            case 'T':
                            case 'U':
                            case 'V':
                            case 'W':
                            case 'X':
                            case 'Y':
                            case 'y':
                            case 'Z':
                            case 'z':
                            case '$':
                            case '_':
                                state = A_state;
                                continue;
                            case '0':
                                state = zero_state;
                                continue;
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                state = decimalinteger_state;
                                continue;
                            case ' ':
                                state = start_state;
                                continue;
                            case '\n':
                                state = start_state;
                                isFirstTokenOnLine = true;
                                continue;
                            case 0:
                                state = start_state;
                                return EOS_TOKEN;
                            default:
                                switch (input.classOfNext())
                                {
                                    case Lu:
                                    case Ll:
                                    case Lt:
                                    case Lm:
                                    case Lo:
                                    case Nl:
                                        state = A_state;
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
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        case 0:
                        case ' ':
                        case '\n':
                        default:
                            switch (input.classOfNext())
                            {
                                case Lu:
                                case Ll:
                                case Lt:
                                case Lm:
                                case Lo:
                                case Nl:
                                case Mn:
                                case Mc:
                                case Nd:
                                case Pc:
                                    state = A_state;
                                    continue;
                                default:
                                    retract();
                                    state = start_state;
                                    return makeTokenInstance(IDENTIFIER_TOKEN, input.copy());
                            }
                    }

                    /*
                     * prefix: <eol>
                     */

                case eol_state:
                    isFirstTokenOnLine = true;
                    switch (nextchar())
                    {
                        case '\n':
                            System.err.println("eating eol");
                            state = eol_state;
                            continue; /* eat extra eols */
                        default:
                            System.err.println("returning eol");
                            state = start_state; /*first = input.positionOfNext();*/
                            retract();
                            state = start_state;
                            continue;
                    }

                    /*
                     * prefix: '
                     */

                case singlequote_state:
                {
                    int c = nextchar(true /*get unnormalized*/);
                    // string literals can span multiple lines, which InputBuffer doesn't handle
                    //  copy all characters in the string literal into a local buffer instead.
                    stringliteralbuf.append((char)c);

                    switch (c)
                    {
                        case '\'':
                            state = start_state;
                            return makeTokenInstance(STRINGLITERAL_TOKEN, input.escapeString(stringliteralbuf, 0, stringliteralbuf.length()-1) );
                        case '\\':
                            c = nextchar(true /*get unnormalized*/);
                            if (c == '\r') // escaped newline is the line continuation character.  Continue string with input from next line.
                            {
                                c = nextchar(true /*get unnormalized*/);
                                if (c != '\n')
                                    stringliteralbuf.deleteCharAt(stringliteralbuf.length()-1);
                            }
                            if (c == '\n') // escaped newline (or escacped CR,NL) is the line continuation character.  Continue string with input from next line.
                            {
                                stringliteralbuf.deleteCharAt(stringliteralbuf.length()-1);
                                c = nextchar(true /*get unnormalized*/);
                            }
                            stringliteralbuf.append((char)c); 
                            state = singlequote_state;
                            continue;
                        case '\n':
                            error(kError_Lexical_LineTerminatorInSingleQuotedStringLiteral);
                            state = start_state;
                            continue;
                        case 0:
                            error(kError_Lexical_EndOfStreamInStringLiteral);
                            state = start_state;
                            return EOS_TOKEN;
                        default:
                            state = singlequote_state;
                            continue;
                    }
                }

                    /*
                     * prefix: "
                     */

                case doublequote_state:
                {
                    int c = nextchar(true /*get unnormalized*/);
                    // string literals can span multiple lines, which InputBuffer doesn't handle
                    //  copy all characters in the string literal into a local buffer instead.
                    stringliteralbuf.append((char)c);

                    switch (c)
                    {
                        case '\"':
                            state = start_state;
                            return makeTokenInstance(STRINGLITERAL_TOKEN, input.escapeString(stringliteralbuf, 0, stringliteralbuf.length()-1));
                            // case '\\': error(kError_Lexical_BackSlashInDoubleQuotedString); state = start_state; continue;
                        case '\\':
                            c = nextchar(true /*get unnormalized*/);
                            if (c == '\r') // escaped newline is the line continuation character.  Continue string with input from next line.
                            {
                                c = nextchar(true /*get unnormalized*/);
                                if (c != '\n')
                                    stringliteralbuf.deleteCharAt(stringliteralbuf.length()-1);
                            }
                            if (c == '\n') // escaped newline (or escaped CR,NL) is the line continuation character.  Continue string with input from next line.
                            {
                                stringliteralbuf.deleteCharAt(stringliteralbuf.length()-1);
                                c = nextchar(true /*get unnormalized*/);
                            }
                            stringliteralbuf.append((char)c); 
                            state = doublequote_state;
                            continue;
                        case '\n':
                            error(kError_Lexical_LineTerminatorInDoubleQuotedStringLiteral);
                            state = start_state;
                            continue;
                        case 0:
                            error(kError_Lexical_EndOfStreamInStringLiteral);
                            state = start_state;
                            return EOS_TOKEN;
                        default:
                            state = doublequote_state;
                            continue;
                    }
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
                        switch(nextchar()) // check that first character is legal
                            {
                                case '0': case '1': case '2': case '3': 
                                 case '4': case '5': case '6': case '7': 
                                 case '8': case '9': case 'a': case 'b': 
                                 case 'c': case 'd': case 'e': case 'f':
                                 case 'A': case 'B': case 'C': case 'D': 
                                 case 'E': case 'F':
                                     retract();
                                    state = hexinteger_state; 
                                    continue;
                                default:
                                     error(kError_Lexical_General); 
                                    state = start_state; 
                                    continue;
                            }
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                        case '.':
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
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                        case 'a':
                        case 'b':
                        case 'c':
                        case 'd':
                        case 'e':
                        case 'f':
                        case 'A':
                        case 'B':
                        case 'C':
                        case 'D':
                        case 'E':
                        case 'F':
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
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = decimal_state;
                            continue;
                        case '.':
                            state = doubledot_state;
                            continue;
                        case '<':
                            state = start_state;
                            return DOTLESSTHAN_TOKEN;
                        default:
                            retract();
                            state = start_state;
                            return DOT_TOKEN;
                    }

                    /*
                     * accepts: ..
                     */

                case doubledot_state:
                    switch (nextchar())
                    {
                        case '.':
                            state = start_state;
                            return TRIPLEDOT_TOKEN;
                        default:
                            retract();
                            state = start_state;
                            return DOUBLEDOT_TOKEN;
                    }

                    /*
                     * prefix: N
                     * accepts: 0.123 | 1.23 | 123 | 1e23 | 1e-23
                     */

                case decimalinteger_state:
                    switch (nextchar())
                    {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
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
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
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
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
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
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
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
                     * tokens: --  -=  -
                     */

                case minus_state:
                    switch (nextchar())
                    {
                        case '-':
                            state = start_state;
                            return MINUSMINUS_TOKEN;
                        case '=':
                            state = start_state;
                            return MINUSASSIGN_TOKEN;
                        default:
                            retract();
                            state = start_state;
                            return MINUS_TOKEN;
                    }

                    /*
                     * prefix: !
                     */

                case not_state:
                    switch (nextchar())
                    {
                        case '=':
                            state = notequals_state;
                            continue;
                        default:
                            retract();
                            state = start_state;
                            return NOT_TOKEN;
                    }

                    /*
                     * prefix: !=
                     */

                case notequals_state:
                    switch (nextchar())
                    {
                        case '=':
                            state = start_state;
                            return STRICTNOTEQUALS_TOKEN;
                        default:
                            retract();
                            state = start_state;
                            return NOTEQUALS_TOKEN;
                    }

                    /*
                     * prefix: %
                     * tokens: %= %
                     */

                case remainder_state:
                    switch (nextchar())
                    {
                        case '=':
                            state = start_state;
                            return MODULUSASSIGN_TOKEN;
                        default:
                            retract();
                            state = start_state;
                            return MODULUS_TOKEN;
                    }

                    /*
                     * prefix: &
                     */

                case and_state:
                    switch (nextchar())
                    {
                        case '&':
                            state = logicaland_state;
                            continue;
                        case '=':
                            state = start_state;
                            return BITWISEANDASSIGN_TOKEN;
                        default:
                            retract();
                            state = start_state;
                            return BITWISEAND_TOKEN;
                    }

                    /*
                     * prefix: &&
                     */

                case logicaland_state:
                    switch (nextchar())
                    {
                        case '=':
                            state = start_state;
                            return LOGICALANDASSIGN_TOKEN;
                        default:
                            retract();
                            state = start_state;
                            return LOGICALAND_TOKEN;
                    }

                    /*
                     * tokens: *=  *
                     */

                case star_state:
                    switch (nextchar())
                    {
                        case '=':
                            state = start_state;
                            return MULTASSIGN_TOKEN;
                        default:
                            retract();
                            state = start_state;
                            return MULT_TOKEN;
                    }

                    /*
                     * prefix: /
                     */

                case slash_state:
                    switch (nextchar())
                    {
                        case '/':
                            if (blockcommentbuf == null) blockcommentbuf = new StringBuffer();
                            state = linecomment_state;
                            continue;
                        case '*':
                            if (blockcommentbuf == null) blockcommentbuf = new StringBuffer();
                            blockcommentbuf.append("/*");
                            state = blockcommentstart_state;
                            continue;
                        default:
                        {
                            retract(); /* since we didn't use the current character
                                        for this decision. */
                            if (isSlashDivContext())
                            {
                                state = slashdiv_state;
                            }
                            else
                            {
                                state = slashregexp_state;
                            }
                            continue;
                        }
                    }

                    /*
                     * tokens: / /=
                     */

                case slashdiv_state:
                    switch (nextchar())
                    {
                        case '>':
                            state = start_state; 
                            return XMLTAGENDEND_TOKEN;
                        case '=':
                            state = start_state;
                            return DIVASSIGN_TOKEN;
                        default:
                            retract();
                            state = start_state;
                            return DIV_TOKEN;
                    }

                    /*
                     * tokens: : ::
                     */

                case colon_state:
                    switch (nextchar())
                    {
                        case ':':
                            state = start_state;
                            return DOUBLECOLON_TOKEN;
                        default:
                            retract();
                            state = start_state;
                            return COLON_TOKEN;
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
                switch ( nextchar() )
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

                    case 'A': 
                    case 'a': 
                    case 'B': 
                    case 'b': 
                    case 'C': 
                    case 'c': 
                    case 'D': 
                    case 'd': 
                    case 'E': 
                    case 'e': 
                    case 'F': 
                    case 'f':
                    case 'G': 
                    case 'H': 
                    case 'h': 
                    case 'I':
                    case 'J': 
                    case 'j': 
                    case 'K': 
                    case 'k': 
                    case 'L': 
                    case 'l': 
                    case 'M': 
                    case 'N': 
                    case 'n': 
                    case 'O': 
                    case 'o':
                    case 'P': 
                    case 'p': 
                    case 'Q': 
                    case 'q': 
                    case 'R': 
                    case 'r': 
                    case 'S': 
                    case 'T': 
                    case 't': 
                    case 'U': 
                    case 'u': 
                    case 'V': 
                    case 'v': 
                    case 'W': 
                    case 'w': 
                    case 'X': 
                    case 'Y': 
                    case 'y': 
                    case 'Z': 
                    case 'z': 
                    case '$': 
                    case '_':
                    case '0': 
                    case '1': 
                    case '2': 
                    case '3': 
                    case '4': 
                    case '5': 
                    case '6': 
                    case '7': 
                    case '8': 
                    case '9':
                        error(kError_Lexical_General); 
                        state = start_state; 
                        continue;
                    case 0: 
                    case ' ': 
                    case '\n': 
                    default: 
                        retract(); 
                        state = start_state; 
                        return makeTokenInstance( REGEXPLITERAL_TOKEN, input.copyWithoutInterpretingEscapedChars() );
                }
                    /*
                     * tokens: ^^ ^^= ^=  ^
                     */

                case bitwisexor_state:
                    switch (nextchar())
                    {
                        case '=':
                            state = start_state;
                            return BITWISEXORASSIGN_TOKEN;
/* not yet supported
                        case '^':
                            state = logicalxor_state;
                            continue;
*/
                        default:
                            retract();
                            state = start_state;
                            return BITWISEXOR_TOKEN;
                    }

                    /*
                     * tokens: ^^ ^=  ^
                     */

                case logicalxor_state:
                    switch (nextchar())
                    {
                        case '=':
                            state = start_state;
                            return LOGICALXORASSIGN_TOKEN;
                        default:
                            retract();
                            state = start_state;
                            return LOGICALXOR_TOKEN;
                    }

                    /*
                     * prefix: |
                     */

                case or_state:
                    switch (nextchar())
                    {
                        case '|':
                            state = logicalor_state;
                            continue;
                        case '=':
                            state = start_state;
                            return BITWISEORASSIGN_TOKEN;
                        default:
                            retract();
                            state = start_state;
                            return BITWISEOR_TOKEN;
                    }

                    /*
                     * prefix: ||
                     */

                case logicalor_state:
                    switch (nextchar())
                    {
                        case '=':
                            state = start_state;
                            return LOGICALORASSIGN_TOKEN;
                        default:
                            retract();
                            state = start_state;
                            return LOGICALOR_TOKEN;
                    }

                    /*
                     * tokens: ++  += +
                     */

                case plus_state:
                    switch (nextchar())
                    {
                        case '+':
                            state = start_state;
                            return PLUSPLUS_TOKEN;
                        case '=':
                            state = start_state;
                            return PLUSASSIGN_TOKEN;
                        default:
                            retract();
                            state = start_state;
                            return PLUS_TOKEN;
                    }


                    /*
                     * prefix: <
                     */

                case lessthan_state:
                    if( isSlashDivContext() )
                    {
                        switch (nextchar())
                        {
                            case '<':
                                state = leftshift_state;
                                continue;
                            case '=':
                                state = start_state;
                                return LESSTHANOREQUALS_TOKEN;
                            case '/': 
                                state = start_state; 
                                return XMLTAGSTARTEND_TOKEN;
                            case '!': 
                                state = xmlcommentorcdatastart_state; 
                                continue;
                            case '?': 
                                state = xmlpi_state; 
                                continue;                            
                            default:
                                retract();
                                state = start_state;
                                return LESSTHAN_TOKEN;
                        }
                    }
                    else
                    {
                        switch ( nextchar() )             
                        {
                            case '/': 
                                state = start_state; 
                                return XMLTAGSTARTEND_TOKEN;
                            case '!': 
                                state = xmlcommentorcdatastart_state; 
                                continue;
                            case '?': 
                                state = xmlpi_state; 
                                continue;
                            default:  
                                retract(); 
                                state = start_state; 
                                return LESSTHAN_TOKEN;
                        }                          
                    }

                /*
                    * prefix: <!
                */
                case xmlcommentorcdatastart_state:
                    switch ( nextchar() )        
                    {
                        case '[':  state = xmlcdatastart_state; continue;
                        case '-':  state = xmlcommentstart_state; continue;
                        default:    error(kError_Lexical_General); state = start_state; continue;
                    }

                case xmlcdatastart_state:
                    switch ( nextchar() )        
                    {
                        case 'C':  state = xmlcdatac_state; continue;
                        default:    error(kError_Lexical_General); state = start_state; continue;
                    }

                case xmlcdatac_state:
                    switch ( nextchar() )          
                    {
                        case 'D':  state = xmlcdatacd_state; continue;
                        default:    error(kError_Lexical_General); state = start_state; continue;
                    }

                case xmlcdatacd_state:
                    switch ( nextchar() )        
                    {
                        case 'A':  state = xmlcdatacda_state; continue;
                        default:   error(kError_Lexical_General); state = start_state; continue;
                    }

                case xmlcdatacda_state:
                    switch ( nextchar() )       
                    {
                        case 'T':  state = xmlcdatacdat_state; continue;
                        default:   error(kError_Lexical_General); state = start_state; continue;
                    }

                case xmlcdatacdat_state:
                    switch ( nextchar() )          
                    {
                        case 'A':  state = xmlcdatacdata_state; continue;
                        default:   error(kError_Lexical_General); state = start_state; continue;
                    }
                case xmlcdatacdata_state:
                    switch ( nextchar() )          
                    {
                        case '[':  state = xmlcdata_state; continue;
                        default:   error(kError_Lexical_General); state = start_state; continue;
                    }
                case xmlcdata_state:
                    switch ( nextchar() )         
                    {
                        case ']':  state = xmlcdataendstart_state; continue;
                        case 0:   error(kError_Lexical_General); state = start_state; continue;
                        default:   state = xmlcdata_state; continue;
                    }
                case xmlcdataendstart_state:
                    switch ( nextchar() )          
                    {
                        case ']':  state = xmlcdataend_state; continue;
                        default:   state = xmlcdata_state; continue;
                    }

                case xmlcdataend_state:
                    switch ( nextchar() )         
                    {
                        case '>':  
                        {
                            state = start_state;
                            return makeTokenInstance(XMLMARKUP_TOKEN,getXMLText(startofxml,input.positionOfNext()+1));
                        }
                        default:   state = xmlcdata_state; continue;
                    }
                case xmlcommentstart_state:
                    switch ( nextchar() )        
                    {
                        case '-':  state = xmlcomment_state; continue;
                        default:   error(kError_Lexical_General); state = start_state; continue;
                    }

                case xmlcomment_state:
                    switch ( nextchar() )
                    {
                        case '-':  state = xmlcommentendstart_state; continue;
                        case 0:   error(kError_Lexical_General); state = start_state; continue;
                        default:   state = xmlcomment_state; continue;
                    }

                case xmlcommentendstart_state:
                    switch ( nextchar() )
                    {
                        case '-':  state = xmlcommentend_state; continue;
                        default:   state = xmlcomment_state; continue;
                    }

                case xmlcommentend_state:
                    switch ( nextchar() )  
                    {
                        case '>':  
                        {
                            state = start_state;
                            return makeTokenInstance(XMLMARKUP_TOKEN,getXMLText(startofxml,input.positionOfNext()+1));
                        }
                        default:   error(kError_Lexical_General); state = start_state; continue;
                    }

                case xmlpi_state:
                    switch ( nextchar() )
                    {
                        case '?':  state = xmlpiend_state; continue;
                        case 0:   error(kError_Lexical_General); state = start_state; continue;
                        default:   state = xmlpi_state; continue;
                    }

                case xmlpiend_state:
                    switch ( nextchar() )
                    {
                        case '>':  
                        {
                            state = start_state;
                            return makeTokenInstance(XMLMARKUP_TOKEN,getXMLText(startofxml,input.positionOfNext()+1));
                        }
                        default:   error(kError_Lexical_General); state = start_state; continue;
                    }
                case xmltext_state:
                { 
                    switch(nextchar())
                    {
                        case '<': case '{':  
                        {
                            retract();
                            String xmltext = getXMLText(startofxml,input.positionOfNext()+1);
                            if( xmltext != null )
                            {
                                state = start_state;
                                return makeTokenInstance(XMLTEXT_TOKEN,xmltext);
                            }
                            else  // if there is no leading text, then just return puncutation token to avoid empty text tokens
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
                                    case '{': state = start_state; return LEFTBRACE_TOKEN;
                                }
                            }
                        }
                        case 0:   state = start_state; return EOS_TOKEN;
                        default:  state = xmltext_state; continue;
                    }
                }

                case xmlliteral_state:
                    switch (nextchar())
                    {
                        case '{':  // return XMLPART_TOKEN
                            {
	                            String xmltext = input.source(startofxml, input.positionOfNext());
                                return makeTokenInstance(XMLPART_TOKEN, xmltext);
                            }
                        case '<':
                            switch (nextchar())
                            {
                                case '/':
                                    --level;
                                    nextchar();
                                    input.mark();
                                    retract();
                                    state = endxmlname_state;
                                    continue;
                                default:
                                    ++level;
                                    state = xmlliteral_state; /*first = input.positionOfNext();*/
                                    continue;
                            }
                        case '/':
                            {
                                switch (nextchar())
                                {
                                    case '>':
                                        {
                                            --level;
                                            if (level == 0)
                                            {
	                                            String xmltext = input.source(startofxml, input.positionOfNext() + 2);
                                                state = start_state;
                                                return makeTokenInstance(XMLLITERAL_TOKEN, xmltext);
                                            }
                                            // otherwise continue
                                            state = xmlliteral_state;
                                            continue;
                                        }
                                    default: /*error(kError_Lexical_General);*/
                                        state = xmlliteral_state;
                                        continue; // keep going anyway
                                }
                            }
                        case 0:
                            retract();
                            error(kError_Lexical_NoMatchingTag);
                            state = start_state;
                            continue;
                        default:
                            continue;
                    }

                case endxmlname_state:  // scan name and compare it to start name
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                        case ':':
                            {
                                // stop looking for matching tag if the names diverge
                                String temp = input.copy();
                                if (xmltagname != null && xmltagname.indexOf(temp) == -1)
                                {
                                    state = xmlliteral_state;
                                    //level -= 2;
                                }
                                else
                                {
                                    state = endxmlname_state;
                                }
                                continue;
                            }
                        case '{':  // return XMLPART_TOKEN
                            {
                                if (xmltagname != null)  // clear xmltagname since there is an expression in it.
                                {
                                    xmltagname = null;
                                }
	                            String xmltext = input.source(startofxml, input.positionOfNext());
                                return makeTokenInstance(XMLPART_TOKEN, xmltext);
                            }
                        case '>':
                            {
                                retract();
                                String temp = input.copy();
                                nextchar();
                                if (level == 0)
                                {
                                    if (xmltagname != null)
                                    {
                                        if (temp.equals(xmltagname))
                                        {
	                                        String xmltext = input.source(startofxml, input.positionOfNext() + 2);
                                            state = start_state;
                                            return makeTokenInstance(XMLLITERAL_TOKEN, xmltext);
                                        }
                                    }
                                    else
                                    {
	                                    String xmltext = input.source(startofxml, input.positionOfNext() + 2);
                                        state = start_state;
                                        return makeTokenInstance(XMLLITERAL_TOKEN, xmltext);
                                    }
                                }
                                state = xmlliteral_state;
                                continue;
                            }
                        default:
                            state = xmlliteral_state;
                            continue;
                    }

                    /*
                     * tokens: <<=  <<
                     */

                case leftshift_state:
                    switch (nextchar())
                    {
                        case '=':
                            state = start_state;
                            return LEFTSHIFTASSIGN_TOKEN;
                        default:
                            retract();
                            state = start_state;
                            return LEFTSHIFT_TOKEN;
                    }

                    /*
                     * tokens: ==  =
                     */

                case equal_state:
                    switch (nextchar())
                    {
                        case '=':
                            state = equalequal_state;
                            break;
                        default:
                            retract();
                            state = start_state;
                            return ASSIGN_TOKEN;
                    }

                    /*
                     * tokens: ===  ==
                     */

                case equalequal_state:
                    switch (nextchar())
                    {
                        case '=':
                            state = start_state;
                            return STRICTEQUALS_TOKEN;
                        default:
                            retract();
                            state = start_state;
                            return EQUALS_TOKEN;
                    }

                    /*
                     * prefix: >
                     */

                case greaterthan_state:
                    if( isSlashDivContext() )       
                    {
                        switch ( nextchar() )          
                        {
                            case '>': state = rightshift_state; break;
                            case '=': state = start_state; return GREATERTHANOREQUALS_TOKEN;
                            //default:  retract(); state = start_state; return greaterthan_token;
                            default:  retract(); state = start_state; return GREATERTHAN_TOKEN;
                        }
                    }
                    else      
                    {
                        state = start_state; 
                        return GREATERTHAN_TOKEN;
                    }

                    /*
                     * prefix: >>
                     */

                case rightshift_state:
                    switch (nextchar())
                    {
                        case '>':
                            state = unsignedrightshift_state;
                            break;
                        case '=':
                            state = start_state;
                            return RIGHTSHIFTASSIGN_TOKEN;
                        default:
                            retract();
                            state = start_state;
                            return RIGHTSHIFT_TOKEN;
                    }

                    /*
                     * prefix: >>>
                     */

                case unsignedrightshift_state:
                    switch (nextchar())
                    {
                        case '=':
                            state = start_state;
                            return UNSIGNEDRIGHTSHIFTASSIGN_TOKEN;
                        default:
                            retract();
                            state = start_state;
                            return UNSIGNEDRIGHTSHIFT_TOKEN;
                    }

                    /*
                     * prefix: a
                     */

                case a_state:
                    switch (nextchar())
                    {
                        case 's':
                            state = as_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: as
                     */

                case as_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                    state = A_state;
                                    continue;
                            }
                            retract();
                            state = start_state;
                            return (HAS_ASOPERATOR) ? AS_TOKEN : makeTokenInstance(IDENTIFIER_TOKEN, input.copy());
                    }

                    /*
                     * prefix: b
                     */

                case b_state:
                    switch (nextchar())
                    {
                        case 'r':
                            state = br_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: bo
                     */

                case bo_state:
                    switch (nextchar())
                    {
                        case 'o':
                            state = boo_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: boo
                     */

                case boo_state:
                    switch (nextchar())
                    {
                        case 'l':
                            state = bool_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: bool
                     */

                case bool_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = boole_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: boole
                     */

                case boole_state:
                    switch (nextchar())
                    {
                        case 'a':
                            state = boolea_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: boolea
                     */

                case boolea_state:
                    switch (nextchar())
                    {
                        case 'n':
                            state = boolean_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                    * prefix: boolean
                    */

                case boolean_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return makeTokenInstance(IDENTIFIER_TOKEN, input.copy());
                    }

                    /*
                     * prefix: br
                     */

                case br_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = bre_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: bre
                     */

                case bre_state:
                    switch (nextchar())
                    {
                        case 'a':
                            state = brea_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: brea
                     */

                case brea_state:
                    switch (nextchar())
                    {
                        case 'k':
                            state = break_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: break
                     */

                case break_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return BREAK_TOKEN;
                    }

                    /*
                     * prefix: by
                     */

                case by_state:
                    switch (nextchar())
                    {
                        case 't':
                            state = byt_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: byt
                     */

                case byt_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = byte_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: byte
                     */

                case byte_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return makeTokenInstance(IDENTIFIER_TOKEN, input.copy());
                    }

                    /*
                     * prefix: c
                     */

                case c_state:
                    switch (nextchar())
                    {
                        case 'a':
                            state = ca_state;
                            continue;
                        case 'l':
                            state = cl_state;
                            continue;
                        case 'o':
                            state = co_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: ca
                     */

                case ca_state:
                    switch (nextchar())
                    {
                        case 's':
                            state = cas_state;
                            continue;
                        case 't':
                            state = cat_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: cas
                     */

                case cas_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = case_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: case
                     */

                case case_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return CASE_TOKEN;
                    }

                    /*
                     * prefix: cat
                     */

                case cat_state:
                    switch (nextchar())
                    {
                        case 'c':
                            state = catc_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: catc
                     */

                case catc_state:
                    switch (nextchar())
                    {
                        case 'h':
                            state = catch_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: catch
                     */

                case catch_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return CATCH_TOKEN;
                    }

                    /*
                     * prefix: ch
                     */

                case ch_state:
                    switch (nextchar())
                    {
                        case 'a':
                            state = cha_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: cha
                     */

                case cha_state:
                    switch (nextchar())
                    {
                        case 'r':
                            state = char_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: char
                     */

                case char_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return makeTokenInstance(IDENTIFIER_TOKEN, input.copy());
                    }

                    /*
                     * prefix: cl
                     */

                case cl_state:
                    switch (nextchar())
                    {
                        case 'a':
                            state = cla_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: cla
                     */

                case cla_state:
                    switch (nextchar())
                    {
                        case 's':
                            state = clas_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: clas
                     */

                case clas_state:
                    switch (nextchar())
                    {
                        case 's':
                            state = class_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: class
                     */

                case class_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return CLASS_TOKEN;
                    }

                    /*
                     * prefix: co
                     */

                case co_state:
                    switch (nextchar())
                    {
                        case 'n':
                            state = con_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: con
                     */

                case con_state:
                    switch (nextchar())
                    {
                        case 's':
                            state = cons_state;
                            continue;
                        case 't':
                            state = cont_state;
                            continue;
/*                        case 'f':
                            state = conf_state;
                            continue;
*/                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: cons
                     */

                case cons_state:
                    switch (nextchar())
                    {
                        case 't':
                            state = const_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: const
                     */

                case const_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return CONST_TOKEN;
                    }

                     /*
                     * prefix: conf
                     */
                case conf_state:
                    switch(nextchar())
                    {
                        case 'i':
                            state = confi_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: confi
                     */
                case confi_state:
                    switch(nextchar())
                    {
                        case 'g':
                            state = config_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: config
                     */

                case config_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return CONFIG_TOKEN;
                    }

                   /*
                     * prefix: cont
                     */

                case cont_state:
                    switch (nextchar())
                    {
                        case 'i':
                            state = conti_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: conti
                     */

                case conti_state:
                    switch (nextchar())
                    {
                        case 'n':
                            state = contin_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: contin
                     */

                case contin_state:
                    switch (nextchar())
                    {
                        case 'u':
                            state = continu_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: continu
                     */

                case continu_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = continue_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: continue
                     */

                case continue_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return CONTINUE_TOKEN;
                    }

                    /*
                     * prefix: d
                     */

                case d_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = de_state;
                            continue;
                        case 'o':
                            state = do_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: de
                     */

                case de_state:
                    switch (nextchar())
                    {
                        case 'b':
                            state = deb_state;
                            continue;
                        case 'f':
                            state = def_state;
                            continue;
                        case 'l':
                            state = del_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: deb
                     */

                case deb_state:
                    switch (nextchar())
                    {
                        case 'u':
                            state = debu_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: debu
                     */

                case debu_state:
                    switch (nextchar())
                    {
                        case 'g':
                            state = debug_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: debug
                     */

                case debug_state:
                    switch (nextchar())
                    {
                        case 'g':
                            state = debugg_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: debugg
                     */

                case debugg_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = debugge_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: debugge
                     */

                case debugge_state:
                    switch (nextchar())
                    {
                        case 'r':
                            state = debugger_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: debugger
                     */

                case debugger_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            retract();
                            state = start_state;
                            return makeTokenInstance(IDENTIFIER_TOKEN, input.copy());
                    }

                    /*
                     * prefix: def
                     */

                case def_state:
                    switch (nextchar())
                    {
                        case 'a':
                            state = defa_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: defa
                     */

                case defa_state:
                    switch (nextchar())
                    {
                        case 'u':
                            state = defau_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: defau
                     */

                case defau_state:
                    switch (nextchar())
                    {
                        case 'l':
                            state = defaul_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: defaul
                     */

                case defaul_state:
                    switch (nextchar())
                    {
                        case 't':
                            state = default_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: default
                     */

                case default_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return DEFAULT_TOKEN;
                    }

                    /*
                     * prefix: del
                     */

                case del_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = dele_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: dele
                     */

                case dele_state:
                    switch (nextchar())
                    {
                        case 't':
                            state = delet_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: delet
                     */

                case delet_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = delete_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: delete
                     */

                case delete_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return DELETE_TOKEN;
                    }

                    /*
                     * prefix: do
                     */

                case do_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return DO_TOKEN;
                    }

                    /*
                     * prefix: dou
                     */

                case dou_state:
                    switch (nextchar())
                    {
                        case 'b':
                            state = doub_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: doub
                     */

                case doub_state:
                    switch (nextchar())
                    {
                        case 'l':
                            state = doubl_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: doubl
                     */

                case doubl_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = double_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: double
                     */

                case double_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return makeTokenInstance(IDENTIFIER_TOKEN, input.copy());
                    }

                    /*
                     * prefix: e
                     */

                case e_state:
                    switch (nextchar())
                    {
                        case 'l':
                            state = el_state;
                            continue;
                        case 'v':
                            state = ev_state;
                            continue;
                        case 'x':
                            state = ex_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: el
                     */

                case el_state:
                    switch (nextchar())
                    {
                        case 's':
                            state = els_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: els
                     */

                case els_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = else_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: else
                     */

                case else_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return ELSE_TOKEN;
                    }

                    /*
                     * prefix: ev
                     */

                case ev_state:
                    switch (nextchar())
                    {
                        case 'a':
                            state = eva_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: eva
                     */

                case eva_state:
                    switch (nextchar())
                    {
                        case 'l':
                            state = eval_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: eval
                     */

                case eval_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            retract();
                            state = start_state;
                            return makeTokenInstance(IDENTIFIER_TOKEN, input.copy());
                    }

                    /*
                     * prefix: ex
                     */

                case ex_state:
                    switch (nextchar())
                    {
                        case 't':
                            state = ext_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: ext
                     */

                case ext_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = exte_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: exte
                     */

                case exte_state:
                    switch (nextchar())
                    {
                        case 'n':
                            state = exten_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: exten
                     */

                case exten_state:
                    switch (nextchar())
                    {
                        case 'd':
                            state = extend_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: extend
                     */

                case extend_state:
                    switch (nextchar())
                    {
                        case 's':
                            state = extends_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: extends
                     */

                case extends_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return EXTENDS_TOKEN;
                    }

                    /*
                     * prefix: f
                     */

                case f_state:
                    switch (nextchar())
                    {
                        case 'a':
                            state = fa_state;
                            continue;
                        case 'i': 
                            state = fi_state; 
                            continue;
                        case 'o':
                            state = fo_state;
                            continue;
                        case 'u':
                            state = fu_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: fa
                     */

                case fa_state:
                    switch (nextchar())
                    {
                        case 'l':
                            state = fal_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: fal
                     */

                case fal_state:
                    switch (nextchar())
                    {
                        case 's':
                            state = fals_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: fals
                     */

                case fals_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = false_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: false
                     */

                case false_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return FALSE_TOKEN;
                    }

                    /*
                     * prefix: fi
                     */

                case fi_state:
                    switch (nextchar())
                    {
                        case 'n':
                            state = fin_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: fin
                     */

                case fin_state:
                    switch (nextchar())
                    {
                        case 'a':
                            state = fina_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: fina
                     */

                case fina_state:
                    switch (nextchar())
                    {
                        case 'l':
                            state = final_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: final
                     */

                case final_state:
                    switch (nextchar())
                    {
                        case 'l':
                            state = finall_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: finall
                     */

                case finall_state:
                    switch (nextchar())
                    {
                        case 'y':
                            state = finally_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: finally
                     */

                case finally_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return FINALLY_TOKEN;
                    }

                    /*
                     * prefix: fl
                     */

                case fl_state:
                    switch (nextchar())
                    {
                        case 'o':
                            state = flo_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: flo
                     */

                case flo_state:
                    switch (nextchar())
                    {
                        case 'a':
                            state = floa_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: floa
                     */

                case floa_state:
                    switch (nextchar())
                    {
                        case 't':
                            state = float_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: float
                     */

                case float_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return makeTokenInstance(IDENTIFIER_TOKEN, input.copy());
                    }

                    /*
                     * prefix: fo
                     */

                case fo_state:
                    switch (nextchar())
                    {
                        case 'r':
                            state = for_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: for
                     */

                case for_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return FOR_TOKEN;
                    }

                    /*
                     * prefix: fu
                     */

                case fu_state:
                    switch (nextchar())
                    {
                        case 'n':
                            state = fun_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: fun
                     */

                case fun_state:
                    switch (nextchar())
                    {
                        case 'c':
                            state = func_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: func
                     */

                case func_state:
                    switch (nextchar())
                    {
                        case 't':
                            state = funct_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: funct
                     */

                case funct_state:
                    switch (nextchar())
                    {
                        case 'i':
                            state = functi_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: functi
                     */

                case functi_state:
                    switch (nextchar())
                    {
                        case 'o':
                            state = functio_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: functio
                     */

                case functio_state:
                    switch (nextchar())
                    {
                        case 'n':
                            state = function_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: function
                     */

                case function_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return FUNCTION_TOKEN;
                    }

                    /*
                     * prefix: g
                     */

                case g_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = ge_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: ge
                     */

                case ge_state:
                    switch (nextchar())
                    {
                        case 't':
                            state = get_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: get
                     */

                case get_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return GET_TOKEN;
                    }

                    /*
                     * prefix: i
                     */

                case i_state:
                    switch (nextchar())
                    {
                        case 'f':
                            state = if_state;
                            continue;
                        case 'm':
                            state = im_state;
                            continue;
                        case 'n':
                            state = in_state;
                            continue;
                        case 's':
                            state = is_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: if
                     */

                case if_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return IF_TOKEN;
                    }

                    /*
                     * prefix: im
                     */

                case im_state:
                    switch (nextchar())
                    {
                        case 'p':
                            state = imp_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: imp
                     */

                case imp_state:
                    switch (nextchar())
                    {
                        case 'l':
                            state = impl_state;
                            continue;
                        case 'o':
                            state = impo_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: impl
                     */

                case impl_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = imple_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: imple
                     */

                case imple_state:
                    switch (nextchar())
                    {
                        case 'm':
                            state = implem_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: implem
                     */

                case implem_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = impleme_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: impleme
                     */

                case impleme_state:
                    switch (nextchar())
                    {
                        case 'n':
                            state = implemen_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: implemen
                     */

                case implemen_state:
                    switch (nextchar())
                    {
                        case 't':
                            state = implement_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: implement
                     */

                case implement_state:
                    switch (nextchar())
                    {
                        case 's':
                            state = implements_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: implements
                     */

                case implements_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return IMPLEMENTS_TOKEN;
                    }

                    /*
                     * prefix: impo
                     */

                case impo_state:
                    switch (nextchar())
                    {
                        case 'r':
                            state = impor_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: impor
                     */

                case impor_state:
                    switch (nextchar())
                    {
                        case 't':
                            state = import_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: import
                     */

                case import_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return IMPORT_TOKEN;
                    }

                    /*
                     * prefix: in
                     */

                case in_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 'T':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        case 'c':
                            state = inc_state;
                            continue;
                        case 's':
                            state = ins_state;
                            continue;
                        case 't':
                            state = int_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return IN_TOKEN;
                    }

                    /*
                     * prefix: inc
                     */

                case inc_state:
                    switch (nextchar())
                    {
                        case 'l':
                            state = incl_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: incl
                     */

                case incl_state:
                    switch (nextchar())
                    {
                        case 'u':
                            state = inclu_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: inclu
                     */

                case inclu_state:
                    switch (nextchar())
                    {
                        case 'd':
                            state = includ_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: includ
                     */

                case includ_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = include_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: include
                     */

                case include_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return INCLUDE_TOKEN;
                    }

                    /*
                     * prefix: ins
                     */

                case ins_state:
                    switch (nextchar())
                    {
                        case 't':
                            state = inst_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: inst
                     */

                case inst_state:
                    switch (nextchar())
                    {
                        case 'a':
                            state = insta_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: insta
                     */

                case insta_state:
                    switch (nextchar())
                    {
                        case 'n':
                            state = instan_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: instan
                     */

                case instan_state:
                    switch (nextchar())
                    {
                        case 'c':
                            state = instanc_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: instanc
                     */

                case instanc_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = instance_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: instance
                     */

                case instance_state:
                    switch (nextchar())
                    {
                        case 'o':
                            state = instanceo_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: instanceo
                     */

                case instanceo_state:
                    switch (nextchar())
                    {
                        case 'f':
                            state = instanceof_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: instanceof
                     */

                case instanceof_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return INSTANCEOF_TOKEN;
                    }

                    /*
                     * prefix: int
                     */

                case int_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = inte_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: inte
                     */

                case inte_state:
                    switch (nextchar())
                    {
                        case 'r':
                            state = inter_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: inter
                     */

                case inter_state:
                    switch (nextchar())
                    {
                        case 'f':
                            state = interf_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: interf
                     */

                case interf_state:
                    switch (nextchar())
                    {
                        case 'a':
                            state = interfa_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: interfa
                     */

                case interfa_state:
                    switch (nextchar())
                    {
                        case 'c':
                            state = interfac_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: interfac
                     */

                case interfac_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = interface_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: interface
                     */

                case interface_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return INTERFACE_TOKEN;
                    }

                    /*
                     * prefix: is
                     */

                case is_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return (HAS_ISOPERATOR) ? IS_TOKEN : makeTokenInstance(IDENTIFIER_TOKEN, input.copy());
                    }

                    /*
                     * prefix: l
                     */

                case l_state:
                    switch (nextchar())
                    {
                        case '\0':
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: lo
                     */

                case lo_state:
                    switch (nextchar())
                    {
                        case 'n':
                            state = lon_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: lon
                     */

                case lon_state:
                    switch (nextchar())
                    {
                        case 'g':
                            state = long_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: long
                     */

                case long_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return makeTokenInstance(IDENTIFIER_TOKEN, input.copy());
                    }

                    /*
                     * prefix: n
                     */

                case n_state:
                    switch (nextchar())
                    {
                        case 'a':
                            state = na_state;
                            continue;
                        case 'e':
                            state = ne_state;
                            continue;
                        case 'u':
                            state = nu_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: na
                     */

                case na_state:
                    switch (nextchar())
                    {
                        case 't':
                            state = nat_state;
                            continue;
                        case 'm':
                            state = nam_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: nam
                     */

                case nam_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = name_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: name
                     */

                case name_state:
                    switch (nextchar())
                    {
                        case 's':
                            state = names_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: names
                     */

                case names_state:
                    switch (nextchar())
                    {
                        case 'p':
                            state = namesp_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: namesp
                     */

                case namesp_state:
                    switch (nextchar())
                    {
                        case 'a':
                            state = namespa_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: namespa
                     */

                case namespa_state:
                    switch (nextchar())
                    {
                        case 'c':
                            state = namespac_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: namespac
                     */

                case namespac_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = namespace_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: namespace
                     */

                case namespace_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return NAMESPACE_TOKEN;
                    }

                    /*
                     * prefix: nat
                     */

                case nat_state:
                    switch (nextchar())
                    {
                        case 'i':
                            state = nati_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: nati
                     */

                case nati_state:
                    switch (nextchar())
                    {
                        case 'v':
                            state = nativ_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: nativ
                     */

                case nativ_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = native_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: native
                     */

                case native_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return makeTokenInstance(IDENTIFIER_TOKEN, input.copy());//return native_token;
                    }

                    /*
                     * prefix: ne
                     */

                case ne_state:
                    switch (nextchar())
                    {
                        case 'w':
                            state = new_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: new
                     */

                case new_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return NEW_TOKEN;
                    }

                    /*
                     * prefix: nu
                     */

                case nu_state:
                    switch (nextchar())
                    {
                        case 'l':
                            state = nul_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: nul
                     */

                case nul_state:
                    switch (nextchar())
                    {
                        case 'l':
                            state = null_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: null
                     */

                case null_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return NULL_TOKEN;
                    }

                    /*
                     * prefix: p
                     */

                case p_state:
                    switch (nextchar())
                    {
                        case 'a':
                            state = pa_state;
                            continue;
                        case 'r':
                            state = pr_state;
                            continue;
                        case 'u':
                            state = pu_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: pa
                     */

                case pa_state:
                    switch (nextchar())
                    {
                        case 'c':
                            state = pac_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: pac
                     */

                case pac_state:
                    switch (nextchar())
                    {
                        case 'k':
                            state = pack_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: pack
                     */

                case pack_state:
                    switch (nextchar())
                    {
                        case 'a':
                            state = packa_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: packa
                     */

                case packa_state:
                    switch (nextchar())
                    {
                        case 'g':
                            state = packag_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: packag
                     */

                case packag_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = package_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: package
                     */

                case package_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return PACKAGE_TOKEN;
                    }

                    /*
                     * prefix: pr
                     */

                case pr_state:
                    switch (nextchar())
                    {
                        case 'i':
                            state = pri_state;
                            continue;
                        case 'o':
                            state = pro_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: pri
                     */

                case pri_state:
                    switch (nextchar())
                    {
                        case 'v':
                            state = priv_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: priv
                     */

                case priv_state:
                    switch (nextchar())
                    {
                        case 'a':
                            state = priva_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: priva
                     */

                case priva_state:
                    switch (nextchar())
                    {
                        case 't':
                            state = privat_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: privat
                     */

                case privat_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = private_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: private
                     */

                case private_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return PRIVATE_TOKEN;
                    }

                    /*
                     * prefix: pro
                     */

                case pro_state:
                    switch (nextchar())
                    {
                        case 't':
                            state = prot_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: prot
                     */

                case prot_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = prote_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: prote
                     */

                case prote_state:
                    switch (nextchar())
                    {
                        case 'c':
                            state = protec_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: protec
                     */

                case protec_state:
                    switch (nextchar())
                    {
                        case 't':
                            state = protect_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: protect
                     */

                case protect_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = protecte_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: protecte
                     */

                case protecte_state:
                    switch (nextchar())
                    {
                        case 'd':
                            state = protected_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: protected
                     */

                case protected_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return PROTECTED_TOKEN;
                    }

                    /*
                     * prefix: public
                     */

                case pu_state:
                    switch (nextchar())
                    {
                        case 'b':
                            state = pub_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: pub
                     */

                case pub_state:
                    switch (nextchar())
                    {
                        case 'l':
                            state = publ_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: publ
                     */

                case publ_state:
                    switch (nextchar())
                    {
                        case 'i':
                            state = publi_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: publi
                     */

                case publi_state:
                    switch (nextchar())
                    {
                        case 'c':
                            state = public_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: public
                     */

                case public_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return PUBLIC_TOKEN;
                    }

                    /*
                     * prefix: r
                     */

                case r_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = re_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: re
                     */

                case re_state:
                    switch (nextchar())
                    {
                        case 't':
                            state = ret_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: ret
                     */

                case ret_state:
                    switch (nextchar())
                    {
                        case 'u':
                            state = retu_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: retu
                     */

                case retu_state:
                    switch (nextchar())
                    {
                        case 'r':
                            state = retur_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: retur
                     */

                case retur_state:
                    switch (nextchar())
                    {
                        case 'n':
                            state = return_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: return
                     */

                case return_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return RETURN_TOKEN;
                    }

                    /*
                     * prefix: s
                     */

                case s_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = se_state;
                            continue;
                        case 't':
                            state = st_state;
                            continue;
                        case 'u':
                            state = su_state;
                            continue;
                        case 'w':
                            state = sw_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: se
                     */

                case se_state:
                    switch (nextchar())
                    {
                        case 't':
                            state = set_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: get
                     */

                case set_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return SET_TOKEN;
                    }

                    /*
                     * prefix: sh
                     */

                case sh_state:
                    switch (nextchar())
                    {
                        case 'o':
                            state = sho_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: sho
                     */

                case sho_state:
                    switch (nextchar())
                    {
                        case 'r':
                            state = shor_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: shor
                     */

                case shor_state:
                    switch (nextchar())
                    {
                        case 't':
                            state = short_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: short
                     */

                case short_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return makeTokenInstance(IDENTIFIER_TOKEN, input.copy());
                    }

                    /*
                     * prefix: st
                     */

                case st_state:
                    switch (nextchar())
                    {
                        case 'a':
                            state = sta_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: sta
                     */

                case sta_state:
                    switch (nextchar())
                    {
                        case 't':
                            state = stat_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: stat
                     */

                case stat_state:
                    switch (nextchar())
                    {
                        case 'i':
                            state = stati_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: stati
                     */

                case stati_state:
                    switch (nextchar())
                    {
                        case 'c':
                            state = static_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: static
                     */

                case static_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return makeTokenInstance(IDENTIFIER_TOKEN, input.copy());
                    }

                    /*
                     * prefix: su
                     */

                case su_state:
                    switch (nextchar())
                    {
                        case 'p':
                            state = sup_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: sup
                     */

                case sup_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = supe_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: supe
                     */

                case supe_state:
                    switch (nextchar())
                    {
                        case 'r':
                            state = super_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: super
                     */

                case super_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return SUPER_TOKEN;
                    }

                    /*
                     * prefix: sw
                     */

                case sw_state:
                    switch (nextchar())
                    {
                        case 'i':
                            state = swi_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: swi
                     */

                case swi_state:
                    switch (nextchar())
                    {
                        case 't':
                            state = swit_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: swit
                     */

                case swit_state:
                    switch (nextchar())
                    {
                        case 'c':
                            state = switc_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: switc
                     */

                case switc_state:
                    switch (nextchar())
                    {
                        case 'h':
                            state = switch_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: switch
                     */

                case switch_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return SWITCH_TOKEN;
                    }

                    /*
                     * prefix: t
                     */

                case t_state:
                    switch (nextchar())
                    {
                        case 'h':
                            state = th_state;
                            continue;
                        case 'r':
                            state = tr_state;
                            continue;
                        case 'y':
                            state = ty_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: th
                     */

                case th_state:
                    switch (nextchar())
                    {
                        case 'i':
                            state = thi_state;
                            continue;
                        case 'r':
                            state = thr_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: thi
                     */

                case thi_state:
                    switch (nextchar())
                    {
                        case 's':
                            state = this_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: this
                     */

                case this_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return THIS_TOKEN;
                    }

                    /*
                     * prefix: thr
                     */

                case thr_state:
                    switch (nextchar())
                    {
                        case 'o':
                            state = thro_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: thro
                     */

                case thro_state:
                    switch (nextchar())
                    {
                        case 'w':
                            state = throw_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: throw
                     */

                case throw_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return THROW_TOKEN;
                    }

                    /*
                     * prefix: tr
                     */

                case tr_state:
                    switch (nextchar())
                    {
                        case 'u':
                            state = tru_state;
                            continue;
                        case 'y':
                            state = try_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: tru
                     */

                case tru_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = true_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: true
                     */

                case true_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return TRUE_TOKEN;
                    }

                    /*
                     * prefix: try
                     */

                case try_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return TRY_TOKEN;
                    }

                    /*
                     * prefix: ty
                     */

                case ty_state:
                    switch (nextchar())
                    {
                        case 'p':
                            state = typ_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: typ
                     */

                case typ_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = type_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: type
                     */

                case type_state:
                    switch (nextchar())
                    {
                        case 'o':
                            state = typeo_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: typeo
                     */

                case typeo_state:
                    switch (nextchar())
                    {
                        case 'f':
                            state = typeof_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: typeof
                     */

                case typeof_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return TYPEOF_TOKEN;
                    }

                    /*
                     * prefix: u
                     */

                case u_state:
                    switch (nextchar())
                    {
                        case 's':
                            state = us_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: us
                     */

                case us_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = use_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: use
                     */

                case use_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return USE_TOKEN;
                    }

                    /*
                     * prefix: v
                     */

                case v_state:
                    switch (nextchar())
                    {
                        case 'a':
                            state = va_state;
                            continue;
                        case 'o':
                            state = vo_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: va
                     */

                case va_state:
                    switch (nextchar())
                    {
                        case 'r':
                            state = var_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: var
                     */

                case var_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return VAR_TOKEN;
                    }

                    /*
                     * prefix: vo
                     */

                case vo_state:
                    switch (nextchar())
                    {
                        case 'i':
                            state = voi_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: voi
                     */

                case voi_state:
                    switch (nextchar())
                    {
                        case 'd':
                            state = void_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: void
                     */

                case void_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return VOID_TOKEN;
                    }

                    /*
                     * prefix: w
                     */

                case w_state:
                    switch (nextchar())
                    {
                        case 'h':
                            state = wh_state;
                            continue;
                        case 'i':
                            state = wi_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: wh
                     */

                case wh_state:
                    switch (nextchar())
                    {
                        case 'i':
                            state = whi_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: whi
                     */

                case whi_state:
                    switch (nextchar())
                    {
                        case 'l':
                            state = whil_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: whil
                     */

                case whil_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = while_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: while
                     */

                case while_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return WHILE_TOKEN;
                    }

                    /*
                     * prefix: wi
                     */

                case wi_state:
                    switch (nextchar())
                    {
                        case 't':
                            state = wit_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: wit
                     */

                case wit_state:
                    switch (nextchar())
                    {
                        case 'h':
                            state = with_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: with
                     */

                case with_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = A_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return WITH_TOKEN;
                    }


                    /*
                     * prefix: x
                     */

                    case x_state:
                    switch ( nextchar() )
                    {
                        case 'm': state = xm_state; continue;
                        default:  retract(); state = A_state; continue;
                    }

                        /*
                        * prefix: xm
                        */

                    case xm_state:
                    switch ( nextchar() )
                    {
                        case 'l': state = xml_state; continue;
                        default:  retract(); state = A_state; continue;
                    }

                        /*
                        * prefix: xml
                        */

                    case xml_state:
                    switch ( nextchar() )
                    {
                        case 'A': case 'a': case 'B': case 'b': case 'C': case 'c': 
                        case 'D': case 'd': case 'E': case 'e': case 'F': case 'f':
                        case 'G': case 'g': case 'H': case 'h': case 'I': case 'i': 
                        case 'J': case 'j': case 'K': case 'k': case 'L': case 'l': 
                        case 'M': case 'm': case 'N': case 'n': case 'O': case 'o':
                        case 'P': case 'p': case 'Q': case 'q': case 'R': case 'r': 
                        case 'S': case 's': case 'T': case 't': case 'U': case 'u': 
                        case 'V': case 'v': case 'W': case 'w': case 'X': case 'x': 
                        case 'Y': case 'y': case 'Z': case 'z': case '$': case '_':
                        case '0': case '1': case '2': case '3': case '4': case '5': 
                        case '6': case '7': case '8': case '9':
                            state = A_state; continue;
                        default: retract(); state = start_state; return makeTokenInstance( IDENTIFIER_TOKEN, input.copy() );
                    }

                    /*
                     * prefix: #
                     */

                case pound_state:
                    switch (nextchar())
                    {
                        case 'i':
                            state = pound_i_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: #i
                     */

                case pound_i_state:
                    switch (nextchar())
                    {
                        case 'n':
                            state = pound_in_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: #in
                     */

                case pound_in_state:
                    switch (nextchar())
                    {
                        case 'c':
                            state = pound_inc_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: #inc
                     */

                case pound_inc_state:
                    switch (nextchar())
                    {
                        case 'l':
                            state = pound_incl_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: #incl
                     */

                case pound_incl_state:
                    switch (nextchar())
                    {
                        case 'u':
                            state = pound_inclu_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: #inclu
                     */

                case pound_inclu_state:
                    switch (nextchar())
                    {
                        case 'd':
                            state = pound_includ_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: #includ
                     */

                case pound_includ_state:
                    switch (nextchar())
                    {
                        case 'e':
                            state = pound_include_state;
                            continue;
                        default:
                            retract();
                            state = A_state;
                            continue;
                    }

                    /*
                     * prefix: #include
                     */

                case pound_include_state:
                    switch (nextchar())
                    {
                        case 'A':
                        case 'a':
                        case 'B':
                        case 'b':
                        case 'C':
                        case 'c':
                        case 'D':
                        case 'd':
                        case 'E':
                        case 'e':
                        case 'F':
                        case 'f':
                        case 'G':
                        case 'g':
                        case 'H':
                        case 'h':
                        case 'I':
                        case 'i':
                        case 'J':
                        case 'j':
                        case 'K':
                        case 'k':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'N':
                        case 'n':
                        case 'O':
                        case 'o':
                        case 'P':
                        case 'p':
                        case 'Q':
                        case 'q':
                        case 'R':
                        case 'r':
                        case 'S':
                        case 's':
                        case 'T':
                        case 't':
                        case 'U':
                        case 'u':
                        case 'V':
                        case 'v':
                        case 'W':
                        case 'w':
                        case 'X':
                        case 'x':
                        case 'Y':
                        case 'y':
                        case 'Z':
                        case 'z':
                        case '$':
                        case '_':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            state = error_state;
                            continue;
                        default:
                            if( isNextIdentifierPart() )
                            {
                                state = A_state;
                                continue;
                            }
                            retract();
                            state = start_state;
                            return INCLUDE_TOKEN;
                    }

                /*
                * prefix: /*
                */

                case blockcommentstart_state:
                {
                    int c = nextchar();
                    blockcommentbuf.append(c);
                    switch ( c )
                    {
                        case '*':
                            switch(nextchar())
                            {
                                case '/':
                                {
                                    state = start_state;
                                    return makeTokenInstance( BLOCKCOMMENT_TOKEN, new String());
                                }
                                default: retract(); state = doccomment_state; continue;
                            }
                        case '\n': isFirstTokenOnLine = true; 
                            state = blockcomment_state; continue;
                        case 0:    error(kError_BlockCommentNotTerminated); state = start_state; continue;
                        default:   state = blockcomment_state; continue;
                    }
                }

                /*
                * prefix: /**
                */

                case doccomment_state:
                    {
                    int c = nextchar();
                    blockcommentbuf.append(c);
                    switch ( c )
                    {
                        case '*':  state = doccommentstar_state; continue;
                        case '@':
                            if (doctextbuf == null) doctextbuf = getDocTextBuffer(doctagname);
                            if( doctagname.length() > 0 ) { doctextbuf.append("]]></").append(doctagname).append(">"); };
                            doctagname = "";
                            state = doccommenttag_state; continue;
                        case '\n': isFirstTokenOnLine = true;
                            if (doctextbuf == null) doctextbuf = getDocTextBuffer(doctagname);
                            doctextbuf.append('\n');
                            state = doccomment_state; continue;
                        case 0:    error(kError_BlockCommentNotTerminated); state = start_state; continue;
                        default:
                            if (doctextbuf == null) doctextbuf = getDocTextBuffer(doctagname);
                            doctextbuf.append((char)(c)); state = doccomment_state; continue;
                    }
                }

                case doccommentstar_state:
                {
                    int c = nextchar();
                    blockcommentbuf.append(c);
                    switch ( c )                    
                    {
                        case '/':
                            {
                            if (doctextbuf == null) doctextbuf = getDocTextBuffer(doctagname);
                            if( doctagname.length() > 0 ) { doctextbuf.append("]]></").append(doctagname).append(">"); };
                            String doctext = doctextbuf.toString();
                            state = start_state; return makeTokenInstance(DOCCOMMENT_TOKEN,doctext);
                        }
                        case '*':  state = doccommentstar_state; continue;
                        case 0:    error(kError_BlockCommentNotTerminated); state = start_state; continue;
                        default:   state = doccomment_state; continue;
                            // if not a slash, then keep looking for an end comment.
                    }
                }

                /*
                * prefix: @
                */

                case doccommenttag_state:
                    {
                    int c = nextchar();
                    switch ( c )
                    {
                        case '*':  state = doccommentstar_state; continue;
                        case ' ': case '\n': 
                            {
                            if (doctextbuf == null) doctextbuf = getDocTextBuffer(doctagname);
                            if( doctagname.length() > 0 ) { doctextbuf.append("\n<").append(doctagname).append("><![CDATA["); };
                            state = doccomment_state; continue;
                        }
                        case 0:    error(kError_BlockCommentNotTerminated); state = start_state; continue;
                        default:   doctagname += (char)(c); state = doccommenttag_state; continue;
                    }
                }

                /*
                * prefix: /**
                */

                case doccommentvalue_state:
                switch ( nextchar() )
                {
                    case '*':  state = doccommentstar_state; continue;
                    case '@':  state = doccommenttag_state; continue;
                    case '\n': state = doccomment_state; continue;
                    case 0:    error(kError_BlockCommentNotTerminated); state = start_state; continue;
                    default:   state = doccomment_state; continue;
                }

                /*
                * prefix: /*
                */

                case blockcomment_state:
                {
                    int c = nextchar();
                    blockcommentbuf.append(c);
                    switch ( c )                    
                    {
                        case '*':  state = blockcommentstar_state; continue;
                        case '\n': isFirstTokenOnLine = true; 
                            state = blockcomment_state; continue;
                        case 0:    error(kError_BlockCommentNotTerminated); state = start_state; continue;
                        default:   state = blockcomment_state; continue;
                    }
                }

                case blockcommentstar_state:
                {
                    int c = nextchar();
                    blockcommentbuf.append(c);
                    switch ( c )
                    {
                        case '/':  
                        {
                            state = start_state;
                            String blocktext = blockcommentbuf.toString();

                            return makeTokenInstance( BLOCKCOMMENT_TOKEN, blocktext );
                        }
                        case '*':  state = blockcommentstar_state; continue;
                        case 0:    error(kError_BlockCommentNotTerminated); state = start_state; continue;
                        default:   state = blockcomment_state; continue;
                            // if not a slash, then keep looking for an end comment.
                    }
                }

                /*
                * prefix: // <comment chars>
                */

                case linecomment_state:
                switch ( nextchar() )
                {
                    case '\n': // don't include newline in line comment. (Sec 7.3)
                        retract(); 
                        state = start_state;
                        return makeTokenInstance( SLASHSLASHCOMMENT_TOKEN, input.copy() );
 
                    case 0:    state = start_state; return EOS_TOKEN;
                    default:   state = linecomment_state; continue;
                }

                /*
                * utf8sigstart_state
                */

                case utf8sig_state:
                    switch (nextchar())
                    {
                        case (char) 0xffffffbb:
                            {
                                switch (nextchar())
                                {
                                    case (char) 0xffffffbf:
                                        // ISSUE: set encoding scheme to utf-8, and implement support for utf8
                                        state = start_state;
                                        continue; // and contine
                                }
                            }
                    }
                    state = error_state;
                    continue;

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

    private boolean isNextIdentifierPart()
    {
        switch (input.classOfNext())
        {
            case Lu:
            case Ll:
            case Lt:
            case Lm:
            case Lo:
            case Nl:
            case Mn:
            case Mc:
            case Nd:
            case Pc:
                return true;
            default:
                return false;
        }
    }

    public void clearUnusedBuffers() {
        input.clearUnusedBuffers();
        input = null;
    }
}
