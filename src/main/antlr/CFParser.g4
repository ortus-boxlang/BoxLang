parser grammar CFParser;

options {
	tokenVocab = CFLexer;
}

@members { 
	// This allows script components to be verified at parse time.
 	public ortus.boxlang.runtime.services.ComponentService componentService = ortus.boxlang.runtime.BoxRuntime.getInstance().getComponentService();
 }

// This is the top level rule, which allow imports always, followed by a component, or an interface, or just a bunch of statements.
script:
	importStatement* (
		boxClass
		| interface
		| functionOrStatement*
	)
	| EOF;

// marks the end of simple statements (no body)
eos: SEMICOLON;

// TODO: This belongs only in the BL grammar. import java:foo.bar.Baz as myAlias;
importStatement:
	IMPORT (prefix = identifier COLON)? fqn (DOT STAR)? (
		AS alias = identifier
	)? eos?;

// include "myFile.cfm";
include: INCLUDE expression;

// class {}
boxClass:
	javadoc? (preannotation)* ABSTRACT? COMPONENT postannotation* LBRACE property*
		functionOrStatement* RBRACE;

interface:
	javadoc? (preannotation)* INTERFACE postannotation* LBRACE interfaceFunction* RBRACE;

// TODO: default method implementations
interfaceFunction: functionSignature eos;

// public String myFunction( String foo, String bar )
functionSignature:
	javadoc? (preannotation)* accessModifier? STATIC? returnType? FUNCTION identifier LPAREN
		functionParamList? RPAREN;

// UDF 
function:
	functionSignature (postannotation)* statementBlock
	// This will "eat" random extra ; at the end of statements 
	eos*;

// Declared arguments for a function
functionParamList: functionParam (COMMA functionParam)*;

// required String param1="default" inject="something"
functionParam: (REQUIRED)? (type)? identifier (
		EQUALSIGN expression
	)? postannotation*;

// @MyAnnotation "value" true
preannotation: AT fqn (literalExpression)*;

// foo=bar baz="bum"
postannotation:
	key = identifier (
		(EQUALSIGN | COLON) value = attributeSimple
	)?;

// This allows [1, 2, 3], "foo", or foo Adobe allows more chars than an identifer, Lucee allows darn
// near anything, but ANTLR is incapable of matching any tokens until the next whitespace. The
// literalExpression is just a BoxLang flourish to allow for more flexible expressions.
attributeSimple: literalExpression | identifier;

// String function foo() or MyClass function foo()
returnType: type | identifier;

// private String function foo()
accessModifier: PUBLIC | PRIVATE | REMOTE | PACKAGE;

type:
	NUMERIC
	| STRING
	| BOOLEAN
	| COMPONENT
	| INTERFACE
	| ARRAY
	| STRUCT
	| QUERY
	| fqn
	| ANY;

// Allow any statement or a function.  TODO: This may need to be changed if functions are allowed inside of functions
functionOrStatement: function | statement;

// property name="foo" type="string" default="bar" inject="something";
property:
	javadoc? (preannotation)* PROPERTY postannotation* eos;

// /** Comment */
javadoc: JAVADOC_COMMENT;

// function() {} or () => {} or () -> {}
anonymousFunction: lambda | closure;

lambda:
	// ( param, param ) -> {}
	LPAREN functionParamList? RPAREN (postannotation)* ARROW anonymousFunctionBody
	// param -> {}
	| identifier ARROW anonymousFunctionBody;

closure:
	// function( param, param ) {}
	FUNCTION LPAREN functionParamList? RPAREN (postannotation)* statementBlock
	// ( param, param ) => {}
	| LPAREN functionParamList? RPAREN (postannotation)* ARROW_RIGHT anonymousFunctionBody
	// param => {}
	| identifier ARROW_RIGHT anonymousFunctionBody;

// Can be a body of statement(s) or a single statement.
anonymousFunctionBody: statementBlock | simpleStatement;

// { statement; statement; }
statementBlock: LBRACE (statement)* RBRACE;

