lexer grammar BoxTemplateLexer;

options {
	caseInsensitive = true;
}

@members {

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
}

/*
 @members {
 
 
 public int popMode() {
 System.out.println( "popMode back to "+
 modeNames[_modeStack.peek()]);
 return super.popMode();
 }
 
 public void pushMode(int m) {
 System.out.println( "pushMode "+modeNames[m]);
 super.pushMode(m);
 }
 
 public Token emit() {
 Token t = _factory.create(_tokenFactorySourcePair, _type, _text, _channel, _tokenStartCharIndex,
 getCharIndex()-1,
 _tokenStartLine, _tokenStartCharPositionInLine);
 emit(t);
 System.out.println(
 t.toString() + " " + _SYMBOLIC_NAMES[t.getType()] );
 return t;
 }
 }
 */

COMMENT_START: '<!---' -> pushMode(COMMENT_MODE);

WS: (' ' | '\t' | '\r'? '\n')+;

SCRIPT_OPEN: '<bx:script' .*? '>' -> pushMode(XFSCRIPT);

OUTPUT_START:
	'<bx:output' -> pushMode(POSSIBLE_COMPONENT), pushMode(COMPONENT_MODE), pushMode(OUTPUT_MODE);

COMPONENT_OPEN: '<' -> pushMode(POSSIBLE_COMPONENT);

HASHHASH: '##' -> type(CONTENT_TEXT);

ICHAR:
	'#' {_modeStack.contains(OUTPUT_MODE)}? -> pushMode(EXPRESSION_MODE_STRING);
ICHAR_1: '#' -> type(CONTENT_TEXT);

