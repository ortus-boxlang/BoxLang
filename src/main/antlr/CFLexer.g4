lexer grammar  CFLexer;

options {
    caseInsensitive = true;
}

/**
 * DEFAULT MODE
 * https://github.com/antlr/antlr4/blob/master/doc/lexer-rules.md#lexical-modes
 */
ABSTRACT        :   'ABSTRACT';
ABORT           :   'ABORT';
ADMIN           :   'ADMIN';
ANY             :   'ANY';
APPLICATION     :   'APPLICATION';
ARGUMENTS       :   'ARGUMENTS';
ARRAY           :   'ARRAY';
AS				:	'AS';
ASSERT			:	'ASSERT';
BOOLEAN         :   'BOOLEAN';
BREAK           :   'BREAK';
CASE            :   'CASE';
CASTAS          :   'CASTAS';
CATCH           :   'CATCH';
CLASS           :   'CLASS';
COMPONENT       :   'COMPONENT';
CONTAIN			:	'CONTAIN';
CONTAINS        :    'CONTAINS';
CONTINUE        :   'CONTINUE';
CREATE        	:   'CREATE';
DEFAULT         :   'DEFAULT';
DOES			: 	'DOES';
DO				: 	'DO';
EXTENDS         :   'EXTENDS';
ELSE            :   'ELSE';
ELIF            :   'ELIF';
FALSE           :   'FALSE';
FINALLY         :   'FINALLY';
FOR             :   'FOR';
FUNCTION        :   'FUNCTION';
GREATER			:	'GREATER';
IF              :   'IF';
IN              :   'IN';
IMPLEMENTS      :   'IMPLEMENTS';
IMPORT          :   'IMPORT';
INCLUDE         :   'INCLUDE';
INIT            :   'INIT';
INTERFACE       :   'INTERFACE';
INSTANCEOF      :   'INSTANCEOF';
IS              :   'IS';
JAVA           	:   'JAVA';
LESS			:	'LESS';
LOCAL           :   'LOCAL';
LOCK            :   'LOCK';
MOD				:	'MOD';
MESSAGE			:	'MESSAGE';
NEW             :   'NEW';
NUMERIC         :   'NUMERIC';
PACKAGE         :   'PACKAGE';
PARAM           :   'PARAM';
PRIVATE         :   'PRIVATE';
PROPERTY        :   'PROPERTY';
PUBLIC          :   'PUBLIC';
QUERY           :   'QUERY';
REMOTE          :   'REMOTE';
REQUIRED        :   'REQUIRED';
REQUEST         :   'REQUEST';
RETURN          :   'RETURN';
RETHROW         :   'RETHROW';
SAVECONTENT     :   'SAVECONTENT';
SETTING         :   'SETTING';
STATIC          :   'STATIC';
STRING          :   'STRING';
STRUCT          :   'STRUCT';
SWITCH          :   'SWITCH';
THIS            :   'THIS';
THAN            :   'THAN';
TO            	:   'TO';
THREAD          :   'THREAD';
THROW           :   'THROW';
TYPE            :   'TYPE';
TRUE            :   'TRUE';
TRY             :   'TRY';
VAR             :   'VAR';
VARIABLES       :   'VARIABLES';
WHEN            :   'WHEN';
WHILE           :   'WHILE';
XOR				:	'XOR';

AMPERSAND       :   '&';
AND             :   '&&' | 'AND';
ARROW           :   '->';
BACKSLASH		:	'\\';
COMMA           :   ',';
COLON           :   ':';
COLONCOLON      :   '::';
DOT             :   '.';
ELVIS           :   '?:';
EQUAL           :   '=' | 'EQUAL';
EQ              :   '==' | 'EQ';
GT              :   '>'  | 'GT';
GTE             :   '>=' | 'GTE' | 'GE';
LBRACE          :   '{';
RBRACE          :   '}';
LPAREN          :   '(';
RPAREN          :   ')';
LBRACKET        :   '[';
RBRACKET        :   ']';
ARROW_RIGHT     :   '=>';
LT              :   '<'  | 'LT';
LTE             :   '<=' | 'LTE' | 'LE';
MINUS           :   '-';
MINUSMINUS      :   '--';
NEQ             :   '!=' | 'NEQ' | '<>';
NOT             :   '!'  | 'NOT';
OR              :   '||' | 'OR';
PERCENT         :   '%';
POWER         	:   '^';
QM              :   '?';
SEMICOLON       :   ';';
SLASH           :   '/';
STAR            :   '*';
CONCATEQUAL   	:   '&=';
PLUSEQUAL       :   '+=';
MINUSEQUAL      :   '-=';
STAREQUAL      	:   '*=';
SLASHEQUAL      :   '/=';
MODEQUAL        :   '%=';
PLUS            :   '+';
PLUSPLUS        :   '++';
TEQ				:   '===';




LCFTAG          :   '<' CFTAG '>' ;
RCFTAG          :   '</' CFTAG '>' ;
fragment CFTAG  :   'cfscript' ;



ICHAR_1         :   '#' {_modeStack.contains(hashMode)}? -> type(ICHAR),popMode,popMode;
ICHAR           :   '#';

WS              :   (' ' | '\t' | '\f' )+ -> skip;
NEWLINE         :   ('\n' | '\r' )+
                    (' ' | '\t' | '\f' | '\n' | '\r' )* -> channel(HIDDEN);
MULTILINE_COMMENT
    :   '/*' (.)*? '*/' -> channel(HIDDEN)
    ;

LINE_COMMENT :
            '//'
            ( ~('\n'|'\r') )*
            ( '\n'|'\r'('\n')? )?
      -> channel(HIDDEN) ;

OPEN_QUOTE      :   '"' -> pushMode(quotesMode);

OPEN_SINGLE     :   '\''-> type(OPEN_QUOTE),pushMode(squotesMode);

FLOAT_LITERAL
  : [0-9]+ DOT [0-9]* ([eE] [+-]? [0-9]+)?
  | DOT [0-9]+ ([eE] [+-]? [0-9]+)?
  | [0-9]+ [eE] [+-]? [0-9]+
  ;

INTEGER_LITERAL :   [0-9]+;
IDENTIFIER      :	[a-z_$]+ ( [_]+ | [a-z]+ | [0-9])*;



mode squotesMode;
CLOSE_SQUOTE
	: '\''	-> type(CLOSE_QUOTE),popMode
    ;

SHASHHASH
	: '##'
    ;

SSTRING_LITERAL
    : (~['#]+ | '\'\'' )* -> type(STRING_LITERAL)
    ;

SHASH
	: '#' -> type(ICHAR),pushMode(hashMode),pushMode(DEFAULT_MODE)
    ;


mode quotesMode;
CLOSE_QUOTE
	: '"'
	-> popMode
    ;

HASHHASH
	: '##'
;
STRING_LITERAL
	: (~["#]+ | '""' )*
;

HASH
	: '#' -> type(ICHAR),pushMode(hashMode),pushMode(DEFAULT_MODE)
;


mode hashMode;
HANY
    :  -> popMode,skip;

