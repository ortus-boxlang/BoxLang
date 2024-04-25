lexer grammar CFScriptLexer;

options {
	caseInsensitive = true;
}

ABSTRACT: 'ABSTRACT';
ANY: 'ANY';
ARRAY: 'ARRAY';
AS: 'AS';
ASSERT: 'ASSERT';
BOOLEAN: 'BOOLEAN';
BREAK: 'BREAK';
CASE: 'CASE';
CASTAS: 'CASTAS';
CATCH: 'CATCH';
CONTAIN: 'CONTAIN';
CONTAINS: 'CONTAINS';
CONTINUE: 'CONTINUE';
DEFAULT: 'DEFAULT';
DO: 'DO';
DOES: 'DOES';
ELSE: 'ELSE';
ELSEIF: 'ELSEIF';
EQV: 'EQV';
FALSE: 'FALSE';
FINALLY: 'FINALLY';
FINAL: 'FINAL';
FOR: 'FOR';
FUNCTION: 'FUNCTION';
GREATER: 'GREATER';
IF: 'IF';
IMP: 'IMP';
IMPORT: 'IMPORT';
IN: 'IN';
INCLUDE: 'INCLUDE';
INSTANCEOF: 'INSTANCEOF';
INTERFACE: 'INTERFACE';
IS: 'IS';
JAVA: 'JAVA';
LESS: 'LESS';
MESSAGE: 'MESSAGE';
MOD: 'MOD';
NEW: 'NEW';
NULL: 'NULL';
NUMERIC: 'NUMERIC';
PARAM: 'PARAM';
PACKAGE: 'PACKAGE';
PRIVATE: 'PRIVATE';
PROPERTY: 'PROPERTY';
PUBLIC: 'PUBLIC';
QUERY: 'QUERY';
REMOTE: 'REMOTE';
REQUIRED: 'REQUIRED';
RETHROW: 'RETHROW';
RETURN: 'RETURN';
REQUEST: 'REQUEST';
SERVER: 'SERVER';
SETTING: 'SETTING';
STATIC: 'STATIC';
STRING: 'STRING';
STRUCT: 'STRUCT';
SWITCH: 'SWITCH';
THAN: 'THAN';
THROW: 'THROW';
TO: 'TO';
TRUE: 'TRUE';
TRY: 'TRY';
TYPE: 'TYPE';
VAR: 'VAR';
VARIABLES: 'VARIABLES';
WHEN: 'WHEN';
WHILE: 'WHILE';
XOR: 'XOR';

CLASS_NAME: 'COMPONENT';

AND: 'AND';
AMPAMP: '&&';

EQ: 'EQ';
EQUAL: 'EQUAL';
EQEQ: '==';

GT: 'GT';
GTSIGN: '>';

GTE: 'GTE';
GE: 'GE';
GTESIGN: '>=';

LT: 'LT';
LTSIGN: '<';

LTE: 'LTE';
LE: 'LE';
LTESIGN: '<=';

NEQ: 'NEQ';
BANGEQUAL: '!=';
LESSTHANGREATERTHAN: '<>';

NOT: 'NOT';
BANG: '!';

OR: 'OR';
PIPEPIPE: '||';

AMPERSAND: '&';
ARROW: '->';
AT: '@';
BACKSLASH: '\\';
COMMA: ',';
COLON: ':';
COLONCOLON: '::';
DOT: '.';
ELVIS: '?:';
EQUALSIGN: '=';
LBRACE: '{';
RBRACE: '}';
LPAREN: '(';
RPAREN: ')';
LBRACKET: '[';
RBRACKET: ']';
ARROW_RIGHT: '=>';
MINUS: '-';
MINUSMINUS: '--';
PIPE: '|';
PERCENT: '%';
POWER: '^';
QM: '?';
SEMICOLON: ';';
SLASH: '/';
STAR: '*';
CONCATEQUAL: '&=';
PLUSEQUAL: '+=';
MINUSEQUAL: '-=';
STAREQUAL: '*=';
SLASHEQUAL: '/=';
MODEQUAL: '%=';
PLUS: '+';
PLUSPLUS: '++';
TEQ: '===';
PREFIX: 'CF';

