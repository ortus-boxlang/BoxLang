lexer grammar BoxLexer;

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
	private int parenCount = 0;
	private int braceCount = 0;
	private int bracketCount = 0;
	private boolean isQuery = false;

	private int countModes(int mode) {
		int count = 0;
		if( _mode == mode ) {
			count++;
		}
		for ( int m : _modeStack.toArray() ) {
			if (m == mode) {
				count++;
			}
		}
		return count;
	}

	public boolean lastModeWas( int mode, int count ) {
		java.util.List<Integer> modes = new java.util.ArrayList<Integer>();
		for ( int m : _modeStack.toArray() ) {
			modes.add( 0, m );
		}
		modes.add( 0, _mode );
		if ( modes.size() - 1 < count ) {
			return false;
		}
		return modes.get( count ) == mode;
	}

	public boolean isTagStart() {
		boolean result = lastModeWas( hashMode, 1 ) 
			&& _modeStack.contains(DEFAULT_TEMPLATE_MODE) 
			&& ( _input.LA( 1 ) == 98 || _input.LA( 1 ) == 66 ) // b or B 
			&& ( _input.LA( 2 ) == 120 || _input.LA( 2 ) == 88 )// x or X
			&&  _input.LA( 2 ) == 58; // colon char (:) 

		if( result ) {
			// pop mode until we're back to the default template mode
			while( _mode != DEFAULT_TEMPLATE_MODE ) {
				popMode();
			}
		}
		return result;
	}

	public boolean isTagEnd() {
		boolean result = lastModeWas( hashMode, 1 ) 
			&& _modeStack.contains(DEFAULT_TEMPLATE_MODE) 
			&& _input.LA( 1 ) == 47  // a forwardslash char (/)
			&& ( _input.LA( 1 ) == 98 || _input.LA( 1 ) == 66 ) // b or B 
			&& ( _input.LA( 2 ) == 120 || _input.LA( 2 ) == 88 )// x or X
			&&  _input.LA( 2 ) == 58; // colon char (:) 

		if( result ) {
			// pop mode until we're back to the default template mode
			while( _mode != DEFAULT_TEMPLATE_MODE ) {
				popMode();
			}
		}
		return result;
	}

	public void reset() {
		resetCounters();
		super.reset();
	}

	public void resetCounters() {
		parenCount = 0;
		braceCount = 0;
		bracketCount = 0;
	}

	private boolean isExpressionComplete() {
		return parenCount == 0 && braceCount == 0 && bracketCount == 0;
	}

}

/*
 @members {
 
 
 public Token emit() {
 Token t =
 _factory.create(_tokenFactorySourcePair, _type, _text, _channel, _tokenStartCharIndex,
 getCharIndex()-1,
 _tokenStartLine, _tokenStartCharPositionInLine);
 emit(t);
 System.out.println(
 t.toString() + " " + _SYMBOLIC_NAMES[t.getType()] );
 return t;
 }
 public int popMode() {
 System.out.println( "popMode back to "+ modeNames[_modeStack.peek()]);
 return super.popMode();
 }
 
 public void pushMode(int m) {
	System.out.println( "pushMode "+modeNames[m]);
	super.pushMode(m);
	
	System.out.print( ">>>>> modes >>>>> " );
	for ( int m2 :	_modeStack.toArray() ) {
		System.out.print( " > " + modeNames[m2] );
	}
	System.out.println( " > " + modeNames[_mode] );
 }

 }
 */

ISSCRIPT   : '__script__'   -> pushMode(DEFAULT_SCRIPT_MODE), channel(HIDDEN);
ISTEMPLATE : '__template__' -> pushMode(DEFAULT_TEMPLATE_MODE), channel(HIDDEN);

mode DEFAULT_SCRIPT_MODE;

COMPONENT_SLASH_CLOSE99:
    '/>' {isExpressionComplete() && _modeStack.contains(TEMPLATE_EXPRESSION_MODE_COMPONENT)}? -> type(COMPONENT_SLASH_CLOSE), popMode, popMode, popMode
        , popMode, popMode
;

COMPONENT_CLOSE99:
    '>' {isExpressionComplete() && _modeStack.contains(TEMPLATE_EXPRESSION_MODE_COMPONENT)}? -> type(COMPONENT_CLOSE), popMode, popMode, popMode,
        popMode, popMode
;

