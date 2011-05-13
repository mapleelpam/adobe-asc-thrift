package tw.maple;


import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;

import macromedia.asc.parser.*;
import macromedia.asc.semantics.Value;
import macromedia.asc.semantics.StringValue;
import macromedia.asc.semantics.StringListValue;
import macromedia.asc.semantics.QNValue;
import macromedia.asc.util.Context;
//import sun.org.mozilla.javascript.internal.EvaluatorException;
import tw.maple.generated.*;
import tw.maple.StringEvaluator;
import static macromedia.asc.parser.Tokens.*;

public final class ProgramNodeDumper implements Evaluator 
{
	AstDumper.Client thrift_cli;
	StringEvaluator	string_evaluator;
	private boolean is_abstract_function = false;
	
    public ProgramNodeDumper(AstDumper.Client cli)
    {
    	thrift_cli = cli;
    	string_evaluator = new StringEvaluator();
    }
    public boolean checkFeature(Context cx, Node node)
    {
    	return true;
    }

	// Base node

	public Value evaluate(Context cx, Node node)
	{
		return null;
	}

	// Expression evaluators

	public Value evaluate(Context cx, IncrementNode node)
	{
		return null;
	}

	public Value evaluate(Context cx, DeleteExpressionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, IdentifierNode node)
	{
		System.out.println((new Throwable()).getStackTrace()[0].toString()+":"+node.name);
        if(node instanceof TypeIdentifierNode)
        {
        	System.out.println( " is type identifier ");
        }
        else if (node.isAttr())
        {
    		System.out.println( " is attr");
        }
        else
        {
				try {
					Identifier id = new Identifier();
					id.name = node.name;
					thrift_cli.identifierExpression(id);
				} catch (org.apache.thrift.TException e1) {

				}			
        }
        
        
		return null;
	}

	public Value evaluate(Context cx, InvokeNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ThisExpressionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, QualifiedIdentifierNode node) 
	{
		System.out.println((new Throwable()).getStackTrace()[0].toString());
		try {
			Identifier id = new Identifier();
			id.name = node.name;
			id.qualifier = "";

			if (node.qualifier != null) {
				{
					Value ret_value = node.qualifier.evaluate(cx,
							string_evaluator);
					id.qualifier = Extract2String(ret_value);
				}
			}
			thrift_cli.identifierExpression(id);
		} catch (org.apache.thrift.TException e1) {

		}		
		return null;
	}

	public Value evaluate(Context cx, QualifiedExpressionNode node) {
		System.out.println((new Throwable()).getStackTrace()[0].toString());
		return null;
	}

    public Value evaluate(Context cx, LiteralBooleanNode node)
    {
    	System.out.println((new Throwable()).getStackTrace()[0].toString());
    	try 
		{
			tw.maple.generated.Literal str = new tw.maple.generated.Literal();
			if( node.value == true )
				str.value = "true";
			else
				str.value = "false";
			thrift_cli.literalBooleanExpression( str );
		}
		catch (org.apache.thrift.TException e1) 
		{
			
		}
		return null;
    }

	public Value evaluate(Context cx, LiteralNumberNode node)
	{
		System.out.println((new Throwable()).getStackTrace()[0].toString());
		try 
		{
			tw.maple.generated.Literal str = new tw.maple.generated.Literal();
			str.value = node.value;
			thrift_cli.literalNumberExpression( str );
		}
		catch (org.apache.thrift.TException e1) 
		{
			
		}	
		return null;
	}

	public Value evaluate(Context cx, LiteralStringNode node)
	{
		try {
			tw.maple.generated.Literal str = new tw.maple.generated.Literal();
			str.value = node.value;
			thrift_cli.literalStringExpression(str);
		} catch (org.apache.thrift.TException e1) {
		}
		
		return null;
	}

	public Value evaluate(Context cx, LiteralNullNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, LiteralRegExpNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, LiteralXMLNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, FunctionCommonNode node)
	{
//		System.out.println((new Throwable()).getStackTrace()[0].toString());
        if (node.signature != null)
        {
            node.signature.evaluate(cx, this);
        }
        if (node.body != null)
        {
            node.body.evaluate(cx, this);
        }
		return null;
	}

