package tw.maple;

import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;

import tw.maple.generated.Identifier;

import macromedia.asc.parser.*;
import macromedia.asc.semantics.Value;
import macromedia.asc.semantics.StringValue;
import macromedia.asc.semantics.StringListValue;
import macromedia.asc.semantics.QNValue;
import macromedia.asc.util.Context;
import static macromedia.asc.parser.Tokens.*;

public final class StringEvaluator implements Evaluator 
{
    public StringEvaluator()
    {
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
		return new StringValue( node.name );
	}
	public Value evaluate(Context cx, InvokeNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ThisExpressionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}
 
	public Value evaluate(Context cx, QualifiedIdentifierNode node) 
	{
		System.out.println((new Throwable()).getStackTrace()[0].toString()+ " "+node.toString());
		String name = node.name;
		if (node.qualifier != null) {
			Value ret_value = node.qualifier.evaluate(cx, this);
			System.out.println((new Throwable()).getStackTrace()[0].toString());
			if (ret_value instanceof StringValue) {
				StringValue qual_value = (StringValue) (ret_value);
				System.out.println((new Throwable()).getStackTrace()[0].toString() + "  "+qual_value.getValue());
				return new QNValue(name,qual_value.getValue());
			} else if (ret_value instanceof StringListValue) {
				StringListValue qual_value = (StringListValue) (ret_value);
				return new QNValue(name,qual_value.values);
			}
		}
		return new QNValue(name,"");
	}

	public Value evaluate(Context cx, QualifiedExpressionNode node) {
		System.out.println((new Throwable()).getStackTrace()[0].toString());
		return null;
	}

    public Value evaluate(Context cx, LiteralBooleanNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, LiteralNumberNode node)
	{
//		System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;
		return new StringValue(node.value);
	}

