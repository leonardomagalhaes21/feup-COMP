grammar Javamm;

@header {
    package pt.up.fe.comp2025;
}

CLASS: 'class';
EXTENDS: 'extends';
IMPORT: 'import';
NEW: 'new';
RETURN: 'return';
STATIC: 'static';
VOID: 'void';
BOOLEAN: 'boolean';
TRUE: 'true';
FALSE: 'false';
THIS: 'this';
WHILE: 'while';
IF: 'if';
ELSE: 'else';
LENGTH: 'length';
INT: 'int';
STRING: 'String';
ELLIPSIS: '...';

INTEGER: [0-9]+;
ID: [a-zA-Z_][a-zA-Z0-9_]*;

WS: [ \t\n\r]+ -> skip;

program
    : importDecl* classDecl EOF
    ;

importDecl
    : IMPORT name+=ID ('.' name+=ID)* ';'
    ;

classDecl
    : CLASS name+=ID (EXTENDS superClass+=ID)? '{' classBody '}'
    ;

classBody
    : varDecl* methodDecl*
    ;

varDecl
    : type name=ID ';'
    ;

type
    : INT ('[' ']')*       # IntType
    | BOOLEAN              # BooleanType
    | ID ('[' ']')*        # ClassType
    | STRING ('[' ']')*    # StringType
    ;

param
    : type (ELLIPSIS name=ID | name=ID)
    ;

paramList
    : param (',' param)*
    ;

methodDecl
    : (STATIC | VOID | type) name=ID '(' paramList? ')' block
    | STATIC VOID mainMethod=ID '(' paramList? ')' block // Tratar o método main
    ;

block
    : '{' stmt* '}'
    ;

stmt
    : block                            # BlockStmt
    | expr ';'                         # ExprStmt
    | IF '(' expr ')' stmt (ELSE stmt)? # IfStmt
    | WHILE '(' expr ')' stmt          # WhileStmt
    | expr '=' expr ';'                # AssignStmt
    | expr '[' expr ']' '=' expr ';'   # ArrayAssignStmt
    | RETURN expr ';'                  # ReturnStmt
    ;

expr
    : expr '.' LENGTH                  # LengthExpr
    | expr '[' expr ']'                # ArrayAccessExpr
    | expr '.' ID '(' argList? ')'     # MemberCallExpr
    | NEW type '(' ')'                 # NewObjectExpr
    | NEW type '[' expr ']'            # NewArrayExpr
    | '(' expr ')'                     # ParenExpr
    | expr op=('+' | '-' | '*' | '/' | '<' | '&&') expr # BinaryExpr
    | '!' expr                         # NotExpr
    | TRUE                             # TrueLiteral
    | FALSE                            # FalseLiteral
    | THIS                             # ThisExpr
    | INTEGER                          # IntLiteral
    | ID                               # VarRefExpr
    | '[' expr (',' expr)* ']'         # ArrayInitExpr
    ;

argList
    : expr (',' expr)*
    ;
