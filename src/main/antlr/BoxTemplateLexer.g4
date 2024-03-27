lexer grammar BoxTemplateLexer;

options {
	caseInsensitive = true;
}

import BaseTemplateLexer;

SCRIPT_OPEN: '<bx:script' .*? '>' -> pushMode(XFSCRIPT);

OUTPUT_START:
	'<bx:output' -> pushMode(POSSIBLE_COMPONENT), pushMode(COMPONENT_MODE), pushMode(OUTPUT_MODE);

// *********************************************************************************************************************
mode XFSCRIPT;

fragment COMPONENT_WHITESPACE2: [ \t\r\n]*;
SCRIPT_END_BODY:
	'</' COMPONENT_WHITESPACE2 'bx:script' COMPONENT_WHITESPACE2 '>' -> popMode;

SCRIPT_BODY: .+?;

// *********************************************************************************************************************
mode POSSIBLE_COMPONENT;

PREFIX: 'bx:' -> pushMode(COMPONENT_MODE);
SLASH_PREFIX: '/bx:' -> pushMode(END_COMPONENT);
ANY: . -> type(CONTENT_TEXT), popMode;