// Any top-level statement that can be in a block.  
statement: (
		do
		| for
		| if
		| switch
		| try
		| while
		| component
		| simpleStatement
		| componentIsland
	)
	// This will "eat" random extra ; at the end of statements 
	eos*;

// Simple statements have no body
simpleStatement: (
		break
		| throw
		| include
		| continue
		| rethrow
		| assert
		| param
		| incrementDecrementStatement
		| return
		| expression
	) eos?;

component:
	// http url="google.com" {}
	(componentName componentAttributes statementBlock)
	// http url="google.com";
	| (componentName componentAttributes eos)
	// cfhttp( url="google.com" ){}   -- Only needed for CF parser
	| (
		prefixedIdentifier LPAREN delimitedComponentAttributes? RPAREN statementBlock
	)
	// cfhttp( url="google.com" )   -- Only needed for CF parser
	| (
		prefixedIdentifier LPAREN delimitedComponentAttributes? RPAREN
	);

// cfSomething
prefixedIdentifier: PREFIXEDIDENTIFIER;

// foo="bar" baz="bum" qux
componentAttributes: (componentAttribute)*;

componentAttribute: identifier (EQUALSIGN expression)?;

// foo="bar", baz="bum"
delimitedComponentAttributes: (componentAttribute) (
		COMMA componentAttribute
	)*;

/*
 ++foo
 foo++
 --foo
 foo--
 */
incrementDecrementStatement:
	PLUSPLUS accessExpression		# preIncrement
	| accessExpression PLUSPLUS		# postIncrement
	| MINUSMINUS accessExpression	# preDecremenent
	| accessExpression MINUSMINUS	# postDecrement;

// var foo = bar
assignment:
	VAR? assignmentLeft (
		EQUALSIGN
		| PLUSEQUAL
		| MINUSEQUAL
		| STAREQUAL
		| SLASHEQUAL
		| MODEQUAL
		| CONCATEQUAL
	) assignmentRight;

assignmentLeft: accessExpression;
assignmentRight: expression;

// Arguments are zero or more named args, or zero or more positional args, but not both (validated in the AST-building stage).
argumentList:
	(namedArgument | positionalArgument) (
		COMMA (namedArgument | positionalArgument)
	)*;

/* 
 func( foo = bar, baz = qux )
 func( foo : bar, baz : qux )
 func( "foo" = bar, "baz" = qux ) 
 func( 'foo' : bar, 'baz' : qux )
 */
namedArgument: (identifier | stringLiteral) (EQUALSIGN | COLON) expression;

// func( foo, bar, baz )
positionalArgument: expression;

// The generic component syntax won't capture all access expressions so we need this rule too param
/*
 param String foo.bar="baz";
 param foo.bar="baz";
 param String foo.bar;
 */
param: PARAM type? accessExpression ( EQUALSIGN expression)?;

// We support if blocks with or without else blocks, and if statements without else blocks. That's
// it - no other valid if constructs.
if:
	IF LPAREN expression RPAREN ifStmtBlock = statementBlock (
		ELSE (
			elseStmtBlock = statementBlock
			| elseStmt = statement
		)
	)?
	| IF LPAREN expression RPAREN ifStmt = statement;

/*
 for( var i = 0; i < 10; i++ ) {}
 or...
 for( var foo in bar ) {}
 */
for:
	FOR LPAREN VAR? accessExpression IN expression RPAREN statementBlock
	| FOR LPAREN forAssignment eos forCondition eos forIncrement RPAREN statementBlock;

// The assignment expression (var i = 0) in a for(var i = 0; i < 10; i++ ) loop
forAssignment: expression;

// The condition expression (i < 10) in a for(var i = 0; i < 10; i++ ) loop
forCondition: expression;

// The increment expression (i++) in a for(var i = 0; i < 10; i++ ) loop
forIncrement: expression;

/*
 do {
 statement;
 } while( expression );
 */
do: DO statementBlock WHILE LPAREN expression RPAREN;

/*
 while( expression ) {
 statement;
 }
 */
while:
	WHILE LPAREN condition = expression RPAREN (
		statementBlock
		| statement
	);

// assert isTrue;
assert: ASSERT expression;

// break;
break: BREAK;

