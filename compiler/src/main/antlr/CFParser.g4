parser grammar  CFParser;

options {
    tokenVocab=CFLexer;
}

script
    :   importStatement*
        (component | interface | functionOrStatement * | tag )
    |
    EOF
    ;

tag
  : LCFTAG script RCFTAG
  ;

eos
    :   SEMICOLON
    ;
package
    :   PACKAGE identifier eos
    ;

importStatement
    : IMPORT identifier (DOT STAR)? eos
    ;

include
    :   INCLUDE expression eos
    ;

component
    :   ABSTRACT? COMPONENT identifier? componentAttribute*
        LBRACE functionOrStatement* RBRACE
    ;

interface
    :   INTERFACE identifier? interfaceAttribute* LBRACE interfaceFunction* RBRACE
    ;

interfaceAttribute
    :   extendAttribute
    |   identifier EQUAL expression
    ;

interfaceFunction
    :    functionSignature eos
    ;

componentAttribute
    :   extendAttribute
    |   implementsAttribute
    |   identifier EQUAL expression
    ;

extendAttribute
    :   EXTENDS EQUAL expression
    ;

implementsAttribute
    :   IMPLEMENTS EQUAL stringLiteral (COMMA stringLiteral)*
    ;

functionOptions
    :   (parameters+=identifier (EQUAL values+=literalExpression)?)+
    ;
functionSignature
    :   accessModifier? STATIC?  returnType?
        FUNCTION identifier LPAREN paramList? RPAREN
    ;
function
    :   functionSignature
        functionOptions?
        statementBlock
    ;

paramList
    :   param (COMMA param)*
    ;

param
  : (REQUIRED)? (type)? identifier ( EQUAL expression )?
  | (REQUIRED)? (type)? identifier ( EQUAL statementBlock )?
  ;

returnType
    :   type
    |   identifier
    ;

accessModifier
    :   PUBLIC
    |   PRIVATE
    |   REMOTE
    |   PACKAGE
	;

type
    :   NUMERIC
    |   STRING
    |   BOOLEAN
    |   COMPONENT
    |   INTERFACE
    |   ANY
    |   ARRAY
    |   STRUCT
    |   QUERY
    |   fqn
    ;

functionOrStatement
    :   constructor
    |   function
    |   statement
    ;

constructor
    :   FUNCTION INIT LPAREN paramList? RPAREN statementBlock
    ;

property
    :   PROPERTY (identifier EQUAL expression)+ (TYPE EQUAL stringLiteral)? (DEFAULT EQUAL stringLiteral)? eos
    ;

anonymousFunction
    :   FUNCTION LPAREN paramList? RPAREN statementBlock
    |   lambda
    ;

lambda
    :   LPAREN lambdaParameters? RPAREN ARROW_RIGHT (expression | statementBlock)
    ;
lambdaParameters
    :   identifier (COMMA identifier)*
    ;

statementBlock
    :   LBRACE (statement)* RBRACE eos?
    ;
statementParameters
    :   (parameters+=accessExpression EQUAL (values+=stringLiteral | expressions+=expression))+
    ;
statement
    :   break
    |   continue
    |   for
    |   if
    |   include
    |   lockStatement
    |   property
    |   rethrow
    |   saveContentStatement
    |   simpleStatement
    |   switch
//    |   statementBlock
    |   threadStatement
    |   throw
    |   try
    |   while
    ;
simpleStatement
    :   ( assignment
        | applicationStatement
        | functionInvokation
        | localDeclaration
        | methodInvokation
        | new
        | incrementDecrementStatement
        | paramStatement
        | return
        | settingStatement
        ) eos?
    ;

incrementDecrementStatement
    :   PLUS PLUS accessExpression      #preIncrement
    |   accessExpression PLUS PLUS      #postIncrement
    |   MINUS MINUS accessExpression    #preDecremenent
    |   accessExpression MINUS MINUS    #postDecrement
    ;

assignment
    :   assignmentLeft (EQUAL | PLUSEQUAL | STRCONCASSIGN) assignmentRight
    ;
assignmentLeft
    :   accessExpression
    |   accessExpression (EQUAL | PLUSEQUAL) assignmentLeft
    ;
assignmentRight
    :   expression
    ;

functionInvokation
    :   identifier LPAREN argumentList? RPAREN
    ;

methodInvokation
    :   objectExpression DOT functionInvokation
    |   accessExpression DOT functionInvokation
    |   methodInvokation DOT functionInvokation
    |   arrayExpression DOT functionInvokation
    ;

localDeclaration
    :   VAR identifier ((EQUAL identifier)* EQUAL expression )
    ;

settingStatement
    :   SETTING assignment
    ;

threadStatement
    :   THREAD statementParameters statementBlock
    ;

lockStatement
    :   LOCK statementParameters statementBlock
    ;

applicationStatement
    :   APPLICATION statementParameters
    ;

paramStatement
    :   PARAM statementParameters
    ;

saveContentStatement
    :   SAVECONTENT statementParameters statementBlock
    ;

argumentList
    :   argument (COMMA argument)*
    ;

argument
  : expression ( (EQUAL | COLON) expression )?
  ;

