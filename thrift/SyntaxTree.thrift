
namespace java tw.maple.generated
namespace cpp tw.maple.generated

typedef list<string> StringList

enum StmtType {
    EnterPackage,
    LeavePackage,
    
    Import,

    VariableDefine,
    FunctionDefine,    
}

struct Stmt {
    1: StmtType type
}

struct Parameter
{
    1: string name,
    2: string type
}

struct FuncSignature
{
    1: string ret_type,
    2: list<Parameter> param
}

struct Func
{
//    1: String name,
//    2: FuncSignature signature,
//    3: list<Stmt>
}

struct ProgramRoot {
    1: list<Stmt> stmt_list
}

struct Variable {
    1: string name,
    2: string type
// TODO expressions
}

struct Expression {
    1: string type1,
    2: string type2
}

service AstDumper
{
    oneway void startPackage( 1: StringList IDs ),
    oneway void addImport( 1: StringList packages ),   // Do we need this? using namespace? or what ever suck?

    oneway void startDefineFunction( 1: string name, 2: FuncSignature signature ),

    oneway void startStmtList(),
       oneway void defineVariables( 1: list<Variable> vars ),
       oneway void defineExpression( 1: list<Expression> exprs ),
    oneway void closeStmtList(),

    oneway void ping(),
    oneway void ping2( 1: i32 echo ),
}