// continue
continue: CONTINUE;

/*
 return;
 return foo;
 */
return: RETURN expression?;

// rethrow;
rethrow: RETHROW;

// throw Exception;
throw: THROW expression;

/*
 switch( expression ) {
 case 1:
 statement;
 break;
 default: {
 statement;
 }
 }
 */
switch: SWITCH LPAREN expression RPAREN LBRACE (case)* RBRACE;

/*
 case 1:
 statement;
 break;
 */
case:
	CASE (expression) COLON (statementBlock | statement+)?
	| DEFAULT COLON (statementBlock | statement+)?;

// foo
identifier: IDENTIFIER | reservedKeyword;

componentName:
	// Ask the component service if the component exists
	{ componentService.hasComponent( _input.LT(1).getText() ) }? identifier;

// These are reserved words in the lexer, but are allowed to be an indentifer (variable name, method name)
reservedKeyword:
	scope
	| ABSTRACT
	| ANY
	| ARRAY
	| AS
	| ASSERT
	| BOOLEAN
	| BREAK
	| CASE
	| CASTAS
	| CATCH
	| CLASS
	| COMPONENT
	| CONTAIN
	| CONTAINS
	| CONTINUE
	| DEFAULT
	| DOES
	| DO
	| ELSE
	| ELIF
	| FALSE
	| FINALLY
	| FOR
	| FUNCTION
	| GREATER
	| IF
	| IN
	| IMPORT
	| INCLUDE
	| INTERFACE
	| INSTANCEOF
	| IS
	| JAVA
	| LESS
	| LOCAL
	| MOD
	| MESSAGE
	| NEW
	| NULL
	| NUMERIC
	| PACKAGE
	| PARAM
	| PRIVATE
	| PROPERTY
	| PUBLIC
	| QUERY
	| REMOTE
	| REQUIRED
	| RETURN
	| RETHROW
	| SETTING
	| STATIC
	| STRING
	| STRUCT
	//| SWITCH --> Could possibly be a var name, but not a function/method name
	| THAN
	| TO
	| THROW
	| TYPE
	| TRUE
	| TRY
	| VAR
	| WHEN
	| WHILE
	| XOR
	| EQV
	| IMP
	| AND
	| EQ
	| EQUAL
	| GT
	| GTE
	| GE
	| LT
	| LTE
	| LE
	| NEQ
	| NOT
	| OR
	| PREFIX
	| PREFIXEDIDENTIFIER;

// ANY NEW LEXER RULES IN DEFAULT MODE FOR WORDS NEED ADDED HERE

// Known scope names. TODO: Should the core parser "know" about scopes in modules that may not be installed?
scope:
	APPLICATION
	| ARGUMENTS
	| LOCAL
	| REQUEST
	| VARIABLES
	| THIS
	| SUPER
	| CGI
	| THREAD
	| SESSION
	| COOKIE
	| URL
	| FORM
	| SERVER;
//  TODO add additional known scopes

/*
 ```
 <cfset components="here">
 ```
 */
componentIsland:
	COMPONENT_ISLAND_START componentIslandBody COMPONENT_ISLAND_END;
componentIslandBody: COMPONENT_ISLAND_BODY*;

/*
 try {
 
 } catch( e ) {
 } finally {
 }
 */
try: TRY statementBlock ( catch_)* finally_?;

// catch( e ) {} 
catch_:
	CATCH LPAREN catchType? (PIPE catchType)* expression RPAREN statementBlock;

// finally {}
finally_: FINALLY statementBlock;

/*
 foo.bar.Baz
 or...
 "foo.bar.Baz"
 */
catchType: stringLiteral | fqn;

/*
 "foo"
 or...
 'foo'
 */
stringLiteral:
	OPEN_QUOTE (stringLiteralPart | ICHAR (expression) ICHAR)* CLOSE_QUOTE;

stringLiteralPart: STRING_LITERAL | HASHHASH;

// 42
integerLiteral: INTEGER_LITERAL;

// 3.14159
floatLiteral: FLOAT_LITERAL;

// true | false
booleanLiteral: TRUE | FALSE;

