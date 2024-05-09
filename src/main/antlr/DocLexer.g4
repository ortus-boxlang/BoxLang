lexer grammar DocLexer;

NAME: [a-zA-Z.0-9]+;

NEWLINE:
	'\n' (SPACE? (STAR {_input.LA(1) != '/'}?)+)?
	| '\r\n' (SPACE? (STAR {_input.LA(1) != '/'}?)+)?
	| '\r' (SPACE? (STAR {_input.LA(1) != '/'}?)+)?;

SPACE: (' ' | '\t')+;

TEXT_CONTENT: ~[\n\r\t @*/a-zA-Z]+;

AT: '@';

STAR: '*';

SLASH: '/';

DOC_START: '/**' STAR*;

DOC_END: SPACE? STAR* '*/';