CONTENT_TEXT: ~[<#]+;

// *********************************************************************************************************************
mode COMMENT_MODE;

// If we reach an "ending" comment, but there are 2 or more TAG_COMMENT modes on the stack, this is
// just the end of a nested comment so we emit a TAG_COMMENT_TEXT token instead.
COMMENT_END_BUT_NOT_REALLY:
	'--->' {countModes(COMMENT_MODE) > 1}? -> type(COMMENT_TEXT), popMode;

COMMENT_END: '--->' -> popMode;

COMMENT_START2:
	'<!---' -> pushMode(COMMENT_MODE), type(COMMENT_START);

COMMENT_TEXT: .+?;

// *********************************************************************************************************************
mode COMMENT_QUIET;

// If we reach an "ending" comment, but there are 2 or more TAG_COMMENT modes on the stack, this is
// just the end of a nested comment so we emit a TAG_COMMENT_TEXT token instead.
COMMENT_END_BUT_NOT_REALLY_QUIET:
	'--->' {countModes(COMMENT_QUIET) > 1}? -> type(COMMENT_TEXT), channel(HIDDEN), popMode;

COMMENT_END_QUIET:
	'--->' -> popMode, channel(HIDDEN), type(COMMENT_END);

COMMENT_START_QUIET:
	'<!---' -> pushMode(COMMENT_QUIET), channel(HIDDEN), type(COMMENT_START);

COMMENT_TEXT_QUIET:
	.+? -> type(COMMENT_TEXT), channel(HIDDEN);

// *********************************************************************************************************************
mode COMPONENT_NAME_MODE;

// The rule of thumb here is that we are doing direct handling of any components for which we have a
// dedicated AST node for. All other components will be handled generically
FUNCTION: 'function' -> pushMode( COMPONENT_MODE );
ARGUMENT: 'argument' -> pushMode( COMPONENT_MODE );

// return may or may not have an expression, so eat any leading whitespace now so it doesn't give us an expression part that's just a space
RETURN:
	'return' [ \t\r\n]* -> pushMode( COMPONENT_MODE ), pushMode(EXPRESSION_MODE_COMPONENT);

IF:
	'if' -> pushMode( COMPONENT_MODE ), pushMode(EXPRESSION_MODE_COMPONENT);
ELSE: 'else' -> pushMode( COMPONENT_MODE );
ELSEIF:
	'elseif' -> pushMode( COMPONENT_MODE ), pushMode(EXPRESSION_MODE_COMPONENT);

SET:
	'set ' -> pushMode( COMPONENT_MODE ), pushMode(EXPRESSION_MODE_COMPONENT);

TRY: 'try' -> pushMode( COMPONENT_MODE );
CATCH: 'catch' -> pushMode( COMPONENT_MODE );
FINALLY: 'finally' -> pushMode( COMPONENT_MODE );
IMPORT: 'import' -> pushMode( COMPONENT_MODE );
WHILE: 'while' -> pushMode( COMPONENT_MODE );
BREAK: 'break' -> pushMode( COMPONENT_MODE );
CONTINUE: 'continue' -> pushMode( COMPONENT_MODE );
INCLUDE: 'include' -> pushMode( COMPONENT_MODE );
PROPERTY: 'property' -> pushMode( COMPONENT_MODE );
RETHROW: 'rethrow' -> pushMode( COMPONENT_MODE );
THROW: 'throw' -> pushMode( COMPONENT_MODE );
SWITCH: 'switch' -> pushMode( COMPONENT_MODE );
CASE: 'case' -> pushMode( COMPONENT_MODE );
DEFAULTCASE: 'defaultcase' -> pushMode( COMPONENT_MODE );

COMPONENT_NAME:
	COMPONENT_NameStartChar COMPONENT_NameChar* -> pushMode( COMPONENT_MODE );

fragment DIGIT: [0-9];

fragment COMPONENT_NameChar:
	COMPONENT_NameStartChar
	| '_'
	| '-'
	| DIGIT
	| ':';

fragment COMPONENT_NameStartChar: [a-z_];

// *********************************************************************************************************************
mode COMPONENT_MODE;

// Comments can live inside of a tag <cfTag <!--- comment ---> foo=bar >
COMMENT_START1:
	'<!---' -> pushMode(COMMENT_QUIET), channel(HIDDEN), type(COMMENT_START);

COMPONENT_CLOSE: '>' -> popMode, popMode, popMode;

COMPONENT_SLASH_CLOSE: '/>' -> popMode, popMode, popMode;

COMPONENT_SLASH: '/';

COMPONENT_EQUALS: '=' -> pushMode(ATTVALUE);

ATTRIBUTE_NAME: ATTRIBUTE_NameStartChar ATTRIBUTE_NameChar*;

COMPONENT_WHITESPACE: [ \t\r\n] -> skip;

fragment ATTRIBUTE_DIGIT: [0-9];

fragment ATTRIBUTE_NameChar:
	ATTRIBUTE_NameStartChar
	| '_'
	| '-'
	| ATTRIBUTE_DIGIT
	| ':';

fragment ATTRIBUTE_NameStartChar: [a-z_];

// *********************************************************************************************************************
mode OUTPUT_MODE;

// Source inside of an output tag is consumed in output mode
COMMENT_START4:
	'<!---' -> pushMode(COMMENT_MODE), type(COMMENT_START);

COMPONENT_CLOSE_OUTPUT:
	'>' -> pushMode(DEFAULT_MODE), type(COMPONENT_CLOSE);

COMPONENT_SLASH_CLOSE_OUTPUT:
	'/>' -> popMode, popMode, popMode, type(COMPONENT_SLASH_CLOSE);

COMPONENT_EQUALS_OUTPUT:
	'=' -> pushMode(ATTVALUE), type(COMPONENT_EQUALS);

ATTRIBUTE_NAME_OUTPUT:
	ATTRIBUTE_NameStartChar ATTRIBUTE_NameChar* -> type(ATTRIBUTE_NAME);

COMPONENT_WHITESPACE_OUTPUT: [ \t\r\n] -> skip;

// *********************************************************************************************************************
mode END_COMPONENT;

IF2: 'if' -> type(IF);
FUNCTION2: 'function' -> type(FUNCTION);
// popping back to: POSSIBLE_COMPONENT -> DEFAULT_MODE -> OUTPUT_MODE -> COMPONENT -> POSSIBLE_COMPONENT -> DEFAULT_MODE
OUTPUT_END:
	'output>' -> popMode, popMode, popMode, popMode, popMode, popMode;
TRY2: 'try' -> type(TRY);
CATCH2: 'catch' -> type(CATCH);
FINALLY2: 'finally' -> type(FINALLY);
IMPORT2: 'import' -> type(IMPORT);
WHILE2: 'while' -> type(WHILE);
BREAK2: 'break' -> type(BREAK);
CONTINUE2: 'continue' -> type(CONTINUE);
INCLUDE2: 'include' -> type(INCLUDE);
PROPERTY2: 'property' -> type(PROPERTY);
RETHROW2: 'rethrow' -> type(RETHROW);
THROW2: 'throw' -> type(THROW);
SWITCH2: 'switch' -> type(SWITCH);
CASE2: 'case' -> type(CASE);
DEFAULTCASE2: 'defaultcase' -> type(DEFAULTCASE);

COMPONENT_WHITESPACE_OUTPUT3: [ \t\r\n] -> skip;

COMPONENT_NAME2:
	COMPONENT_NameStartChar COMPONENT_NameChar* -> type(COMPONENT_NAME);
COMPONENT_CLOSE2:
	'>' -> popMode, popMode, type(COMPONENT_CLOSE);

// *********************************************************************************************************************
mode ATTVALUE;

COMPONENT_WHITESPACE_OUTPUT2: [ \t\r\n] -> skip;

IDENTIFIER: [a-z_$0-9-{}]+ -> popMode;

ICHAR20:
	'#' -> type(ICHAR), pushMode(EXPRESSION_MODE_UNQUOTED_ATTVALUE);

OPEN_QUOTE: '"' -> pushMode(quotesModeCOMPONENT);

OPEN_SINGLE:
	'\'' -> type( OPEN_QUOTE ), pushMode(squotesModeCOMPONENT);

// *********************************************************************************************************************
mode EXPRESSION_MODE_COMPONENT;

// Allow => inside an expression without ending closing the component
FAT_ARROW: '=>' -> type(EXPRESSION_PART);
SKINNY_ARROW: '->' -> type(EXPRESSION_PART);

COMPONENT_SLASH_CLOSE1:
	'/>' -> type(COMPONENT_SLASH_CLOSE), popMode, popMode, popMode, popMode;

COMPONENT_CLOSE1:
	'>' -> type(COMPONENT_CLOSE), popMode, popMode, popMode, popMode;

EXPRESSION_PART: ~[-=>'"/]+;

EXPRESSION_PART1: '/' -> type(EXPRESSION_PART);

EXPRESSION_PART6: '=' -> type(EXPRESSION_PART);

EXPRESSION_PART7: '-' -> type(EXPRESSION_PART);

OPEN_QUOTE2:
	'"' -> pushMode(quotesModeExpression), type(OPEN_QUOTE);

OPEN_SINGLE2:
	'\'' -> type(OPEN_QUOTE), pushMode(squotesModeExpression);

// *********************************************************************************************************************
mode EXPRESSION_MODE_UNQUOTED_ATTVALUE;
ICHAR4: '#' -> type(ICHAR), popMode, popMode;

STRING_EXPRESSION_PART2: ~[#'"]+ -> type(EXPRESSION_PART);

OPEN_QUOTE4:
	'"' -> pushMode(quotesModeExpression), type(OPEN_QUOTE);

OPEN_SINGLE4:
	'\'' -> type( OPEN_QUOTE ), pushMode(squotesModeExpression);

// *********************************************************************************************************************
mode EXPRESSION_MODE_STRING;
ICHAR1: '#' -> type(ICHAR), popMode;

STRING_EXPRESSION_PART: ~[#'"]+ -> type(EXPRESSION_PART);

OPEN_QUOTE3:
	'"' -> pushMode(quotesModeExpression), type(OPEN_QUOTE);

OPEN_SINGLE3:
	'\'' -> type( OPEN_QUOTE ), pushMode(squotesModeExpression);

// *********************************************************************************************************************
mode squotesModeCOMPONENT;
ICHAR2: '#' -> pushMode(EXPRESSION_MODE_STRING), type(ICHAR);
CLOSE_SQUOTE:
	'\'' {
		//if ( modeNames [_modeStack.peek()].equals ("ATTVALUE")	) {
			//System.out.println( "Extra POP (single)" );
		//	popMode();
		//	}
		 } -> type( CLOSE_QUOTE), popMode, popMode;

SHASHHASH: '##' -> type(HASHHASH);
SSTRING_LITERAL: (~['#]+ | '\'\'')+ -> type(STRING_LITERAL);

// *********************************************************************************************************************
mode quotesModeCOMPONENT;
ICHAR3: '#' -> type(ICHAR), pushMode(EXPRESSION_MODE_STRING);
CLOSE_QUOTE:
	'"' {  
		//if (modeNames[_modeStack.peek()].equals( "ATTVALUE" )) {
			//System.out.println( "Extra POP" );
			//popMode();
		//	} 
		} -> popMode, popMode;

HASHHASH1: '##' -> type(HASHHASH);
STRING_LITERAL: (~["#]+ | '""')+;

// *********************************************************************************************************************
mode squotesModeExpression;
ICHAR5: '#' -> pushMode(EXPRESSION_MODE_STRING), type(ICHAR);
CLOSE_SQUOTE3: '\'' -> type( CLOSE_QUOTE), popMode;

SHASHHASH3: '##' -> type(HASHHASH);
SSTRING_LITERAL3: (~['#]+ | '\'\'')+ -> type(STRING_LITERAL);

// *********************************************************************************************************************
mode quotesModeExpression;
ICHAR6: '#' -> type(ICHAR), pushMode(EXPRESSION_MODE_STRING);
CLOSE_QUOTE4: '"' -> popMode, type(CLOSE_QUOTE);

HASHHASH4: '##' -> type(HASHHASH);
STRING_LITERAL4: (~["#]+ | '""')+ -> type(STRING_LITERAL);

// *********************************************************************************************************************
mode XFSCRIPT;

fragment COMPONENT_WHITESPACE2: [ \t\r\n]*;
SCRIPT_END_BODY:
	'</' COMPONENT_WHITESPACE2 'bx:script' COMPONENT_WHITESPACE2 '>' -> popMode;

SCRIPT_BODY: .+?;

// *********************************************************************************************************************
mode POSSIBLE_COMPONENT;

PREFIX: 'bx:' -> pushMode(COMPONENT_NAME_MODE);
SLASH_PREFIX: '/bx:' -> pushMode(END_COMPONENT);

ICHAR7:
	'#' {_modeStack.contains(OUTPUT_MODE)}? -> type(ICHAR), popMode, pushMode(EXPRESSION_MODE_STRING
		);

ANY: . -> type(CONTENT_TEXT), popMode;