	public Value evaluate(Context cx, ParenExpressionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ParenListExpressionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, LiteralObjectNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, LiteralFieldNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, LiteralArrayNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}
	
	public Value evaluate(Context cx, LiteralVectorNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, SuperExpressionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, SuperStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, MemberExpressionNode node)
	{
		try
		{
			System.out.println((new Throwable()).getStackTrace()[0].toString());
			StringListValue slv = new StringListValue();
			System.out.println((new Throwable()).getStackTrace()[0].toString());
	        if (node.base != null)
	        {
	        	thrift_cli . startMemberExpression();
	            Value v = node.base.evaluate(cx, this);
	        }
	        System.out.println((new Throwable()).getStackTrace()[0].toString());
	        if (node.selector != null)
	        {
	            Value v = node.selector.evaluate(cx, this);
	            slv.values.add( Extract2String( v ) );
	        }  
	        if (node.base != null)
	        	thrift_cli . endMemberExpression( );
	        
			return slv;

		} 
		catch (org.apache.thrift.TException e1)
		{
		}

		return null;
	}

	public Value evaluate(Context cx, CallExpressionNode node)
	{
		CallExpression call_expression;
		
		try {
			call_expression = new CallExpression();
			call_expression.is_new = node.is_new;
			call_expression.mode = (node.getMode() == LEFTBRACKET_TOKEN ? " bracket" :
	            	node.getMode() == LEFTPAREN_TOKEN ? " filter" :
	                node.getMode() == DOUBLEDOT_TOKEN ? " descend" :
	                node.getMode() == EMPTY_TOKEN ? " lexical" : " dot");
			thrift_cli.startCallExpression(call_expression);
			
			if (node.expr != null) {
				// Callee
				node.expr.evaluate(cx, this);
			}

			if (node.args != null) {
				thrift_cli.startArgumentList();
					node.args.evaluate(cx, this);
				thrift_cli.endArgumentList();
			}
			
			thrift_cli.endCallExpression();
		} 
		catch (org.apache.thrift.TException e1) 
		{
		}

		return null;
	}

	public Value evaluate(Context cx, GetExpressionNode node)
	{
		System.out.println((new Throwable()).getStackTrace()[0].toString());
		if (node.expr != null)
        {
            return node.expr.evaluate(cx, this);
        }
		return null;
	}

	public Value evaluate(Context cx, SetExpressionNode node) 
	{
		System.out.println((new Throwable()).getStackTrace()[0].toString());
		try {
			thrift_cli.startAssignment();
				thrift_cli.startExpressionList();
				if (node.expr != null) 
					node.expr.evaluate(cx, this);
				thrift_cli.endExpressionList();
			
				thrift_cli.startExpressionList();
				if (node.args != null) 
					node.args.evaluate(cx, this);
				thrift_cli.endExpressionList();
			thrift_cli.endAssignment();
		} catch (org.apache.thrift.TException e1) {
		}
		return null;
	}

    public Value evaluate(Context cx, ApplyTypeExprNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, UnaryExpressionNode node) 
	{
		System.out.println((new Throwable()).getStackTrace()[0].toString());
		try {
			UnaryExpression unary_expression = new UnaryExpression();
			unary_expression.op = Token.getTokenClassName(node.op);
			thrift_cli.startUnaryExpression(unary_expression);
			if (node.expr != null) {
				node.expr.evaluate(cx, this);
			}
			thrift_cli.endUnaryExpression();
		} catch (org.apache.thrift.TException e1) {
		}
		return null;
	}

