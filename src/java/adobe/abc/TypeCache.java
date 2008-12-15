package adobe.abc;

import static adobe.abc.OptimizerConstants.*;

import java.util.HashSet;
import java.util.Set;

public class TypeCache 
{

	public Type OBJECT, FUNCTION, CLASS, ARRAY;
	public Type INT, UINT, NUMBER, BOOLEAN, STRING, NAMESPACE;
	public Type XML, XMLLIST, QNAME;
	public Type NULL, VOID;
	public Type ANY;
	
	Set<Type>builtinTypes = new HashSet<Type>();
	Set<Type>baseTypes    = new HashSet<Type>();
	
	private static TypeCache the_instance = new TypeCache();
	
	public static TypeCache instance()
	{
		return the_instance;
	}
	
	public Type ANY()
	{
		if ( null == this.ANY )
		{
			this.ANY = new Type(new Name("*"),null);
			this.ANY.ctype = CTYPE_ATOM;
		}
		
		return this.ANY;
	}
	
	void polishBuiltins( Object undefined, Object boolean_false, Object numeric_nan)
	{
		OBJECT.ctype = CTYPE_ATOM;
		NULL.ctype = CTYPE_ATOM;
		VOID.ctype = CTYPE_VOID;
		INT.ctype = CTYPE_INT;
		UINT.ctype = CTYPE_UINT;
		NUMBER.ctype = CTYPE_DOUBLE;
		BOOLEAN.ctype = CTYPE_BOOLEAN;
		STRING.ctype = CTYPE_STRING;
		NAMESPACE.ctype = CTYPE_NAMESPACE;
		// everything else defaults to CTYPE_OBJECT

		INT.numeric = NUMBER.numeric = UINT.numeric = BOOLEAN.numeric = true;
		
		STRING.primitive = BOOLEAN.primitive = true;
		INT.primitive = NUMBER.primitive = UINT.primitive = true;
		VOID.primitive = NULL.primitive = true;
		
		ANY.atom = OBJECT.atom = VOID.atom = true;
		
		INT.ref = INT.ref.nonnull();
		NUMBER.ref = NUMBER.ref.nonnull();
		UINT.ref = UINT.ref.nonnull();
		BOOLEAN.ref = BOOLEAN.ref.nonnull();
		
		OBJECT.defaultValue = NULL;
		NULL.defaultValue = NULL;
		ANY.defaultValue = undefined;
		VOID.defaultValue = undefined;
		BOOLEAN.defaultValue = boolean_false;
		NUMBER.defaultValue = numeric_nan;
		INT.defaultValue = 0;
		UINT.defaultValue = 0;
		
		builtinTypes.add(CLASS);
		builtinTypes.add(FUNCTION);
		builtinTypes.add(ARRAY);
		builtinTypes.add(INT);
		builtinTypes.add(UINT);
		builtinTypes.add(NUMBER);
		builtinTypes.add(BOOLEAN);
		builtinTypes.add(STRING);
		builtinTypes.add(NAMESPACE);
		builtinTypes.add(XML);
		builtinTypes.add(XMLLIST);
		builtinTypes.add(QNAME);
		builtinTypes.add(VOID);
	}
}