// [1,2,3]
arrayExpression: LBRACKET arrayValues? RBRACKET;

arrayValues: expression (COMMA expression)*;

// { foo: "bar", baz = "bum" }
structExpression:
	LBRACE structMembers? RBRACE
	| LBRACKET structMembers RBRACKET
	| LBRACKET COLON RBRACKET;

structMembers: structMember (COMMA structMember)* COMMA?;

structMember:
	identifier (COLON | EQUALSIGN) expression
	| stringLiteral (COLON | EQUALSIGN) expression;

// +foo -bar
unary: (MINUS | PLUS) expression;

// new java:String( param1 )
new:
	NEW (identifier COLON)? (fqn | stringLiteral) LPAREN argumentList? RPAREN;

// foo.bar.Baz
fqn: (identifier DOT)* identifier;

expression:
	// foo = bar
	assignment
	// null 
	| NULL
	| anonymousFunction
	| accessExpression
	| unary
	| pre = PLUSPLUS expression
	| pre = MINUSMINUS expression
	| expression post = PLUSPLUS
	| expression post = MINUSMINUS
	| ICHAR expression ICHAR // #expression# outside of a string
	| expression ( POWER) expression
	| expression (STAR | SLASH | PERCENT | BACKSLASH) expression
	| expression (PLUS | MINUS | MOD) expression
	| expression ( XOR | INSTANCEOF) expression
	| expression (AMPERSAND expression)+
	| expression (
		eq
		| (
			gte
			| GREATER THAN OR EQ TO
			| GREATER THAN OR EQUAL TO
		)
		| (gt | GREATER THAN)
		| (lte | LESS THAN OR EQ TO | LESS THAN OR EQUAL TO)
		| (lt | LESS THAN)
		| neq
		| EQV
		| IMP
		| CONTAINS
		| NOT CONTAINS
		| TEQ
	) expression // Comparision
	| expression ELVIS expression // Elvis operator
	| expression IS expression // IS operator
	| expression CASTAS expression // CastAs operator
	| expression INSTANCEOF expression // InstanceOf operator
	| expression DOES NOT CONTAIN expression
	| notOrBang expression
	| expression (and | or) expression
	| expression QM expression COLON expression; // Ternary
// Logical

and: AND | AMPAMP;

eq: EQ | EQUAL | EQEQ;

gt: GT | GTSIGN;

gte: GTE | GE | GTESIGN;

lt: LT | LTSIGN;

lte: LTE | LE | LTESIGN;

neq: NEQ | BANGEQUAL | LESSTHANGREATERTHAN;

notOrBang: NOT | BANG;

or: OR | PIPEPIPE;

// All literal expressions
literalExpression:
	integerLiteral
	| floatLiteral
	| stringLiteral
	| booleanLiteral
	| structExpression
	| arrayExpression;

// These can be the "start" an access expression. Basically, you need one of these in order to chain
// dotAccess, arrayAccess, methodInvokation, etc. Note some expressions can't have access slapped
// onto them unless they are contained in parens. i.e. (1 + 2).toString() since 1 + 2.toString()
// would mean something totally different.
objectExpression:
	LPAREN expression RPAREN
	| functionInvokation
	| literalExpression
	| new
	| identifier;

// "access" an expression with array notation (doesn't mean the object is an array per se)
arrayAccess: LBRACKET expression RBRACKET;

// "access" an expression with dot notation
dotAccess: QM? DOT identifier;

// invoke a method on an expression as obj.foo() or obj["foo"]()
methodInvokation:
	QM? DOT functionInvokation
	| arrayAccess invokationExpression;

// a top level function which must be an identifier
functionInvokation: identifier invokationExpression;

// Used to invoke an expression as a function
invokationExpression: LPAREN argumentList? RPAREN;

// Access expressions represent any expression which can be "accessed" in some way by directly
// chaining method invokation, dot access, array access, etc. This rule is recusive, matching any
// number of chained access expressions. This is important to avoid recsion in the grammar.
accessExpression:
	objectExpression (
		methodInvokation
		| dotAccess
		| arrayAccess
		| invokationExpression
	)*;