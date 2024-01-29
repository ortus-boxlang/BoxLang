parser grammar CFParser;

options {
	tokenVocab = CFLexer;
}

script:
	importStatement* (
		component
		| interface
		| functionOrStatement*
	)
	| EOF;

eos: SEMICOLON;

// We should probably move this to the BL grammar.
importStatement:
	IMPORT (prefix = identifier COLON)? fqn (DOT STAR)? (
		AS alias = identifier
	)? eos?;

include: INCLUDE expression eos?;

component:
	javadoc? (preannotation)* ABSTRACT? COMPONENT postannotation* LBRACE property*
		functionOrStatement* RBRACE;

interface:
	javadoc? (preannotation)* INTERFACE postannotation* LBRACE interfaceFunction* RBRACE;

// TODO: default method implementations
interfaceFunction: functionSignature eos;

functionSignature:
	javadoc? (preannotation)* accessModifier? STATIC? returnType? FUNCTION identifier LPAREN
		paramList? RPAREN;
function: functionSignature (postannotation)* statementBlock;

paramList: param (COMMA param)*;

param: (REQUIRED)? (type)? identifier (EQUALSIGN expression)? postannotation*
	| (REQUIRED)? (type)? identifier (EQUALSIGN statementBlock)?;

preannotation: AT fqn (literalExpression)*;

postannotation:
	key = identifier (
		(EQUALSIGN | COLON) value = attributeSimple
	)?;

// This allows [1, 2, 3], "foo", or foo Adobe allows more chars than an identifer, Lucee allows darn
// near anything, but ANTLR is incapable of matching any tokens until the next whitespace. The
// literalExpression is just a BoxLang flourish to allow for more flexible expressions.
attributeSimple: literalExpression | identifier;

returnType: type | identifier;

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

functionOrStatement: function | statement;

property:
	javadoc? (preannotation)* PROPERTY postannotation* eos;

javadoc: JAVADOC_COMMENT;

anonymousFunction: lambda | closure;

lambda:
	// ( param, param ) -> {}
	LPAREN paramList? RPAREN (postannotation)* ARROW anonymousFunctionBody
	// param -> {}
	| identifier ARROW anonymousFunctionBody;

closure:
	// function( param, param ) {}
	FUNCTION LPAREN paramList? RPAREN (postannotation)* statementBlock
	// ( param, param ) => {}
	| LPAREN paramList? RPAREN (postannotation)* ARROW_RIGHT anonymousFunctionBody
	// param => {}
	| identifier ARROW_RIGHT anonymousFunctionBody;

// Can be a body of statement(s) or a single statement.
anonymousFunctionBody: statementBlock | simpleStatement;

statementBlock: LBRACE (statement)* RBRACE eos?;

statementParameters: (
		parameters += accessExpression EQUALSIGN (
			values += stringLiteral
			| expressions += expression
		)
	)+;

statement:
	break
	| continue
	| do
	| for
	| if
	| include
	| lockStatement
	| rethrow
	| saveContentStatement
	| switch
	| threadStatement
	| throw
	| try
	| while
	| simpleStatement
	| tagIsland;

simpleStatement: (
		assert
		| applicationStatement
		| incrementDecrementStatement
		| paramStatement
		| return
		| settingStatement
		| expression
	) eos?;

incrementDecrementStatement:
	PLUSPLUS accessExpression		# preIncrement
	| accessExpression PLUSPLUS		# postIncrement
	| MINUSMINUS accessExpression	# preDecremenent
	| accessExpression MINUSMINUS	# postDecrement;

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

invokable: LPAREN RPAREN | ARROW LPAREN RPAREN;

settingStatement: SETTING assignment;

threadStatement: THREAD statementParameters statementBlock;

lockStatement: LOCK statementParameters statementBlock;

applicationStatement: APPLICATION statementParameters;

paramStatement: PARAM statementParameters;

saveContentStatement:
	SAVECONTENT statementParameters statementBlock;

// Arguments are zero or more named args, or zero or more positional args, but not both.
argumentList:
	(namedArgument | positionalArgument) (
		COMMA (namedArgument | positionalArgument)
	)*;

/* 
 foo = bar, baz = qux 
 foo : bar, baz : qux
 "foo" = bar, "baz" = qux 
 'foo' : bar, 'baz' :
 qux
 */
namedArgument: (identifier | stringLiteral) (EQUALSIGN | COLON) expression;
// foo, bar, baz
positionalArgument: expression;

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
for:
	FOR LPAREN VAR? accessExpression IN expression RPAREN statementBlock
	| FOR LPAREN forAssignment eos forCondition eos forIncrement RPAREN statementBlock;
forAssignment: expression;
forCondition: expression;
forIncrement: expression;

do: DO statementBlock WHILE LPAREN expression RPAREN;

while:
	WHILE LPAREN condition = expression RPAREN (
		statementBlock
		| statement
	);

assert: ASSERT expression;
break: BREAK eos?;
continue: CONTINUE eos?;
return: RETURN expression?;

rethrow: RETHROW eos?;
throw:
	THROW LPAREN (TYPE EQUALSIGN)? expression (
		COMMA (MESSAGE EQUALSIGN)? stringLiteral
	)? RPAREN eos?
	| THROW expression eos?;

switch: SWITCH LPAREN expression RPAREN LBRACE (case)* RBRACE;

case:
	CASE (expression) COLON (statementBlock | statement)? break?
	| DEFAULT COLON (statementBlock | statement)?;

identifier: IDENTIFIER | reservedKeyword;
reservedKeyword:
	scope
	| ABSTRACT
	| ABORT
	| ADMIN
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
	| LOCK
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
	| REQUEST
	| RETURN
	| RETHROW
	| SAVECONTENT
	| SETTING
	| STATIC
	| STRING
	| STRUCT
	//| SWITCH --> Could possibly be a var name, but not a function/method name
	| THAN
	| TO
	| THREAD
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
	| FORM;
//  TODO add additional known scopes

tagIslandBody: TAG_ISLAND_BODY*;
tagIsland: TAG_ISLAND_START tagIslandBody TAG_ISLAND_END;

try: TRY statementBlock ( catch_)* finally_?;

catch_:
	CATCH LPAREN catchType? (PIPE catchType)* expression RPAREN statementBlock;

finally_: FINALLY statementBlock;

catchType: stringLiteral | fqn;
stringLiteral:
	OPEN_QUOTE (stringLiteralPart | ICHAR (expression) ICHAR)* CLOSE_QUOTE;

stringLiteralPart: STRING_LITERAL | HASHHASH;

integerLiteral: INTEGER_LITERAL;
floatLiteral: FLOAT_LITERAL;

booleanLiteral: TRUE | FALSE;

arrayExpression: LBRACKET arrayValues? RBRACKET;

arrayValues: expression (COMMA expression)*;

structExpression:
	LBRACE structMembers? RBRACE
	| LBRACKET structMembers RBRACKET
	| LBRACKET COLON RBRACKET;

structMembers: structMember (COMMA structMember)* COMMA?;

structMember:
	identifier (COLON | EQUALSIGN) expression
	| stringLiteral (COLON | EQUALSIGN) expression;

unary: (MINUS | PLUS) expression;

// TODO: remove hard-coded Java
new:
	NEW (JAVA COLON)? (fqn | stringLiteral) LPAREN argumentList? RPAREN;
// TODO add namespace

fqn: (identifier DOT)* identifier;

expression:
	assignment
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