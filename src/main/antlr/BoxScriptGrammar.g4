parser grammar BoxScriptGrammar;

// Please note that this is still a WIP, but is essentially complete

// $antlr-format alignTrailingComments true, columnLimit 150, minEmptyLines 1, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortBlocksOnASingleLine true, alignSemicolons hanging, alignColons hanging
// $antlr-format alignColons hanging, allowShortRulesOnASingleLine on, alignFirstTokens on

options {
    tokenVocab = BoxScriptLexer;
}

@members {
	// This allows script components to be verified at parse time.
 	public ortus.boxlang.runtime.services.ComponentService componentService = ortus.boxlang.runtime.BoxRuntime.getInstance().getComponentService();
 }

// foo
identifier: IDENTIFIER | reservedKeyword
    ;

componentName
    :
    // Ask the component service if the component exists
    { componentService.hasComponent( _input.LT(1).getText() ) }? identifier
    ;

// These are reserved words in the lexer, but are allowed to be an indentifer (variable name, method name)
reservedKeyword
    : ABSTRACT
    | AND
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
    | DO
    | DOES
    | ELIF
    | ELSE
    | EQ
    | EQUAL
    | EQV
    | FALSE
    | FINAL
    | FINALLY
    | FOR
    | FUNCTION
    | GE
    | GREATER
    | GT
    | GTE
    | IF
    | IMP
    | IMPORT
    | IN
    | INCLUDE
    | INSTANCEOF
    | INTERFACE
    | IS
    | JAVA
    | LE
    | LESS
    | LT
    | LTE
    | MESSAGE
    | MOD
    | NEQ
    | NEW
    | NOT
    | NULL
    | NUMERIC
    | OR
    | PACKAGE
    | PARAM
    | PRIVATE
    | PROPERTY
    | PUBLIC
    | QUERY
    | REMOTE
    | REQUEST
    | REQUIRED
    | RETHROW
    | RETURN
    | SERVER
    | SETTING
    | STATIC
    | STRING
    | STRUCT
    // | SWITCH --> Could possibly be a var name, but not a function/method name
    | THAN
    | THROW
    | TO
    | TRUE
    | TRY
    | TYPE
    | VAR
    | VARIABLES
    | WHEN
    | WHILE
    | XOR
    ;

// ANY NEW LEXER RULES IN DEFAULT MODE FOR WORDS NEED ADDED HERE

// This is the top level rule for a class or an interface
classOrInterface: boxClass | interface
    ;

// This is the top level rule for a script of statements.
script: functionOrStatement* EOF
    ;

// import java:foo.bar.Baz as myAlias;
importStatement
    : IMPORT PREFIX? importFQN (
        AS identifier // Note that we will change to expression in a future revision
    )?
    ;

importFQN: fqn (DOT STAR)?
    ;

// include "myFile.bxm";
include: INCLUDE expression
    ;

// class {}
boxClass
    : importStatement* preAnnotation* ABSTRACT? CLASS postAnnotation* LBRACE property* classBody RBRACE
    ;

classBody: classBodyStatement*
    ;

classBodyStatement:
	  staticInitializer
	| functionOrStatement
	;

staticInitializer: STATIC statementBlock
    ;

// interface {}
interface
    : importStatement* (preAnnotation)* INTERFACE postAnnotation* LBRACE (
        function
        | abstractFunction
        | staticInitializer
    )* RBRACE
    ;

// Default method implementations
abstractFunction: (preAnnotation)* functionSignature (postAnnotation)*
    ;

// public String myFunction( String foo, String bar )
functionSignature
    : preAnnotation* modifier* returnType? FUNCTION identifier LPAREN functionParamList? RPAREN
    ;

modifier: accessModifier | DEFAULT | STATIC | ABSTRACT | FINAL
    ;

// String function foo() or MyClass function foo()
returnType: type | identifier
    ;

