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

//import macromedia.asc.util.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.*;
import java.nio.*;

import static macromedia.asc.parser.CharacterClasses.*;
import static macromedia.asc.embedding.avmplus.Features.*;

/**
 * InputBuffer.h
 *
 * @author Jeff Dyer
 * 
 * Notes on current restructuring:
 *  This is taking a lot of time. The existing use of this module is complex and fragile,
 *  a lot of the work will be eliminating unnecessary and cumbersome code.
 *  
 *  1. Since the full source text is saved, why not block load in the first place?                  DONE
 *  2. The line map is only used if an error/warning message is done, so why do it unless needed?   DONE
 *  
 *  
 *  3. The strip of special formatting characters, defined in ecma3, should be optional.
 *  4. All the things like line buffers etc. don't seem necessary.                                  GONE
 *  5. Line scan seems unnecessary                                                                  GONE
 *  6. utf-8, utf-16 etc. decode should be clearly done.                                            DONE
 *  7. bom read should be simple                                                                    DONE
 *      But not really simple, needs to be recognized and modify pos correctly.
 *  
 *  8. The scanner should just index the source buffer directly. no read() method needed.
 *      --Cant be done really, not while supporting the stripping of control-chars and keeping pos correct...
 *      
 *  9. Rather than blocking the file in memory, use NIO (mmap)
 */
public class InputBuffer
{
    /**
     * input text, if a fragment, startSourcePos is non-zero
     */
	private final char[] text;
    private int textPos = 0;    // Scanner input cursor, current char + 1
    private int textMarkPos = 0;
    
    /**
     * 0..n array of newline positions found in text.
     */
    private int lineMap[]; 
    
    /**
     * Map to starting source line number for this InputBuffer 
     */
    private int startLineNumber;
    
    /*
     * Map to starting source position for this InputBuffer
     * --Not an offset on the text input buffer.
     */
    private int startSourcePos;
    
    public String origin; // sourcefilename
    public boolean report_pos = true;

    
    
	public InputBuffer(InputStream in, String encoding, String origin)
	{   
		CharBuffer cb = createBuffer(in, encoding);
        text = cb.array();
		init(origin,0,0);
	}

	public InputBuffer(String in, String origin)
	{  
		text = in.toCharArray(); // assumes any encoding required is already done.
		init(origin,0,0);
	}
    
    public InputBuffer(String in, String origin, int startPos, int startLine)
    {  
        text = in.toCharArray(); // assumes any encoding required is already done.
        init(origin,startPos,startLine);
        startSourcePos = startPos;
        startLineNumber = startLine;
    }
    
	/**
	 * No arg constructor for subclasses that aren't InputStream or String based.
	 */
	protected InputBuffer()
	{   
        text = null;
        init(null,0,0);
	}

	private void init(String origin, int startPos, int startLine)
	{
		this.origin = origin;
        startSourcePos = startPos;
        startLineNumber = startLine;
	}

