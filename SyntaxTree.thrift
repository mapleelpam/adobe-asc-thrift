
namespace java ast.dumper
namespace cpp ast.dumper

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
/*
    bool startProgram(),
    bool startPackage( 1: StringList IDs ),
    bool addImport( 1: StringList packages ),   // Do we need this? using namespace? or what ever suck?

    bool startDefineFunction( 1: string name, 2: FuncSignature signature ),

    bool startStmtList(),
       bool defineVariables( 1: list<Variable> vars ),
       bool defineExpression( 1: list<Expression> exprs ),
    bool closeStmtList(),
*/

    void ping()
}

