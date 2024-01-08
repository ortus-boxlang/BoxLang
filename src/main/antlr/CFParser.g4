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

// We should probably move this to the BL grammar. TODO: remove hard-coded java.
importStatement:
	IMPORT (JAVA COLON)? fqn (DOT STAR)? (AS identifier)? eos?;

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

param: (REQUIRED)? (type)? identifier (EQUAL expression)? postannotation*
	| (REQUIRED)? (type)? identifier (EQUAL statementBlock)?;

preannotation: AT fqn (literalExpression)*;

postannotation:
	key = identifier ((EQUAL | COLON) value = attributeSimple)?;

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

anonymousFunction:
	FUNCTION LPAREN paramList? RPAREN (postannotation)* (
		simpleStatement
		| statementBlock
	)
	| lambda
	| closure;

lambda:
	LPAREN paramList? RPAREN (postannotation)* ARROW (
		statementBlock
		| simpleStatement
	)
	| identifier ARROW (simpleStatement | statementBlock);

closure:
	LPAREN paramList? RPAREN (postannotation)* ARROW_RIGHT (
		statementBlock
		| simpleStatement
	)
	| identifier ARROW_RIGHT (simpleStatement | statementBlock);

statementBlock: LBRACE (statement)* RBRACE eos?;
statementParameters: (
		parameters += accessExpression EQUAL (
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
	| simpleStatement
	| switch
	//    |   statementBlock
	| threadStatement
	| throw
	| try
	| while;

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
		EQUAL
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

argumentList: argument (COMMA argument)*;

argument: expression ( (EQUAL | COLON) expression)?;

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
	THROW LPAREN (TYPE EQUAL)? expression (
		COMMA (MESSAGE EQUAL)? stringLiteral
	)? RPAREN eos?
	| THROW expression eos?;

switch: SWITCH LPAREN expression RPAREN LBRACE (case)* RBRACE;

case:
	CASE (expression) COLON (statementBlock | statement)? break?
	| DEFAULT COLON (statementBlock | statement)?;

identifier: IDENTIFIER | reservedKeyword;
reservedKeyword:
	scope
	| ARRAY
	| CONTAINS
	| DEFAULT
	| FUNCTION
	| INIT
	| MOD
	| NEW
	| NUMERIC
	| SETTING
	| STRING
	| STRUCT
	| PRIVATE
	| QUERY
	| TYPE
	| VAR
	| WHEN
	| DOES
	| ANY
	| PARAM
	//    | 	NOT
	| CONTAIN
	| JAVA
	| MESSAGE
	| NULL
	| PROPERTY;
//    | 	ASSERT
scope:
	APPLICATION
	| ARGUMENTS
	| LOCAL
	| REQUEST
	| VARIABLES
	| THIS
	| THREAD;
//  TODO add additional known scopes

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
	identifier (COLON | EQUAL) expression
	| stringLiteral (COLON | EQUAL) expression;

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
	| expression QM expression COLON expression // Ternary
	| expression ( POWER) expression
	| expression (STAR | SLASH | PERCENT | BACKSLASH) expression
	| expression (PLUS | MINUS | MOD) expression
	| expression ( XOR | INSTANCEOF) expression
	| expression (AMPERSAND expression)+
	| expression (
		EQ
		| (GT | GREATER THAN)
		| (GTE | GREATER THAN OR EQUAL TO)
		| (LT | LESS THAN)
		| (LTE | LESS THAN OR EQUAL TO)
		| NEQ
		| CONTAINS
		| NOT CONTAINS
		| TEQ
	) expression // Comparision
	| expression ELVIS expression // Elvis operator
	| expression IS expression // IS operator
	| expression CASTAS expression // CastAs operator
	| expression INSTANCEOF expression // InstanceOf operator
	| expression DOES NOT CONTAIN expression
	| NOT expression
	| expression (AND | OR) expression;
// Logical

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