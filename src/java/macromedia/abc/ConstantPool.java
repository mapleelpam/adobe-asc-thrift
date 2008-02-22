////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2005-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package macromedia.abc;

import macromedia.asc.util.IntegerPool;
import static macromedia.asc.embedding.avmplus.ActionBlockConstants.*;

import java.io.OutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Constant {
 * U8 kind
 * union {
 * kind=1 { // CONSTANT_utf8
 * U16 length
 * U8[length]
 * }
 * kind=3 { // CONSTANT_Integer
 * S32 value
 * }
 * kind=4 { // CONSTANT_UInteger
 * U32 value
 * }
 * kind=6 { // CONSTANT_Double
 * U64 doublebits (little endian)
 * }
 * kind=7,13 { // CONSTANT_Qname + CONSTANT_QnameA
 * U16 namespace_index			// CONSTANT_Namespace, 0=AnyNamespace wildcard
 * U16 name_index					// CONSTANT_Utf8, 0=AnyName wildcard
 * }
 * kind=8,5 { // CONSTANT_Namespace, CONSTANT_PrivateNamespace
 * U16 name_index                    // CONSTANT_Utf8 uri (maybe 0)
 * }
 * kind=9,14 { // CONSTANT_Multiname, CONSTANT_MultinameA
 * U16 name_index                    // CONSTANT_Utf8  simple name.  0=AnyName wildcard
 * U16 namespaces_count              // (256 may seem like enough, but 64K use to seem like a lot of memory)
 * U16 namespaces[namespaces_count]  // CONSTANT_Namespace (0 = error)
 * }
 * kind=10 // CONSTANT_False
 * kind=11 // CONSTANT_True
 * kind=12 // CONSTANT_Null
 * kind=15,16 { // CONSTANT_RTQname + CONSTANT_RTQnameA
 * U16 name_index				// CONSTANT_utf8, 0=AnyName wildcard
 * }
 * kind=17,18 // CONSTANT_RTQnameL + CONSTANT_RTQnameLA
 * }
 * }
 *
 * @author Clement Wong
 */
public class ConstantPool
{
	public static final Object NULL = new Object();

	public static ConstantPool merge(ConstantPool[] pools)
	{
		// create a new ConstantPool big enough for the combined pools.
		int preferredSize = 0;

		for (int i = 0, size = pools.length; i < size; i++)
		{
			preferredSize += pools[i].mnEnd;
		}

		ConstantPool newPool = new ConstantPool();
		newPool.in = new BytecodeBuffer(preferredSize);
		newPool.history = new IndexHistory(pools);

		return newPool;
	}

	ConstantPool()
	{
	}

	public ConstantPool(BytecodeBuffer in) throws DecoderException
	{
		this.in = in;
		scan();
	}

	private void scan() throws DecoderException
	{
		intpositions = Scanner.scanIntConstants(in);
		intEnd = in.pos();
		uintpositions = Scanner.scanUIntConstants(in);
		uintEnd = in.pos();
		doublepositions = Scanner.scanDoubleConstants(in);
		doubleEnd = in.pos();
		strpositions = Scanner.scanStrConstants(in);
		strEnd = in.pos();
		nspositions = Scanner.scanNsConstants(in);
		nsEnd = in.pos();
		nsspositions = Scanner.scanNsSetConstants(in);
		nssEnd = in.pos();
		mnpositions = Scanner.scanMultinameConstants(in);
		mnEnd = in.pos();

		size = ((intpositions.length == 0) ? 0 : (intpositions.length - 1)) +
			   ((uintpositions.length == 0) ? 0: (uintpositions.length - 1)) +
			   ((doublepositions.length == 0) ? 0 : (doublepositions.length - 1)) +
			   ((strpositions.length == 0) ? 0 : (strpositions.length - 1)) +
			   ((nspositions.length == 0) ? 0 : (nspositions.length - 1)) +
			   ((nsspositions.length == 0) ? 0 : (nsspositions.length - 1)) +
			   ((mnpositions.length == 0) ? 0 : (mnpositions.length - 1));
	}

	BytecodeBuffer in;
	IndexHistory history;
	private int size;

    int[] intpositions;
    int[] uintpositions;
    int[] doublepositions;
    int[] strpositions;
    int[] nspositions;
    int[] nsspositions;
    int[] mnpositions;

