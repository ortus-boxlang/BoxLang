parser grammar BoxScriptGrammar;

options {
	tokenVocab = BoxScriptLexer;
}

@members {
	// This allows script components to be verified at parse time.
 	public ortus.boxlang.runtime.services.ComponentService componentService = ortus.boxlang.runtime.BoxRuntime.getInstance().getComponentService();
 }

// marks the end of simple statements (no body)
eos: SEMICOLON;

// This is the top level rule, which allow imports always, followed by a component, or an interface, or just a bunch of statements.
script:
	importStatement* (
		boxClass
		| interface
		| functionOrStatement*
	)
	| EOF;

// include "myFile.bxm";
include: INCLUDE expression;

// class {}
boxClass:
	javadoc? (preannotation)* ABSTRACT? CLASS_NAME postannotation* LBRACE property*
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

// @MyAnnotation "value". This is BL specific, so it's disabled in the CF grammar, but defined here
// in the base grammar for better rule reuse.
preannotation: AT fqn (literalExpression)*;

// foo=bar baz="bum"
postannotation:
	key = identifier (
		(EQUALSIGN | COLON) value = attributeSimple
	)?;

// This allows [1, 2, 3], "foo", or foo Adobe allows more chars than an identifer, Lucee allows darn
// near anything, but ANTLR is incapable of matching any tokens until the next whitespace. The
// literalExpression is just a BoxLang flourish to allow for more flexible expressions.
attributeSimple: literalExpression | identifier | fqn;

// String function foo() or MyClass function foo()
returnType: type | identifier;

// private String function foo()
accessModifier: PUBLIC | PRIVATE | REMOTE | PACKAGE;

type:
	NUMERIC
	| STRING
	| BOOLEAN
	| CLASS_NAME
	| INTERFACE
	| ARRAY
	| STRUCT
	| QUERY
	| fqn
	| ANY;

// Allow any statement or a function.  TODO: This may need to be changed if functions are allowed inside of functions
functionOrStatement: function | statement;

// TODO: This belongs only in the BL grammar. import java:foo.bar.Baz as myAlias;
importStatement:
	IMPORT (prefix = identifier COLON)? fqn (DOT STAR)? (
		AS alias = identifier
	)? eos?;

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
statementBlock: LBRACE (statement)* RBRACE eos?;