// We have to capture this scenario and break it apart in our lexer super class into two tokens.  If we don't capture this here, the >= rule below will match it and 
// fail to end the component for us, causing an invalid parse
COMPONENT_CLOSE_EQUAL:
    '>=' {isExpressionComplete() && _modeStack.contains(TEMPLATE_EXPRESSION_MODE_COMPONENT)}? -> popMode, popMode, popMode, popMode, popMode
;

fragment COMPONENT_WHITESPACE2: [ \t\r\n]*;
SCRIPT_END_BODY:
    '</' COMPONENT_WHITESPACE2 'bx:script' COMPONENT_WHITESPACE2 '>' {_modeStack.contains(TEMPLATE_BXSCRIPT)}? -> popMode, popMode
;

// Try and detect truncated expressions in tag code '<bx:' 
UNEXPECTED_EXPRESSION_END: '<' {isTagStart()}? -> pushMode(TEMPLATE_POSSIBLE_COMPONENT);
// Try and detect truncated expressions in tag code '</bx:' 
UNEXPECTED_EXPRESSION_SLASH_END:
    '<' {isTagEnd()}? -> type(UNEXPECTED_EXPRESSION_END), pushMode(TEMPLATE_POSSIBLE_COMPONENT)
;

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
LBRACE      : '{' { braceCount++; };
RBRACE      : '}' { braceCount--; };
LPAREN      : '(' { parenCount++; };
RPAREN      : ')' { parenCount--; };
LBRACKET    : '[' { bracketCount++; };
RBRACKET    : ']' { bracketCount--; };
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

ICHAR_2 : '#' {lastModeWas(TEMPLATE_ATTVALUE,2)}? -> type(ICHAR), popMode, popMode, popMode;
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
IDENTIFIER                     : ID_BODY;
DOT_NUMBER_PREFIXED_IDENTIFIER : DOT_FLOAT ID_BODY;
ILLEGAL_IDENTIFIER             : DIGIT+ ID_BODY;

COMPONENT_ISLAND_START: '```' -> pushMode(componentIsland), pushMode(DEFAULT_TEMPLATE_MODE);

// Any character that is not matched in any other rule is an error.
// However, we don't want the lexer to throw an error, we want the parser to
// throw an error. So, we eat bad characters into their own token
BADC: .;

mode componentIsland;

// This mode is only used for tracking on the stack, it will never match any chars
DUMMY3: .;

// *********************************************************************************************************************

