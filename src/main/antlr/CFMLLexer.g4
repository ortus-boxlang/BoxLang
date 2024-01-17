lexer grammar CFMLLexer;

options {
	caseInsensitive = true;
}

COMMENT: '<!---' .*? '--->' -> channel(HIDDEN);

SEA_WS: (' ' | '\t' | '\r'? '\n')+ -> channel(HIDDEN);

SCRIPT_OPEN: '<cfscript' .*? '>' -> pushMode(XFSCRIPT);

TAG_OPEN: '<' -> pushMode(POSSIBLE_TAG);

INTERPOLATION: HASHHASH;

POSSIBLE_INTERPOLATION_START:
	'#' -> skip, pushMode(POSSIBLE_INTERPOLATION);

CONTENT_TEXT: ~[<#]+;

mode POSSIBLE_INTERPOLATION;
INTPR_CONTENT: (~[#<]+ '\r'? '\n' | '#') { setText("#" + getText()); } -> type(CONTENT_TEXT),
		popMode;
INTRP_CONTENT:
	~[#]+ '#' { setText("#" + getText()); } -> type(INTERPOLATION), popMode;

mode POSSIBLE_TAG;

PREFIX: 'cf' -> pushMode(TAG);
SLASH_PREFIX: '/cf' -> pushMode(END_TAG);
ANY: . -> type(CONTENT_TEXT), popMode;

mode TAG;

COMPONENT: 'component';
ARGUMENT: 'argument';
DUMP: 'dump';
FUNCTION: 'function';
SCRIPT: 'script' -> pushMode(XFSCRIPT);

RETURN: 'return' -> pushMode(EXPRESSION_MODE);
IF: 'if' -> pushMode(EXPRESSION_MODE);
ELSE: 'else';
ELSEIF: 'elseif' -> pushMode(EXPRESSION_MODE);
QUERY: 'query';
INTERFACE: 'interface';
THROW: 'throw';
LOOP: 'loop';
PARAM: 'param';
TRY: 'try';
CATCH: 'catch';
ABORT: 'abort';
LOCK: 'lock';
INCLUDE: 'include';
INVOKE: 'invoke';
SET: 'set ' -> pushMode(EXPRESSION_MODE);
INVOKEARGUMENT: 'invokeargument';
FILE: 'file';
OUTPUT: 'output';

TAG_CLOSE: '>' -> popMode, popMode;

TAG_SLASH_CLOSE: '/>' -> popMode, popMode;

TAG_SLASH: '/';

TAG_EQUALS: '=' -> pushMode(ATTVALUE);

TAG_NAME: TAG_NameStartChar TAG_NameChar*;

TAG_WHITESPACE: [ \t\r\n] -> skip;

fragment DIGIT: [0-9];

fragment TAG_NameChar: TAG_NameStartChar | '_' | DIGIT;

fragment TAG_NameStartChar: [:a-z];

mode END_TAG;

IF2: 'if' -> type(IF);
COMPONENT2: 'component' -> type(COMPONENT);
DUMP2: 'dump' -> type(DUMP);
FUNCTION2: 'function' -> type(FUNCTION);
SCRIPT2: 'script' -> type(SCRIPT);
QUERY2: 'query' -> type(QUERY);
INTERFACE2: 'interface' -> type(INTERFACE);
LOOP2: 'loop' -> type(LOOP);
TRY2: 'try' -> type(TRY);
CATCH2: 'catch' -> type(CATCH);
LOCK2: 'lock' -> type(LOCK);
INVOKE2: 'invoke' -> type(INVOKE);
OUTPUT2: 'output' -> type(OUTPUT);

TAG_NAME2: TAG_NameStartChar TAG_NameChar* -> type(TAG_NAME);
TAG_CLOSE2: '>' -> popMode, popMode, type(TAG_CLOSE);

mode XFSCRIPT;

SCRIPT_BODY: .*? '</cfscript>' -> popMode;

mode ATTVALUE;

fragment DIGIT2: [0-9];
IDENTIFIER: [a-z_$]+ ( [_]+ | [a-z]+ | DIGIT2)* -> popMode;

fragment HASHHASH: '#' ~[#]+ '#';

DOUBLE_QUOTE_STRING:
	'"' (~["#]+? | '##' | HASHHASH)* '"' -> popMode;

SINGLE_QUOTE_STRING:
	'\'' (~['#]+? | '##' | HASHHASH) '\'' -> popMode;

mode EXPRESSION_MODE;
TAG_CLOSE1: '>' -> type(TAG_CLOSE), popMode, popMode;

EXPRESSION: (~[/>'"]+ | SQSTR | DQSTR)+ -> popMode;

fragment SQSTR: '\'' ~[']*? '\'';
fragment DQSTR: '"' ~["]*? '"';