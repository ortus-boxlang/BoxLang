parser grammar DocGrammar;

options {
	tokenVocab = DocLexer;
}

documentation:
	skipWhitespace* DOC_START skipWhitespace* documentationContent? NEWLINE? DOC_END? NEWLINE?;

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
	| DOC_START;

descriptionNewline: NEWLINE;

tagSection: blockTag+;

blockTag:
	SPACE? AT blockTagName SPACE? blockTagContent* NEWLINE*;

blockTagName: NAME+;

blockTagContent:
	blockTagText
	| ( (SPACE+)? NEWLINE+ blockTagTextElementNoAt);

blockTagText: blockTagTextElementNoAt blockTagTextElement*;

blockTagTextElement:
	TEXT_CONTENT
	| NAME
	| SPACE
	| STAR
	| SLASH
	| DOC_START
	| AT;

blockTagTextElementNoAt:
	TEXT_CONTENT
	| NAME
	| SPACE
	| STAR
	| SLASH
	| DOC_START;