	int intEnd;
	int uintEnd;
	int doubleEnd;
	int strEnd;
	int nsEnd;
	int nssEnd;
	int mnEnd;

	public int size()
	{
		return size;
	}

	public int getInt(int index) throws DecoderException
	{
		if (index == 0)
		{
			return 0;
		}

		int pos = intpositions[index];
		int originalPos = in.pos();
		in.seek(pos);

        int value = (int) in.readU32();
        in.seek(originalPos);
        return value;

	}

	public long getLong(int index) throws DecoderException
	{
		if (index == 0)
		{
			return 0;
		}

		int pos = uintpositions[index];
		int originalPos = in.pos();
		in.seek(pos);
        long value = in.readU32();
        in.seek(originalPos);
        return value;
	}

	public double getDouble(int index) throws DecoderException
	{
		if (index == 0)
		{
			return 0;
		}

		int pos = doublepositions[index];
		int originalPos = in.pos();
		in.seek(pos);
        double value = in.readDouble();
        in.seek(originalPos);
        return value;
	}

	public String getString(int index) throws DecoderException
	{
		if (index == 0)
		{
			return null;
		}

		int pos = strpositions[index];
		int originalPos = in.pos();
		in.seek(pos);
        String value = in.readString((int) in.readU32());
        in.seek(originalPos);
        if (value != null)
        {
            return value;
        }
        else
        {
            throw new DecoderException("abc Decoder Erro: problem reading UTF-8 encoded strings.");
        }
	}

    public String getNamespaceName(int index) throws DecoderException
    {
        if( index == 0 )
        {
            return null;
        }
        int pos = nspositions[index];
        int originalPos = in.pos();
        in.seek(pos);
        int kind = in.readU8();
        String value = "";
        switch(kind)
        {
            case CONSTANT_PrivateNamespace:
            case CONSTANT_Namespace:
            case CONSTANT_PackageNamespace:
            case CONSTANT_PackageInternalNs:
            case CONSTANT_ProtectedNamespace:
		    case CONSTANT_ExplicitNamespace:
		    case CONSTANT_StaticProtectedNs:
                value = getString((int)in.readU32());
                break;
            default:
                throw new DecoderException("abc Decoder Error: constant pool index '" + index + "' is not a Namespace type. The actual type is '" + kind + "'");
        }
        in.seek(originalPos);
        return value;
    }

	public String[] getNamespaceSet(int index) throws DecoderException
	{
		if (index == 0)
		{
			return null;
		}

		int pos = nsspositions[index];
		int originalPos = in.pos();
		in.seek(pos);
        int count = (int) in.readU32();
        String[] value = new String[count];
        for (int j = 0; j < count; j++)
        {
            value[j] = getNamespaceName((int) in.readU32());
        }
        in.seek(originalPos);
        if (value != null)
        {
            return value;
        }
        else
        {
            throw new DecoderException("abc Decoder Erro: problem reading namespace set.");
        }
	}

	public int getNamespaceIndexForQName(int index) throws DecoderException {
		if (index == 0)
		{
			return 0;
		}

		int pos = mnpositions[index];
		int originalPos = in.pos();
		in.seek(pos);
		int kind = in.readU8();

		switch (kind)
		{
		case CONSTANT_Qname:
		case CONSTANT_QnameA:
			int namespaceIndex = (int) in.readU32();
			in.seek(originalPos);
			return namespaceIndex;
		default:
			in.seek(originalPos);
			throw new DecoderException("abc Decoder Error: constant pool index '" + index + "' is not a QName type. The actual type is '" + kind + "'");
		}
	}
	
	public int getNamespaceKind(int namespaceIndex) {
		if(namespaceIndex == 0)
			return -1;
		
		int pos = nspositions[namespaceIndex];
        int originalPos = in.pos();
        in.seek(pos);
        int kind = in.readU8();
        in.seek(originalPos);
        return kind;
	}

	public QName getQName(int index) throws DecoderException
	{
		if (index == 0)
		{
			return null;
		}

		int pos = mnpositions[index];
		int originalPos = in.pos();
		in.seek(pos);
		int kind = in.readU8();

		switch (kind)
		{
		case CONSTANT_Qname:
		case CONSTANT_QnameA:
			int namespaceIndex = (int) in.readU32();
			int nameIndex = (int) in.readU32();
			QName value = createQName(getNamespaceName(namespaceIndex), getString(nameIndex));
			in.seek(originalPos);
			return value;
		default:
			in.seek(originalPos);
			throw new DecoderException("abc Decoder Error: constant pool index '" + index + "' is not a QName type. The actual type is '" + kind + "'");
		}
	}

