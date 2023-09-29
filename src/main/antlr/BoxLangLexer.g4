lexer grammar  BoxLangLexer;

options {
    caseInsensitive = true;
}
BEGIN:  -> skip, pushMode(codeMode);

mode codeMode;
ABORT           :   'ABORT';
ADMIN           :   'ADMIN';
ANY             :   'ANY';
ARRAY           :   'ARRAY';
BOOLEAN         :   'BOOLEAN';
CASE            :   'CASE';
CLASS           :   'CLASS';
COMPONENT       :   'COMPONENT';
DEFAULT         :   'DEFAULT';
EXTENDS         :   'EXTENDS';
ELSE            :   'ELSE';
ELIF            :   'ELIF';
FOR             :   'FOR';
FUNCTION        :   'FUNCTION';
IF              :   'IF';
IN              :   'IN';
IMPLEMENTS      :   'IMPLEMENTS';
IMPORT          :   'IMPORT';
INIT            :   'INIT';
INTERFACE       :   'INTERFACE';
NUMERIC         :   'NUMERIC';
PACKAGE         :   'PACKAGE';
PRIVATE         :   'PRIVATE';
PUBLIC          :   'PUBLIC';
REMOTE          :   'REMOTE';
REQUIRED        :   'REQUIRED';
STATIC          :   'STATIC';
STRING          :   'STRING';
STRUCT          :   'STRUCT';
SWITCH          :   'SWITCH';
VAR             :   'VAR';
WHEN            :   'WHEN';

ARROW           :   '->';
COMMA           :   ',';
COLON           :   ':';
DOT             :   '.';
LBRACE          :   '{';
RBRACE          :   '}';
LPAREN          :   '(';
RPAREN          :   ')';
SEMICOLON       :   ';';
STAR            :   '*';
EQUAL           :   '=';
EQ              :   '==' | 'EQ';
PLUSEQUAL       :   '+=';
PLUS            :   '+';



ICHAR_1         :   '#' {_modeStack.contains(hashMode)}? -> type(ICHAR),popMode,popMode;
ICHAR           :   '#';

WS              :   (' ' | '\t' | '\f' )+ -> skip;
NEWLINE         :   ('\n' | '\r' )+
                    (' ' | '\t' | '\f' | '\n' | '\r' )* -> channel(HIDDEN);
OPEN_QUOTE      :   '"' -> pushMode(quotesMode);

INTEGER_LITERAL :   [0-9]+;
IDENTIFIER      :	[a-z]+ ( [_]+ | [a-z]+ | [0-9])*;


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
	: '#' -> type(ICHAR),pushMode(hashMode),pushMode(codeMode)
;


mode hashMode;
HANY
    :  -> popMode,skip;