// Any top-level statement that can be in a block.
statement: (
		do
		| for
		| if
		| switch
		| try
		| while
		// include is really a component or a simple statement, but the `include expression;` case
		// needs checked PRIOR to the compnent case, which needs checked prior to simple statements
		// due to its ambiguity
		| include
		// component needs to be checked BEFORE simple statement, which includes expressions, and
		// will detect things like abort; as a access expression or cfinclude( template="..." ) as a
		// function invocation
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
	| (componentName componentAttributes eos);

// foo="bar" baz="bum" qux
componentAttributes: (componentAttribute)*;

componentAttribute: identifier (EQUALSIGN expression)?;

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
 func(
 'foo' : bar, 'baz' : qux )
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
	IF LPAREN expression RPAREN (
		ifStmtBlock = statementBlock
		| ifStmt = statement
	) (
		ELSE (
			elseStmtBlock = statementBlock
			| elseStmt = statement
		)
	)?;

/*
 for( var i = 0; i < 10; i++ ) {}
 or...
 for( var i = 0; i < 10; i++ ) echo(i)
 or...
 for( var
 foo
 in bar ) {}
 or...
 for( var foo in bar ) echo(i)
 */
for:
	FOR LPAREN VAR? accessExpression IN expression RPAREN (
		statementBlock
		| statement
	)
	| FOR LPAREN forAssignment eos forCondition eos forIncrement RPAREN (
		statementBlock
		| statement
	);

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
	| CLASS_NAME
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
	| OR;

// ANY NEW LEXER RULES IN DEFAULT MODE FOR WORDS NEED ADDED HERE

// These are ONLY the scopes that always exist and never go away. All other scopes that may or may
// not exist at runtime, are handled dynamically in the runtime.
scope: REQUEST | VARIABLES | SERVER;

/*
 ```
 <bx:set components="here">
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
floatLiteral:
	FLOAT_LITERAL
	| floatLiteralDecimalOnly
	| FLOAT_LITERAL_DECIMAL_ONLY_E_NOTATION;

floatLiteralDecimalOnly: FLOAT_LITERAL_DECIMAL_ONLY;

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

/*
 foo : bar
 42 : bar
 "foo" : bar
 */
structMember:
	identifier (COLON | EQUALSIGN) expression
	| integerLiteral ( COLON | EQUALSIGN) expression
	| stringLiteral (COLON | EQUALSIGN) expression;

// +foo -bar b~baz
unary: (MINUS | PLUS | bitwiseCompliment) expression;

// new java:String( param1 )
new:
	NEW (identifier COLON)? (fqn | stringLiteral) LPAREN argumentList? RPAREN;

// foo.bar.Baz
fqn: (identifier DOT)* identifier;

// ternary and non-ternary are broken out to handle nested ternarys correctly assignment is
// DUPLICATED inside of expression and this is correct and desired
expression: assignment | ternary | notTernaryExpression;

// foo ? bar : baz
ternary: notTernaryExpression QM expression COLON expression;

// All other expressions other than ternary
notTernaryExpression:
	// foo = bar
	assignment
	// null
	| NULL
	| anonymousFunction
	| accessExpression
	| unary
	| pre = PLUSPLUS notTernaryExpression
	| pre = MINUSMINUS notTernaryExpression
	| notTernaryExpression post = PLUSPLUS
	| notTernaryExpression post = MINUSMINUS
	| ICHAR notTernaryExpression ICHAR // #expression# outside of a string
	| notTernaryExpression ( POWER) notTernaryExpression
	| notTernaryExpression (STAR | SLASH | PERCENT | BACKSLASH) notTernaryExpression
	| notTernaryExpression (PLUS | MINUS | MOD) notTernaryExpression
	| notTernaryExpression (
		bitwiseSignedLeftShift
		| bitwiseSignedRightShift
		| bitwiseUnsignedRightShift
	) notTernaryExpression
	| notTernaryExpression XOR notTernaryExpression
	| notTernaryExpression instanceOf notTernaryExpression
	| notTernaryExpression (AMPERSAND notTernaryExpression)+
	| notTernaryExpression (
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
	) notTernaryExpression // Comparision
	| notTernaryExpression bitwiseAnd notTernaryExpression // Bitwise AND operator
	| notTernaryExpression bitwiseXOR notTernaryExpression // Bitwise XOR operator
	| notTernaryExpression bitwiseOr notTernaryExpression // Bitwise OR operator
	| notTernaryExpression ELVIS notTernaryExpression // Elvis operator
	| notTernaryExpression IS notTernaryExpression // IS operator
	| notTernaryExpression castAs notTernaryExpression
	| notTernaryExpression DOES NOT CONTAIN notTernaryExpression
	| notOrBang notTernaryExpression
	| notTernaryExpression (and | or) notTernaryExpression;
// Logical

// foo b<< bar
bitwiseSignedLeftShift: BITWISE_SIGNED_LEFT_SHIFT;

// foo b>> bar
bitwiseSignedRightShift: BITWISE_SIGNED_RIGHT_SHIFT;

// foo b>>> bar
bitwiseUnsignedRightShift: BITWISE_UNSIGNED_RIGHT_SHIFT;

// foo b& bar
bitwiseAnd: BITWISE_AND;

// foo b^ bar
bitwiseXOR: BITWISE_XOR;

// foo |b bar
bitwiseOr: BITWISE_OR;

// b~baz
bitwiseCompliment: BITWISE_COMPLEMENT;

// foo castAs bar
castAs: CASTAS;

// foo instanceOf bar
instanceOf: INSTANCEOF;

and: AND | AMPAMP;

eq: EQ | EQUAL | EQEQ;

gt: GT | GTSIGN;

gte: GTE | GE | GTESIGN;

lt: LT | LTSIGN;

lte: LTE | LE | LTESIGN;

neq: NEQ | IS NOT | BANGEQUAL | LESSTHANGREATERTHAN;

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
dotAccess: QM? ((DOT identifier) | floatLiteralDecimalOnly);

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