	public MultiName getMultiName(int index) throws DecoderException
	{
		if (index == 0)
		{
			return null;
		}

		int pos = mnpositions[index];
		int originalPos = in.pos();
		in.seek(pos);
		int kind = in.readU8();

		switch (kind)
		{
		case CONSTANT_Multiname:
		case CONSTANT_MultinameA:
			String name = getString((int) in.readU32());
			int namespace_set = (int) in.readU32();
			String[] namespaces = getNamespaceSet(namespace_set);
			MultiName value = createMultiName(name, namespaces);
			in.seek(originalPos);
			return value;
		default:
			in.seek(originalPos);
			throw new DecoderException("abc Decoder Error: constant constantPool index '" + index + "' is not a MultiName type. The actual type is '" + kind + "'");
		}
	}

    public Object getGeneralMultiname(int index) throws DecoderException
    {
        if (index == 0)
        {
            return null;
        }

        int pos = mnpositions[index];
        int originalPos = in.pos();
        in.seek(pos);
        int kind = in.readU8();

        switch (kind)
        {
        case CONSTANT_Qname:
        case CONSTANT_QnameA:
        {
            int namespaceIndex = (int) in.readU32();
            int nameIndex = (int) in.readU32();
            QName value = createQName(getNamespaceName(namespaceIndex), getString(nameIndex));
            in.seek(originalPos);
            return value;
        }
        case CONSTANT_Multiname:
        case CONSTANT_MultinameA:
        {
            String name = getString((int) in.readU32());
            int namespace_set = (int) in.readU32();
            String[] namespaces = getNamespaceSet(namespace_set);
            MultiName value = createMultiName(name, namespaces);
            in.seek(originalPos);
            return value;
        }
        case CONSTANT_RTQnameL:
	        in.seek(originalPos);
            return "CONSTANT_RTQnameL"; // Boolean.FALSE;
        case CONSTANT_RTQnameLA:
	        in.seek(originalPos);
            return "CONSTANT_RTQnameLA"; // Boolean.TRUE;
        case CONSTANT_MultinameL:
        case CONSTANT_MultinameLA:
        {
	        int namespacesetIndex = (int) in.readU32();
	        String[] value = getNamespaceSet(namespacesetIndex);
	        ArrayList<String> a = new ArrayList<String>();
	        for (int k = 0; k < value.length; k++)
	        {
		        a.add(value[k]);
	        }
	        in.seek(originalPos);
            return a;
        }
        case CONSTANT_RTQname:
        case CONSTANT_RTQnameA:
	    {
		    int idx = (int) in.readU32();
		    String s = getString(idx);
	        in.seek(originalPos);
            return s;
	    }
        default:
            in.seek(originalPos);
            throw new DecoderException("abc Decoder Error: constant pool index '" + index + "' is not a QName type. The actual type is '" + kind + "'");
        }
    }

	public Object get(int index, int kind) throws DecoderException
	{
		if (index == 0)
		{
			return null;
		}

        Object value;
        switch(kind)
        {
            case CONSTANT_Utf8:
                value = getString(index);
                return value;
            case CONSTANT_Integer:
                value = createInteger(getInt(index));
                return value;
            case CONSTANT_UInteger:
                value = createLong(getLong(index));
                return value;
            case CONSTANT_Double:
                value = createDouble(getDouble(index));
                return value;
            case CONSTANT_Qname:
            case CONSTANT_QnameA:
                value = getQName(index);
                return value;
            case CONSTANT_Namespace:
            case CONSTANT_PrivateNamespace:
            case CONSTANT_PackageNamespace:
            case CONSTANT_PackageInternalNs:
            case CONSTANT_ProtectedNamespace:
            case CONSTANT_ExplicitNamespace:
            case CONSTANT_StaticProtectedNs:
                value = getNamespaceName(index);
                return value;
            case CONSTANT_Multiname:
            case CONSTANT_MultinameA:
                value = getMultiName(index);
                return value;
            case CONSTANT_False:
                value = Boolean.FALSE;
                return value;
            case CONSTANT_True:
                value = Boolean.TRUE ;
                return value;
            case CONSTANT_Null:
                value = NULL;
                return value;
            case CONSTANT_RTQname:
            case CONSTANT_RTQnameA:
				value = getGeneralMultiname(index);
                return value;
            case CONSTANT_RTQnameL:
                value = "CONSTANT_RTQnameL"; // Boolean.FALSE;
                return value;
            case CONSTANT_RTQnameLA:
                value = "CONSTANT_RTQnameLA"; // Boolean.TRUE;
                return value;
            case CONSTANT_MultinameL:
	            value = getNamespaceSet(getInt(index));
                return value;
            case CONSTANT_MultinameLA:
	            value = getNamespaceSet(getInt(index));
                return value;
            case CONSTANT_Namespace_Set:
                value = getNamespaceSet(index);
                return value;
            default:
                throw new DecoderException("Error: Unhandled constant type - " + kind);
        }
	}

