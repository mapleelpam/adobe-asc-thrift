package macromedia.asc.parser;

import macromedia.asc.semantics.Value;
import macromedia.asc.util.Context;

/**
 * Node
 *
 * @author Erik Tierney
 */
public class TypeIdentifierNode extends IdentifierNode {

    public ListNode typeArgs;

    public TypeIdentifierNode(String name, ListNode typeArgs, int pos) {
        super(name, pos);
        this.typeArgs = typeArgs;
    }
    public Value evaluate(Context cx, Evaluator evaluator)
    {
        if (evaluator.checkFeature(cx, this))
        {
            return evaluator.evaluate(cx, this);
        }
        else
        {
            return null;
        }
    }

    public String toString()
    {
        if(Node.useDebugToStrings)
             return "TypeIdentifier@" + pos();
          else
             return "TypeIdentifier";
    }
}
