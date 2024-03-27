lexer grammar CFTemplateLexer;

options {
	caseInsensitive = true;
}

import BaseTemplateLexer;

SCRIPT_OPEN: '<cfscript' .*? '>' -> pushMode(XFSCRIPT);

OUTPUT_START:
	'<cfoutput' -> pushMode(POSSIBLE_COMPONENT), pushMode(COMPONENT_MODE), pushMode(OUTPUT_MODE);

// *********************************************************************************************************************
mode XFSCRIPT;

fragment COMPONENT_WHITESPACE2: [ \t\r\n]*;
SCRIPT_END_BODY:
	'</' COMPONENT_WHITESPACE2 'cfscript' COMPONENT_WHITESPACE2 '>' -> popMode;

SCRIPT_BODY: .+?;

// *********************************************************************************************************************
mode POSSIBLE_COMPONENT;

PREFIX: 'cf' -> pushMode(COMPONENT_MODE);
SLASH_PREFIX: '/cf' -> pushMode(END_COMPONENT);
ANY: . -> type(CONTENT_TEXT), popMode;