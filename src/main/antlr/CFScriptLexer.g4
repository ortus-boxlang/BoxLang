lexer grammar CFScriptLexer;

// $antlr-format alignTrailingComments true
// $antlr-format maxEmptyLinesToKeep 1
// $antlr-format columnLimit 150
// $antlr-format reflowComments false
// $antlr-format useTab false
// $antlr-format allowShortRulesOnASingleLine true
// $antlr-format allowShortBlocksOnASingleLine true
// $antlr-format allowShortBlocksOnASingleLine true
// $antlr-format minEmptyLines 0
// $antlr-format alignSemicolons ownLine
// $antlr-format alignColons trailing
// $antlr-format singleLineOverrulesHangingColon true
// $antlr-format alignLexerCommands true
// $antlr-format alignLabels true
// $antlr-format alignTrailers true
options {
    caseInsensitive = true;
}

@members {
	private int countModes(int mode) {
		int count = 0;
		for ( int m : _modeStack.toArray() ) {
			if (m == mode) {
				count++;
			}
		}
		return count;
	}
}

ABSTRACT   : 'ABSTRACT';
ANY        : 'ANY';
ARRAY      : 'ARRAY';
AS         : 'AS';
BOOLEAN    : 'BOOLEAN';
BREAK      : 'BREAK';
CASE       : 'CASE';
CASTAS     : 'CASTAS';
CATCH      : 'CATCH';
CONTAIN    : 'CONTAIN';
CONTAINS   : 'CONTAINS';
CONTINUE   : 'CONTINUE';
DEFAULT    : 'DEFAULT';
DO         : 'DO';
DOES       : 'DOES';
ELSEIF     : 'ELSEIF';
ELSE       : 'ELSE';
EQV        : 'EQV';
FALSE      : 'FALSE';
FINAL      : 'FINAL';
FINALLY    : 'FINALLY';
FOR        : 'FOR';
FUNCTION   : 'FUNCTION';
GREATER    : 'GREATER';
IF         : 'IF';
IMP        : 'IMP';
IMPORT     : 'IMPORT';
IN         : 'IN';
INCLUDE    : 'INCLUDE';
INSTANCEOF : 'INSTANCEOF';
INTERFACE  : 'INTERFACE';
IS         : 'IS';
JAVA       : 'JAVA';
LESS       : 'LESS';
MESSAGE    : 'MESSAGE';
MOD        : 'MOD';
NEW        : 'NEW';
NULL       : 'NULL';
NUMERIC    : 'NUMERIC';
PARAM      : 'PARAM';
PACKAGE    : 'PACKAGE';
PRIVATE    : 'PRIVATE';
PROPERTY   : 'PROPERTY';
PUBLIC     : 'PUBLIC';
QUERY      : 'QUERY';
REMOTE     : 'REMOTE';
REQUIRED   : 'REQUIRED';
RETHROW    : 'RETHROW';
RETURN     : 'RETURN';
REQUEST    : 'REQUEST';
SERVER     : 'SERVER';
SETTING    : 'SETTING';
STATIC     : 'STATIC';
STRING     : 'STRING';
STRUCT     : 'STRUCT';
SWITCH     : 'SWITCH';
THAN       : 'THAN';
THROW      : 'THROW';
TO         : 'TO';
TRUE       : 'TRUE';
TRY        : 'TRY';
TYPE       : 'TYPE';
VAR        : 'VAR';
VARIABLES  : 'VARIABLES';
WHEN       : 'WHEN';
WHILE      : 'WHILE';
XOR        : 'XOR';

COMPONENT: 'COMPONENT';

AND    : 'AND';
AMPAMP : '&&';

EQ    : 'EQ';
EQUAL : 'EQUAL';
EQEQ  : '==';

GT     : 'GT';
GTSIGN : '>';

GTE     : 'GTE';
GE      : 'GE';
GTESIGN : '>=';

LT     : 'LT';
LTSIGN : '<';

LTE     : 'LTE';
LE      : 'LE';
LTESIGN : '<=';

NEQ                 : 'NEQ';
BANGEQUAL           : '!=';
LESSTHANGREATERTHAN : '<>';

NOT  : 'NOT';
BANG : '!';

OR       : 'OR';
PIPEPIPE : '||';

AMPERSAND   : '&';
ARROW       : '->';
AT          : '@';
BACKSLASH   : '\\';
COMMA       : ',';
COLON       : ':';
COLONCOLON  : '::';
DOT         : '.';
ELVIS       : '?:';
EQUALSIGN   : '=';
LBRACE      : '{';
RBRACE      : '}';
LPAREN      : '(';
RPAREN      : ')';
LBRACKET    : '[';
RBRACKET    : ']';
ARROW_RIGHT : '=>';
MINUS       : '-';
MINUSMINUS  : '--';
PIPE        : '|';
PERCENT     : '%';
POWER       : '^';
QM          : '?';
SEMICOLON   : ';';
SLASH       : '/';
STAR        : '*';
CONCATEQUAL : '&=';
PLUSEQUAL   : '+=';
MINUSEQUAL  : '-=';
STAREQUAL   : '*=';
SLASHEQUAL  : '/=';
MODEQUAL    : '%=';
PLUS        : '+';
PLUSPLUS    : '++';
TEQ         : '===';