mode squotesMode;
// If this is the end of a quoted string directly inside an attribute value, then pop out of the att value as well
CLOSE_SQUOTE1: '\'' {lastModeWas(TEMPLATE_ATTVALUE,1)}? -> popMode, popMode, type(CLOSE_QUOTE);
// Otherwise, just pop out of the quotes mode
CLOSE_SQUOTE    : '\''               -> type(CLOSE_QUOTE), popMode;
SHASHHASH       : '##'               -> type(HASHHASH);
SSTRING_LITERAL : (~['#]+ | '\'\'')+ -> type(STRING_LITERAL);
SHASH           : '#'                -> type(ICHAR), pushMode(hashMode), pushMode(DEFAULT_SCRIPT_MODE);

// *********************************************************************************************************************

mode quotesMode;
// If this is the end of a quoted string directly inside an attribute value, then pop out of the att value as well
CLOSE_QUOTE1: '"' {lastModeWas(TEMPLATE_ATTVALUE,1)}? -> popMode, popMode, type(CLOSE_QUOTE);
// Otherwise, just pop out of the quotes mode
CLOSE_QUOTE    : '"' -> popMode;
HASHHASH       : '##';
STRING_LITERAL : (~["#]+ | '""')+;
HASH           : '#' -> type(ICHAR), pushMode(hashMode), pushMode(DEFAULT_SCRIPT_MODE);

// *********************************************************************************************************************

mode hashMode;
HANY: [.]+ -> popMode, skip;

/******************************************************
	TEMPLATE MODES
*******************************************************/

// *********************************************************************************************************************
mode DEFAULT_TEMPLATE_MODE;

COMPONENT_ISLAND_END: '```' {_modeStack.contains(componentIsland)}? -> popMode, popMode;

COMMENT_START: '<!---' -> pushMode(TEMPLATE_COMMENT_MODE);

TEMPLATE_WS: (' ' | '\t' | '\r'? '\n')+;

SCRIPT_OPEN:
    '<bx:script' .*? '>' -> pushMode(TEMPLATE_BXSCRIPT), pushMode(DEFAULT_SCRIPT_MODE)
;

OUTPUT_START:
    '<bx:output' -> pushMode(TEMPLATE_POSSIBLE_COMPONENT), pushMode(TEMPLATE_COMPONENT_MODE), pushMode(TEMPLATE_OUTPUT_MODE)
;

COMPONENT_OPEN: '<' -> pushMode(TEMPLATE_POSSIBLE_COMPONENT);

TEMPLATE_HASHHASH: '##' -> type(CONTENT_TEXT);

TEMPLATE_ICHAR1:
    '#' {_modeStack.contains(TEMPLATE_OUTPUT_MODE)}? -> type(ICHAR), pushMode(hashMode), pushMode(DEFAULT_SCRIPT_MODE)
;
TEMPLATE_ICHAR_1: '#' -> type(CONTENT_TEXT);

CONTENT_TEXT2: '`' -> type(CONTENT_TEXT);

CONTENT_TEXT: ~[<#`]+;

// *********************************************************************************************************************
mode TEMPLATE_COMMENT_MODE;

// If we reach an "ending" comment, but there are 2 or more TAG_COMMENT modes on the stack, this is
// just the end of a nested comment so we emit a TAG_COMMENT_TEXT token instead.
COMMENT_END_BUT_NOT_REALLY:
    '--->' {countModes(TEMPLATE_COMMENT_MODE) > 1}? -> type(COMMENT_TEXT), popMode
;

COMMENT_END: '--->' -> popMode;

COMMENT_START2: '<!---' -> pushMode(TEMPLATE_COMMENT_MODE), type(COMMENT_START);

COMMENT_TEXT: .+?;

// *********************************************************************************************************************
mode TEMPLATE_COMMENT_QUIET;

// If we reach an "ending" comment, but there are 2 or more TAG_COMMENT modes on the stack, this is
// just the end of a nested comment so we emit a TAG_COMMENT_TEXT token instead.
COMMENT_END_BUT_NOT_REALLY_QUIET:
    '--->' {countModes(TEMPLATE_COMMENT_QUIET) > 1}? -> type(COMMENT_TEXT), channel(HIDDEN), popMode
;

COMMENT_END_QUIET: '--->' -> popMode, channel(HIDDEN), type(COMMENT_END);

COMMENT_START_QUIET:
    '<!---' -> pushMode(TEMPLATE_COMMENT_QUIET), channel(HIDDEN), type(COMMENT_START)
;

COMMENT_TEXT_QUIET: .+? -> type(COMMENT_TEXT), channel(HIDDEN);

// *********************************************************************************************************************
mode TEMPLATE_COMPONENT_NAME_MODE;

// The rule of thumb here is that we are doing direct handling of any components for which we have a
// dedicated AST node for. All other components will be handled generically
TEMPLATE_COMPONENT : 'component' -> pushMode( TEMPLATE_COMPONENT_MODE );
TEMPLATE_INTERFACE : 'interface' -> pushMode( TEMPLATE_COMPONENT_MODE );
TEMPLATE_FUNCTION  : 'function'  -> pushMode( TEMPLATE_COMPONENT_MODE );
TEMPLATE_ARGUMENT  : 'argument'  -> pushMode( TEMPLATE_COMPONENT_MODE );
// bx:query doesn't have a dedicated AST node, but we need to pretend we're in a bx:output when we enter a bx:query.
TEMPLATE_QUERY:
    'query' {isQuery=true;} -> type(COMPONENT_NAME), pushMode( TEMPLATE_COMPONENT_MODE )
;

// return may or may not have an expression, so eat any leading whitespace now so it doesn't give us an expression part that's just a space
TEMPLATE_RETURN:
    'return' [ \t\r\n]* {resetCounters();} -> pushMode( TEMPLATE_COMPONENT_MODE ), pushMode( TEMPLATE_EXPRESSION_MODE_COMPONENT), pushMode(
        DEFAULT_SCRIPT_MODE)
;

TEMPLATE_IF:
    'if' [ \t\r\n]+ {resetCounters();} -> pushMode( TEMPLATE_COMPONENT_MODE ), pushMode(TEMPLATE_EXPRESSION_MODE_COMPONENT), pushMode(
        DEFAULT_SCRIPT_MODE)
;
TEMPLATE_ELSE: 'else' -> pushMode( TEMPLATE_COMPONENT_MODE );
TEMPLATE_ELSEIF:
    'elseif' [ \t\r\n]+ {resetCounters();} -> pushMode( TEMPLATE_COMPONENT_MODE ), pushMode(TEMPLATE_EXPRESSION_MODE_COMPONENT), pushMode(
        DEFAULT_SCRIPT_MODE)
;

TEMPLATE_SET:
    'set' [ \t\r\n]+ {resetCounters();} -> pushMode( TEMPLATE_COMPONENT_MODE ), pushMode( TEMPLATE_EXPRESSION_MODE_COMPONENT), pushMode(
        DEFAULT_SCRIPT_MODE)
;

TEMPLATE_TRY         : 'try'         -> pushMode( TEMPLATE_COMPONENT_MODE );
TEMPLATE_CATCH       : 'catch'       -> pushMode( TEMPLATE_COMPONENT_MODE );
TEMPLATE_FINALLY     : 'finally'     -> pushMode( TEMPLATE_COMPONENT_MODE );
TEMPLATE_IMPORT      : 'import'      -> pushMode( TEMPLATE_COMPONENT_MODE );
TEMPLATE_WHILE       : 'while'       -> pushMode( TEMPLATE_COMPONENT_MODE );
TEMPLATE_BREAK       : 'break'       -> pushMode( TEMPLATE_COMPONENT_MODE );
TEMPLATE_CONTINUE    : 'continue'    -> pushMode( TEMPLATE_COMPONENT_MODE );
TEMPLATE_INCLUDE     : 'include'     -> pushMode( TEMPLATE_COMPONENT_MODE );
TEMPLATE_PROPERTY    : 'property'    -> pushMode( TEMPLATE_COMPONENT_MODE );
TEMPLATE_RETHROW     : 'rethrow'     -> pushMode( TEMPLATE_COMPONENT_MODE );
TEMPLATE_THROW       : 'throw'       -> pushMode( TEMPLATE_COMPONENT_MODE );
TEMPLATE_SWITCH      : 'switch'      -> pushMode( TEMPLATE_COMPONENT_MODE );
TEMPLATE_CASE        : 'case'        -> pushMode( TEMPLATE_COMPONENT_MODE );
TEMPLATE_DEFAULTCASE : 'defaultcase' -> pushMode( TEMPLATE_COMPONENT_MODE );

COMPONENT_NAME: COMPONENT_NameStartChar COMPONENT_NameChar* -> pushMode( TEMPLATE_COMPONENT_MODE );

fragment TEMPLATE_DIGIT: [0-9];

fragment COMPONENT_NameChar: COMPONENT_NameStartChar | '_' | '-' | TEMPLATE_DIGIT | ':';

fragment COMPONENT_NameStartChar: [a-z_];

// *********************************************************************************************************************
mode TEMPLATE_COMPONENT_MODE;

// Comments can live inside of a tag <bx:Tag <!--- comment ---> foo=bar >
COMMENT_START1:
    '<!---' -> pushMode(TEMPLATE_COMMENT_QUIET), channel(HIDDEN), type(COMMENT_START)
;

COMPONENT_CLOSE4:
    {isQuery}? '>' -> popMode, popMode, popMode, pushMode(TEMPLATE_OUTPUT_MODE), pushMode(DEFAULT_TEMPLATE_MODE), type(COMPONENT_CLOSE)
;

COMPONENT_CLOSE: '>' -> popMode, popMode, popMode;

COMPONENT_SLASH_CLOSE: '/>' -> popMode, popMode, popMode;

COMPONENT_SLASH: '/';

COMPONENT_EQUALS: '=' -> pushMode(TEMPLATE_ATTVALUE);

ATTRIBUTE_NAME: ATTRIBUTE_NameStartChar ATTRIBUTE_NameChar*;

COMPONENT_WHITESPACE: [ \t\r\n] -> skip;

fragment ATTRIBUTE_DIGIT: [0-9];

fragment ATTRIBUTE_NameChar: ATTRIBUTE_NameStartChar | '_' | '-' | ATTRIBUTE_DIGIT | ':';

fragment ATTRIBUTE_NameStartChar: [a-z_];

// *********************************************************************************************************************
mode TEMPLATE_OUTPUT_MODE;

// Source inside of an output tag is consumed in output mode
COMMENT_START4:
    '<!---' -> pushMode(TEMPLATE_COMMENT_QUIET), channel(HIDDEN), type(COMMENT_START)
;

COMPONENT_CLOSE_OUTPUT: '>' -> pushMode(DEFAULT_TEMPLATE_MODE), type(COMPONENT_CLOSE);

COMPONENT_SLASH_CLOSE_OUTPUT: '/>' -> popMode, popMode, popMode, type(COMPONENT_SLASH_CLOSE);

COMPONENT_EQUALS_OUTPUT: '=' -> pushMode(TEMPLATE_ATTVALUE), type(COMPONENT_EQUALS);

ATTRIBUTE_NAME_OUTPUT: ATTRIBUTE_NameStartChar ATTRIBUTE_NameChar* -> type(ATTRIBUTE_NAME);

COMPONENT_WHITESPACE_OUTPUT: [ \t\r\n] -> skip;

// *********************************************************************************************************************
mode TEMPLATE_END_COMPONENT;

TEMPLATE_IF2        : 'if'        -> type(TEMPLATE_IF);
TEMPLATE_COMPONENT2 : 'component' -> type(TEMPLATE_COMPONENT);
TEMPLATE_FUNCTION2  : 'function'  -> type(TEMPLATE_FUNCTION);
// If we're ending a bx:query, we need to use the special exit below with the extra pop out of output mode
TEMPLATE_QUERY2: 'query' {isQuery=true;} -> type(COMPONENT_NAME);

// popping back to: POSSIBLE_COMPONENT -> DEFAULT_MODE -> OUTPUT_MODE -> COMPONENT -> POSSIBLE_COMPONENT -> DEFAULT_MODE
OUTPUT_END:
    'output' COMPONENT_WHITESPACE_OUTPUT3* '>' -> popMode, popMode, popMode, popMode, popMode, popMode
;
TEMPLATE_INTERFACE2   : 'interface'   -> type(TEMPLATE_INTERFACE);
TEMPLATE_TRY2         : 'try'         -> type(TEMPLATE_TRY);
TEMPLATE_CATCH2       : 'catch'       -> type(TEMPLATE_CATCH);
TEMPLATE_FINALLY2     : 'finally'     -> type(TEMPLATE_FINALLY);
TEMPLATE_IMPORT2      : 'import'      -> type(TEMPLATE_IMPORT);
TEMPLATE_WHILE2       : 'while'       -> type(TEMPLATE_WHILE);
TEMPLATE_BREAK2       : 'break'       -> type(TEMPLATE_BREAK);
TEMPLATE_CONTINUE2    : 'continue'    -> type(TEMPLATE_CONTINUE);
TEMPLATE_INCLUDE2     : 'include'     -> type(TEMPLATE_INCLUDE);
TEMPLATE_PROPERTY2    : 'property'    -> type(TEMPLATE_PROPERTY);
TEMPLATE_RETHROW2     : 'rethrow'     -> type(TEMPLATE_RETHROW);
TEMPLATE_THROW2       : 'throw'       -> type(TEMPLATE_THROW);
TEMPLATE_SWITCH2      : 'switch'      -> type(TEMPLATE_SWITCH);
TEMPLATE_CASE2        : 'case'        -> type(TEMPLATE_CASE);
TEMPLATE_DEFAULTCASE2 : 'defaultcase' -> type(TEMPLATE_DEFAULTCASE);

COMPONENT_WHITESPACE_OUTPUT3: [ \t\r\n] -> skip;

COMPONENT_NAME2: COMPONENT_NameStartChar COMPONENT_NameChar* -> type(COMPONENT_NAME);
// If we're coming out of a bx:query, then pop 2 extra times to get out of the output mode and additional default template mode
COMPONENT_CLOSE2: '>' {isQuery}? -> popMode, popMode, popMode, popMode, type(COMPONENT_CLOSE);
// all other ending tags
COMPONENT_CLOSE23: '>' -> popMode, popMode, type(COMPONENT_CLOSE);

// *********************************************************************************************************************
mode TEMPLATE_ATTVALUE;

COMPONENT_WHITESPACE_OUTPUT2: [ \t\r\n] -> skip;

TEMPLATE_ICHAR20: '#' -> type(ICHAR), pushMode(hashMode), pushMode(DEFAULT_SCRIPT_MODE);

TEMPLATE_OPEN_QUOTE2: '"' -> type(OPEN_QUOTE), pushMode(quotesMode);

TEMPLATE_OPEN_SINGLE: '\'' -> type(OPEN_QUOTE), pushMode(squotesMode);

// If we're in a bx:output tag, don't pop as far and stay in output mode
COMPONENT_CLOSE_OUTPUT2:
    '>' {lastModeWas(TEMPLATE_OUTPUT_MODE,1)}? -> popMode, pushMode(DEFAULT_TEMPLATE_MODE), type( COMPONENT_CLOSE )
;

// If we're in a bx:output tag, pop all the way out of the component
COMPONENT_SLASH_CLOSE2:
    '/>' {lastModeWas(TEMPLATE_OUTPUT_MODE,1)}? -> popMode, popMode, popMode, type( COMPONENT_SLASH_CLOSE )
;

// There may be no value, so we need to pop out of ATTVALUE if we find the end of the component
COMPONENT_CLOSE6:
    '>' {isQuery}? -> popMode, popMode, popMode, popMode, pushMode(TEMPLATE_OUTPUT_MODE), pushMode(DEFAULT_TEMPLATE_MODE), type(COMPONENT_CLOSE)
;

COMPONENT_CLOSE5: '>' -> popMode, popMode, popMode, popMode, type(COMPONENT_CLOSE);

COMPONENT_SLASH_CLOSE3:
    '/>' -> popMode, popMode, popMode, popMode, type(COMPONENT_SLASH_CLOSE)
;

// Any char not matching one of the rules above means we have an unquoted value like foo=bar
UNQUOTED_VALUE_PART: . -> pushMode(TEMPLATE_UNQUOTED_VALUE_MODE);

// *********************************************************************************************************************
mode TEMPLATE_UNQUOTED_VALUE_MODE;

// first whitespace pops all the way out of ATTVALUE back to component mode
COMPONENT_WHITESPACE_OUTPUT4: [ \t\r\n] -> popMode, popMode, skip;

// If we're in a bx:output tag, don't pop as far and stay in outut mode
COMPONENT_CLOSE_OUTPUT3:
    '>' {lastModeWas(TEMPLATE_OUTPUT_MODE,2)}? -> popMode, popMode, pushMode(DEFAULT_TEMPLATE_MODE), type( COMPONENT_CLOSE)
;

COMPONENT_CLOSE7:
    '>' {isQuery}? -> popMode, popMode, popMode, popMode, popMode, pushMode(TEMPLATE_OUTPUT_MODE), pushMode(DEFAULT_TEMPLATE_MODE), type(
        COMPONENT_CLOSE)
;

// If we find the end of the component, pop all the way out of the component
COMPONENT_CLOSE3: '>' -> popMode, popMode, popMode, popMode, popMode, type(COMPONENT_CLOSE);

// If we're in a bx:output tag, pop all the way out of the component
COMPONENT_SLASH_CLOSE5:
    '/>' {lastModeWas(TEMPLATE_OUTPUT_MODE,1)}? -> popMode, popMode, popMode, popMode, type( COMPONENT_SLASH_CLOSE )
;

COMPONENT_SLASH_CLOSE4:
    '/>' -> popMode, popMode, popMode, popMode, popMode, type(COMPONENT_SLASH_CLOSE)
;

UNQUOTED_VALUE_PART2: . -> type(UNQUOTED_VALUE_PART);

// *********************************************************************************************************************
mode TEMPLATE_EXPRESSION_MODE_COMPONENT;

//  This mode is only used for tracking on the stack, it will never match any chars
DUMMY: .;

// *********************************************************************************************************************
mode TEMPLATE_BXSCRIPT;

// This mode is only used for tracking on the stack, it will never match any chars
DUMMY2: .;

// *********************************************************************************************************************
mode TEMPLATE_POSSIBLE_COMPONENT;

PREFIX       : 'bx:'  {isQuery=false;} -> pushMode(TEMPLATE_COMPONENT_NAME_MODE);
SLASH_PREFIX : '/bx:' {isQuery=false;} -> pushMode(TEMPLATE_END_COMPONENT);

COMPONENT_OPEN2: '<' -> type(COMPONENT_OPEN);

TEMPLATE_ICHAR7:
    '#' {_modeStack.contains(TEMPLATE_OUTPUT_MODE)}? -> type(ICHAR), popMode, pushMode(hashMode), pushMode( DEFAULT_SCRIPT_MODE )
;

TEMPLATE_ANY: . -> type(CONTENT_TEXT), popMode;