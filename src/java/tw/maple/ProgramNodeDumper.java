package tw.maple;


import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;

import macromedia.asc.parser.*;
import macromedia.asc.semantics.Value;
import macromedia.asc.util.Context;
//import sun.org.mozilla.javascript.internal.EvaluatorException;
import tw.maple.generated.*;
import static macromedia.asc.parser.Tokens.*;

public final class ProgramNodeDumper implements Evaluator 
{
	AstDumper.Client thrift_cli;
    public ProgramNodeDumper(AstDumper.Client cli)
    {
    	thrift_cli = cli;
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
        if(node instanceof TypeIdentifierNode)
        {
        }
        else if (node.isAttr())
        {
        }
        
        else
        {
        	try
        	{
        		Identifier id = new Identifier();
        		id.name = node.name;
        		thrift_cli.identifierExpression( id );
        	}
        	catch (org.apache.thrift.TException e1) 
    		{
    			
    		}
        }
        
		return null;
	}

	public Value evaluate(Context cx, InvokeNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ThisExpressionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, QualifiedIdentifierNode node) 
	{
		System.out.print("\n ----------> QualifiedIdentifierNode \n");
//		System.out.println((new Throwable()).getStackTrace()[0].toString());
		
		if (node.qualifier != null)
        {
            node.qualifier.evaluate(cx, this);
        }
		
		try 
		{
//			tw.maple.generated.LiteralString str = new tw.maple.generated.LiteralString();
//			str.value = node.name;
//			thrift_cli.identifierExpression( str );
			
    		Identifier id = new Identifier();
    		id.name = node.name;
    		thrift_cli.identifierExpression( id );
		}
		catch (org.apache.thrift.TException e1) 
		{
			
		}
//        node.qualifier.evaluate(cx, this);
		return null;
	}