	public Value evaluate(Context cx, BinaryExpressionNode node) 
	{	
		System.out.println((new Throwable()).getStackTrace()[0].toString());
		try {
			if( Token.getTokenClassName(node.op) == "instanceof" ) {
				thrift_cli.startInstanceOfExpression();
				if (node.lhs != null) {
					node.lhs.evaluate(cx, this);
				}

				if (node.rhs != null) {
					node.rhs.evaluate(cx, this);
				}
				thrift_cli.endInstanceOfExpression();	
			} else if( Token.getTokenClassName(node.op) == "is" ) {
				thrift_cli.startIsOperator();
				if (node.lhs != null) {
					node.lhs.evaluate(cx, this);
				}

				if (node.rhs != null) {
					node.rhs.evaluate(cx, this);
				}
				thrift_cli.endIsOperator();
			} else {
				BinaryExpression binary_expression = new BinaryExpression();
				binary_expression.op = Token.getTokenClassName(node.op);
				thrift_cli.startBinaryExpression(binary_expression);
				if (node.lhs != null) {
					node.lhs.evaluate(cx, this);
				}

				if (node.rhs != null) {
					node.rhs.evaluate(cx, this);
				}
				thrift_cli.endBinaryExpression();				
			}
		} catch (org.apache.thrift.TException e1) {
		}

		
		return null;
	}

	public Value evaluate(Context cx, ConditionalExpressionNode node) 
	{
		System.out.println((new Throwable()).getStackTrace()[0].toString());
		return null;
	}

	public Value evaluate(Context cx, ArgumentListNode node)
	{
		System.out.println((new Throwable()).getStackTrace()[0].toString());
		try {
			for (Node n : node.items) {
				thrift_cli.startOneArgument();
					n.evaluate(cx, this);
				thrift_cli.endOneArgument();
			}
		} catch (org.apache.thrift.TException e1) {

		}

		return null;
	}

	public Value evaluate(Context cx, ListNode node)
	{
		System.out.println((new Throwable()).getStackTrace()[0].toString());
		StringListValue values = new StringListValue();
		for ( Node n : node.items )
		{
			Value v = n.evaluate( cx, this );
			values.values.add( Extract2String( v ) );
		}
		System.out.println((new Throwable()).getStackTrace()[0].toString());

		return values;
	}

	// Statements

	public Value evaluate(Context cx, StatementListNode node)
	{
		System.out.println((new Throwable()).getStackTrace()[0].toString());
		try {
			thrift_cli.startStmtList();
			for (Node n : node.items) {
				if (n != null) {
					n.evaluate(cx, this);
				}
			}
			thrift_cli.endStmtList();
		} catch (org.apache.thrift.TException e1) {

		}        
        return null;
	}

	public Value evaluate(Context cx, EmptyElementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, EmptyStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ExpressionStatementNode node)
	{
		try 
		{
			thrift_cli.startStmtExpression();
			thrift_cli.startExpressionList();
        		if (node.expr != null)
        		{
        			node.expr.evaluate(cx, this);
        		}
        	thrift_cli.endExpressionList();
        	thrift_cli.endStmtExpression();
		} catch( org.apache.thrift.TException e1 ) {
			System.out.print("\nERROR - "+e1.toString());
			System.exit(1);		
		}

		return null;
	}

	public Value evaluate(Context cx, LabeledStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, IfStatementNode node) 
	{
		try { thrift_cli.startIfStatement();
		} catch (org.apache.thrift.TException e1) { }
		
		try { thrift_cli.startIfStatement_Condition();
		} catch (org.apache.thrift.TException e1) { }
		if (node.condition != null) {
			node.condition.evaluate(cx, this);
		}
		try { thrift_cli.endIfStatement_Condition();
		} catch (org.apache.thrift.TException e1) { }

		try { thrift_cli.startIfStatement_Then();
		} catch (org.apache.thrift.TException e1) { }
		if (node.thenactions != null)
			node.thenactions.evaluate(cx, this);
		try { thrift_cli.endIfStatement_Then();
		} catch (org.apache.thrift.TException e1) { }

		try { thrift_cli.startIfStatement_Else();
		} catch (org.apache.thrift.TException e1) { }
		if (node.elseactions != null)
			node.elseactions.evaluate(cx, this);
		try { thrift_cli.endtIfStatement_Else();
		} catch (org.apache.thrift.TException e1) { }
		
		try { thrift_cli.endIfStatement();
		} catch (org.apache.thrift.TException e1) { }
		return null;
	}