// private String function foo()
accessModifier: PUBLIC | PRIVATE | REMOTE | PACKAGE
    ;

// UDF
function: functionSignature (postAnnotation)* statementBlock
    ;

// Declared arguments for a function
functionParamList: functionParam (COMMA functionParam)* COMMA?
    ;

// required String param1="default" inject="something"
functionParam: (REQUIRED)? (type)? identifier (EQUALSIGN expression)? postAnnotation*
    ;

// @MyAnnotation "value". This is BL specific, so it's disabled in the CF grammar, but defined here
// in the base grammar for better rule reuse.
preAnnotation: AT fqn expression*
    ;

// foo=bar baz="bum"
postAnnotation: identifier ((EQUALSIGN | COLON) expression)?
    ;

type
    : (NUMERIC | STRING | BOOLEAN | CLASS | INTERFACE | ARRAY | STRUCT | QUERY | fqn | ANY) (
        LBRACKET RBRACKET
    )?
    ;

// Allow any statement or a function.  TODO: This may need to be changed if functions are allowed inside of functions
functionOrStatement: function | abstractFunction | statement
    ;

// property name="foo" type="string" default="bar" inject="something";
property: preAnnotation* PROPERTY postAnnotation*
    ;

// /** Comment */
javadoc: JAVADOC_COMMENT
    ;

// function() {} or () => {} or () -> {}
anonymousFunction:
    // function( param, param ) {}
    FUNCTION LPAREN functionParamList? RPAREN (postAnnotation)* statementBlock #closureFunc
    // ( param, param ) => {}, param => {} (param, param) -> {}, param -> {}
    | (LPAREN functionParamList? RPAREN | identifier) (postAnnotation)* op=(ARROW|ARROW_RIGHT) statement #lambdaFunc
    ;

// { statement; statement; }
statementBlock: LBRACE statement* RBRACE
    ;

// Any top-level statement that can be in a block.
statement
    : importStatement
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
    | expression // Allows for statements like complicated.thing.foo.bar--
    | componentIsland
    | varDecl
    ;

varDecl: varModifier+ expression
    ;

// Note that we use op= because this may become a set if modifiers other than VAR are added:
// op=(VAR | FINAL | PRIVATE) etc
varModifier: op = VAR
    ;

// Simple statements have no body
simpleStatement
    :   break
        | continue
        | rethrow
        | assert
        | param
        | return
        | throw
        | SEMICOLON // Just treat semicolons as statements and ignore them!
    ;

// http url="google.com" {}?
component
    : componentName componentAttribute* statementBlock?
    ;

componentAttribute: identifier ((EQUALSIGN | COLON) expression)?
    ;

// Arguments are zero or more named args, or zero or more positional args, but not both (validated in the AST-building stage).
argumentList: argument (COMMA argument)* COMMA?
    ;

argument: (namedArgument | positionalArgument)
    ;

/*
 func( foo = bar, baz = qux )
 func( foo : bar, baz : qux )
 func( "foo" = bar, "baz" = qux )
 func(
 'foo' : bar, 'baz' : qux )
 */
namedArgument: (identifier | stringLiteral) (EQUALSIGN | COLON) expression
    ;

// func( foo, bar, baz )
positionalArgument: expression
    ;

// The generic component syntax won't capture all access expressions so we need this rule too param
/*
 param String foo.bar="baz";
 param foo.bar="baz";
 param String foo.bar;
 */
param: PARAM type? expression?
    ; // Expression will capture x=y

// We support if blocks with or without else blocks, and if statements without else blocks. That's
// it - no other valid if constructs.
if: IF LPAREN expression RPAREN ifStmt = statement (ELSE elseStmt = statement)?
    ;

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
for
    : PREFIX? FOR LPAREN (
        VAR? expression IN expression
        | expression? SEMICOLON expression? SEMICOLON expression?
    ) RPAREN statement
    ;

/*
 do {
 statement;
 } while( expression );
 */
