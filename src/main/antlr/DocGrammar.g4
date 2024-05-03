parser grammar DocGrammar;

options {
	tokenVocab = DocLexer;
}

documentation:
	EOF
	| skipWhitespace* JAVADOC_START skipWhitespace* documentationContent? JAVADOC_END? NEWLINE? EOF;

documentationContent:
	description skipWhitespace*
	| skipWhitespace* tagSection
	| description NEWLINE+ skipWhitespace* tagSection;

space: SPACE;

skipWhitespace: SPACE | NEWLINE;

description:
	descriptionLine (
		(descriptionNewline | space)+ descriptionLine
	)*;

descriptionLine:
	SPACE? descriptionLineNoSpaceNoAt+ (
		descriptionLineNoSpaceNoAt
		| SPACE
		| AT
	)*;

descriptionLineNoSpaceNoAt:
	TEXT_CONTENT
	| NAME
	| STAR
	| SLASH
	| JAVADOC_START;

descriptionNewline: NEWLINE;

tagSection: blockTag+;

blockTag: SPACE? AT blockTagName SPACE? blockTagContent*;

blockTagName: NAME;

blockTagContent: blockTagText | NEWLINE;

blockTagText: blockTagTextElement+;

blockTagTextElement:
	TEXT_CONTENT
	| NAME
	| SPACE
	| STAR
	| SLASH
	| JAVADOC_START
	| AT;