	public Value evaluate(Context cx, SwitchStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, CaseLabelNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, DoStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, WhileStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ForStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, WithStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ContinueStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, BreakStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ReturnStatementNode node)
	{
		if (node.expr == null) 
			return null;
		
		try {
			thrift_cli.startReturnStatement();
			if (node.expr != null) {
				node.expr.evaluate(cx, this);
			}
			thrift_cli.endReturnStatement();
		} catch (org.apache.thrift.TException e1) {
			System.out.print("\nERROR - " + e1.toString());
			System.exit(1);
		}

		return null;
		
	}

	public Value evaluate(Context cx, ThrowStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, TryStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, CatchClauseNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, FinallyClauseNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, UseDirectiveNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, IncludeDirectiveNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ImportNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, MetaDataNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}
	
	public Value evaluate(Context cx, DocCommentNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	// Definitions

	public Value evaluate(Context cx, ImportDirectiveNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, AttributeListNode node) {
		System.out.println((new Throwable()).getStackTrace()[0].toString());
		// This Function always evaluate "value" insteadof "thrift"

		StringListValue attrs = new StringListValue();
		
        for (Node n : node.items)
        {
            Value v = n.evaluate(cx, string_evaluator);
            attrs.values.add( Extract2String( v ) );
        }
       
		return attrs;
	}

	public Value evaluate(Context cx, VariableDefinitionNode node) 
	{
//        if (node.kind == CONST_TOKEN)
//        {
//            out.print("const");
//        }
//        else
//        {
//            out.print("var");
//        }
		try {
			
			String str_attrs = "";
	        if (node.attrs != null)
	        {
	            Value v = node.attrs.evaluate( cx, string_evaluator );

	            str_attrs =  Extract2String( v );
	        }
//	        if (node.list != null)
//	        {
//	            node.list.evaluate(cx, this);
//	        }
        
	        for (Node n : node.list.items)
			{
	        	if( n instanceof VariableBindingNode ){

	        		VariableBindingNode vbnode = (VariableBindingNode)(n);
	        		
//	        		String str_type = "";
	        		
	        		String str_name = vbnode.variable.identifier.name;
	        		System.out.println("variable's node type "+vbnode.variable.type.toString());
	        		Value type_value = vbnode.variable.type.evaluate(cx, string_evaluator);
	        		List<String> sl_type =  Extract2StringList( type_value );

	        		thrift_cli.startVariableDeclare(str_name, sl_type, str_attrs);
	        		System.out.println("variable declare - "+str_name+":"+sl_type+"'");
	        		if(vbnode.initializer != null)
	        			vbnode.initializer.evaluate(cx, this);
	        		thrift_cli.endVariableDeclare();		
	        	} else{
	        		System.out.println("variable declare - ERROR!!");
	        	}
			}
        
		} catch (org.apache.thrift.TException e1) {
		}
		return null;
	}

	public Value evaluate(Context cx, VariableBindingNode node) {
		if (node.variable != null)
        {
            node.variable.evaluate(cx, this);
        }
        
		
		if (node.initializer != null) {
			try {
				thrift_cli.startExpressionList();

				node.initializer.evaluate(cx, this);

				thrift_cli.endExpressionList();
			} catch (org.apache.thrift.TException e1) {
			}
		}
        
		return null;
	}

	public Value evaluate(Context cx, UntypedVariableBindingNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, TypedIdentifierNode node) 
	{
	
		if (node.identifier != null)
        {
            node.identifier.evaluate(cx, this);
        }
		if (node.type != null)
        {
            node.type.evaluate(cx, this);
        }
		return null;
	}

    public Value evaluate(Context cx, TypeExpressionNode node)
    {
    	System.out.println((new Throwable()).getStackTrace()[0].toString());
    	if (node.expr != null)
        {
            return node.expr.evaluate(cx, this);
        }
    	return null;
    }

	public Value evaluate(Context cx, FunctionDefinitionNode node)
	{
		System.out.println((new Throwable()).getStackTrace()[0].toString());  
		try {
			thrift_cli.startFunctionDefinition( is_abstract_function );
			
			if (node.attrs != null) {
				StringListValue sv = (StringListValue)(node.attrs.evaluate(cx, this));
				thrift_cli.functionAttribute( sv.values );
			} else
				thrift_cli.functionAttribute( new ArrayList<String>() );
			
			if (node.name != null) {
				node.name.evaluate(cx, this);
			}
		
			thrift_cli.startFunctionCommon();
				if (node.fexpr != null) {
					node.fexpr.evaluate(cx, this);
				}
			thrift_cli.endFunctionCommon();
			thrift_cli.endFunctionDefinition();
		} catch (org.apache.thrift.TException e1) {

		}
		return null;
	}

    public Value evaluate(Context cx, BinaryFunctionDefinitionNode node)
    {
		System.out.println((new Throwable()).getStackTrace()[0].toString());  
        return null;	
    }

	public Value evaluate(Context cx, FunctionNameNode node)
	{
		System.out.println((new Throwable()).getStackTrace()[0].toString());
		try {

			String str_fname = "_unknown_";
			if (node.identifier != null) {
				
				Value str_value = node.identifier.evaluate(cx, string_evaluator);
				str_fname =  Extract2String( str_value );
			}

			thrift_cli.functionName(str_fname);
		} catch (org.apache.thrift.TException e1) {

		}
		return null;
	}

	public Value evaluate(Context cx, FunctionSignatureNode node) 
	{
		System.out.println((new Throwable()).getStackTrace()[0].toString());
		try {

			String str_func_type = "";
			if (node.result != null) {
				Value v = node.result.evaluate(cx, string_evaluator);
				str_func_type =  Extract2String( v );
			} else
				str_func_type =  "void";
			
			thrift_cli.startFunctionSignature( str_func_type );
			
			if (node.parameter != null) {
				node.parameter.evaluate(cx, this);
			}
			if (node.inits != null) {
				node.inits.evaluate(cx, this);
			}

			thrift_cli.endFunctionSignature();
		} catch (org.apache.thrift.TException e1) {

		}
		return null;
	}

	public Value evaluate(Context cx, ParameterNode node)
	{
		try{
			String para_name = "";
			
			if (node.identifier != null) {
				Value v = node.identifier.evaluate(cx, string_evaluator);
				para_name =  Extract2String( v );
			}
			if (node.init != null)
			{
				node.init.evaluate(cx, string_evaluator);
			}
			
			List<String> para_type = new ArrayList<String>();
			if (node.type != null)
			{
				Value v = node.type.evaluate(cx, string_evaluator);
				para_type =  Extract2StringList( v );
			} 
				
			
			thrift_cli.startFunctionSignatureParameterMember( para_name, para_type );
			thrift_cli.endFunctionSignatureParameterMember();
		} catch (org.apache.thrift.TException e1) {
		}
		return null;
	}

	public Value evaluate(Context cx, ParameterListNode node) 
	{
		try{
			thrift_cli.startFunctionSignatureParameters();
	        for (int i = 0, size = node.items.size(); i < size; i++)
	        {
	            ParameterNode param = node.items.get(i);
	
	            if (param != null)
	            {
	                param.evaluate(cx, this);
	            }
	        }
        	thrift_cli.endFunctionSignatureParameters();
		} catch (org.apache.thrift.TException e1) {
		}
		return null;
	}

	public Value evaluate(Context cx, RestExpressionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, RestParameterNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, InterfaceDefinitionNode node)
	{
		System.out.println((new Throwable()).getStackTrace()[0].toString());
		try {
			ClassDefinition class_define = new ClassDefinition();
			class_define.has_attr = (node.attrs != null);
			class_define.has_baseclass = (node.baseclass != null);
			class_define.has_interface = (node.interfaces != null);
			class_define.has_stmt = (node.statements != null);
			class_define.object_type = ObjectType.TYPE_INTERFACE;
			

//			if (node.attrs != null) {
//				node.attrs.evaluate(cx, this);
//			}
			
			String s_classname = "";
			if (node.name != null) {
				Value v = node.name.evaluate(cx, string_evaluator );
				s_classname = Extract2String( v );
			}

			List<String>	sl_inherits = null;
			if (node.baseclass != null) {
				Value v = node.baseclass.evaluate(cx, string_evaluator);
				if( v instanceof StringListValue )
				{
					StringListValue sv = (StringListValue)( v );
					sl_inherits = sv.values;
				}				
			} else {
				sl_inherits = new ArrayList<String>();
			}
			
			List<String>	sl_interfaces  = null;
			if (node.interfaces != null) {
				Value v = node.interfaces.evaluate(cx, string_evaluator);
				if( v instanceof StringListValue )
				{
					StringListValue sv = (StringListValue)( v );
					sl_interfaces = sv.values;
				}				
			} else {
				sl_interfaces = new ArrayList<String>();
			}
			
			class_define.name = s_classname;
			class_define.inherits = sl_inherits;
			class_define.interfaces = sl_interfaces;
			thrift_cli.startClassDefinition( class_define );
			
				is_abstract_function = true;
				if (node.statements != null) {
					thrift_cli.startClassStmt();
					node.statements.evaluate(cx, this);
					thrift_cli.endClassStmt();
				}
				is_abstract_function = false;
			
			thrift_cli.endClassDefinition( );

		} catch (org.apache.thrift.TException e1) {
		}
		System.out.println((new Throwable()).getStackTrace()[0].toString());
		return null;

	}

	public Value evaluate(Context cx, ClassDefinitionNode node) 
	{
		System.out.println((new Throwable()).getStackTrace()[0].toString());
		try {
			ClassDefinition class_define = new ClassDefinition();
			class_define.has_attr = (node.attrs != null);
			class_define.has_baseclass = (node.baseclass != null);
			class_define.has_interface = (node.interfaces != null);
			class_define.has_stmt = (node.statements != null);
			class_define.object_type = ObjectType.TYPE_CLASS;

			if (node.attrs != null) {
				Value v = node.attrs.evaluate(cx, this);
				class_define.attribute = Extract2String( v );
			}
			
			String s_classname = "";
			
			if (node.name != null) {
				Value v = node.name.evaluate(cx, string_evaluator );
				s_classname = Extract2String( v );
			}

			List<String>	sl_inherits = null;
			if (node.baseclass != null) {
				Value v = node.baseclass.evaluate(cx, string_evaluator);
				if( v instanceof StringListValue )
				{
					StringListValue sv = (StringListValue)( v );
					sl_inherits = sv.values;
				}				
			} else {
				sl_inherits = new ArrayList<String>();
			}
			
			List<String>	sl_interfaces  = null;
			if (node.interfaces != null) {
				Value v = node.interfaces.evaluate(cx, string_evaluator);
				if( v instanceof StringListValue )
				{
					StringListValue sv = (StringListValue)( v );
					sl_interfaces = sv.values;
				}				
			} else {
				sl_interfaces = new ArrayList<String>();
			}
			
			class_define.name = s_classname;
			class_define.inherits = sl_inherits;
			class_define.interfaces = sl_interfaces;
			
			boolean do_pkgdef = false;
			if( node.pkgdef != null && node.pkgdef.pkg_inside_dirty == false) {
				node.pkgdef.evaluate(cx, this);
				do_pkgdef = true;
			}
			
			thrift_cli.startClassDefinition( class_define );
			
				if (node.statements != null) {
					thrift_cli.startClassStmt();
					node.statements.evaluate(cx, this);
					thrift_cli.endClassStmt();
				}
			
			thrift_cli.endClassDefinition( );
			
			if( node.pkgdef != null && do_pkgdef == true) {
				node.pkgdef.evaluate(cx, this);
			}

		} catch (org.apache.thrift.TException e1) {
		}
		return null;
	}

    public Value evaluate(Context cx, BinaryClassDefNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

    public Value evaluate(Context cx, BinaryInterfaceDefinitionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ClassNameNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, InheritanceNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, NamespaceDefinitionNode node)
	{
		System.out.println((new Throwable()).getStackTrace()[0].toString());  
		return null;
	}

	public Value evaluate(Context cx, ConfigNamespaceDefinitionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, PackageDefinitionNode node)
	{
		try {
			List<String> pkg_name_list = new ArrayList<String>();
			if( node.name != null )
			{
		        for (IdentifierNode id : node.name.id.list)
		        {
		            pkg_name_list.add( id.name );
		        }
			}
			
			if( node.flipDirty() )
				thrift_cli.endPackage( pkg_name_list );
			else
				thrift_cli.startPackage( pkg_name_list );
        
		} catch( org.apache.thrift.TException e1 ) {
			System.out.print("\nERROR - "+e1.toString());
			System.exit(1);
		}
		
		return null;
	}

	public Value evaluate(Context cx, PackageIdentifiersNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, PackageNameNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ProgramNode node)
	{
		try {
			thrift_cli.startProgram();
		    	if (node.statements != null)
		    	{
		    		node.statements.evaluate(cx, this);
		    	}
			thrift_cli.endProgram();
		} catch( org.apache.thrift.TException e1 ) {
			System.out.print("\nERROR - "+e1.toString());
			System.exit(1);
		}
		return null;
	}

    public Value evaluate(Context cx, BinaryProgramNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ErrorNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ToObjectNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, LoadRegisterNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, StoreRegisterNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

    public Value evaluate(Context cx, RegisterNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, HasNextNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

    public Value evaluate(Context cx, BoxNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, CoerceNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, PragmaNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

    public Value evaluate(Context cx, UsePrecisionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, UseNumericNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, UseRoundingNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;} 

    public Value evaluate(Context cx, PragmaExpressionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

    public Value evaluate(Context cx, DefaultXMLNamespaceNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

    
    private String Extract2String( Value v )
    {
		if( v!=null && v instanceof StringValue ) {
			StringValue sv = (StringValue)(v);
			return sv.getValue();
		} else if( v!=null && v instanceof StringListValue ) {
			StringListValue sv = (StringListValue)(v);
			return sv.values.size() > 0 ? sv.values.get(0) : "";
//			return sv.values.get(0);
		} else if( v instanceof QNValue)   {
			QNValue qual_value = (QNValue)(v);
			return qual_value.getName();
		} else
			return "";
    }
	
    private List<String> Extract2StringList( Value v )
    {
    	
    	
		if( v!=null && v instanceof StringValue ) {
			System.out.println((new Throwable()).getStackTrace()[0].toString());
			StringValue sv = (StringValue)(v);
			List<String> string_list = new ArrayList<String>();
			string_list.add( sv.getValue() );
			return string_list;
		} else if( v!=null && v instanceof StringListValue ) {
			System.out.println((new Throwable()).getStackTrace()[0].toString());
			StringListValue sv = (StringListValue)(v);
			List<String> answer = new ArrayList<String>();
			for( int idx=0; idx < sv.values.size() ; idx ++ ) 
			{
				List<String> tokens = SplitQualifiedName( sv.values.get(idx) );
				for( int tIdx = 0 ; tIdx < tokens.size() ; tIdx ++)
					answer.add( tokens.get(tIdx) );
			}
			return answer;
		} else if( v instanceof QNValue)   {
			System.out.println((new Throwable()).getStackTrace()[0].toString());
			QNValue qual_value = (QNValue)(v);
			
			List<String> string_list = new ArrayList<String>();
			string_list.add( qual_value.getName() );
			for( int idx=0; idx < qual_value.getQualifier().size() ; idx ++ ) 
			{
				List<String> tokens = SplitQualifiedName( qual_value.getQualifier().get(idx) );
				for( int tIdx = 0 ; tIdx < tokens.size() ; tIdx ++)
					string_list.add( tokens.get(tIdx) );
			}
				
			return string_list;
		} 
		
		return null;	
    }
    
    private List<String> SplitQualifiedName( String token )
    {
    	String[] splited =  token.split("\\.");
    	List<String> answer = new ArrayList<String>();
    	for( int idx = 0 ; idx < splited.length ; idx ++ )
    		answer . add( splited[idx]) ;
    	return answer;
    }
//    private boolean dont_throw_thrift = false;
}

