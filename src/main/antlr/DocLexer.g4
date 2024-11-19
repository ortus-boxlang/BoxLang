lexer grammar DocLexer;

NEWLINE:
	'\n' (SPACE? (STAR {_input.LA(1) != '/'}?)+)?
	| '\r\n' (SPACE? (STAR {_input.LA(1) != '/'}?)+)?
	| '\r' (SPACE? (STAR {_input.LA(1) != '/'}?)+)?;

SPACE: (' ' | '\t')+;

TEXT_CONTENT: ~[\n\r\t @*/]+;

AT: '@' -> pushMode(TAG_NAME);

STAR: '*';

SLASH: '/';

DOC_START: '/**' STAR*;

DOC_END: SPACE? STAR* '*/';

// Match @anythinghere
mode TAG_NAME;

// line breaks mark the end of a tag name
NEWLINE2:
	(
		'\n' (SPACE? (STAR {_input.LA(1) != '/'}?)+)?
		| '\r\n' (SPACE? (STAR {_input.LA(1) != '/'}?)+)?
		| '\r' (SPACE? (STAR {_input.LA(1) != '/'}?)+)?
	) -> popMode, type(NEWLINE);

// spaces or tabs mark the end of a tag name
SPACE2: (' ' | '\t')+ -> popMode, type(SPACE);

STAR2: '*' -> type(NAME);

// The end of the doc comment also ends the tag
DOC_END2: SPACE? STAR* '*/' -> popMode, type(DOC_END);

// All other text is part of the name
NAME: (~[ \t\r\n*])+;