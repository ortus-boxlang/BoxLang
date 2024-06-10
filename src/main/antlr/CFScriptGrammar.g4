parser grammar CFScriptGrammar;

options {
	tokenVocab = CFScriptLexer;
}

@members {
	// This allows script components to be verified at parse time.
 	public ortus.boxlang.runtime.services.ComponentService componentService = ortus.boxlang.runtime.BoxRuntime.getInstance().getComponentService();
 }

// foo
identifier: IDENTIFIER | reservedKeyword;

componentName:
	// Ask the component service if the component exists
	{ componentService.hasComponent( _input.LT(1).getText() ) }? identifier;

// cfSomething
prefixedIdentifier: PREFIXEDIDENTIFIER;

// These are ONLY the scopes that always exist and never go away. All other scopes that may or may
// not exist at runtime, are handled dynamically in the runtime.
scope: REQUEST | VARIABLES | SERVER;

// These are reserved words in the lexer, but are allowed to be an indentifer (variable name, method name)
reservedKeyword:
	scope
	| ABSTRACT
	| ANY
	| ARRAY
	| AS
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
	| ELSE IF
	| ELSE
	| ELSEIF
	| FALSE
	| FINALLY
	| FINAL
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
	| OR
	| PREFIX
	| PREFIXEDIDENTIFIER;

// marks the end of simple statements (no body)
eos: SEMICOLON;

// This is the top level rule for a class or an interface
classOrInterface: boxClass | interface;

// This is the top level rule for a script of statements.
script: functionOrStatement* | EOF;

// import foo.bar.Baz;
importStatement: IMPORT importFQN eos?;

importFQN: stringLiteral | fqn (DOT STAR)?;

// include "myFile.bxm";
include: INCLUDE expression;

// component {}
boxClass:
	importStatement* ABSTRACT? boxClassName postannotation* LBRACE property* classBody RBRACE;

// the actual word "component"
boxClassName: CLASS_NAME;

staticInitializer: STATIC statementBlock;

classBody: ( staticInitializer | functionOrStatement)*;

// interface {}
interface:
	importStatement* boxInterfaceName postannotation* LBRACE (
		abstractFunction
		| function
	)* RBRACE;

// the actual word "interface"
boxInterfaceName: INTERFACE;

// function String foo( required integer param1=42 );
abstractFunction: functionSignature ( postannotation)* eos?;

// public String myFunction( String foo, String bar )
functionSignature:
	modifiers? returnType? FUNCTION identifier LPAREN functionParamList? RPAREN;

modifiers: (accessModifier | DEFAULT | STATIC | ABSTRACT | FINAL)+;

// String function foo() or MyClass function foo()
returnType: type | identifier;

// private String function foo()
accessModifier: PUBLIC | PRIVATE | REMOTE | PACKAGE;

// UDF
function:
	functionSignature (postannotation)* statementBlock
	// This will "eat" random extra ; at the end of statements
	eos*;

// Declared arguments for a function
functionParamList: functionParam (COMMA functionParam)* COMMA?;

// required String param1="default" inject="something"
functionParam: (REQUIRED)? (type)? identifier (
		EQUALSIGN expression
	)? postannotation*;

// foo=bar baz="bum"
postannotation:
	key = identifier (
		(EQUALSIGN | COLON) value = attributeSimple
	)?;

// This allows [1, 2, 3], "foo", or foo Adobe allows more chars than an identifer, Lucee allows darn
// near anything, but ANTLR is incapable of matching any tokens until the next whitespace. The
// literalExpression is just a BoxLang flourish to allow for more flexible expressions.
attributeSimple: literalExpression | identifier | fqn;

type:
	(
		NUMERIC
		| STRING
		| BOOLEAN
		| CLASS_NAME
		| INTERFACE
		| ARRAY
		| STRUCT
		| QUERY
		| fqn
		| ANY
	) (LBRACKET RBRACKET)?;