if
    :   IF LPAREN expression RPAREN ifStmt=statement ( ELSE elseStmt=statement )?
    |   IF LPAREN expression RPAREN ifStmtBlock=statementBlock ( ELSE elseStmt=statement )?
    |   IF LPAREN expression RPAREN ifStmt=statement ( ELSE elseStmtBlock=statementBlock )?
    |   IF LPAREN expression RPAREN ifStmtBlock=statementBlock ( ELSE elseStmtBlock=statementBlock )?
    ;
for
    :   FOR LPAREN VAR? identifier IN expression RPAREN statementBlock
    |   FOR LPAREN forAssignment eos forCondition eos forIncrement RPAREN statementBlock
    ;
forAssignment
    :   VAR? identifier EQUAL expression
    ;
forCondition
    :   expression
    ;
forIncrement
    :   incrementDecrementStatement
    |   assignment
    ;

while
    :   WHILE LPAREN expression RPAREN statementBlock
    ;

break
    :   BREAK eos
    ;
continue
    :   CONTINUE eos
    ;
return
    :   RETURN expression?
    ;

rethrow
    :   RETHROW eos
    ;
throw
    :   THROW LPAREN argumentList? RPAREN eos
    ;

switch
  : SWITCH expression LBRACE
    (
      case
    )*

    RBRACE
  ;

case
	: CASE (expression) COLON (statement | statementBlock)?
	| DEFAULT COLON (statement | statementBlock)?
  ;

identifier
    :   IDENTIFIER
    |	reservedKeyword
    ;
reservedKeyword
    :   scope
    |	CONTAINS
    |   DEFAULT
    |   EXTENDS
    |	FUNCTION
    |   IMPLEMENTS
    |   INCLUDE
    |   INIT
    |	MOD
    |   NEW
    |	NUMERIC
    |   SETTING
    |	STRING
    |   STRUCT
    |   PRIVATE
    |   QUERY
    |   TYPE
    |   VAR
    |   WHEN
    | 	DOES
    | 	NOT
    | 	CONTAIN
    ;
scope
    :   APPLICATION
    |   ARGUMENTS
    |   LOCAL
    |   REQUEST
    |   VARIABLES
    |   THIS
    |   THREAD
    //  TODO add additional known scopes
    ;

try
    :   TRY statementBlock ( catch_ )* finally_?
    ;

catch_
    :   CATCH LPAREN catchType? expression RPAREN statementBlock
    ;

finally_
    :   FINALLY statementBlock
    ;

catchType
    :   type
    |   stringLiteral
    ;
stringLiteral
    :  OPEN_QUOTE (stringLiteralPart | ICHAR (expression) ICHAR)* CLOSE_QUOTE;

stringLiteralPart
    :  STRING_LITERAL | HASHHASH;

integerLiteral
    :   INTEGER_LITERAL
    ;
floatLiteral
    :   FLOAT_LITERAL
    ;

booleanLiteral
    :   TRUE | FALSE
    ;

arrayExpression
    :   LBRACKET arrayValues? RBRACKET
//    |   identifier LBRACKET arrayValues? RBRACKET
    ;

arrayValues
    :   expression (COMMA expression)*
    ;

arrayAccess
    :   identifier arrayAccessIndex
    |   arrayAccess arrayAccessIndex
    ;
arrayAccessIndex
    :   LBRACKET
        expression
        RBRACKET
    ;

structExpression
    :   LBRACE structMembers? RBRACE
    ;
structMembers
    :   structMember (COMMA structMember)* COMMA?
    ;
structMember
    :   identifier  (COLON | EQUAL) expression
    |   stringLiteral  (COLON | EQUAL) expression
    ;

unary
    :   (MINUS | PLUS) literalExpression
    ;

not
    :   NOT expression
    ;
new
    :   NEW (fqn | stringLiteral) LPAREN argumentList? RPAREN
    // TODO add namespace
    ;

fqn
    :   (identifier DOT)* identifier
    ;

expression
    :   unary
    |   not
    |   new
    |   incrementDecrementStatement
    |   literalExpression
    |   objectExpression
    |	identifier
    |   LPAREN expression RPAREN
    |   accessExpression
    |   methodInvokation
    |   anonymousFunction
    |   expression QM expression COLON expression // Ternary
    |   expression (AND | OR) expression // Logical
    |   expression (PLUS | MINUS | STAR | SLASH | PERCENT | AMPERSAND | MOD) expression // Math
    |   expression (EQ | GT | GTE | LT | LTE | NEQ | CONTAINS | NOT CONTAINS) expression // Comparision
    |   expression ELVIS expression // Elvis operator
    |   expression IS expression // IS operator
    |	expression DOES NOT CONTAIN expression
    ;

literalExpression
    :   integerLiteral
    |   floatLiteral
    |   stringLiteral
    |   booleanLiteral
    ;

objectExpression
    :   anonymousFunction
    |   arrayExpression
    |   arrayAccess
    |   structExpression
    |   functionInvokation
    |   identifier
    |   new
    ;

accessExpression
    :   identifier
//    |   functionInvokation
    |   arrayAccess
    |   objectExpression DOT accessExpression
    |	stringLiteral
    ;
