parser grammar  BoxLangParser;

options {
    tokenVocab=BoxLangLexer;
}

script
    :   package
        importStatement*
        (component | interface | functionOrStatement * )
    EOF
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

component
    :   COMPONENT identifier componentAttribute*
        LBRACE functionOrStatement* RBRACE
    |    CLASS identifier componentAttribute*
                LBRACE functionOrStatement* RBRACE
    ;

interface
    :   INTERFACE identifier interfaceAttribute* LBRACE RBRACE
    ;

interfaceAttribute
    :   extendAttribute
    |   identifier EQUAL expression
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
    :   IMPLEMENTS EQUAL expression (COMMA expression)
    ;

function
    :   accessModifier? STATIC?  returnType?
        FUNCTION identifier LPAREN paramList? RPAREN statementBlock
    ;
//
functionAlt1
    :   accessModifier? STATIC?
        FUNCTION identifier LPAREN paramList? RPAREN (COLON returnType)? statementBlock
    ;
paramList
    :   param (COMMA param)*
    ;

param
  : (REQUIRED)? (type)? identifier ( EQUAL expression )?
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
    ;

functionOrStatement
    :   constructor
    |   functionAlt1
    |   statement
    ;

constructor
    :   FUNCTION INIT LPAREN paramList? RPAREN statementBlock
    ;

statementBlock
    :   LBRACE (statement)* RBRACE
    ;
statement
    :   assignment
    |   localDeclaration
    |   if
    |   for
    |   switch
    |   staticInvokation
    |   statementBlock
    // Strumenta extension
    |   when
    ;

assignment
    :   identifier EQUAL expression
    |   identifier EQUAL ifEspression
    |   identifier PLUSEQUAL expression
    ;

localDeclaration
    :   VAR identifier ((EQUAL identifier)* EQUAL expression ) eos
    ;

staticInvokation
    :   identifier COLON COLON identifier LPAREN agrumentList? RPAREN
    ;
agrumentList
    :   argument (COMMA argument)*
    ;

argument
  : expression ( EQUAL expression )?
  ;

if
    :   IF LPAREN expression RPAREN statement ( ELSE statement )?
    ;
for
    :   FOR LPAREN identifier IN expression RPAREN statementBlock
    ;
switch
  : SWITCH expression LBRACE
    (
      case
    )*

    RBRACE
  ;

case
  : ( CASE (expression) COLON statement*)
    |
    ( DEFAULT COLON statement* )
  ;

when
    :   WHEN LBRACE
        (
            expression ARROW statementBlock
        )*
        ELSE ARROW statementBlock
        RBRACE
    |   WHEN expression LBRACE
        (
            expression ARROW statementBlock
        )*
        ELSE ARROW statementBlock
        RBRACE

    ;

identifier
    : IDENTIFIER (DOT IDENTIFIER)*
    ;

stringLiteral
    :  OPEN_QUOTE (stringLiteralPart | ICHAR (expression) ICHAR)* CLOSE_QUOTE;

stringLiteralPart
    :  STRING_LITERAL | DOUBLEHASH;

integerLiteral
    :   INTEGER_LITERAL
    ;
// Strumenta expression
ifEspression
    : IF LPAREN expression RPAREN expression ( ELSE expression )?
    ;

functionExpression
    :   identifier  LPAREN agrumentList? RPAREN
    |   identifier DOT DOT identifier LPAREN agrumentList? RPAREN
    ;

expression
    :   identifier
    |   integerLiteral
    |   stringLiteral
    |   LPAREN expression RPAREN
    |   functionExpression
    |   expression EQ expression
    |   expression PLUS expression
    |   expression DOT DOT expression
    // Strumenta
    |   ifEspression
    ;
