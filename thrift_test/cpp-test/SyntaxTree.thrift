
namespace java tw.maple.generated
namespace cpp ast.dumper

typedef list<string> StringList

enum ExpressionType
{
    IDENTIFIER,
    CALL
}

enum IdentifierType
{
    TYPE_IDENTIFIER,
    ATTR_IDENTIFIER,
    IDENTIFIER, 
}

struct Identifier
{
    1: IdentifierType type,
    2: string name,
}

struct CallExpression
{
    1: bool is_new = 0,
}

service AstDumper
{

    oneway void ping(),
    oneway void ping2( 1: i32 echo ),
}

