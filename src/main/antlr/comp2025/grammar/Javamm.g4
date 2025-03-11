grammar Javamm;

@header {
    package pt.up.fe.comp2025;
}

PUBLIC: 'public';
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
ID: [a-zA-Z_$][a-zA-Z0-9_$]*;

WS: [ \t\n\r\f]+ -> skip;
SINGLE_LINE_COMMENT : '//' .*? '\n' -> skip;
MULTI_LINE_COMMENT  : '/*' .*? '*/' -> skip;


program
    : importDecl* classDecl EOF
    ;

importDecl
    : IMPORT name+=ID ('.' name+=ID)* ';'       # ImportDeclaration
    ;

classDecl
    : CLASS name=ID (EXTENDS superClass=ID)? '{' varDecl* methodDecl* '}'   # ClassDeclaration
    ;

varDecl
    : typename=type name=ID ';'      # Variable
    ;


//rever
type locals[boolean isArray=false, boolean isVarargs=false]
    : name=INT ('[' INTEGER? ']' {$isArray=true;})?
    | name=INT (ELLIPSIS {$isArray=true; $isVarargs=true;})?
    | name=BOOLEAN
    | name=VOID
    | name='String' ('[' ']' {$isArray=true;})?
    | name=ID;



methodDecl locals[boolean isPublic=false, boolean isStatic=false]
    : (PUBLIC {$isPublic=true;})?
      (STATIC {$isStatic=true;})?
      typename=type name=ID
      parameters=params
      block                 #Method
    ;

params
    : '(' (param (',' param)*)? ')'     #Parameters
    ;
param
    : typename=type (ELLIPSIS name=ID | name=ID) #Parameter
    ;

block
    : '{' varDecl* stmt* '}'
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


//rever
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
