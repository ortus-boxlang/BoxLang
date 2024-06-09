parser grammar BoxScriptGrammar;

// Please note that this is still a WIP, but is essentially complete

options {
	tokenVocab = BoxScriptLexer;
}

@members {
	// This allows script components to be verified at parse time.
 	public ortus.boxlang.runtime.services.ComponentService componentService = ortus.boxlang.runtime.BoxRuntime.getInstance().getComponentService();
 }

// foo
identifier: IDENTIFIER | reservedKeyword;

componentName:
	// Ask the component service if the component exists
	{ componentService.hasComponent( _input.LT(1).getText() ) }?
	identifier;

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
	| ASSERT
	| BOOLEAN
	| BREAK
	| CASE
	| CASTAS
	| CATCH
	| CLASS
	| CONTAIN
	| CONTAINS
	| CONTINUE
	| DEFAULT
	| DOES
	| DO
	| ELSE
	| ELIF
	| FALSE
	| FINAL
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

// This is the top level rule for a class or an interface
classOrInterface: boxClass | interface;

// This is the top level rule for a script of statements.
script: functionOrStatement* EOF;

// import java:foo.bar.Baz as myAlias;
importStatement:
	IMPORT PREFIX? importFQN (
		AS identifier  // Note that we will change to expression in a future revision
	)?;

importFQN: fqn (DOT STAR)?;

// include "myFile.bxm";
include: INCLUDE expression;

// class {}
boxClass:
	importStatement* (preannotation)* ABSTRACT? CLASS postannotation* LBRACE property*
		classBody RBRACE;
;

classBody: ( staticInitializer | functionOrStatement)*;

staticInitializer: STATIC statementBlock;

// interface {}
interface:
	importStatement* (preannotation)* INTERFACE postannotation* LBRACE (
		| function
		| abstractFunction
		| staticInitializer
	)* RBRACE;


// Default method implementations
abstractFunction: (preannotation)* functionSignature (
		postannotation
	)*;

// public String myFunction( String foo, String bar )
functionSignature:
	(preannotation)* modifiers? returnType? FUNCTION identifier LPAREN functionParamList? RPAREN;

modifiers: (accessModifier | DEFAULT | STATIC | ABSTRACT | FINAL)+;

// String function foo() or MyClass function foo()
returnType: type | identifier;

// private String function foo()
accessModifier: PUBLIC | PRIVATE | REMOTE | PACKAGE;

// UDF
function:
	functionSignature (postannotation)* statementBlock
	;

// Declared arguments for a function
functionParamList: functionParam (COMMA functionParam)* COMMA?;

// required String param1="default" inject="something"
functionParam: (REQUIRED)? (type)? identifier (
		EQUALSIGN expression
	)? postannotation*;

// @MyAnnotation "value". This is BL specific, so it's disabled in the CF grammar, but defined here
// in the base grammar for better rule reuse.
preannotation: AT fqn expression*;

// foo=bar baz="bum"
postannotation:
	key = identifier (
		(EQUALSIGN | COLON) value = expression
	)?;

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
property: preannotation* PROPERTY postannotation*;

// /** Comment */
javadoc: JAVADOC_COMMENT;

// function() {} or () => {} or () -> {}
anonymousFunction: lambda | closure;

lambda:
	// ( param, param ) -> {}
	LPAREN functionParamList? RPAREN (postannotation)* ARROW statement
	// param -> {}
	| identifier ARROW statement;

closure:
	// function( param, param ) {}
	FUNCTION LPAREN functionParamList? RPAREN (postannotation)* statementBlock
	// ( param, param ) => {}
	| LPAREN functionParamList? RPAREN (postannotation)* ARROW_RIGHT statement
	// param => {}
	| identifier ARROW_RIGHT statement;

// { statement; statement; }
statementBlock: LBRACE (statement)* RBRACE;

// Any top-level statement that can be in a block.
statement:
	      importStatement
		| do
		| for
		| if
		| switch
		| try
		| while
		| expression   // Allows for statements like complicated.thing.foo.bar--

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
		| varDecl
	;

varDecl: varModifier+ expression ;

// Note that we use op= because this may become a set if modifiers other than VAR are added:
// op=(VAR | FINAL | PRIVATE) etc
varModifier: op=VAR;

// Simple statements have no body
simpleStatement: (
		break
		| continue
		| rethrow
		| assert
		| param
		| return
		| throw
		| SEMICOLON    // Just treat semicolons as statements and ignore them!
	);

component:
	// http url="google.com" {}
	(componentName componentAttributes statementBlock)
	// http url="google.com";
	| (componentName componentAttributes);

// foo="bar" baz="bum" qux
componentAttributes: (componentAttribute)*;

componentAttribute:
	identifier ((EQUALSIGN | COLON) expression)?;


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
param: PARAM type? expression?;  // Expression will capture x=y

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
	  PREFIX? FOR LPAREN
	    (
	       VAR? expression IN expression
	     | expression? SEMICOLON expression? SEMICOLON expression?
	  	)
		RPAREN statement;

/*
 do {
 statement;
 } while( expression );
 */
do: PREFIX? DO statement WHILE LPAREN expression RPAREN;

/*
 while( expression ) {
 statement;
 }
 */
while:
	PREFIX? WHILE LPAREN condition = expression RPAREN statement;

// assert isTrue;
assert: ASSERT expression;

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
	CATCH LPAREN expression? (PIPE expression)* expression RPAREN statementBlock;

// finally {}
finally_: FINALLY statementBlock;

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