do: PREFIX? DO statement WHILE LPAREN expression RPAREN
    ;

/*
 while( expression ) {
 statement;
 }
 */
while: PREFIX? WHILE LPAREN expression RPAREN statement
    ;

// assert isTrue;
assert: ASSERT expression
    ;

// break label;
break: BREAK identifier?
    ;

// continue label
continue: CONTINUE identifier?
    ;

/*
 return;
 return foo;
 */
return: RETURN expression?
    ;

// rethrow;
rethrow: RETHROW
    ;

// throw Exception;
throw: THROW expression
    ;

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
switch: SWITCH LPAREN expression RPAREN LBRACE case* RBRACE
    ;

/*
 case 1:
 statement;
 break;
 */
case: CASE (expression |  DEFAULT) COLON statement*
    ;

/*
 ```
 <bx:set components="here">
 ```
 */
componentIsland: COMPONENT_ISLAND_START componentIslandBody COMPONENT_ISLAND_END
    ;

componentIslandBody: COMPONENT_ISLAND_BODY*
    ;

/*
 try {

 } catch( e ) {
 } finally {
 }
 */
try: TRY statementBlock catches* finallyBlock?
    ;

// catch( e ) {}
catches: CATCH LPAREN ct+=expression? (PIPE ct+=expression)* ex=expression RPAREN statementBlock
    ;

// finally {}
finallyBlock: FINALLY statementBlock
    ;

/*
 "foo"
 or...
 'foo'
 */
stringLiteral: OPEN_QUOTE (stringLiteralPart | ICHAR (expression) ICHAR)* CLOSE_QUOTE
    ;

stringLiteralPart: STRING_LITERAL | HASHHASH
    ;

// { foo: "bar", baz = "bum" }
structExpression
    : LBRACE structMembers? RBRACE
    | LBRACKET structMembers RBRACKET
    | LBRACKET (COLON | EQUALSIGN) RBRACKET
    ;

structMembers: structMember (COMMA structMember)* COMMA?
    ;

/*
 foo : bar
 42 : bar
 "foo" : bar
 */
structMember: expression (COLON | EQUALSIGN) expression
    ;

// new java:String( param1 )
new: NEW PREFIX? expression LPAREN argumentList? RPAREN
    ;

// foo.bar.Baz
fqn: (identifier DOT)* identifier
    ;

