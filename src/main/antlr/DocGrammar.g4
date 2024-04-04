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
	| BRACE_OPEN
	| BRACE_CLOSE
	| JAVADOC_START;

descriptionNewline: NEWLINE;

tagSection: blockTag+;

blockTag: SPACE? AT blockTagName SPACE? blockTagContent*;

blockTagName: NAME;

blockTagContent: blockTagText | inlineTag | NEWLINE;

blockTagText: blockTagTextElement+;

blockTagTextElement:
	TEXT_CONTENT
	| NAME
	| SPACE
	| STAR
	| SLASH
	| BRACE_OPEN
	| BRACE_CLOSE
	| JAVADOC_START;

inlineTag:
	INLINE_TAG_START inlineTagName SPACE* inlineTagContent? BRACE_CLOSE;

inlineTagName: NAME;

inlineTagContent: braceContent+;

braceExpression: BRACE_OPEN braceContent* BRACE_CLOSE;

braceContent: braceExpression | braceText (NEWLINE* braceText)*;

braceText: TEXT_CONTENT | NAME | SPACE | STAR | SLASH | NEWLINE;