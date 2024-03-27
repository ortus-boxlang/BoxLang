parser grammar CFScriptGrammar;

options {
	tokenVocab = CFScriptLexer;
}

import BaseScriptGrammar;

// cfSomething
prefixedIdentifier: PREFIXEDIDENTIFIER;

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

// foo="bar", baz="bum"
delimitedComponentAttributes: (componentAttribute) (
		COMMA componentAttribute
	)*;

// @foo style annotations disabled for CF grammar
preannotation: {false}? '.';

// Lambdas disabled for CF grammar
lambda: {false}? '.';

// Throw statement disabled for CF grammar. (throw component and BIF still work)
throw: {false}? '.';

// Bitwise operators all disabled in CF grammar
bitwiseSignedLeftShift: {false}? '.';
bitwiseSignedRightShift: {false}? '.';
bitwiseUnsignedRightShift: {false}? '.';
bitwiseAnd: {false}? '.';
bitwiseXOR: {false}? '.';
bitwiseOr: {false}? '.';
bitwiseCompliment: {false}? '.';

// castAs disabled for CF grammar
castAs: {false}? '.';

// instanceOf disabled for CF grammar
instanceOf: {false}? '.';

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
	| OR
	| prefixedIdentifier;