// { foo: "bar", baz = "bum" }
structExpression:
	LBRACE structMembers? RBRACE
	| LBRACKET structMembers RBRACKET
	| LBRACKET (COLON | EQUALSIGN) RBRACKET;

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


// new java:String( param1 )
new:
	NEW PREFIX? (fqn | stringLiteral) LPAREN argumentList? RPAREN;

// foo.bar.Baz
fqn: (identifier DOT)* identifier;


// Universal expression rule. This is the top level rule for all expressions. It's left recursive, covers
// precedence, implements precedence climbing, and handles all other expressions. This is the only rule needed for
// all expressions.
//
// Precedence is implemented here by placing the highest precedence expressions at the top of the rule, and the very
// lowest at the bottom. The precedence table is the equivalent of Java and is as follows:
//
// Parentheses
// Unary operators
// Multiplicative operators
//
// Note the use of labels allows our visitor to know what it is visiting without complicated token checking etc
expression:
      LPAREN expression RPAREN       								#exprPrecedence
    | <assoc=right> (PLUSPLUS | MINUSMINUS | NOT
    				| BANG | MINUS | PLUS) expression 				#exprUnary          // ++foo, --foo, !foo, -foo, +foo
    | expression (PLUSPLUS | MINUSMINUS)            				#exprPostfix
    | <assoc=right> (PLUSPLUS | MINUSMINUS | BITWISE_COMPLEMENT)
    			expression 											#exprPrefix			 // ++foo, --foo, ~foo

    | expression QM? DOT expression       							#exprDotAccess      // xc.y?.z. recursive

	| expression POWER expression									#exprPower          // foo ^ bar

    | expression
    	(STAR | SLASH | PERCENT  |MOD |BACKSLASH) expression 		#exprMult           // foo * bar
	| expression (PLUS | MINUS) expression 							#exprAdd            // foo + bar
	| expression (
		 BITWISE_SIGNED_LEFT_SHIFT
		| BITWISE_SIGNED_RIGHT_SHIFT
		| BITWISE_UNSIGNED_RIGHT_SHIFT
	) expression                                   					#exprBitShift	    // foo b<< bar

	| expression (	  GT | GTSIGN | GREATER THAN
					| GTE | GE | GTESIGN | GREATER THAN OR EQ TO
					| GREATER THAN OR EQUAL TO
					| EQV | IMP | CONTAINS | NOT CONTAINS | TEQ
					| LTE | LE | LTESIGN | LESS THAN OR EQ TO
					| LESS THAN OR EQUAL TO
					| LT | LTSIGN | LESS THAN
					| NEQ | IS NOT | BANGEQUAL
					| LESSTHANGREATERTHAN
												) expression		#exprRelational     // foo > bar


    | expression (EQ | EQUAL |EQEQ) expression						#exprEqual          // foo == bar

	| expression BITWISE_AND expression								#exprBAnd	 		// foo b& bar
	| expression BITWISE_XOR expression								#exprBXor	 		// foo b^ bar
	| expression BITWISE_OR expression								#exprBor	 		// foo |b bar

	| expression XOR expression 									#exprXor	 		// foo XOR bar

	| left = expression AMPERSAND right = expression                #exprCat			// foo & bar - string concatenation

	| expression DOES NOT CONTAIN expression 						#exprNotContains	// foo DOES NOT CONTAIN bar
	| expression (AND|AMPAMP) expression 							#exprAnd			// foo AND bar
	| expression (OR | PIPEPIPE) expression 						#exprOr				// foo OR bar


	| expression ELVIS expression 									#exprElvis			// Elvis operator

	| expression IS expression 										#exprIs				// IS operator
	| expression INSTANCEOF expression 								#exprInstanceOf		// InstanceOf operator
	| expression CASTAS expression 								    #exprCastAs			// CastAs operator


	| <assoc=right> expression QM expression COLON expression 		#exprTernary        // foo ? bar : baz

	| expression
		op=(  EQUALSIGN
		 | PLUSEQUAL
		 | MINUSEQUAL
		 | STAREQUAL
		 | SLASHEQUAL
		 | MODEQUAL
		 | CONCATEQUAL
		)
		expression													#exprAssign         // foo = bar

	// The rest are expression elements but have no operators so will be seleceted in order other than LL(*) solving
	| ICHAR expression ICHAR       									#exprInString       // #expression# inside of a string

    | expression LBRACKET expressionList? RBRACKET                  #exprArrayAccess    // foo[bar]
    | LBRACKET expressionList? RBRACKET                          	#exprArrayLiteral   // [1,2,3]

    | anonymousFunction												#exprAnonymousFunction // function() {} or () => {} or () -> {}

    | expression LPAREN argumentList? RPAREN                        #exprFunctionCall   // foo(bar, baz)

    | COLONCOLON expression                                         #exprStaticAccess   // foo::bar

    | new														  	#exprNew            // new foo.bar.Baz()

    | identifier   													#exprIdentifier     // foo
	| literals														#exprLiterals       // foo, 42, "bar", true, false, null, [1,2,3], {foo:bar}
	| atoms                                                         #exprAtoms          // foo, 42, "bar", true, false, null, [1,2,3], {foo:bar}
	;

// Use this instead of redoing it as arrayValues, arguments etc.
expressionList: expression (COMMA expression)* COMMA?;

atoms:
	 a=(
	  	  NULL
		| TRUE
		| FALSE
	 )
	;

// All literal expressions
literals:
	  integerLiteral
	| floatLiteral
	| stringLiteral
	| structExpression
	;

