package adobe.abc;

import adobe.abc.GlobalOptimizer.Binding;
import adobe.abc.GlobalOptimizer.Symtab;


import static adobe.abc.OptimizerConstants.*;
import static macromedia.asc.embedding.avmplus.ActionBlockConstants.*;

public class Type
{
	Name name;
	Type base;
	Type[] interfaces = notypes;
	Symtab<Binding> defs;
	Method init;
	Type itype;
	int flags;
	Namespace protectedNs;
	Typeref[] scopes = notyperefs; // captured outer scopes
	boolean numeric;
	boolean primitive;
	boolean atom;
	Object defaultValue;
	Typeref ref;
	int size;
	int slotCount;
	int ctype;
	boolean obscure_natives;
	
	Type()
	{
		this.ref = new Typeref(this, true);
		this.obscure_natives = false;
		
		defaultValue = TypeCache.instance().NULL;
		this.ctype = CTYPE_OBJECT;
	}

	Type(Name name, Type base)
	{
		this();
		this.name = name;
		this.base = base;
		this.defs = new Symtab<Binding>();
		this.obscure_natives = false;
	}
	
	boolean emitAsAny()
	{
		// not sure about this, attempting to coerce native class as *
		//  FIXME: need to account for interfaces now...
		return base == null && 
		this != TypeCache.instance().VOID && 
		defs.size() == 0 && 
		! TypeCache.instance().builtinTypes.contains(this) && 
		!TypeCache.instance().baseTypes.contains(this);
		
	}
	
	public String toString()
	{
		return String.valueOf(name);
	}
	
	boolean isFinal()
	{
		return (flags & CLASS_FLAG_final) != 0;
	}
	
	void setFinal()
	{
		flags |= CLASS_FLAG_final;
	}
	
	Binding find(Name n)
	{
		// look up the inheritance tree
		for (Type t = this; t != null; t = t.base)
		{
			Binding b = t.defs.get(n);
			if (b != null)
				return b;
		}
		return null;
	}
	
	Binding findGet(Name n)
	{
		Binding first = find(n);
		if (first != null && GlobalOptimizer.isSetter(first))
		{
			if (first.peer != null)
				return first.peer;
			Binding second;
			if (base != null && GlobalOptimizer.isGetter(second=base.findGet(n)))
				return second;
		}
		return first;
	}
	
	Binding findSlot(int slot)
	{
		for (Binding b: defs.values())
		{
			if (GlobalOptimizer.isSlot(b) && b.slot == slot)
				return b;
		}
		return null;
	}
	
	boolean hasProtectedNs()
	{
		return (flags & CLASS_FLAG_protected) != 0;
	}
	
	boolean isMachineCompatible(Type t)
	{
		boolean result;
		
		result  = equals(t);
		result |= equals(TypeCache.instance().NULL) && !t.isMachineType();
		result |= t.equals(TypeCache.instance().NULL) && !isMachineType();
		result |= !isMachineType() && !t.isMachineType() && !equals(TypeCache.instance().ANY) && !t.equals(TypeCache.instance().ANY);
		
		return result;
	}
	
	boolean isMachineType() 
	{
		return
			equals(TypeCache.instance().OBJECT) ||
			equals(TypeCache.instance().VOID) ||
			equals(TypeCache.instance().INT) ||
			equals(TypeCache.instance().UINT) ||
			equals(TypeCache.instance().BOOLEAN) ||
			equals(TypeCache.instance().ARRAY) ||  // TODO: AVM doesn't make this a machine type, but it acts like one.
			equals(TypeCache.instance().NUMBER);
	}
	
	boolean isAtom()
	{
		return this.atom;
	}
}