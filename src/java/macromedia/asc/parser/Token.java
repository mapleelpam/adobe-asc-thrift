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

import static macromedia.asc.parser.Tokens.*;

/**
 * Represents token instances: literals and identifiers.
 *
 * This file implements the class Token that is used to carry
 * information from the Scanner to the Parser.
 *
 * @author Jeff Dyer
 */

public final class Token
{
	private int tokenClass;
	private String lexeme;

	public Token(int tokenClass, String lexeme)
	{
        // InputBuffer's escapeString handles all escapes in a string, including u's
		// RegExp literals should not escape the u's either
        if (tokenClass != STRINGLITERAL_TOKEN && lexeme.indexOf("\\u") != -1) // contains a unicode escape char?
        {
            StringBuffer buffer = new StringBuffer();
            int len = lexeme.length();
            for(int x =0; x < len; x++)
            {
                if ( x+2<len && lexeme.charAt(x) == '\\' && lexeme.charAt(x+1) == 'u' )
                {
                    if (x!= 0 && lexeme.charAt(x-1) == '\\')  // watch out for '\\u'
                    {
                        buffer.append(lexeme.charAt(x));
                        continue;
                    }
                    int thisChar = 0;
                    int y, digit;
                    // calculate numeric value, bail if invalid
                    for( y=x+2; y<x+6 && y < len; y++ )
                    {
                        digit = Character.digit( lexeme.charAt(y),16 );
                        if (digit == -1)
                            break;
                        thisChar = (thisChar << 4) + digit;
                    }
                    if ( y != x+6 || Character.isDefined((char)thisChar) == false )  // if there was a problem or the char is invalid just escape the '\''u' with 'u'
                    {
                        buffer.append(lexeme.charAt(++x));
                    }
                    else // use Character class to convert unicode codePoint into a char ( note, this will handle a wider set of unicode codepoints than the c++ impl does).
                    {
                        // jdk 1.5.2 only, but handles extended chars:  char[] ca = Character.toChars(thisChar);
                        char c = (char)thisChar;
                        buffer.append(c);
                        x += 5;
                    }
                }
                else
                {
                    buffer.append(lexeme.charAt(x));
                }
            }
		    this.tokenClass = tokenClass;
		    this.lexeme = buffer.toString();
        }

        else
        {
            this.tokenClass = tokenClass;
            this.lexeme = lexeme;
        }
	}

	public int getTokenClass()
	{
		return tokenClass;
	}

	/*
	 * Return a copy of the token text string. Caller deletes it.
	 */

	public String getTokenText()
	{
		if (tokenClass == STRINGLITERAL_TOKEN)
		{
			return (lexeme.length() == 1) ? "" : lexeme.substring(1, lexeme.length() - 1);
		}

		return lexeme;
	}

	public String getTokenSource()
	{
		return lexeme;
	}

	public static String getTokenClassName(int token_class)
	{
        // etierney 8/11/06 - don't move this calculation inline in the array access, JRockit doesn't like
        // it and crashes on 64 bit linux.  Doing the calculation and assigning the result to a temporary variable
        // doesn't crash though.  Go figure.  
        int temp = -1 * token_class;
		return tokenClassNames[temp];
	}

	public boolean equals(Object obj)
	{
		return tokenClass == ((Token) obj).tokenClass && lexeme.equals(((Token) obj).lexeme);
	}
}