// BITWISE OPERATORS
BITWISE_OR                   : 'b|';
BITWISE_AND                  : 'b&';
BITWISE_XOR                  : 'b^';
BITWISE_COMPLEMENT           : 'b~';
BITWISE_SIGNED_LEFT_SHIFT    : 'b<<';
BITWISE_SIGNED_RIGHT_SHIFT   : 'b>>';
BITWISE_UNSIGNED_RIGHT_SHIFT : 'b>>>';

// This totally should not be allowed in script, but Lucee allows it and it's in code :/

TAG_COMMENT_START: '<!---' -> pushMode(TAG_COMMENT), channel(HIDDEN);

// ANY NEW LEXER RULES FOR AN ENGLISH WORD NEEDS ADDED TO THE identifer RULE IN THE PARSER

ICHAR_1 : '#' {_modeStack.contains(hashMode)}? -> type(ICHAR), popMode, popMode;
ICHAR   : '#';

WS              : (' ' | '\t' | '\f')+                              -> channel(HIDDEN);
NEWLINE         : ('\n' | '\r')+ (' ' | '\t' | '\f' | '\n' | '\r')* -> channel(HIDDEN);
JAVADOC_COMMENT : '/**' .*? '*/'                                    -> channel(HIDDEN);

COMMENT: '/*' .*? '*/' -> channel(HIDDEN);

LINE_COMMENT: '//' ~[\r\n]* -> channel(HIDDEN);

OPEN_QUOTE: '"' -> pushMode(quotesMode);

OPEN_SINGLE: '\'' -> type(OPEN_QUOTE), pushMode(squotesMode);

fragment DIGIT     : [0-9];
fragment DOT_FLOAT : '.' DIGIT+ ([e] [+-]? DIGIT+)? | DIGIT+ [e] [+-]? DIGIT+;
FLOAT_LITERAL      : DIGIT+ DOT_FLOAT;
DOT_FLOAT_LITERAL  : DOT_FLOAT;
INTEGER_LITERAL    : DIGIT+;

fragment ID_BODY   : [a-z_$]+ ( [_]+ | [a-z]+ | DIGIT)*;
PREFIXEDIDENTIFIER : 'CF' ID_BODY;
IDENTIFIER         : ID_BODY;

COMPONENT_ISLAND_START: '```' -> pushMode(componentIsland);

// Any character that is not matched in any other rule is an error.
// However, we don't want the lexer to throw an error, we want the parser to
// throw an error. So, we eat bad characters into their own token
BADC: .;

// *********************************************************************************************************************

mode TAG_COMMENT;

// If we reach an "ending" comment, but there are 2 or more TAG_COMMENT modes on the stack, this is
// just the end of a nested comment so we emit a TAG_COMMENT_TEXT token instead.
TAG_COMMENT_END_BUT_NOT_REALLY:
    '--->' {countModes(TAG_COMMENT) > 1}? -> type(TAG_COMMENT_TEXT), popMode, channel(HIDDEN)
;

TAG_COMMENT_END: '--->' -> popMode, channel(HIDDEN);

TAG_COMMENT_START2:
    '<!---' -> pushMode(TAG_COMMENT), type(TAG_COMMENT_START), channel(HIDDEN)
;

TAG_COMMENT_TEXT: .+? -> channel(HIDDEN);

// *********************************************************************************************************************

mode componentIsland;
COMPONENT_ISLAND_END  : '```' -> popMode;
COMPONENT_ISLAND_BODY : .+?;

// *********************************************************************************************************************

mode squotesMode;
CLOSE_SQUOTE    : '\''               -> type(CLOSE_QUOTE), popMode;
SHASHHASH       : '##'               -> type(HASHHASH);
SSTRING_LITERAL : (~['#]+ | '\'\'')+ -> type(STRING_LITERAL);
SHASH           : '#'                -> type(ICHAR), pushMode(hashMode), pushMode(DEFAULT_MODE);

// *********************************************************************************************************************

mode quotesMode;
CLOSE_QUOTE    : '"' -> popMode;
HASHHASH       : '##';
STRING_LITERAL : (~["#]+ | '""')+;
HASH           : '#' -> type(ICHAR), pushMode(hashMode), pushMode(DEFAULT_MODE);

// *********************************************************************************************************************

mode hashMode;
HANY: [.]+ -> popMode, skip;