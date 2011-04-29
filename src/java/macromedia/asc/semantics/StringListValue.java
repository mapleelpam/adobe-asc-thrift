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

package macromedia.asc.semantics;

import macromedia.asc.util.Context;
import macromedia.asc.util.IntList;
import macromedia.asc.util.Names;
import macromedia.asc.util.Namespaces;
import macromedia.asc.util.ObjectList;
import macromedia.asc.util.Qualifiers;
import macromedia.asc.util.Slots;
import macromedia.asc.util.NumberUsage;
import macromedia.asc.parser.ClassDefinitionNode;
import macromedia.asc.parser.Node;
import macromedia.asc.parser.Tokens;
import macromedia.asc.embedding.avmplus.ClassBuilder;
import macromedia.asc.embedding.avmplus.InstanceBuilder;

import java.util.HashMap;
import java.util.Comparator;

/*
 * This class is the building block for all ECMA values. Object values
 * are a sequence of instances linked through the [[Prototype]] slot.
 * Classes are a sequence of instances of the sub-class TypeValue, also
 * linked through the [[Prototype]] slot. Lookup of class and instance
 * properties uses the same algorithm to find a name. Names are bound
 * to slots. Slots contain values that are methods for accessing or
 * computing program values (i.e running code.)
 *
 * An object consists of a table of names, and a vector of slots. The
 * slots hold methods for accessing and computing values. An object
 * also has a private data accessible to the methods and accessors of
 * the object.
 *
 * All symbolic references to the properties of an object are bound
 * to the method of a slot. Constant references to fixed, final prop-
 * erties can be compiled to direct access of the data value. This
 * is the case with access to private, final or global variables.
 *
 * The instance and class hierarchies built at compile-time can be
 * compressed into a single instance prototype and class object for
 * faster lookup and dispatch.
 */

import java.util.List;
import java.util.ArrayList;

// add by maple for stupid reason

public class StringListValue extends Value
{
    public StringListValue()
    {
    	values = new ArrayList<String>();
    }    
    
    public List<String>	values;
}
