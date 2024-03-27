lexer grammar CFScriptLexer;

options {
	caseInsensitive = true;
}

import BaseScriptLexer;

CLASS_NAME: 'COMPONENT';
PREFIXEDIDENTIFIER: 'cf' IDENTIFIER;