// Allow any statement or a function.  TODO: This may need to be changed if functions are allowed inside of functions
functionOrStatement: function | abstractFunction | statement;

// property name="foo" type="string" default="bar" inject="something";
property: PROPERTY postannotation* eos;

// function() {} or () => {}
anonymousFunction: closure;

closure:
	// function( param, param ) {}
	FUNCTION LPAREN functionParamList? RPAREN (postannotation)* statementBlock
	// ( param, param ) => {}
	| LPAREN functionParamList? RPAREN (postannotation)* ARROW_RIGHT statement
	// param => {}
	| identifier ARROW_RIGHT statement;

// { statement; statement; }
statementBlock: LBRACE (statement)* RBRACE eos?;

// Any top-level statement that can be in a block.
statement:
	// This will "eat" random extra ; at the start of statements
	eos* (
		importStatement
		| function
		| do
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
		// Must be before simple statement so {foo=bar} is a statement block, not a struct literal 
		| statementBlock
		| simpleStatement
		| componentIsland
	)
	// This will "eat" random extra ; at the end of statements
	eos*;

// Simple statements have no body
simpleStatement: (
		variableDeclaration
		| break
		| continue
		| rethrow
		| param
		| incrementDecrementStatement
		| return
		| throw
		| expression
	) eos?;

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

// var foo
variableDeclaration: VAR identifier;

