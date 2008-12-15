package adobe.abc;

import adobe.abc.GlobalOptimizer.InputAbc;

import static adobe.abc.OptimizerConstants.*;
import static macromedia.asc.embedding.avmplus.ActionBlockConstants.*;

import java.util.HashMap;
import java.util.Map;

public class Method implements Comparable<Method>
{
	InputAbc abc;
	final int id;
	int emit_id;
	Edge entry;
	Typeref[] params;
	Object[] values;
	int optional_count;
	Typeref returns;
	Name name;
	String debugName;
	Name[] paramNames;
	int flags;
	Type cx;
	int blockId;
	int exprId;
	int edgeId;
	String kind;
	
	// body fields
	int max_stack;
	int local_count;
	int max_scope;
	int code_len;
	
	Typeref activation;
	Handler[] handlers = nohandlers;
	
	Map<Expr,Typeref> verifier_types = null;
	
	Map<Expr,Integer>fixedLocals = new HashMap<Expr,Integer>();
	
	Method(int id, InputAbc abc)
	{
		this.id = id;
		this.abc = abc;
	}
	boolean needsRest()
	{
		return (flags & METHOD_Needrest) != 0;
	}
	boolean needsArguments()
	{
		return (flags & METHOD_Arguments) != 0;
	}
	boolean hasParamNames()
	{
		return (flags & METHOD_HasParamNames) != 0;
	}
	boolean hasOptional()
	{
		return (flags & METHOD_HasOptional) != 0;
	}
	boolean isNative()
	{
		return (flags & METHOD_Native) != 0;
	}
	
	public int compareTo(Method m)
	{
		return id - m.id;
	}
	
	public String toString()
	{
		return kind + " " + String.valueOf(name);
	}
}