	public Value evaluate(Context cx, LiteralStringNode node)
	{
		System.out.println((new Throwable()).getStackTrace()[0].toString() +" value = "+node.value);
		return new StringValue(node.value);
	}
	public Value evaluate(Context cx, LiteralNullNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, LiteralRegExpNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, LiteralXMLNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, FunctionCommonNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

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
		System.out.println((new Throwable()).getStackTrace()[0].toString());
		StringListValue slv = new StringListValue();
        if (node.base != null)
        {
            Value v = node.base.evaluate(cx, this);
            List<String> stringlist = Extract2StringList( v );
            for( int idx=0; idx < stringlist.size() ; idx ++ )
            	slv.values.add( stringlist.get(idx) );
        }

        if (node.selector != null)
        {
            Value v = node.selector.evaluate(cx, this);
            List<String> stringlist = Extract2StringList( v );
            for( int idx=0; idx < stringlist.size() ; idx ++ ){
            	System.out.println(" -------> selector -> "+stringlist.get(idx));
            	slv.values.add( stringlist.get(idx) );
            }
        }  
		return slv;
	}
	public Value evaluate(Context cx, CallExpressionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}
	public Value evaluate(Context cx, GetExpressionNode node)
	{
		System.out.println((new Throwable()).getStackTrace()[0].toString());
		if (node.expr != null)
        {
            return node.expr.evaluate(cx, this);
        }
		return null;
	}
	public Value evaluate(Context cx, SetExpressionNode node) {System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

    public Value evaluate(Context cx, ApplyTypeExprNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, UnaryExpressionNode node) {System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, BinaryExpressionNode node) {System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}
	public Value evaluate(Context cx, ConditionalExpressionNode node) 
	{
		System.out.println((new Throwable()).getStackTrace()[0].toString());
		return null;
	}

	public Value evaluate(Context cx, ArgumentListNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ListNode node)
	{
		System.out.println((new Throwable()).getStackTrace()[0].toString());
		StringListValue values = new StringListValue();
		for ( Node n : node.items )
		{
			Value v = n.evaluate( cx, this );
			if( v != null && v instanceof StringValue )
			{
				StringValue sv = (StringValue)( v );
				values.values.add(sv.getValue());
			} 
			else if( v != null && v instanceof StringListValue )
			{
				StringListValue sv = (StringListValue)( v );
				for( int idx = 0 ; idx < sv.values.size() ; idx ++) {
					System.out.println( " hey i add '"+sv.values.get(idx)+"' into strings" );
					values.values.add( sv.values.get(idx) );
				}
			}
			else if ( v != null )
			{
				System.out.println(" !!!!!!!! hey look here!! somthing wrong !!"+v.getPrintableName());
			}
		}
		return values;
	}
	// Statements

	public Value evaluate(Context cx, StatementListNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}
	public Value evaluate(Context cx, EmptyElementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, EmptyStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ExpressionStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}
	public Value evaluate(Context cx, LabeledStatementNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, IfStatementNode node) {System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}
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

	public Value evaluate(Context cx, AttributeListNode node) {
		System.out.println((new Throwable()).getStackTrace()[0].toString());
		// This Function always evaluate "value" insteadof "thrift"

		StringListValue attrs = new StringListValue();
		
        for (Node n : node.items)
        {
            Value v = n.evaluate(cx, this);
            if( v instanceof StringValue )
            {
            	StringValue s = (StringValue)( v );
            	attrs.values.add( s.getValue() );
            } 
            else if( v != null && v instanceof StringListValue )
            {
				StringListValue sv = (StringListValue)( v );
				for( int idx = 0 ; idx < sv.values.size() ; idx ++) {
					System.out.println( "attrs hey i add '"+sv.values.get(idx)+"' into strings" );
					attrs.values.add( sv.values.get(idx) );
				}
            } 
            else 
            {
	            if( v == null)
	            	System.out.println(" ------------> v == null" ) ;
	            else
	            	System.out.println(" ------------>"+  v.getPrintableName() ) ;
            }
            
        }
		return attrs;
	}

	public Value evaluate(Context cx, VariableDefinitionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, VariableBindingNode node) {System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, UntypedVariableBindingNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, TypedIdentifierNode node) {System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

    public Value evaluate(Context cx, TypeExpressionNode node)
    {
    	System.out.println((new Throwable()).getStackTrace()[0].toString());
    	if (node.expr != null)
        {
            return node.expr.evaluate(cx, this);
        }
    	System.out.println((new Throwable()).getStackTrace()[0].toString());
    	return null;
    }

	public Value evaluate(Context cx, FunctionDefinitionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}
    public Value evaluate(Context cx, BinaryFunctionDefinitionNode node)
    {
		System.out.println((new Throwable()).getStackTrace()[0].toString());  
        return null;	
    }

	public Value evaluate(Context cx, FunctionNameNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, FunctionSignatureNode node) 
	{System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}
	public Value evaluate(Context cx, ParameterNode node)
	{System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ParameterListNode node) 
	{System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, RestExpressionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, RestParameterNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, InterfaceDefinitionNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ClassDefinitionNode node) 
	{System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

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
		System.out.println((new Throwable()).getStackTrace()[0].toString());
		List<String> pkg_name_list = new ArrayList<String>();
		if( node.name != null )
		{
	        for (IdentifierNode id : node.name.id.list)
	        {
	            pkg_name_list.add( id.name );
	        }
		}
		StringListValue v = new StringListValue( pkg_name_list );
		return v;
	}
	public Value evaluate(Context cx, PackageIdentifiersNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, PackageNameNode node){System.out.println((new Throwable()).getStackTrace()[0].toString());  return null;}

	public Value evaluate(Context cx, ProgramNode node)
	{
		System.out.println((new Throwable()).getStackTrace()[0].toString());  
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
			return sv.values;
//			return sv.values.get(0);
		} else if( v instanceof QNValue)   {
			System.out.println((new Throwable()).getStackTrace()[0].toString());
			QNValue qual_value = (QNValue)(v);
			
			List<String> string_list = new ArrayList<String>();
			for( int idx=0; idx < qual_value.getQualifier().size() ; idx ++ )
				string_list.add( qual_value.getQualifier().get(idx) );
			string_list.add( qual_value.getName() );
			return string_list;
		} 
		
		return null;	
    }
}