	private CharBuffer createBuffer(InputStream in, String encoding)
	{
        
        // load the input stream into a byte array

// Buffered reads of the input file aren't too important if we're blocking the whole thing in memory
// If we ever         
//        if ( in instanceof FileInputStream )
//        {
//            // if in is a File, we can mmap it.
//        }
//        else {
//        }
  
        int i, len;
        byte [] b;
        ByteBuffer bb;
        
        try {
            i = in.available(); // assumes we can eat the whole file...
            
            if ( i == 0 )
            {
                return CharBuffer.allocate(0);
            }

            b = new byte[i];
            
            // Read the whole file...is there a faster read primitive?
            
            len = in.read(b,0,i);
            
            // ??? Check: is length read as expected?
            if ( len != i )
            {
                assert true:"file read error:"+origin;
            }
            
            // ??? Check: are we at the EOF?
            if ( in.read() != -1 )
            {
                assert true:"file EOF error:"+origin;              
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            return null;
        }
          
        // select the charset decoder
        // According to old code, presence of a byte order mark defines encoding
        // no matter what the user passed in. 
        // ??? Note that a FileInputStream has the bom already marked out...maybe we dont need to do this.
        // ??? I also thought the decoder would strip the bom...it does not.
        
        if (b.length > 3 && b[0] == (byte)0xef && b[1] == (byte)0xbb && b[2] == (byte)0xbf)
        {
            encoding = "UTF8";
            bb = ByteBuffer.wrap(b,3,b.length-3);
        }
        else if (b.length > 3 && b[0] == (byte)0xff && b[1] == (byte)0xfe || b[0] == (byte)0xfe && b[1] == (byte)0xff)
        {
            encoding = "UTF16"; // which seems to ignore the endian mark....
            bb = ByteBuffer.wrap(b,3,b.length-3);
        }
        else 
        {
            if ( encoding == null )
            {
                encoding = "UTF8";              
            }
            bb = ByteBuffer.wrap(b);
        }
        
        Charset cs = null;
        
        try
        {
            cs = Charset.forName(encoding);
        }
        catch (IllegalCharsetNameException ex)
        {
            cs = Charset.defaultCharset(); // ok, try with a default Charset...
        }
        
        // Convert/decode to CharBuffer
        
        CharBuffer cb = cs.decode(bb);
       
        if ( false ) // If some stripping is needed, do it in place.
        {
            trim_format_controls(cb);
        }
        return cb;
	}
    
    
    
     private void buildLineMap(char[] src, int max) 
     {
         int line = 0;
         int pos = 0;
         int[] lb = new int[max+1]; // ???fixme: use a dynamic array, max can be a lot bigger than needed.
         
         while (pos < max) {
             lb[line++] = pos;
             do {
                 char ch = src[pos];
                 if (ch == '\r' || ch == '\n') {
                     if (ch == '\r' && (pos+1) < max && src[pos+1] == '\n')
                         pos += 2;
                     else
                         ++pos;
                     break;
                 }
             } while (++pos < max);
         }
         
         lb[line++] = pos; // fake line at EOF
         
         lineMap = new int[line];
         System.arraycopy(lb, 0, lineMap, 0, line);
     }
 
     private int cachedLastLineMapPos = 0;
     private int cachedLastLineMapIndex = 0;

     /**
      * Binary search for nearest newline, given a position in text, note that lines start at 1
      * Returns 0 if pos is low, max if high
      * @param sourcePos -- file based source position, converted to text[pos]
      * @return line map index
      */
     private int getLineMapIndex(int srcPos) 
     {
         int pos = srcPos - startSourcePos;  // Adjust for file offset (get textPos)
         
         if ( pos < 0 )
             return 0;
         
         if (lineMap == null)
             buildLineMap(text,text.length);
         
         if (pos == cachedLastLineMapPos)    
             return cachedLastLineMapIndex;

         cachedLastLineMapPos = pos;

         int low = 0;
         int high = lineMap.length-1;
         
         while (low <= high) 
         {
             int mid = (low + high) >> 1;
             int midPos = lineMap[mid];

             if ( midPos < pos )
                 low = mid + 1;
             else if ( midPos > pos )
                 high = mid - 1;
             else {
                 cachedLastLineMapIndex = mid+1;                                                                                                                 
                 return cachedLastLineMapIndex;
             }
         }
         cachedLastLineMapIndex = low;
         return cachedLastLineMapIndex;                                                                                                                                         
     }

     public int getLnNum(int srcPos)
     {   
         int l = getLineMapIndex(srcPos);
         if ( l == 0 )
             l = 1;
         return l + startLineNumber;  
     }
     
     private final int getLineStartPos(int srcPos)
     {
         int l = getLineMapIndex(srcPos);
         
         assert l <= lineMap.length : "line number out of range";
         
         if ( l == 0 )
             l = 1;
         
         return lineMap[l-1];
     }
     
     /**
      * ecma standard says strip the following characters
      * --This is off for now, until we find a user that wants it.
      * --when we do, it'll get enabled according to an --strict-ecma flag or something.
      * --FIXME -- this needs to be implemented as a screener in the nextchar/retract methods to keep filepos references correct.
      * @param cb - the source text
      */
     
     private void trim_format_controls(CharBuffer cb)
     {
         char[] buf = cb.array();
         cb.flip();
         
         int len = cb.remaining();
         char c;
         int i,j;

         for ( i = j = 0; j < len;)
         {
             c = buf[j];
         
             // Skip Unicode 3.0 format-control (general category Cf in
             // Unicode Character Database) characters.

             switch (c)
             {
             case 0x070f: // SYRIAC ABBREVIATION MARK
             case 0x180b: // MONGOLIAN FREE VARIATION SELECTOR ONE
             case 0x180c: // MONGOLIAN FREE VARIATION SELECTOR TWO
             case 0x180d: // MONGOLIAN FREE VARIATION SELECTOR
                 // THREE
             case 0x180e: // MONGOLIAN VOWEL SEPARATOR
             case 0x200c: // ZERO WIDTH NON-JOINER
             case 0x200d: // ZERO WIDTH JOINER
             case 0x200e: // LEFT-TO-RIGHT MARK
             case 0x200f: // RIGHT-TO-LEFT MARK
             case 0x202a: // LEFT-TO-RIGHT EMBEDDING
             case 0x202b: // RIGHT-TO-LEFT EMBEDDING
             case 0x202c: // POP DIRECTIONAL FORMATTING
             case 0x202d: // LEFT-TO-RIGHT OVERRIDE
             case 0x202e: // RIGHT-TO-LEFT OVERRIDE
             case 0x206a: // INHIBIT SYMMETRIC SWAPPING
             case 0x206b: // ACTIVATE SYMMETRIC SWAPPING
             case 0x206c: // INHIBIT ARABIC FORM SHAPING
             case 0x206d: // ACTIVATE ARABIC FORM SHAPING
             case 0x206e: // NATIONAL DIGIT SHAPES
             case 0x206f: // NOMINAL DIGIT SHAPES
             case 0xfeff: // ZERO WIDTH NO-BREAK SPACE
             case 0xfff9: // INTERLINEAR ANNOTATION ANCHOR
             case 0xfffa: // INTERLINEAR ANNOTATION SEPARATOR
             case 0xfffb: // INTERLINEAR ANNOTATION TERMINATOR
                 j++;
                 break; // skip it.
             default:
                 if ( i != j )
                     buf[i] = c;
                 i++;
                 j++;
                 break;
             }
         }
         
         while ( j > i )
              buf[i++] = 0;
     }


    /**
     * Scanner position mark
      * Scanner only.
     */

    public int textMark()
    {
         textMarkPos = textPos-1;
         return textMarkPos;
    }
    
    /**
     * Scanner text pos
     * Scanner only
     */
    
    public int textPos()
    {
        return textPos;
    }
    
	/**
	 * nextchar, advance pos
	 */

	public int nextchar()
	{
		int c;
        
        if ( textPos >= text.length){
            textPos = text.length + 1;
            return 0;
        }
        
		c = text[textPos++];
		return c;
	}
	
	/**
	 * Backup one character position in the input. 
	 */

	public void retract()
	{
        if ( textPos > 0 )
            textPos--;
	}

	/*
	 * classOfNext
	 */

    // utilities used by classOfNext
    final private int unHex(char c)
    {
       return Character.digit(c,16);
    }

    final private boolean isHex(char c)
    {
        return Character.digit(c,16) != -1;
    }

	// utility for java branch only to convert a Character.getType() unicode type specifier to one of the enums
	//  defined in CharacterClasses.java.  In the c++ branch, we use the character as an index to a table defined
	//  in CharacterClasses.h
	final private char javaTypeOfToCharacterClass(int javaClassOf)
	{
		switch(javaClassOf)
		{
			case Character.UPPERCASE_LETTER:		    return Lu;	// = 0x01 Letter, Uppercase
			case Character.LOWERCASE_LETTER:		    return Ll;	// = 0x02 Letter, Lowercase
			case Character.TITLECASE_LETTER:		    return Lt;	// = 0x03 Letter, Titlecase
			case Character.NON_SPACING_MARK:		    return Mn;	// = 0x04 Mark, Non-Spacing
			case Character.COMBINING_SPACING_MARK:	    return Mc;	// = 0x05 Mark, Spacing Combining
			case Character.ENCLOSING_MARK:			    return Me;	// = 0x06 Mark, Enclosing
			case Character.DECIMAL_DIGIT_NUMBER:	    return Nd;	// = 0x07 Number, Decimal Digit
			case Character.LETTER_NUMBER:			    return Nl;	// = 0x08 Number, Letter
			case Character.OTHER_NUMBER:			    return No;	// = 0x09 Number, Other
			case Character.SPACE_SEPARATOR:			    return Zs;	// = 0x0a Separator, Space
			case Character.LINE_SEPARATOR:			    return Zl;	// = 0x0b Separator, Line
			case Character.PARAGRAPH_SEPARATOR:		    return Zp;	// = 0x0c Separator, Paragraph
			case Character.CONTROL:                     return Cc;	// = 0x0d Other, Control
			case Character.FORMAT:                      return Cf;	// = 0x0e Other, Format
			case Character.SURROGATE:                   return Cs;	// = 0x0f Other, Surrogate
			case Character.PRIVATE_USE:                 return Co;	// = 0x10 Other, Private Use
			case Character.UNASSIGNED:                  return Cn;	// = 0x11 Other, Not Assigned (no characters in the file have this property)

				// Non-normative classes.
			case Character.MODIFIER_LETTER:			    return Lm;	// = 0x12 Letter, Modifier
			case Character.OTHER_LETTER:			    return Lo;	// = 0x13 Letter, Other
			case Character.CONNECTOR_PUNCTUATION:       return Pc;	// = 0x14 Punctuation, Connector
			case Character.DASH_PUNCTUATION:		    return Pd;	// = 0x15 Punctuation, Dash
			case Character.START_PUNCTUATION:		    return Ps;	// = 0x16 Punctuation, Open
			case Character.END_PUNCTUATION:			    return Pe;	// = 0x17 Punctuation, Close
			case Character.INITIAL_QUOTE_PUNCTUATION:	return Pi;	// = 0x18 Punctuation, Initial quote (may behave like Ps or Pe depending on usage)
			case Character.FINAL_QUOTE_PUNCTUATION:		return Pf;	// = 0x19 Punctuation, Final quote (may behave like Ps or Pe depending on usage)
			case Character.OTHER_PUNCTUATION:           return Po;	// = 0x1a Punctuation, Other
			case Character.MATH_SYMBOL:                 return Sm;	// = 0x1b Symbol, Math
			case Character.CURRENCY_SYMBOL:             return Sc;	// = 0x1c Symbol, Currency
			case Character.MODIFIER_SYMBOL:             return Sk;	// = 0x1d Symbol, Modifier
			case Character.OTHER_SYMBOL:                return So;	// = 0x1e Symbol, Other
			
			default: // DIRECTIONALITY_LEFT_TO_RIGHT, DIRECTIONALITY_RIGHT_TO_LEFT, DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC, etc.
				// DIRECTIONALITY_EUROPEAN_NUMBER, etc.
				return Cn; // or So ?
		}
	}

    /**
     * Needs work, note that this method advances the input cursor 'textPos'
     * @return the Unicode character class of the current
     * character
     */
    
	public char classOfNext()
	{
        char c;
        
        c = text[textPos-1];

        if( c == '\\' && text[textPos] == 'u' )
        {
            char c1,c2,c3,c4;
            c1 = text[textPos+1];
            c2 = text[textPos+2];
            c3 = text[textPos+3];
            c4 = text[textPos+4];
            if ( isHex(c1) & isHex(c2) & isHex(c3) & isHex(c4) ){
                
                /*  This is a jdk 1.5 only feature.  However, we only support 16bit chars, so its
                *    o.k. to cast to (char) instead
                * char[] ca = Character.toChars(ic);
                * c = ca[0];
                */
                
                int ic = (((((unHex(c1)  << 4) + unHex(c2)) << 4) + unHex(c3)) << 4) + unHex(c4);
                c = (char) ic;
                textPos += 5; // ??? advances textPos
            }  
        }
        return javaTypeOfToCharacterClass(Character.getType(c));
	}

	/**
	 * positionOfNext: returns *source* relative character position
     * NOT for scanner use, used by node/token position mapping
	 */

	public int positionOfNext()
	{
		return textPos + startSourcePos;
	}

	/** 
     * positionOfMark: returns *source* relative mark position
     * NOT for scanner use, used by node/token position mapping
     * @return mark
	 */

	public int positionOfMark()
	{
        if( !report_pos )
            return -1;

		// This may happen with abc imports
        
        if ( textMarkPos == -1 )
            return textMarkPos + startSourcePos;
        
        return textMarkPos + startSourcePos;
	}

	/*
	 * copy
	 */

    /**
     * Needs work: should be a general scan utility function.
     * Copies a string from index <from> to <to>, interpreting escape characters
  	 */
	private String escapeString(char[] src, int from, int to)
	{
		// C: only 1 string in 1000 needs escaping and the lengths of these strings are usually small,
		//    so we can cut StringBuilder usage if we check '\\' up front.
           
		int len = 1+to-from;
		boolean required = false;

		for (int i = from; i <= to; i++)
		{
			if (src[i] == '\\')
			{
				required = true;
				break;
			}
		}
		if (!required)
		{
            return String.copyValueOf(src,from,len);
		}

        final StringBuilder buf = new StringBuilder(len);
        
		for (int i = from; i <= to; i++)
		{
			char c = src[i];
			if (c == '\\')
			{
				int c2 = src[i + 1];
				switch (c2)
				{
					case '\'':
					case '\"':
						continue;
                        
                    // strip escaped newlines    
                    case '\r':
                        if ( src[i+2] == '\n' )
                        {
                            i++;
                        }
                    case '\n':
                        i++;
                        continue;
						
					case '\\': // escaped escape char
						c = '\\';
                        ++i;
                        break;

					case 'u': // Token constructor will handle all embedded backslash u characters, within a string or not
                    {
                        int thisChar = 0;
                        int y, digit;
                        // calculate numeric value, bail if invalid
                        for( y=i+2; y<i+6 && y < to+1; y++ )
                        {
                            digit = Character.digit( src[y],16 );
                            if (digit == -1)
                                break;
                            thisChar = (thisChar << 4) + digit;
                        }
                        if ( y != i+6 || Character.isDefined((char)thisChar) == false )  // if there was a problem or the char is invalid just escape the '\''u' with 'u'
                        {
                            c = src[++i];
                        }
                        else // use Character class to convert unicode codePoint into a char ( note, this will handle a wider set of unicode codepoints than the c++ impl does).
                        {
                            // jdk 1.5.2 only, but handles extended chars:  char[] ca = Character.toChars(thisChar);
                            c = (char)thisChar;
                            i += 5;
                        }
                        break;
                    }
					default:
				    {
						if (PASS_ESCAPES_TO_BACKEND)
						{
							c = src[++i];
							break; // else, unescape the unrecognized escape char
						}
	                    
						switch (c2)
						{
							case 'b':
								c = '\b';
								++i;
								break;
							case 'f':
								c = '\f';
								++i;
								break;
							case 'n':
								c = '\n';
								++i;
								break;
							case 'r':
								c = '\r';
								++i;
								break;
							case 't':
								c = '\t';
								++i;
								break;
							case 'v':
								// C: There is no \v in Java...
								c = 0xb;
								++i;
								break;
							case 'x':
							{
								if ( i+3 < to && isHex(src[i+2]) && isHex(src[i+3]))
								{
									c = (char) ((unHex(src[i+2]) << 4) + unHex(src[i+3]));
									i += 3;
									/*  Character.toChars is a jdk 1.5 only feature.  However, we only support 16bit chars, so its
									 *    o.k. to cast to (char) instead
									 * char[] ca = Character.toChars(ic);
									 * c = ca[0];
									 */
								}
								else // invalid number, just skip the '\' escape char
								{
									i++;
									c = 'x';
								}
                                break;
							} // end case 'x'

							default:
								c = src[++i];
								break; // else, unescape the unrecognized escape char

						} // end switch
					}
				} // end switch
			}
			buf.append(c);
		}
		return buf.toString();
	}

	/** 
     * Copies interpreting escaped characters
     * @return String
     */
    
	public String copy()
	{
        assert textMarkPos >= 0 && textPos > textMarkPos : "copy(): negative length copy textMarkPos =" + textMarkPos + " textPos = "+textPos + "text.length = " + text.length;
        
		return escapeString(text, textMarkPos, textPos-1);
	}
    
    public String copy(boolean needs_escape)
    {
        assert textMarkPos >= 0 && textPos > textMarkPos : "copy(boolean): negative length copy textMarkPos =" + textMarkPos + " textPos = "+textPos;
        
        if ( needs_escape )
            return escapeString(text, textMarkPos, textPos-1);
        return String.copyValueOf(text,textMarkPos,textPos-textMarkPos);
    }
    
    public String copy(int begin, int end)
    {
        int len = (end-begin) + 1;
        
        assert ( len > 0);
        
        return String.copyValueOf(text, begin, len);
    }
    
    // ??? the following two methods are for temporary experimentation with reserved word lookup in Scanner
    
    public char markCharAt(int offset)
    {
        return text[textMarkPos-offset]; //???looks wrong...markPos+offset 
    }
    
    public int markLength()
    {
        return textPos - textMarkPos; // assumes pos is +1
    }

    /**
     * FIXME: called from one random method way off somewhere else.
     * This method should be deleted.
     * @return should return a source reference.
     */
    public int getCurrentPos()
    {
            return textPos + startSourcePos;
    }
    
    public int getColPos(int srcPos)
	{
        int start = getLineStartPos(srcPos);
		return (srcPos-start)+1; // 0..n + 1 because columns start at 1, unlike array indexes....
	}

    /*
     * The text gathered here can has tabs stripped so error pointer lines up?
     */
    
    public String getLineText(int srcPos)
    {
        int i, start;
        final StringBuilder buf = new StringBuilder(128);
        
        start = getLineStartPos(srcPos);

        for (i = start; i < text.length; i++ )
        {
            char c = text[i];
            
            if ( c == '\n' || c == '\r' || c == 0x00 || c == 0x2028 || c == 0x2029 )
                break;
            
            // Turn the following on to get linepointer ....^ to line up
            //if ( c == '\t' )
            //    c = ' ';
            buf.append(c); 
        }
        return buf.toString();
    }
    
    /**
     * This method returns an error pointer string drawing ...^ at the proper column.
     * Since it's static, we can't assume we have any source text to work with.
     * This implies that source text display (in get LineText) must be massaged to replace all tabs
     * with spaces.
     * If you scan thru the source text (and dont have to emit ....) you could use the original formatting characters
     * to line up the error pointer.
     */
    
	public static String getLinePointer(int col)
	{
 
		final StringBuilder padding = new StringBuilder(1+col);
		for (int i = 0; i < col-1; i++)
		{
			padding.append(".");
		}
		padding.append("^");
		return padding.toString();
	}

	public void clearUnusedBuffers() 
	{
	}
}