	private Integer createInteger(int number)
	{
		return IntegerPool.getNumber(number);
	}

	private Long createLong(long number)
	{
		return new Long(number);
	}

	private Double createDouble(double number)
	{
		return new Double(number);
	}

	private QName createQName(String ns, String name)
	{
		return new QName(ns, name);
	}

	private MultiName createMultiName(String name, String[] ns)
	{
		return new MultiName(name, ns);
	}

	public void writeTo(OutputStream out) throws IOException
	{
		history.writeTo(in);
		in.writeTo(out);
	}
}

final class IndexHistory
{
	IndexHistory(ConstantPool[] pools)
	{
		this.pools = pools;
		poolSizes = new int[pools.length];

		int size = 0, preferredSize = 0;
		for (int i = 0, length = pools.length; i < length; i++)
		{
			poolSizes[i] = (i == 0) ? 0 : size;
			size += pools[i].size();
			preferredSize += (pools[i].mnEnd - pools[i].strEnd);
		}

		map = new int[size];
		in4 = new BytecodeBuffer(preferredSize);
		in5 = new BytecodeBuffer(preferredSize);
		in6 = new BytecodeBuffer(preferredSize);

		intP = new ByteArrayPool();
		uintP = new ByteArrayPool();
		doubleP = new ByteArrayPool();
		stringP = new ByteArrayPool();
		nsP = new NSPool();
		nssP = new NSSPool();
		mnP = new MultiNamePool();

		total = 0;
		duplicate = 0;
		totalBytes = 0;
		duplicateBytes = 0;

		// nss = new HashSet<Integer>();
	}

	public int total, duplicate, totalBytes, duplicateBytes;

	private ConstantPool[] pools;
	private int[] poolSizes;
	private int[] map;

	private ByteArrayPool intP, uintP, doubleP, stringP, nsP, nssP, mnP;
	private BytecodeBuffer in4, in5, in6;
	// private Set<Integer> nss;

    // Needed so we can strip out the index for all CONSTANT_PrivateNamespace entries
    // since the name for private namespaces is not important
    private boolean disableDebuggingInfo = false;
    void disableDebugging()
    {
        disableDebuggingInfo = true;
    }


	public int getIndex(int poolIndex, int kind, int index)
	{
		if (index == 0)
		{
			return 0;
		}
		else
		{
			int newIndex = calculateIndex(poolIndex, kind, index);

			if (map[newIndex] == 0)
			{
				decodeOnDemand(poolIndex, kind, index, newIndex);
			}

			return map[newIndex];
		}
	}

	public void writeTo(BytecodeBuffer b)
	{
		intP.writeTo(b);
		uintP.writeTo(b);
		doubleP.writeTo(b);
		stringP.writeTo(b);
		nsP.writeTo(b);
		nssP.writeTo(b);
		mnP.writeTo(b);
	}