// Arguments are zero or more named args, or zero or more positional args, but not both (validated in the AST-building stage).
argumentList:
	(namedArgument | positionalArgument) (
		COMMA (namedArgument | positionalArgument)
	)* COMMA?;

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
	IF LPAREN expression RPAREN ifStmt = statement (
		ELSE elseStmt = statement
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
	(label = identifier COLON)? FOR LPAREN VAR? accessExpression IN expression RPAREN statement
	| (label = identifier COLON)? FOR LPAREN forAssignment? eos forCondition? eos forIncrement?
		RPAREN statement;

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
do: (label = identifier COLON)? DO statement WHILE LPAREN expression RPAREN;

/*
 while( expression ) {
 statement;
 }
 */
while:
	(label = identifier COLON)? WHILE LPAREN condition = expression RPAREN statement;

// break label;
break: BREAK identifier?;

// continue label
continue: CONTINUE identifier?;

/*
 return;
 return foo;
 */
return: RETURN expression?;

// rethrow;
rethrow: RETHROW;

// throw Exception; Yes, CF does support this, at least in the form of throw "My message";
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
	CASE (expression) COLON statement*?
	| DEFAULT COLON statement*?;

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

// value, value, value
arrayValues: expression (COMMA expression)* COMMA?;

// { foo: "bar", baz = "bum" }
structExpression:
	LBRACE structMembers? RBRACE
	| LBRACKET structMembers RBRACKET
	| LBRACKET (COLON | EQUALSIGN) RBRACKET;

structMembers: structMember (COMMA structMember)* COMMA?;

/*
 foo.bar : baz
 foo : bar
 42 : bar
 "foo" : bar
 */
structMember:
	structKeyFqn (COLON | EQUALSIGN) expression
	| structKeyIdentifer (COLON | EQUALSIGN) expression
	| integerLiteral ( COLON | EQUALSIGN) expression
	| stringLiteral (COLON | EQUALSIGN) expression;

// Like an identifer, but allows a number in front
structKeyIdentifer: integerLiteral? identifier;

// foo.bar.Baz
structKeyFqn: (identifier DOT)+ identifier;

// +foo -bar b~baz
unary: (MINUS | PLUS) expression;

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
	| staticAccessExpression
	| unary
	| pre = PLUSPLUS notTernaryExpression
	| pre = MINUSMINUS notTernaryExpression
	| notTernaryExpression post = PLUSPLUS
	| notTernaryExpression post = MINUSMINUS
	| ICHAR notTernaryExpression ICHAR // #expression# outside of a string
	| notTernaryExpression ( POWER) notTernaryExpression
	| notTernaryExpression (
		STAR
		| SLASH
		| PERCENT
		| MOD
		| BACKSLASH
	) notTernaryExpression
	| notTernaryExpression (PLUS | MINUS) notTernaryExpression
	| notTernaryExpression XOR notTernaryExpression
	| left = notTernaryExpression AMPERSAND right = notTernaryExpression
	| notTernaryExpression (
		eq
		| gte
		| gt
		| lte
		| lt
		| neq
		| EQV
		| IMP
		| CONTAINS
		| NOT CONTAINS
		| TEQ
	) notTernaryExpression // Comparision
	| notTernaryExpression ELVIS notTernaryExpression // Elvis operator
	| notTernaryExpression IS notTernaryExpression // IS operator
	| notTernaryExpression DOES NOT CONTAIN notTernaryExpression
	| notOrBang notTernaryExpression
	| notTernaryExpression and notTernaryExpression
	| notTernaryExpression or notTernaryExpression;
// Logical

and: AND | AMPAMP;

eq: EQ | EQUAL | EQEQ;

gt: GT | GTSIGN | GREATER THAN;

gte:
	GTE
	| GE
	| GTESIGN
	| GREATER THAN OR EQ TO
	| GREATER THAN OR EQUAL TO;

lt: LT | LTSIGN | LESS THAN;

lte:
	LTE
	| LE
	| LTESIGN
	| LESS THAN OR EQ TO
	| LESS THAN OR EQUAL TO;

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

staticObjectExpression: identifier | fqn;

// "access" an expression with array notation (doesn't mean the object is an array per se)
arrayAccess: LBRACKET expression RBRACKET;

// "access" an expression with dot notation
dotAccess: QM? ((DOT identifier) | floatLiteralDecimalOnly);

// "access" an expression with static notation obj::field
staticAccess: (COLONCOLON identifier) | floatLiteralDecimalOnly;

// invoke a method on an expression as obj.foo() or obj["foo"]()
methodInvokation:
	QM? DOT functionInvokation
	| arrayAccess invokationExpression;

// invoke a static method on an expression as obj::foo()
staticMethodInvokation: COLONCOLON functionInvokation;

// a top level function which must be an identifier
functionInvokation: identifier invokationExpression;

// Used to invoke an expression as a function
invokationExpression: LPAREN argumentList? RPAREN;

// Access expressions represent any expression which can be "accessed" in some way by directly
// chaining method invokation, dot access, array access, etc. This rule is recursive, matching any
// number of chained access expressions. This is important to avoid recsion in the grammar.
accessExpression:
	objectExpression (
		methodInvokation
		| dotAccess
		| arrayAccess
		| invokationExpression
	)*;

staticAccessExpression:
	staticObjectExpression (
		staticAccess
		| staticMethodInvokation
	);

// foo="bar" baz="bum" qux
componentAttributes: (componentAttribute)*;

componentAttribute:
	identifier ((EQUALSIGN | COLON) expression)?;

// foo="bar", baz="bum"
delimitedComponentAttributes: (componentAttribute) (
		COMMA componentAttribute
	)*;

component:
	// http url="google.com" {}
	(componentName componentAttributes statementBlock)
	// http url="google.com";
	| (componentName componentAttributes eos)
	// cfhttp( url="google.com", timeout=20 ){}   -- Only needed for CF parser
	| (
		prefixedIdentifier LPAREN delimitedComponentAttributes RPAREN statementBlock
	)
	// cfhttp( url="google.com", timeout=20 )   -- Only needed for CF parser
	| (
		prefixedIdentifier LPAREN delimitedComponentAttributes RPAREN
	)
	// cfhttp( url="google.com" timeout=20 ){}   -- Only needed for CF parser
	| (
		prefixedIdentifier LPAREN componentAttributes RPAREN statementBlock
	)
	// cfhttp( url="google.com" timeout=20 )   -- Only needed for CF parser
	| (prefixedIdentifier LPAREN componentAttributes RPAREN);