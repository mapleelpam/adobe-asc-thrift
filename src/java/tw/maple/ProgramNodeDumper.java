package tw.maple;


import java.io.PrintWriter;

import macromedia.asc.parser.*;
import macromedia.asc.semantics.Value;
import macromedia.asc.util.Context;
//import sun.org.mozilla.javascript.internal.EvaluatorException;
import tw.maple.generated.AstDumper;
import static macromedia.asc.parser.Tokens.*;

public final class ProgramNodeDumper implements Evaluator 
{
	AstDumper.Client thirft_cli;
    public ProgramNodeDumper(AstDumper.Client cli)
    {
    	thirft_cli = cli;
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

	public Value evaluate(Context cx, DeleteExpressionNode node){return null;}

	public Value evaluate(Context cx, IdentifierNode node){return null;}

	public Value evaluate(Context cx, InvokeNode node){return null;}

	public Value evaluate(Context cx, ThisExpressionNode node){return null;}

    public Value evaluate(Context cx, QualifiedIdentifierNode node){return null;}

    public Value evaluate(Context cx, QualifiedExpressionNode node){return null;}

    public Value evaluate(Context cx, LiteralBooleanNode node){return null;}

	public Value evaluate(Context cx, LiteralNumberNode node){return null;}

	public Value evaluate(Context cx, LiteralStringNode node){return null;}

	public Value evaluate(Context cx, LiteralNullNode node){return null;}

	public Value evaluate(Context cx, LiteralRegExpNode node){return null;}

	public Value evaluate(Context cx, LiteralXMLNode node){return null;}

	public Value evaluate(Context cx, FunctionCommonNode node){return null;}

	public Value evaluate(Context cx, ParenExpressionNode node){return null;}

	public Value evaluate(Context cx, ParenListExpressionNode node){return null;}

	public Value evaluate(Context cx, LiteralObjectNode node){return null;}

	public Value evaluate(Context cx, LiteralFieldNode node){return null;}

	public Value evaluate(Context cx, LiteralArrayNode node){return null;}
	
	public Value evaluate(Context cx, LiteralVectorNode node){return null;}

	public Value evaluate(Context cx, SuperExpressionNode node){return null;}

	public Value evaluate(Context cx, SuperStatementNode node){return null;}

	public Value evaluate(Context cx, MemberExpressionNode node){return null;}

	public Value evaluate(Context cx, CallExpressionNode node){return null;}

	public Value evaluate(Context cx, GetExpressionNode node){return null;}

	public Value evaluate(Context cx, SetExpressionNode node){return null;}

    public Value evaluate(Context cx, ApplyTypeExprNode node){return null;}

	public Value evaluate(Context cx, UnaryExpressionNode node){return null;}

	public Value evaluate(Context cx, BinaryExpressionNode node){return null;}

	public Value evaluate(Context cx, ConditionalExpressionNode node){return null;}

	public Value evaluate(Context cx, ArgumentListNode node){return null;}

	public Value evaluate(Context cx, ListNode node){return null;}

	// Statements

	public Value evaluate(Context cx, StatementListNode node){return null;}

	public Value evaluate(Context cx, EmptyElementNode node){return null;}

	public Value evaluate(Context cx, EmptyStatementNode node){return null;}

	public Value evaluate(Context cx, ExpressionStatementNode node){return null;}

	public Value evaluate(Context cx, LabeledStatementNode node){return null;}

	public Value evaluate(Context cx, IfStatementNode node){return null;}

	public Value evaluate(Context cx, SwitchStatementNode node){return null;}

	public Value evaluate(Context cx, CaseLabelNode node){return null;}

	public Value evaluate(Context cx, DoStatementNode node){return null;}

	public Value evaluate(Context cx, WhileStatementNode node){return null;}

	public Value evaluate(Context cx, ForStatementNode node){return null;}

	public Value evaluate(Context cx, WithStatementNode node){return null;}

	public Value evaluate(Context cx, ContinueStatementNode node){return null;}

	public Value evaluate(Context cx, BreakStatementNode node){return null;}

	public Value evaluate(Context cx, ReturnStatementNode node){return null;}

	public Value evaluate(Context cx, ThrowStatementNode node){return null;}

	public Value evaluate(Context cx, TryStatementNode node){return null;}

	public Value evaluate(Context cx, CatchClauseNode node){return null;}

	public Value evaluate(Context cx, FinallyClauseNode node){return null;}

	public Value evaluate(Context cx, UseDirectiveNode node){return null;}

	public Value evaluate(Context cx, IncludeDirectiveNode node){return null;}

	public Value evaluate(Context cx, ImportNode node){return null;}

	public Value evaluate(Context cx, MetaDataNode node){return null;}
	
	public Value evaluate(Context cx, DocCommentNode node){return null;}

	// Definitions

	public Value evaluate(Context cx, ImportDirectiveNode node){return null;}

	public Value evaluate(Context cx, AttributeListNode node){return null;}

	public Value evaluate(Context cx, VariableDefinitionNode node){return null;}

	public Value evaluate(Context cx, VariableBindingNode node){return null;}

	public Value evaluate(Context cx, UntypedVariableBindingNode node){return null;}

	public Value evaluate(Context cx, TypedIdentifierNode node){return null;}

    public Value evaluate(Context cx, TypeExpressionNode node){return null;}

	public Value evaluate(Context cx, FunctionDefinitionNode node){return null;}

    public Value evaluate(Context cx, BinaryFunctionDefinitionNode node){return null;}

	public Value evaluate(Context cx, FunctionNameNode node){return null;}

	public Value evaluate(Context cx, FunctionSignatureNode node){return null;}

	public Value evaluate(Context cx, ParameterNode node){return null;}

	public Value evaluate(Context cx, ParameterListNode node){return null;}

	public Value evaluate(Context cx, RestExpressionNode node){return null;}

	public Value evaluate(Context cx, RestParameterNode node){return null;}

	public Value evaluate(Context cx, InterfaceDefinitionNode node){return null;}

	public Value evaluate(Context cx, ClassDefinitionNode node){return null;}

    public Value evaluate(Context cx, BinaryClassDefNode node){return null;}

    public Value evaluate(Context cx, BinaryInterfaceDefinitionNode node){return null;}

	public Value evaluate(Context cx, ClassNameNode node){return null;}

	public Value evaluate(Context cx, InheritanceNode node){return null;}

	public Value evaluate(Context cx, NamespaceDefinitionNode node){return null;}

	public Value evaluate(Context cx, ConfigNamespaceDefinitionNode node){return null;}

	public Value evaluate(Context cx, PackageDefinitionNode node){return null;}

	public Value evaluate(Context cx, PackageIdentifiersNode node){return null;}

	public Value evaluate(Context cx, PackageNameNode node){return null;}

	public Value evaluate(Context cx, ProgramNode node){return null;}

    public Value evaluate(Context cx, BinaryProgramNode node){return null;}

	public Value evaluate(Context cx, ErrorNode node){return null;}

	public Value evaluate(Context cx, ToObjectNode node){return null;}

	public Value evaluate(Context cx, LoadRegisterNode node){return null;}

	public Value evaluate(Context cx, StoreRegisterNode node){return null;}

    public Value evaluate(Context cx, RegisterNode node){return null;}

	public Value evaluate(Context cx, HasNextNode node){return null;}

    public Value evaluate(Context cx, BoxNode node){return null;}

	public Value evaluate(Context cx, CoerceNode node){return null;}

	public Value evaluate(Context cx, PragmaNode node){return null;}

    public Value evaluate(Context cx, UsePrecisionNode node){return null;}

	public Value evaluate(Context cx, UseNumericNode node){return null;}

	public Value evaluate(Context cx, UseRoundingNode node){return null;} 

    public Value evaluate(Context cx, PragmaExpressionNode node){return null;}

    public Value evaluate(Context cx, DefaultXMLNamespaceNode node){return null;}

	
}