	/**
	 * @param poolIndex 0-based
	 * @param kind 0-based
	 * @param oldIndex 1-based
	 */
	private final int calculateIndex(final int poolIndex, final int kind, final int oldIndex)
	{
		int index = poolSizes[poolIndex];

		if (kind > 0)
		{
			index += (pools[poolIndex].intpositions.length == 0) ? 0 : (pools[poolIndex].intpositions.length - 1);
		}

		if (kind > 1)
		{
			index += (pools[poolIndex].uintpositions.length == 0) ? 0 : (pools[poolIndex].uintpositions.length - 1);
		}

		if (kind > 2)
		{
			index += (pools[poolIndex].doublepositions.length == 0) ? 0 : (pools[poolIndex].doublepositions.length - 1);
		}

		if (kind > 3)
		{
			index += (pools[poolIndex].strpositions.length == 0) ? 0 : (pools[poolIndex].strpositions.length - 1);
		}

		if (kind > 4)
		{
			index += (pools[poolIndex].nspositions.length == 0) ? 0 : (pools[poolIndex].nspositions.length - 1);
		}

		if (kind > 5)
		{
			index += (pools[poolIndex].nsspositions.length == 0) ? 0 : (pools[poolIndex].nsspositions.length - 1);
		}

		if (kind > 6)
		{
			index += (pools[poolIndex].mnpositions.length == 0) ? 0 : (pools[poolIndex].mnpositions.length - 1);
		}

		index += (oldIndex - 1);

		return index;
	}

	private final void decodeOnDemand(final int poolIndex, final int kind, final int j, final int j2)
    {
	    ConstantPool pool = pools[poolIndex];
	    ByteArrayPool baPool = null;
	    BytecodeBuffer poolIn = null;
	    int[] positions = null;
	    int length = 0, endPos = 0;

	    if (kind == 0)
	    {
		    positions = pool.intpositions;
		    length = positions.length;
		    endPos = pool.intEnd;
		    baPool = intP;
		    poolIn = pool.in;
	    }
	    else if (kind == 1)
	    {
		    positions = pool.uintpositions;
		    length = positions.length;
		    endPos = pool.uintEnd;
		    baPool = uintP;
		    poolIn = pool.in;
	    }
	    else if (kind == 2)
	    {
		    positions = pool.doublepositions;
		    length = positions.length;
		    endPos = pool.doubleEnd;
		    baPool = doubleP;
		    poolIn = pool.in;
	    }
	    else if (kind == 3)
	    {
		    positions = pool.strpositions;
		    length = positions.length;
		    endPos = pool.strEnd;
		    baPool = stringP;
		    poolIn = pool.in;
	    }
	    else if (kind == 4)
	    {
		    positions = pool.nspositions;
		    length = positions.length;
		    endPos = pool.nsEnd;
		    baPool = nsP;
		    poolIn = pool.in;
	    }
	    else if (kind == 5)
	    {
		    positions = pool.nsspositions;
		    length = positions.length;
		    endPos = pool.nssEnd;
		    baPool = nssP;
		    poolIn = pool.in;
	    }
	    else if (kind == 6)
	    {
		    positions = pool.mnpositions;
		    length = positions.length;
		    endPos = pool.mnEnd;
		    baPool = mnP;
		    poolIn = pool.in;
	    }

	    int start = positions[j];
	    int end = (j != length - 1) ? positions[j + 1] : endPos;

	    if (kind == 4)
	    {
		    int pos = positions[j];
		    int originalPos = poolIn.pos();
		    poolIn.seek(pos);
		    start = in4.size();
		    int nsKind = poolIn.readU8();
		    in4.writeU8(nsKind);
		    switch (nsKind)
		    {
		    case CONSTANT_PrivateNamespace:
                if( this.disableDebuggingInfo )
                {
                    in4.writeU32(0); // name not important for private namespace
                    break;
                }
                // else fall through and treat like a normal namespace
		    case CONSTANT_Namespace:
            case CONSTANT_PackageNamespace:
            case CONSTANT_PackageInternalNs:
            case CONSTANT_ProtectedNamespace:
            case CONSTANT_ExplicitNamespace:
            case CONSTANT_StaticProtectedNs:				
			    int index = (int) poolIn.readU32();
			    int newIndex = getIndex(poolIndex, 3, index); // 3 = String
			    in4.writeU32(newIndex);
			    break;
		    default:
			    assert false; // can't possibly happen...
		    }
            poolIn.seek(originalPos);
            end = in4.size();
            poolIn = in4;
	    }
	    else if (kind == 5)
	    {
		    int pos = positions[j];
		    int originalPos = poolIn.pos();
		    poolIn.seek(pos);
		    start = in5.size();

		    /*
		    nss.clear();
		    int count = (int) poolIn.readU32();
		    for (int k = 0; k < count; k++)
		    {
			    nss.add((int) poolIn.readU32());
		    }
		    count = nss.size();
		    in5.writeU32(count);
		    for (Iterator<Integer> k = nss.iterator(); k.hasNext();)
		    {
			    int index = k.next();
			    int newIndex = getIndex(poolIndex, 4, index);
			    in5.writeU32(newIndex);
		    }
            */

		    int count = (int) poolIn.readU32();
		    in5.writeU32(count);
		    for (int k = 0; k < count; k++)
		    {
			    int index = (int) poolIn.readU32();
			    int newIndex = getIndex(poolIndex, 4, index);
			    in5.writeU32(newIndex);
		    }

		    poolIn.seek(originalPos);
		    end = in5.size();
		    poolIn = in5;
	    }
	    else if (kind == 6)
	    {
		    int pos = positions[j];
		    int originalPos = poolIn.pos();
		    poolIn.seek(pos);
		    start = in6.size();
		    int constKind = poolIn.readU8();
		    in6.writeU8(constKind);

		    switch (constKind)
		    {
		    case CONSTANT_Qname:
		    case CONSTANT_QnameA:
		    {
			    int namespaceIndex = (int) poolIn.readU32();
			    int newNamespaceIndex = getIndex(poolIndex, 4, namespaceIndex);
			    in6.writeU32(newNamespaceIndex);
			    int nameIndex = (int) poolIn.readU32();
			    int newNameIndex = getIndex(poolIndex, 3, nameIndex);
			    in6.writeU32(newNameIndex);
			    break;
		    }
		    case CONSTANT_Multiname:
		    case CONSTANT_MultinameA:
		    {
			    int nameIndex = (int) poolIn.readU32();
			    int newNameIndex = getIndex(poolIndex, 3, nameIndex);
			    in6.writeU32(newNameIndex);
			    int namespace_set = (int) poolIn.readU32();
			    int newNamespace_set = getIndex(poolIndex, 5, namespace_set);
			    in6.writeU32(newNamespace_set);
			    break;
		    }
		    case CONSTANT_RTQname:
		    case CONSTANT_RTQnameA:
		    {
			    int index = (int) poolIn.readU32();
			    int newIndex = getIndex(poolIndex, 3, index);
			    in6.writeU32(newIndex);
			    break;
		    }
		    case CONSTANT_RTQnameL:
		    case CONSTANT_RTQnameLA:
				break;
		    case CONSTANT_MultinameL:
		    case CONSTANT_MultinameLA:
			{
				int namespace_set = (int) poolIn.readU32();
				int newNamespace_set = getIndex(poolIndex, 5, namespace_set);
				in6.writeU32(newNamespace_set);
				break;
			}
		    default:
			    assert false; // can't possibly happen...
		    }

		    poolIn.seek(originalPos);
		    end = in6.size();
		    poolIn = in6;
	    }

	    int newIndex = baPool.contains(poolIn, start, end);
	    if (newIndex == -1)
	    {
		    newIndex = baPool.store(poolIn, start, end);
	    }
	    else
	    {
		    duplicate++;
		    duplicateBytes += (end - start);
	    }

	    total++;
	    totalBytes += (end - start);

	    if (j != 0)
	    {
		    map[j2] = newIndex;
	    }
    }
}

