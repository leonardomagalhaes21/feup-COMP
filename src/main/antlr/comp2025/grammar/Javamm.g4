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

methodDecl locals[boolean isPublic=false, boolean isStatic=false]
    : (PUBLIC {$isPublic=true;})?
      (STATIC {$isStatic=true;})?
      typename=type name=ID
      parameters=params
      '{' varDecl* stmt* '}'                 #Method
    ;

type locals[boolean isArray=false, boolean isVarargs=false]
    : name=INT ('[' INTEGER? ']' {$isArray=true;})?
    | name=INT (ELLIPSIS {$isArray=true; $isVarargs=true;})?
    | name=BOOLEAN
    | name=VOID
    | name='String' ('[' ']' {$isArray=true;})?
    | name=ID;

params
    : '(' (param (',' param)*)? ')'     #Parameters
    ;

param
    : typename=type name=ID #Parameter
    ;

stmt
    : '{' varDecl* stmt* '}'           # BlockStmt
    | expr ';'                         # ExprStmt
    | IF '(' expr ')' stmt (ELSE stmt)? # IfStmt
    | WHILE '(' expr ')' stmt          # WhileStmt
    | expr '=' expr ';'                # AssignStmt
    | expr '[' expr ']' '=' expr ';'   # ArrayAssignStmt
    | RETURN expr ';'                  # ReturnStmt
    ;

varDecl
    : typename=type name=ID ';'      # Variable
    ;

expr
    : '(' expr ')'                      # ParenExpr
    | '!' expr                          # UnaryExpr
    | expr '.' methodname='length'      #ArrayLengthExpr
    | expr '.' methodname=ID            # FuncExpr
    | expr '.' methodname=ID
      '(' (expr (',' expr)* )? ')'      # FuncExpr
    | expr ('.' expr)+                  # MemberExpr
    | value=INTEGER                     # IntegerLiteral
    | value=('true' | 'false')          # BooleanLiteral
    | name=ID                           # VarRefExpr
    | expr ('[' expr ']')+              # ArrayAccessExpr
    | '[' (expr (',' expr)*)? ']'       # ArrayExpr
    | NEW classname=ID '(' ')'          # NewExpr
    | NEW INT '[' expr ']'              # NewArrayExpr
    | name=THIS                         # ThisExpr
    | expr op=('*' | '/') expr          # BinaryExpr
    | expr op=('+' | '-') expr          # BinaryExpr
    | expr op=('<=' | '<' | '>' | '>=')
      expr                              # BinaryExpr
    | expr op=('=='| '!=') expr         # BinaryExpr
    | expr op=('||' | '&&') expr        # BinaryExpr
    ;
