lexer grammar CFTemplateLexer;

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

	public boolean lastModeWas( int mode, int count ) {
		java.util.List<Integer> modes = new java.util.ArrayList<Integer>();
		modes.add( _mode );
		for ( int m : _modeStack.toArray() ) {
			modes.add( m );
		}
		if ( modes.size() - 1 < count ) {
			return false;
		}
		return modes.get( modes.size() - count ) == mode;
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
 
 System.out.println( "*****
 modes ******" );
 System.out.println( "mode: " + modeNames[_mode] );
 for ( int m2 :
 _modeStack.toArray() ) {
 System.out.println( "mode: " + modeNames[m2] );
 }
 System.out.println(
 "***** end modes ******" );
 }
 
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
 }
 */

COMMENT_START: '<!---' -> pushMode(COMMENT_MODE);

WS: (' ' | '\t' | '\r'? '\n')+;

SCRIPT_OPEN: '<cfscript' .*? '>' -> pushMode(XFSCRIPT);

OUTPUT_START:
	'<cfoutput' -> pushMode(POSSIBLE_COMPONENT), pushMode(COMPONENT_MODE), pushMode(OUTPUT_MODE);

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
mode COMMENT_EXPRESSION;

COMMENT_END3: '--->' -> popMode, type(EXPRESSION_PART);

COMMENT_START3:
	'<!---' -> pushMode(COMMENT_EXPRESSION), type(EXPRESSION_PART);

COMMENT_TEXT2: .+? -> type(EXPRESSION_PART);

// *********************************************************************************************************************
mode COMPONENT_NAME_MODE;

// The rule of thumb here is that we are doing direct handling of any components for which we have a
// dedicated AST node for. All other components will be handled generically
COMPONENT: 'component' -> pushMode( COMPONENT_MODE );
INTERFACE: 'interface' -> pushMode( COMPONENT_MODE );
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
COMPONENT2: 'component' -> type(COMPONENT);
FUNCTION2: 'function' -> type(FUNCTION);
// popping back to: POSSIBLE_COMPONENT -> DEFAULT_MODE -> OUTPUT_MODE -> COMPONENT -> POSSIBLE_COMPONENT -> DEFAULT_MODE
OUTPUT_END:
	'output>' -> popMode, popMode, popMode, popMode, popMode, popMode;
INTERFACE2: 'interface' -> type(INTERFACE);
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

ICHAR20:
	'#' -> type(ICHAR), pushMode(EXPRESSION_MODE_UNQUOTED_ATTVALUE);

OPEN_QUOTE: '"' -> pushMode(quotesModeCOMPONENT);

OPEN_SINGLE:
	'\'' -> type( OPEN_QUOTE ), pushMode(squotesModeCOMPONENT);

// If we're in a cfoutput tag, don't pop as far and stay in outut mode
COMPONENT_CLOSE_OUTPUT2:
	'>' {lastModeWas(OUTPUT_MODE,1)}? -> popMode, pushMode(DEFAULT_MODE), type( COMPONENT_CLOSE );

// If we're in a cfoutput tag, pop all the way out of the component
COMPONENT_SLASH_CLOSE2:
	'/>' {lastModeWas(OUTPUT_MODE,1)}? -> popMode, popMode, popMode, type( COMPONENT_SLASH_CLOSE );

// There may be no value, so we need to pop out of ATTVALUE if we find the end of the component
COMPONENT_CLOSE5:
	'>' -> popMode, popMode, popMode, popMode, type(COMPONENT_CLOSE);

COMPONENT_SLASH_CLOSE3:
	'/>' -> popMode, popMode, popMode, popMode, type(COMPONENT_SLASH_CLOSE);

UNQUOTED_VALUE_PART: . -> pushMode(UNQUOTED_VALUE_MODE);

// *********************************************************************************************************************
mode UNQUOTED_VALUE_MODE;

// first whitespace pops all the way out of ATTVALUE back to component mode
COMPONENT_WHITESPACE_OUTPUT4:
	[ \t\r\n] -> popMode, popMode, skip;

