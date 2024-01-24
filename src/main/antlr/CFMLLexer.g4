lexer grammar CFMLLexer;

options {
	caseInsensitive = true;
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
COMMENT: '<!---' .*? '--->' -> channel(HIDDEN);

SEA_WS: (' ' | '\t' | '\r'? '\n')+ -> channel(HIDDEN);

SCRIPT_OPEN: '<cfscript' .*? '>' -> pushMode(XFSCRIPT);

TAG_OPEN: '<' -> pushMode(POSSIBLE_TAG);

HASHHASH: '##' -> type(CONTENT_TEXT);
ICHAR:
	'#' {_modeStack.contains(OUTPUT_MODE)}? -> pushMode(EXPRESSION_MODE_STRING);
ICHAR_1: '#' -> type(CONTENT_TEXT);

CONTENT_TEXT: ~[<#]+;

// *********************************************************************************************************************
mode POSSIBLE_TAG;

PREFIX: 'cf' -> pushMode(TAG);
SLASH_PREFIX: '/cf' -> pushMode(END_TAG);
ANY: . -> type(CONTENT_TEXT), popMode;

// *********************************************************************************************************************
mode TAG;

// The rule of thumb here is that we are doing direct handling of any tags for which we have a
// dedicated AST node for. All other tags will be handled generically
COMPONENT: 'component';
INTERFACE: 'interface';
FUNCTION: 'function';
ARGUMENT: 'argument';

SCRIPT: 'script' -> pushMode(XFSCRIPT);
OUTPUT: 'output' -> pushMode(OUTPUT_MODE);
RETURN: 'return' -> pushMode(EXPRESSION_MODE_TAG);

IF: 'if' -> pushMode(EXPRESSION_MODE_TAG);
ELSE: 'else';
ELSEIF: 'elseif' -> pushMode(EXPRESSION_MODE_TAG);

SET: 'set ' -> pushMode(EXPRESSION_MODE_TAG);

TRY: 'try';
CATCH: 'catch';
FINALLY: 'finally';
IMPORT: 'import';
WHILE: 'while';
BREAK: 'break';
CONTINUE: 'continue';
INCLUDE: 'include';
PROPERTY: 'property';
RETHROW: 'rethrow';

TAG_CLOSE: '>' -> popMode, popMode;

TAG_SLASH_CLOSE: '/>' -> popMode, popMode;

TAG_SLASH: '/';

TAG_EQUALS: '=' -> pushMode(ATTVALUE);

TAG_NAME: TAG_NameStartChar TAG_NameChar*;

TAG_WHITESPACE: [ \t\r\n] -> skip;

fragment DIGIT: [0-9];

fragment TAG_NameChar: TAG_NameStartChar | '_' | DIGIT;

fragment TAG_NameStartChar: [:a-z];

// *********************************************************************************************************************
mode OUTPUT_MODE;

TAG_CLOSE_OUTPUT:
	'>' -> pushMode(DEFAULT_MODE), type(TAG_CLOSE);

TAG_SLASH_CLOSE_OUTPUT:
	'/>' -> popMode, popMode, popMode, type(TAG_SLASH_CLOSE);

TAG_EQUALS_OUTPUT:
	'=' -> pushMode(ATTVALUE), type(TAG_EQUALS);

TAG_NAME_OUTPUT:
	TAG_NameStartChar TAG_NameChar* -> type(TAG_NAME);

TAG_WHITESPACE_OUTPUT: [ \t\r\n] -> skip;

// *********************************************************************************************************************
mode END_TAG;

IF2: 'if' -> type(IF);
COMPONENT2: 'component' -> type(COMPONENT);
FUNCTION2: 'function' -> type(FUNCTION);
// popping back to: POSSIBLE_TAG -> DEFAULT_MODE -> OUTPUT_MODE -> TAG -> POSSIBLE_TAG -> DEFAULT_MODE
OUTPUT2:
	'output>' -> type(OUTPUT), popMode, popMode, popMode, popMode, popMode, popMode;
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

TAG_NAME2: TAG_NameStartChar TAG_NameChar* -> type(TAG_NAME);
TAG_CLOSE2: '>' -> popMode, popMode, type(TAG_CLOSE);

// *********************************************************************************************************************
mode XFSCRIPT;

fragment TAG_WHITESPACE2: [ \t\r\n]*;
SCRIPT_END_BODY:
	'</' TAG_WHITESPACE2 'cfscript' TAG_WHITESPACE2 '>' -> popMode;

SCRIPT_BODY: .+?;

// *********************************************************************************************************************
mode ATTVALUE;

fragment DIGIT2: [0-9];
IDENTIFIER: [a-z_$]+ ( [_]+ | [a-z]+ | DIGIT2)* -> popMode;

OPEN_QUOTE: '"' -> pushMode(quotesModeTag);

OPEN_SINGLE:
	'\'' -> type( OPEN_QUOTE ), pushMode(squotesModeTag);

// *********************************************************************************************************************
mode EXPRESSION_MODE_TAG;

TAG_CLOSE1: '>' -> type(TAG_CLOSE), popMode, popMode;

EXPRESSION_PART: ~[>'"]+;

OPEN_QUOTE2:
	'"' -> pushMode(quotesModeExpression), type(OPEN_QUOTE);

OPEN_SINGLE2:
	'\'' -> type(OPEN_QUOTE), pushMode(squotesModeExpression);

// *********************************************************************************************************************
mode EXPRESSION_MODE_STRING;
ICHAR1: '#' -> type(ICHAR), popMode;

STRING_EXPRESSION_PART: ~[#'"]+ -> type(EXPRESSION_PART);

OPEN_QUOTE3:
	'"' -> pushMode(quotesModeExpression), type(OPEN_QUOTE);

OPEN_SINGLE3:
	'\'' -> type( OPEN_QUOTE ), pushMode(squotesModeExpression);

// *********************************************************************************************************************
mode squotesModeTag;
ICHAR2: '#' -> pushMode(EXPRESSION_MODE_STRING);
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
mode quotesModeTag;
ICHAR3: '#' -> type(ICHAR), pushMode(EXPRESSION_MODE_STRING);
CLOSE_QUOTE:
	'"' {  
		//if (modeNames[_modeStack.peek()].equals( "ATTVALUE" )) {
			//System.out.println( "Extra POP" );
			//popMode();
		//	} 
		} -> popMode, popMode;

HASHHASH1: '##';
STRING_LITERAL: (~["#]+ | '""')+;

// *********************************************************************************************************************
mode squotesModeExpression;
ICHAR4: '#' -> pushMode(EXPRESSION_MODE_STRING), type(ICHAR);
CLOSE_SQUOTE3: '\'' -> type( CLOSE_QUOTE), popMode;

SHASHHASH3: '##' -> type(HASHHASH);
SSTRING_LITERAL3: (~['#]+ | '\'\'')+ -> type(STRING_LITERAL);

// *********************************************************************************************************************
mode quotesModeExpression;
ICHAR5: '#' -> type(ICHAR), pushMode(EXPRESSION_MODE_STRING);
CLOSE_QUOTE4: '"' -> popMode, type(CLOSE_QUOTE);

HASHHASH4: '##' -> type(HASHHASH);
STRING_LITERAL4: (~["#]+ | '""')+ -> type(STRING_LITERAL);