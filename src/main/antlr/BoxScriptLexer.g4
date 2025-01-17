lexer grammar BoxScriptLexer;

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

ABORT       : 'ABORT';
ABSTRACT    : 'ABSTRACT';
ANY         : 'ANY';
ARRAY       : 'ARRAY';
AS          : 'AS';
ASSERT      : 'ASSERT';
BOOLEAN     : 'BOOLEAN';
BREAK       : 'BREAK';
CASE        : 'CASE';
CASTAS      : 'CASTAS';
CATCH       : 'CATCH';
CONTAIN     : 'CONTAIN';
CONTAINS    : 'CONTAINS';
CONTINUE    : 'CONTINUE';
DEFAULT     : 'DEFAULT';
DO          : 'DO';
DOES        : 'DOES';
ELIF        : 'ELIF';
ELSE        : 'ELSE';
EQV         : 'EQV';
EXIT        : 'EXIT';
FALSE       : 'FALSE';
FINAL       : 'FINAL';
FINALLY     : 'FINALLY';
FOR         : 'FOR';
FUNCTION    : 'FUNCTION';
GREATER     : 'GREATER';
IF          : 'IF';
IMP         : 'IMP';
IMPORT      : 'IMPORT';
IN          : 'IN';
INCLUDE     : 'INCLUDE';
INSTANCEOF  : 'INSTANCEOF';
INTERFACE   : 'INTERFACE';
IS          : 'IS';
JAVA        : 'JAVA';
LESS        : 'LESS';
LOCK        : 'LOCK';
MESSAGE     : 'MESSAGE';
MOD         : 'MOD';
NEW         : 'NEW';
NULL        : 'NULL';
NUMERIC     : 'NUMERIC';
PARAM       : 'PARAM';
PACKAGE     : 'PACKAGE';
PRIVATE     : 'PRIVATE';
PROPERTY    : 'PROPERTY';
PUBLIC      : 'PUBLIC';
QUERY       : 'QUERY';
REMOTE      : 'REMOTE';
REQUIRED    : 'REQUIRED';
RETHROW     : 'RETHROW';
RETURN      : 'RETURN';
REQUEST     : 'REQUEST';
SERVER      : 'SERVER';
SETTING     : 'SETTING';
STATIC      : 'STATIC';
STRING      : 'STRING';
STRUCT      : 'STRUCT';
SWITCH      : 'SWITCH';
THAN        : 'THAN';
THREAD      : 'THREAD';
THROW       : 'THROW';
TO          : 'TO';
TRANSACTION : 'TRANSACTION';
TRUE        : 'TRUE';
TRY         : 'TRY';
TYPE        : 'TYPE';
VAR         : 'VAR';
VARIABLES   : 'VARIABLES';
WHEN        : 'WHEN';
WHILE       : 'WHILE';
XOR         : 'XOR';

CLASS: 'CLASS';

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
TENQ        : '!==';

// BITWISE OPERATORS
BITWISE_OR                   : 'b|';
BITWISE_AND                  : 'b&';
BITWISE_XOR                  : 'b^';
BITWISE_COMPLEMENT           : 'b~';
BITWISE_SIGNED_LEFT_SHIFT    : 'b<<';
BITWISE_SIGNED_RIGHT_SHIFT   : 'b>>';
BITWISE_UNSIGNED_RIGHT_SHIFT : 'b>>>';

COMPONENT_PREFIX: 'bx:';

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

fragment DIGIT: [0-9];
fragment DOT_FLOAT:
    '.' INTEGER_LITERAL ([e] [+-]? INTEGER_LITERAL)?
    | INTEGER_LITERAL [e] [+-]? INTEGER_LITERAL
;
FLOAT_LITERAL                  : INTEGER_LITERAL DOT_FLOAT;
DOT_FLOAT_LITERAL              : DOT_FLOAT;
INTEGER_LITERAL                : DIGIT+ ([_]? DIGIT)*;
fragment ID_BODY               : [a-z_$]+ ( [_]+ | [a-z]+ | DIGIT)*;
DOT_NUMBER_PREFIXED_IDENTIFIER : DOT_FLOAT ID_BODY;
ILLEGAL_IDENTIFIER             : DIGIT+ ID_BODY;

IDENTIFIER: ID_BODY;

COMPONENT_ISLAND_START: '```' -> pushMode(componentIsland);

// Any character that is not matched in any other rule is an error.
// However, we don't want the lexer to throw an error, we want the parser to
// throw an error. So, we eat bad characters into their own token
BADC: .;

mode componentIsland;
COMPONENT_ISLAND_END  : '```' -> popMode;
COMPONENT_ISLAND_BODY : .+?;

mode squotesMode;
CLOSE_SQUOTE    : '\''               -> type(CLOSE_QUOTE), popMode;
SHASHHASH       : '##'               -> type(HASHHASH);
SSTRING_LITERAL : (~['#]+ | '\'\'')+ -> type(STRING_LITERAL);
SHASH           : '#'                -> type(ICHAR), pushMode(hashMode), pushMode(DEFAULT_MODE);

mode quotesMode;
CLOSE_QUOTE    : '"' -> popMode;
HASHHASH       : '##';
STRING_LITERAL : (~["#]+ | '""')+;
HASH           : '#' -> type(ICHAR), pushMode(hashMode), pushMode(DEFAULT_MODE);

mode hashMode;
HANY: [.]+ -> popMode, skip;