// Universal expression rule. This is the top level rule for all expressions. It's left recursive, covers
// precedence, implements precedence climbing, and handles all other expressions. This is the only rule needed for
// all expressions.
//
// Precedence is implemented here by placing the highest precedence expressions at the top of the rule, and the very
// lowest at the bottom. This is a form of precedence climbing, which is what ANTLR ends up genmerating, but
// it saves us from teh tedious manual expansion required of LL grammars and looks more like the LALR grammars
// that yacc/bison process.
//
// Note the use of labels allows our visitor to know what it is visiting without complicated token checking etc
expression
    : LPAREN expression RPAREN                                                       	# exprPrecedence
    | <assoc = right> op=(NOT | BANG | MINUS | PLUS) expression 						# exprUnary 	//  !foo, -foo, +foo
    | expression op=(PLUSPLUS | MINUSMINUS)                                             # exprPostfix	// foo++, bar--
    | <assoc = right> op=(PLUSPLUS | MINUSMINUS | BITWISE_COMPLEMENT) expression        # exprPrefix    // ++foo, --foo, ~foo
    | expression POWER expression                                                    	# exprPower     // foo ^ bar
    | expression op=(STAR | SLASH | PERCENT | MOD | BACKSLASH) expression               # exprMult      // foo * bar
    | expression op=(PLUS | MINUS) expression                                           # exprAdd       // foo + bar
    | expression op=(
        BITWISE_SIGNED_LEFT_SHIFT
        | BITWISE_SIGNED_RIGHT_SHIFT
        | BITWISE_UNSIGNED_RIGHT_SHIFT
    ) expression 												# exprBitShift // foo b<< bar

    | expression BITWISE_AND expression                         # exprBAnd        // foo b& bar
    | expression BITWISE_XOR expression                         # exprBXor        // foo b^ bar
    | expression BITWISE_OR expression                          # exprBor         // foo |b bar

    | expression binOps expression                              # exprBinary  	  // foo eqv bar
    | expression relOps expression                              # exprRelational  // foo > bar
    | expression LPAREN argumentList? RPAREN       				# exprFunctionCall  // foo(bar, baz)
    | expression QM? DOT expression               				# exprDotAccess 	// xc.y?.z. recursive
    | expression (EQ | EQUAL | EQEQ | IS) expression            # exprEqual       // foo == bar
    | expression XOR expression                                 # exprXor         // foo XOR bar
    | expression AMPERSAND expression            				# exprCat         // foo & bar - string concatenation
    | expression DOES NOT CONTAIN expression                    # exprNotContains // foo DOES NOT CONTAIN bar
    | expression (AND | AMPAMP) expression                      # exprAnd         // foo AND bar
    | expression (OR | PIPEPIPE) expression                     # exprOr          // foo OR bar
    | expression ELVIS expression                               # exprElvis       // Elvis operator
    | expression INSTANCEOF expression                          # exprInstanceOf  // InstanceOf operator
    | expression CASTAS expression                              # exprCastAs      // CastAs operator
    // Ternary operations are right associative, which means that if they are nested,
    // the rightmost operation is evaluated first.
    | <assoc = right> expression QM expression COLON expression # exprTernary     // foo ? bar : baz


    // Expression elements that have no operators so will be seleceted in order other than LL(*) solving
    | ICHAR expression ICHAR                       # exprOutString          // #expression# not within a string literal
    | expression LBRACKET expression RBRACKET 	   # exprArrayAccess       	   // foo[bar]
    | LBRACKET expressionList? RBRACKET            # exprArrayLiteral      // [1,2,3]
    | anonymousFunction                            # exprAnonymousFunction // function() {} or () => {} or () -> {}
    | expression COLONCOLON expression             # exprStaticAccess      // foo::bar
    | new                                          # exprNew               // new foo.bar.Baz()
    | literals                                     # exprLiterals          // "bar", [1,2,3], {foo:bar}
    | atoms                                        # exprAtoms             // foo, 42, true, false, null, [1,2,3], {foo:bar}
    | identifier                                   # exprIdentifier        // foo


    // Evaluate assign last so that we can assign the result of an expression to a variable
    | <assoc = right> expression op = (
            EQUALSIGN
            | PLUSEQUAL
            | MINUSEQUAL
            | STAREQUAL
            | SLASHEQUAL
            | MODEQUAL
            | CONCATEQUAL
        ) expression # exprAssign // foo = bar
    ;

// Use this instead of redoing it as arrayValues, arguments etc.
expressionList: expression (COMMA expression)* COMMA?
    ;

atoms: a = (NULL | TRUE | FALSE | INTEGER_LITERAL | FLOAT_LITERAL)
    ;

// All literal expressions
literals: stringLiteral | structExpression
    ;

// Relational operatos as their own rule so we can have teh visitor generate theAST
relOps:
  	  GT
	| GTSIGN
	| GREATER THAN
	| GTE
	| GE
	| GTESIGN
	| GREATER THAN OR EQ TO
	| GREATER THAN OR EQUAL TO
	| TEQ
	| LTE
	| LE
	| LTESIGN
	| LESS THAN OR EQ TO
	| LESS THAN OR EQUAL TO
	| LT
	| LTSIGN
	| LESS THAN
	| NEQ
	| IS NOT
	| BANGEQUAL
	| LESSTHANGREATERTHAN
    ;

binOps
	: EQV
  	| IMP
  	| CONTAINS
  	| NOT CONTAINS
	;