// BITWISE OPERATORS
BITWISE_OR: 'b|';
BITWISE_AND: 'b&';
BITWISE_XOR: 'b^';
BITWISE_COMPLEMENT: 'b~';
BITWISE_SIGNED_LEFT_SHIFT: 'b<<';
BITWISE_SIGNED_RIGHT_SHIFT: 'b>>';
BITWISE_UNSIGNED_RIGHT_SHIFT: 'b>>>';

// ANY NEW LEXER RULES FOR AN ENGLISH WORD NEEDS ADDED TO THE identifer RULE IN THE PARSER

ICHAR_1:
	'#' {_modeStack.contains(hashMode)}? -> type(ICHAR), popMode, popMode;
ICHAR: '#';

WS: (' ' | '\t' | '\f')+ -> channel(HIDDEN);
NEWLINE: ('\n' | '\r')+ (' ' | '\t' | '\f' | '\n' | '\r')* -> channel(HIDDEN);
JAVADOC_COMMENT: '/**' .*? '*/' -> channel(HIDDEN);

COMMENT: '/*' .*? '*/' -> skip;

LINE_COMMENT: '//' ~[\r\n]* -> skip;

// This totally should not be allowed in script, but Lucee allows it and it's in code :/ 

TAG_COMMENT_START:
	'<!---' -> pushMode(TAG_COMMENT), channel(HIDDEN);

OPEN_QUOTE: '"' -> pushMode(quotesMode);

OPEN_SINGLE: '\'' -> type(OPEN_QUOTE), pushMode(squotesMode);

fragment DIGIT: [0-9];
fragment E_SIGN: [e];
fragment E_NOTATION: E_SIGN [+-]? DIGIT+;
FLOAT_LITERAL:
	DIGIT+ DOT DIGIT* (E_NOTATION)?
	| DIGIT+ E_NOTATION;

FLOAT_LITERAL_DECIMAL_ONLY_E_NOTATION: DOT DIGIT+ E_NOTATION;

FLOAT_LITERAL_DECIMAL_ONLY: DOT DIGIT+;

INTEGER_LITERAL: DIGIT+;
PREFIXEDIDENTIFIER: PREFIX IDENTIFIER;
IDENTIFIER: [a-z_$]+ ( [_]+ | [a-z]+ | DIGIT)*;

COMPONENT_ISLAND_START: '```' -> pushMode(componentIsland);

// *********************************************************************************************************************

mode TAG_COMMENT;

TAG_COMMENT_END: '--->' -> popMode, channel(HIDDEN);

TAG_COMMENT_START2:
	'<!---' -> pushMode(TAG_COMMENT), type(TAG_COMMENT_START), channel(HIDDEN);

TAG_COMMENT_TEXT: .+? -> channel(HIDDEN);

// *********************************************************************************************************************

mode componentIsland;

COMPONENT_ISLAND_END: '```' -> popMode;

COMPONENT_ISLAND_BODY: .+?;

// *********************************************************************************************************************

mode squotesMode;
CLOSE_SQUOTE: '\'' -> type(CLOSE_QUOTE), popMode;

SHASHHASH: '##' -> type(HASHHASH);

SSTRING_LITERAL: (~['#]+ | '\'\'')+ -> type(STRING_LITERAL);

SHASH:
	'#' -> type(ICHAR), pushMode(hashMode), pushMode(DEFAULT_MODE);

// *********************************************************************************************************************

mode quotesMode;
CLOSE_QUOTE: '"' -> popMode;

HASHHASH: '##';
STRING_LITERAL: (~["#]+ | '""')+;

HASH:
	'#' -> type(ICHAR), pushMode(hashMode), pushMode(DEFAULT_MODE);

// *********************************************************************************************************************

mode hashMode;
HANY: [.]+ -> popMode, skip;