final class NSPool extends ByteArrayPool
{
	NSPool()
	{
		super();
	}

	ByteArray newByteArray()
	{
		return new NS();
	}
}

final class NS extends ByteArray
{
	int nsKind = 0, index = 0;

	void init()
	{
		super.init();

		int originalPos = b.pos();
		b.seek(start);
		nsKind = b.readU8();
		switch (nsKind)
		{
		case CONSTANT_PrivateNamespace:
		case CONSTANT_Namespace:
        case CONSTANT_PackageNamespace:
        case CONSTANT_PackageInternalNs:
        case CONSTANT_ProtectedNamespace:
        case CONSTANT_ExplicitNamespace:
        case CONSTANT_StaticProtectedNs:				
			index = (int) b.readU32();
			break;
		default:
			assert false; // can't possibly happen...
		}
		b.seek(originalPos);

		long num = 1234 ^ nsKind ^ index;
		hash = (int) ((num >> 32) ^ num);
	}

	void clear()
	{
		super.clear();
		nsKind = 0;
		index = 0;
	}

	public boolean equals(Object obj)
	{
        boolean equal = false;
		if (obj instanceof NS)
		{
            NS ns = (NS) obj;
            if( this.nsKind == CONSTANT_PrivateNamespace )
            {
                // Private namespaces are only equal if they are literally the same namespace,
                // the name is not important.
                equal = (this.b == ns.b) && (this.start == ns.start) && (this.end == ns.end);
            }
            else
            {
                equal = (ns.nsKind == this.nsKind) && (ns.index == this.index);
            }
		}
        return equal;
	}
}