    public Value evaluate(Context cx, QualifiedExpressionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

    public Value evaluate(Context cx, LiteralBooleanNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, LiteralNumberNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, LiteralStringNode node)
	{
		try 
		{
//			tw.maple.generated.Identifier id = new tw.maple.generated.Identifier( );
//			id.name = node.value;
//			thrift_cli.identifierExpression( id );
			
			tw.maple.generated.LiteralString str = new tw.maple.generated.LiteralString();
			str.value = node.value;
			thrift_cli.literalStringExpression( str );
		}
		catch (org.apache.thrift.TException e1) 
		{
			
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
        if (node.base != null)
        {
            node.base.evaluate(cx, this);
        }

        if (node.selector != null)
        {
            node.selector.evaluate(cx, this);
        }  
		return null;
	}

	public Value evaluate(Context cx, CallExpressionNode node)
	{
		CallExpression call_expression;
		
		System.out.print(" in call expression node \n");
		try {
			call_expression = new CallExpression();
			call_expression.is_new = node.is_new;
			thrift_cli.startCallExpression(call_expression);
			
			if (node.expr != null) {
				// Callee
				node.expr.evaluate(cx, this);
			}

			if (node.args != null) {
				thrift_cli.startAgumentList();
					node.args.evaluate(cx, this);
				thrift_cli.endAgumentList();
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
//		System.out.println((new Throwable()).getStackTrace()[0].toString());
		if (node.expr != null)
        {
            node.expr.evaluate(cx, this);
        }
		return null;
	}

	public Value evaluate(Context cx, SetExpressionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

    public Value evaluate(Context cx, ApplyTypeExprNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, UnaryExpressionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, BinaryExpressionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ConditionalExpressionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ArgumentListNode node)
	{
        for (Node n : node.items)
        {
            n.evaluate(cx, this);
        }
		return null;
	}

	public Value evaluate(Context cx, ListNode node)
	{
	
		for ( Node n : node.items )
		{
			n.evaluate( cx, this );
		}
		return null;
	}

	// Statements

	public Value evaluate(Context cx, StatementListNode node)
	{
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
			thrift_cli.startExpressionList();
        		if (node.expr != null)
        		{
        			node.expr.evaluate(cx, this);
        		}
        	thrift_cli.endExpressionList();
		} catch( org.apache.thrift.TException e1 ) {
			System.out.print("\nERROR - "+e1.toString());
			System.exit(1);		
		}

		return null;
	}

	public Value evaluate(Context cx, LabeledStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, IfStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, SwitchStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, CaseLabelNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, DoStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, WhileStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ForStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, WithStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ContinueStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, BreakStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ReturnStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

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

	public Value evaluate(Context cx, AttributeListNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, VariableDefinitionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, VariableBindingNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, UntypedVariableBindingNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, TypedIdentifierNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

    public Value evaluate(Context cx, TypeExpressionNode node)
    {
    	System.out.println((new Throwable()).getStackTrace()[0].toString());
    	if (node.expr != null)
        {
            node.expr.evaluate(cx, this);
        }
    	return null;
    }

	public Value evaluate(Context cx, FunctionDefinitionNode node)
	{
//		System.out.println((new Throwable()).getStackTrace()[0].toString());  
		try {
			thrift_cli.startFunctionDefinition();
			if (node.attrs != null) {
				node.attrs.evaluate(cx, this);
			}
		

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
//		System.out.println((new Throwable()).getStackTrace()[0].toString());  
        return null;	
    }

	public Value evaluate(Context cx, FunctionNameNode node)
	{
//		System.out.println((new Throwable()).getStackTrace()[0].toString());
		try {
			thrift_cli.startFunctionName();
		} catch (org.apache.thrift.TException e1) {

		}
		if (node.identifier != null) {
			node.identifier.evaluate(cx, this);
		}

		try {
			thrift_cli.endFunctionName();
		} catch (org.apache.thrift.TException e1) {

		}
		return null;
	}

	public Value evaluate(Context cx, FunctionSignatureNode node) 
	{
//		System.out.println((new Throwable()).getStackTrace()[0].toString());
		try {
			thrift_cli.startFunctionSignature();
		} catch (org.apache.thrift.TException e1) {

		}
        if (node.result != null)
        {
    		try {
    			thrift_cli.startFunctionSignatureReturnType();
    		} catch (org.apache.thrift.TException e1) {
    		}
            node.result.evaluate(cx, this);
    		try {
    			thrift_cli.endFunctionSignatureReturnType();
    		} catch (org.apache.thrift.TException e1) {
    		}
        }
		if (node.parameter != null)
        {
            node.parameter.evaluate(cx, this);
        }
        if (node.inits != null)
        {
        	node.inits.evaluate(cx, this);
        }
		
		try {
			thrift_cli.endFunctionSignature();
		} catch (org.apache.thrift.TException e1) {

		}
		return null;
	}

	public Value evaluate(Context cx, ParameterNode node)
	{
		try{
			thrift_cli.startFunctionSignatureParameterMember();
		} catch (org.apache.thrift.TException e1) {
		}
		
		if (node.identifier != null)
        {
            node.identifier.evaluate(cx, this);
        }
        if (node.init != null)
        {
            node.init.evaluate(cx, this);
        }
        if (node.type != null)
        {
            node.type.evaluate(cx, this);
        }
        try{
			thrift_cli.endFunctionSignatureParameterMember();
		} catch (org.apache.thrift.TException e1) {
		}
		return null;
	}

	public Value evaluate(Context cx, ParameterListNode node) 
	{
		System.out.println(" 0000000000000000000000000 in param list " );
		try{
			thrift_cli.startFunctionSignatureParameters();
		} catch (org.apache.thrift.TException e1) {
		}
        for (int i = 0, size = node.items.size(); i < size; i++)
        {
            ParameterNode param = node.items.get(i);

            if (param != null)
            {
                param.evaluate(cx, this);
            }
        }
        try{
			thrift_cli.endFunctionSignatureParameters();
		} catch (org.apache.thrift.TException e1) {
		}
		return null;
	}

	public Value evaluate(Context cx, RestExpressionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, RestParameterNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, InterfaceDefinitionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ClassDefinitionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

    public Value evaluate(Context cx, BinaryClassDefNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

    public Value evaluate(Context cx, BinaryInterfaceDefinitionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ClassNameNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, InheritanceNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, NamespaceDefinitionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

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

	
}
