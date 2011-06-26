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
	private boolean DEBUG = false;
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

	public Value evaluate(Context cx, DeleteExpressionNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, IdentifierNode node)
	{
		return new StringValue( node.name );
	}
	public Value evaluate(Context cx, InvokeNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, ThisExpressionNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}
 
	public Value evaluate(Context cx, QualifiedIdentifierNode node) 
	{
		System.out.println((new Throwable()).getStackTrace()[0].toString()+ " "+node.toString());
		String name = node.name;
		if (node.qualifier != null) {
			Value ret_value = node.qualifier.evaluate(cx, this);
			if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}
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
		if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}
		return null;
	}

    public Value evaluate(Context cx, LiteralBooleanNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, LiteralNumberNode node)
	{
//		if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;
		return new StringValue(node.value);
	}

	public Value evaluate(Context cx, LiteralStringNode node)
	{
		System.out.println((new Throwable()).getStackTrace()[0].toString() +" value = "+node.value);
		return new StringValue(node.value);
	}
	public Value evaluate(Context cx, LiteralNullNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, LiteralRegExpNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, LiteralXMLNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, FunctionCommonNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, ParenExpressionNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, ParenListExpressionNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, LiteralObjectNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, LiteralFieldNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, LiteralArrayNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}
	
	public Value evaluate(Context cx, LiteralVectorNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, SuperExpressionNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, SuperStatementNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, MemberExpressionNode node)
	{
		if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}
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
            if( stringlist != null )
            for( int idx=0; idx < stringlist.size() ; idx ++ ){
            	System.out.println(" -------> selector -> "+stringlist.get(idx));
            	slv.values.add( stringlist.get(idx) );
            }
        }  
		return slv;
	}
	public Value evaluate(Context cx, CallExpressionNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}
	public Value evaluate(Context cx, GetExpressionNode node)
	{
		if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}
		if (node.expr != null)
        {
            return node.expr.evaluate(cx, this);
        }
		return null;
	}
	public Value evaluate(Context cx, SetExpressionNode node) {if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

    public Value evaluate(Context cx, ApplyTypeExprNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, UnaryExpressionNode node) {if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, BinaryExpressionNode node) {if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}
	public Value evaluate(Context cx, ConditionalExpressionNode node) 
	{
		if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}
		return null;
	}

	public Value evaluate(Context cx, ArgumentListNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, ListNode node)
	{
		if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}
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

	public Value evaluate(Context cx, StatementListNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}
	public Value evaluate(Context cx, EmptyElementNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, EmptyStatementNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, ExpressionStatementNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}
	public Value evaluate(Context cx, LabeledStatementNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, IfStatementNode node) {if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}
	public Value evaluate(Context cx, SwitchStatementNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, CaseLabelNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, DoStatementNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, WhileStatementNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, ForStatementNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, WithStatementNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, ContinueStatementNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, BreakStatementNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, ReturnStatementNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, ThrowStatementNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, TryStatementNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, CatchClauseNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, FinallyClauseNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, UseDirectiveNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, IncludeDirectiveNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, ImportNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, MetaDataNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}
	
	public Value evaluate(Context cx, DocCommentNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	// Definitions

	public Value evaluate(Context cx, ImportDirectiveNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, AttributeListNode node) {
		if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}
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

	public Value evaluate(Context cx, VariableDefinitionNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, VariableBindingNode node) {if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, UntypedVariableBindingNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, TypedIdentifierNode node) {if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

    public Value evaluate(Context cx, TypeExpressionNode node)
    {
    	if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}
    	if (node.expr != null)
        {
            return node.expr.evaluate(cx, this);
        }
    	if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}
    	return null;
    }

	public Value evaluate(Context cx, FunctionDefinitionNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}
    public Value evaluate(Context cx, BinaryFunctionDefinitionNode node)
    {
		if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  
        return null;	
    }

	public Value evaluate(Context cx, FunctionNameNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, FunctionSignatureNode node) 
	{if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}
	public Value evaluate(Context cx, ParameterNode node)
	{if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, ParameterListNode node) 
	{if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, RestExpressionNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, RestParameterNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, InterfaceDefinitionNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, ClassDefinitionNode node) 
	{if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

    public Value evaluate(Context cx, BinaryClassDefNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

    public Value evaluate(Context cx, BinaryInterfaceDefinitionNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, ClassNameNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, InheritanceNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, NamespaceDefinitionNode node)
	{
		if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  
		return null;
	}

	public Value evaluate(Context cx, ConfigNamespaceDefinitionNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, PackageDefinitionNode node)
	{
		if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}
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
	public Value evaluate(Context cx, PackageIdentifiersNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, PackageNameNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, ProgramNode node)
	{
		if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  
		return null;
	}

    public Value evaluate(Context cx, BinaryProgramNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, ErrorNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, ToObjectNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, LoadRegisterNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, StoreRegisterNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

    public Value evaluate(Context cx, RegisterNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, HasNextNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

    public Value evaluate(Context cx, BoxNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, CoerceNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, PragmaNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

    public Value evaluate(Context cx, UsePrecisionNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, UseNumericNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

	public Value evaluate(Context cx, UseRoundingNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;} 

    public Value evaluate(Context cx, PragmaExpressionNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

    public Value evaluate(Context cx, DefaultXMLNamespaceNode node){if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}  return null;}

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
			if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}
			StringValue sv = (StringValue)(v);
			List<String> string_list = new ArrayList<String>();
			string_list.add( sv.getValue() );
			return string_list;
		} else if( v!=null && v instanceof StringListValue ) {
			if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}
			StringListValue sv = (StringListValue)(v);
			return sv.values;
//			return sv.values.get(0);
		} else if( v instanceof QNValue)   {
			if(DEBUG){System.out.println((new Throwable()).getStackTrace()[0].toString());}
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

