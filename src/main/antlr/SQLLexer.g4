// Based on MIT-licensed SQL Lite ANTLR4 grammar
// https://github.com/antlr/grammars-v4/tree/master/sql/sqlite

// $antlr-format alignTrailingComments on, columnLimit 150, maxEmptyLinesToKeep 1, reflowComments off, useTab off
// $antlr-format allowShortRulesOnASingleLine on, alignSemicolons ownLine

lexer grammar SQLLexer;

options {
    caseInsensitive = true;
}

SCOL: ';';
DOT: '.';
OPEN_PAR: '(';
CLOSE_PAR: ')';
COMMA: ',';
ASSIGN: '=';
STAR: '*';
PLUS: '+';
CARET: '^';
MINUS: '-';
TILDE: '~';
PIPE2: '||';
DIV: '/';
MOD: '%';
// Not used?
LT2: '<<';
// Not used?
GT2: '>>';
AMP: '&';
PIPE: '|';
LT: '<';
LT_EQ: '<=';
GT: '>';
GT_EQ: '>=';
EQ: '==';
NOT_EQ1: '!=';
BANG: '!';
NOT_EQ2: '<>';

ALL_: 'ALL';
ALTER_: 'ALTER';
AND_: 'AND';
AS_: 'AS';
ASC_: 'ASC';
BEGIN_: 'BEGIN';
BETWEEN_: 'BETWEEN';
BY_: 'BY';
CASE_: 'CASE';
CAST_: 'CAST';
CONVERT_: 'CONVERT';
CROSS_: 'CROSS';
DESC_: 'DESC';
DISTINCT_: 'DISTINCT';
ELSE_: 'ELSE';
END_: 'END';
ESCAPE_: 'ESCAPE';
FROM_: 'FROM';
FULL_: 'FULL';
GROUP_: 'GROUP';
HAVING_: 'HAVING';
IN_: 'IN';
INNER_: 'INNER';
IS_: 'IS';
JOIN_: 'JOIN';
LEFT_: 'LEFT';
LIKE_: 'LIKE';
LIMIT_: 'LIMIT';
NOT_: 'NOT';
NULL_: 'NULL';
ON_: 'ON';
OR_: 'OR';
ORDER_: 'ORDER';
OUTER_: 'OUTER';
RIGHT_: 'RIGHT';
SELECT_: 'SELECT';
THEN_: 'THEN';
TOP: 'TOP';
UNION_: 'UNION';
WHEN_: 'WHEN';
WHERE_: 'WHERE';
TRUE_: 'TRUE';
FALSE_: 'FALSE';

FUNCTION_NAME: . {false}?;

IDENTIFIER:
    '"' (~'"' | '""')* '"'
    | '`' (~'`' | '``')* '`'
    | '[' ~']'* ']'
    | [A-Z$_\u007F-\uFFFF] [A-Z$_0-9\u007F-\uFFFF]*
;

NUMERIC_LITERAL: ((DIGIT+ ('.' DIGIT*)?) | ('.' DIGIT+)) ('E' [-+]? DIGIT+)? | '0x' HEX_DIGIT+;

BIND_PARAMETER: '?' | ':' IDENTIFIER;

STRING_LITERAL: '\'' ( ~'\'' | '\'\'')* '\'';

BLOB_LITERAL: 'X' STRING_LITERAL;

SINGLE_LINE_COMMENT: '--' ~[\r\n]* (('\r'? '\n') | EOF) -> channel(HIDDEN);

MULTILINE_COMMENT: '/*' .*? '*/' -> channel(HIDDEN);

SPACES: [ \u000B\t\r\n] -> channel(HIDDEN);

UNEXPECTED_CHAR: .;

fragment HEX_DIGIT: [0-9A-F];
fragment DIGIT: [0-9];