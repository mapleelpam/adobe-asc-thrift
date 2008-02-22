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

import macromedia.asc.semantics.*;
import macromedia.asc.util.*;

/**
 * Node
 *
 * @author Jeff Dyer
 */
public class IdentifierNode extends Node
{
	public String name;
    public ReferenceValue ref;

	public IdentifierNode(String name, int pos)
	{
		super(pos);
		this.name = name.intern();
        
        if (name.equals("*"))
        {
            setAny(true);
        }
    }

	int authOrigTypeToken = -1;
	
	public void setOrigTypeToken(int token){
		authOrigTypeToken = token;
	}
	public int getOrigTypeToken(){
		return authOrigTypeToken;
	}
	
	public Value evaluate(Context cx, Evaluator evaluator)
	{
		if (evaluator.checkFeature(cx, this))
		{
			return evaluator.evaluate(cx, this);
		}
		else
		{
			return null;
		}
	}

    public boolean isAttribute()
    {
        return true;
    }
    
    public boolean isIdentifier()
	{
		return true;
	}

	public boolean hasAttribute(String name)
	{
		if (this.name.equals(name))
		{
			return true;
		}
		return false;
	}

    public String toString()
    {
      if(Node.useDebugToStrings)
         return "Identifier@" + pos() + (name != null ? ": " + name.toString() : "");
      else
         return "Identifier";
    }

    public String toIdentifierString()
    {
        return name;
    }

	public void setAttr(boolean is_attr)
	{
		flags = is_attr ? (flags|IS_ATTR_FLAG) : (flags&~IS_ATTR_FLAG);
	}

	public boolean isAttr()
	{
		return (flags&IS_ATTR_FLAG)!=0;
	}

	public void setAny(boolean is_any)
	{
		flags = is_any ? (flags|IS_ANY_FLAG) : (flags&~IS_ANY_FLAG);
	}

	public boolean isAny()
	{
		return (flags&IS_ANY_FLAG)!=0;
	}
	
	public boolean isLValue()
	{
		return true;
	}
}
