package adobe.abc;

import static macromedia.asc.embedding.avmplus.ActionBlockConstants.*;

import java.util.ArrayList;

public class Name implements Comparable<Name>
{
	public static final Namespace PUBLIC = new Namespace("");
	public static final Namespace PKG_PUBLIC = new Namespace(CONSTANT_PackageNamespace, "");
	public static final Namespace AS3 = new Namespace("http://adobe.com/AS3/2006/builtin");
	
	final int kind;
	final Nsset nsset;
	final String name;
	final String type_param;	// null if none

	Name(int kind)
	{
		this(kind, GlobalOptimizer.uniqueNs(), GlobalOptimizer.unique());
	}

	Name(Namespace ns, String name)
	{
		this(CONSTANT_Qname, ns, name);
	}
	Name(int kind, Namespace ns, String name)
	{
		this(kind, name, new Nsset(new Namespace[] { ns }), null);
	}
	Name(int kind, String name, Nsset nsset)
	{
		this(kind, name, nsset, null);
	}
	Name(int kind, String name, Nsset nsset, String type_param_name)
	{
		assert(nsset != null);
		this.kind = kind;
		this.nsset = nsset;
		this.name = name;
		this.type_param = type_param_name;
	}
	Name(String name)
	{
		this(CONSTANT_Qname, PUBLIC, name);
	}
	
	Namespace nsset(int i)
	{
		return nsset.nsset[i];
	}
	
	public String toString()
	{
		return name;
	}
	
	public String format()
	{
		if (nsset.length == 1)
			return nsset(0) + "::" + name;
		else
		{
			ArrayList<Namespace> list = new ArrayList<Namespace>();
			for (Namespace n : nsset)
				list.add(n);
			return list + "::" + name;
		}
	}
	
	public Name append(String s)
	{
		return new Name(kind, name+s, nsset);
	}

	public Name prepend(String s)
	{
		return new Name(kind, s+name, nsset);
	}
	
	int hc(Object o)
	{
		return o != null ? o.hashCode() : 0;
	}
	
	public int hashCode()
	{
		return kind ^ hc(nsset) ^ hc(name);
	}
	
	/**
	 * exact equality.  Both names must have the same kind, name,
	 * and equal namespace sets.
	 */
	public boolean equals(Object other)
	{
		if (!(other instanceof Name))
			return false;
		Name o = (Name) other;
		return kind == o.kind && name.equals(o.name) && nsset.equals(o.nsset);
	}
	
	public int compareTo(Name other)
	{
		int d = kind - other.kind;
		if (d != 0) return d;
		d = name.compareTo(other.name);
		if (d != 0) return d;
		return nsset.compareTo(other.nsset);
	}
	
	int attr()
	{
		return kind == CONSTANT_MultinameA || kind == CONSTANT_QnameA ||
			kind == CONSTANT_RTQnameA || kind == CONSTANT_RTQnameLA || kind == CONSTANT_MultinameLA ? 1 : 0;
	}
	
	boolean isQname()
	{
		return kind == CONSTANT_Qname || kind == CONSTANT_QnameA ||
			kind == CONSTANT_RTQname || kind == CONSTANT_RTQnameA ||
			kind == CONSTANT_RTQnameL || kind == CONSTANT_RTQnameLA;
	}
}