final class NSSPool extends ByteArrayPool
{
	NSSPool()
	{
		super();
	}

	ByteArray newByteArray()
	{
		return new NSS();
	}
}

final class NSS extends ByteArray
{
	int[] set = null;
	int size = 0;

	void init()
	{
		super.init();

		int originalPos = b.pos();
		b.seek(start);
		int count = (int) b.readU32();

		if (set == null || count > set.length)
		{
			set = new int[count];
		}
		size = count;

		for (int k = 0; k < count; k++)
		{
			set[k] = (int) b.readU32();
		}
		b.seek(originalPos);

		long num = 1234;
		for (int k = 0; k < count; k++)
		{
			num ^= set[k];
		}
		hash = (int) ((num >> 32) ^ num);
	}

	void clear()
	{
		super.clear();
		size = 0;
	}

	public boolean equals(Object obj)
	{
		if (obj instanceof NSS)
		{
			NSS nss = (NSS) obj;
			if (size == nss.size)
			{
				for (int i = 0; i < size; i++)
				{
					if (set[i] != nss.set[i])
					{
						return false;
					}
				}
				return true;
			}
			else
			{
				return false;
			}
		}
		else
		{
			return false;
		}
	}
}

final class MultiNamePool extends ByteArrayPool
{
	MultiNamePool()
	{
		super();
	}

	ByteArray newByteArray()
	{
		return new MN();
	}
}

final class MN extends ByteArray
{
	int constKind = 0, index1 = 1, index2 = 1;

	void init()
	{
		super.init();

		int originalPos = b.pos();
		b.seek(start);
		constKind = b.readU8();

		switch (constKind)
		{
		case CONSTANT_Qname:
		case CONSTANT_QnameA:
		{
			index1 = (int) b.readU32();
			index2 = (int) b.readU32();
			long num = 1234 ^ constKind ^ index1 ^ index2;
			hash = (int) ((num >> 32) ^ num);
			break;
		}
		case CONSTANT_Multiname:
		case CONSTANT_MultinameA:
		{
			index1 = (int) b.readU32();
			index2 = (int) b.readU32();
			long num = 1234 ^ constKind ^ index1 ^ index2;
			hash = (int) ((num >> 32) ^ num);
			break;
		}
		case CONSTANT_RTQname:
		case CONSTANT_RTQnameA:
		{
			index1 = (int) b.readU32();
			long num = 1234 ^ constKind ^ index1;
			hash = (int) ((num >> 32) ^ num);
			break;
		}
		case CONSTANT_RTQnameL:
		case CONSTANT_RTQnameLA:
		{
			long num = 1234 ^ constKind;
			hash = (int) ((num >> 32) ^ num);
			break;
		}
		case CONSTANT_MultinameL:
		case CONSTANT_MultinameLA:
		{
			index1 = (int) b.readU32();
			long num = 1234 ^ constKind ^ index1;
			hash = (int) ((num >> 32) ^ num);
			break;
		}
		default:
			assert false; // can't possibly happen...
		}

		b.seek(originalPos);
	}

	void clear()
	{
		super.clear();
		constKind = 0;
		index1 = 0;
		index2 = 0;
	}

	public boolean equals(Object obj)
	{
		if (obj instanceof MN)
		{
			MN mn = (MN) obj;

			switch (constKind)
			{
			case CONSTANT_Qname:
			case CONSTANT_QnameA:
			case CONSTANT_Multiname:
			case CONSTANT_MultinameA:
			{
				return (constKind == mn.constKind) && (index1 == mn.index1) && (index2 == mn.index2);
			}
			case CONSTANT_RTQname:
			case CONSTANT_RTQnameA:
			{
				return (constKind == mn.constKind) && (index1 == mn.index1);
			}
			case CONSTANT_RTQnameL:
			case CONSTANT_RTQnameLA:
				return (constKind == mn.constKind);
			case CONSTANT_MultinameL:
			case CONSTANT_MultinameLA:
			{
				return (constKind == mn.constKind) && (index1 == mn.index1);
			}
			default:
				return false;
			}
		}
		else
		{
			return false;
		}
	}
}