// If we're in a cfoutput tag, don't pop as far and stay in outut mode
COMPONENT_CLOSE_OUTPUT3:
	'>' {lastModeWas(OUTPUT_MODE,2)}? -> popMode, popMode, pushMode(DEFAULT_MODE), type(
		COMPONENT_CLOSE);

// If we find the end of the component, pop all the way out of the component
COMPONENT_CLOSE3:
	'>' -> popMode, popMode, popMode, popMode, popMode, type(COMPONENT_CLOSE);

// If we're in a cfoutput tag, pop all the way out of the component
COMPONENT_SLASH_CLOSE5:
	'/>' {lastModeWas(OUTPUT_MODE,1)}? -> popMode, popMode, popMode, popMode, type(
		COMPONENT_SLASH_CLOSE );

COMPONENT_SLASH_CLOSE4:
	'/>' -> popMode, popMode, popMode, popMode, popMode, type(COMPONENT_SLASH_CLOSE);

UNQUOTED_VALUE_PART2: . -> type(UNQUOTED_VALUE_PART);

// *********************************************************************************************************************
mode EXPRESSION_MODE_COMPONENT;

// Allow => inside an expression without ending closing the component
FAT_ARROW: '=>' -> type(EXPRESSION_PART);

COMPONENT_SLASH_CLOSE1:
	'/>' -> type(COMPONENT_SLASH_CLOSE), popMode, popMode, popMode, popMode;

COMPONENT_CLOSE1:
	'>' -> type(COMPONENT_CLOSE), popMode, popMode, popMode, popMode;

EXPRESSION_PART: ~[=>'"/<]+;

EXPRESSION_PART1: '/' -> type(EXPRESSION_PART);

EXPRESSION_PART3: '<' -> type(EXPRESSION_PART);

EXPRESSION_PART6: '=' -> type(EXPRESSION_PART);

OPEN_QUOTE2:
	'"' -> pushMode(quotesModeExpression), type(OPEN_QUOTE);

OPEN_SINGLE2:
	'\'' -> type(OPEN_QUOTE), pushMode(squotesModeExpression);

COMMENT_START6:
	'<!---' -> pushMode(COMMENT_EXPRESSION), type(EXPRESSION_PART);

// *********************************************************************************************************************
mode EXPRESSION_MODE_UNQUOTED_ATTVALUE;
ICHAR4: '#' -> type(ICHAR), popMode, popMode;

STRING_EXPRESSION_PART2: ~[#'"<]+ -> type(EXPRESSION_PART);

EXPRESSION_PART4: '<' -> type(EXPRESSION_PART);

OPEN_QUOTE4:
	'"' -> pushMode(quotesModeExpression), type(OPEN_QUOTE);

OPEN_SINGLE4:
	'\'' -> type( OPEN_QUOTE ), pushMode(squotesModeExpression);

COMMENT_START5:
	'<!---' -> pushMode(COMMENT_EXPRESSION), type(EXPRESSION_PART);

// *********************************************************************************************************************
mode EXPRESSION_MODE_STRING;
ICHAR1: '#' -> type(ICHAR), popMode;

STRING_EXPRESSION_PART: ~[#'"<]+ -> type(EXPRESSION_PART);

EXPRESSION_PART5: '<' -> type(EXPRESSION_PART);

OPEN_QUOTE3:
	'"' -> pushMode(quotesModeExpression), type(OPEN_QUOTE);

OPEN_SINGLE3:
	'\'' -> type( OPEN_QUOTE ), pushMode(squotesModeExpression);

COMMENT_START7:
	'<!---' -> pushMode(COMMENT_EXPRESSION), type(EXPRESSION_PART);

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
	'</' COMPONENT_WHITESPACE2 'cfscript' COMPONENT_WHITESPACE2 '>' -> popMode;

SCRIPT_BODY: .+?;

// *********************************************************************************************************************
mode POSSIBLE_COMPONENT;

PREFIX: 'cf' -> pushMode(COMPONENT_NAME_MODE);
SLASH_PREFIX: '/cf' -> pushMode(END_COMPONENT);

ICHAR7:
	'#' {_modeStack.contains(OUTPUT_MODE)}? -> type(ICHAR), popMode, pushMode(EXPRESSION_MODE_STRING
		);

ANY: . -> type(CONTENT_TEXT), popMode;