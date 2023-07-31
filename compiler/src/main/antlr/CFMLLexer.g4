lexer grammar CFMLLexer;

options {
    caseInsensitive = true;
}

HTML_COMMENT
    : '<!--' .*? '-->'
    ;

CFML_COMMENT
    : '<!---' .*? '--->'
    ;

HTML_CONDITIONAL_COMMENT
    : '<![' .*? ']>'
    ;

XML_DECLARATION
    : '<?xml' .*? '>'
    ;

CDATA
    : '<![CDATA[' .*? ']]>'
    ;

DTD
    : '<!' .*? '>'
    ;

SCRIPTLET
    : '<?' .*? '?>'
    | '<%' .*? '%>'
    ;

SEA_WS
    :  (' '|'\t'|'\r'? '\n')+  -> channel(HIDDEN)
    ;

SCRIPT_OPEN
    : '<script' .*? '>' ->pushMode(SCRIPT)
    ;

CFSCRIPT_OPEN
    : '<cfscript' .*? '>' ->pushMode(XFSCRIPT)
    ;

STYLE_OPEN
    : '<style' .*? '>'  ->pushMode(STYLE)
    ;

TAG_OPEN
    : '<' -> pushMode(TAG)
    ;

HTML_TEXT
    : ~'<'+
    ;



//
// tag declarations
//
mode TAG;


CFCOMPONENT     : 'cfcomponent';
CFARGUMENT      : 'cfargument';
CFDUMP          : 'cfdump';
CFFUNCTION      : 'cffunction';
CFSCRIPT        : 'cfscript' ->  pushMode(XFSCRIPT);

CFRETURN        : 'cfreturn' -> pushMode(CFEXPRESSION_MODE);
CFIF            : 'cfif' -> pushMode(CFEXPRESSION_MODE);
CFELSE          : 'cfelse';
CFELSEIF        :  'cfelseif' -> pushMode(CFEXPRESSION_MODE);
CFQUERY         : 'cfquery';
CFINTERFACE     : 'cfinterface';
CFTHROW         : 'cfthrow';
CFLOOP          : 'cfloop';
CFPARAM         : 'cfparam';
CFTRY           : 'cftry';
CFCATCH         : 'cfcatch';
CFABORT         : 'cfabort';
CFLOCK          : 'cflock';
CFINCLUDE       : 'cfinclude';
CFINVOKE        : 'cfinvoke';
CFSET	        : 'cfset ' -> pushMode(CFEXPRESSION_MODE);
CFINVOKEARGUMENT: 'cfinvokeargument';
CFFILE          : 'cffile' ;


TAG_CLOSE
    : '>' -> popMode
    ;

TAG_SLASH_CLOSE
    : '/>' -> popMode
    ;

TAG_SLASH
    : '/'
    ;

//
// lexing mode for attribute values
//
TAG_EQUALS
    : '=' -> pushMode(ATTVALUE)
    ;

TAG_NAME
    : TAG_NameStartChar TAG_NameChar*
    ;

TAG_WHITESPACE
    : [ \t\r\n] -> skip
    ;

fragment
HEXDIGIT
    : [a-fA-F0-9]
    ;

fragment
DIGIT
    : [0-9]
    ;

fragment
TAG_NameChar
    : TAG_NameStartChar
    | '-'
    | '_'
    | '.'
    | DIGIT
    |   '\u00B7'
    |   '\u0300'..'\u036F'
    |   '\u203F'..'\u2040'
    ;

fragment
TAG_NameStartChar
    :   [:a-zA-Z]
    |   '\u2070'..'\u218F'
    |   '\u2C00'..'\u2FEF'
    |   '\u3001'..'\uD7FF'
    |   '\uF900'..'\uFDCF'
    |   '\uFDF0'..'\uFFFD'
    ;

//
// <scripts>
//
mode SCRIPT;

SCRIPT_BODY
    : .*? '</script>' -> popMode
    ;

SCRIPT_SHORT_BODY
    : .*? '</>' -> popMode
    ;

mode XFSCRIPT;

CFSCRIPT_BODY
    : .*? '</cfscript>' -> popMode
    ;

//
// <styles>
//
mode STYLE;

STYLE_BODY
    : .*? '</style>' -> popMode
    ;

STYLE_SHORT_BODY
    : .*? '</>' -> popMode
    ;

//
// attribute values
//
mode ATTVALUE;

// an attribute value may have spaces b/t the '=' and the value
ATTVALUE_VALUE
    : [ ]* ATTRIBUTE -> popMode
    ;

ATTRIBUTE
    : DOUBLE_QUOTE_STRING
    | SINGLE_QUOTE_STRING
    | ATTCHARS
    | HEXCHARS
    | DECCHARS
    ;

fragment ATTCHAR
    : '-'
    | '_'
    | '.'
    | '/'
    | '+'
    | ','
    | '?'
    | '='
    | ':'
    | ';'
    | '#'
    | [0-9a-zA-Z]
    ;

fragment ATTCHARS
    : ATTCHAR+ ' '?
    ;

fragment HEXCHARS
    : '#' [0-9a-fA-F]+
    ;

fragment DECCHARS
    : [0-9]+ '%'?
    ;

fragment DOUBLE_QUOTE_STRING
    : '"' (~[<"]+? | HASHHASH)* '"'
    ;

fragment SINGLE_QUOTE_STRING
    : '\'' (~[<']+? | HASHHASH) '\''
    ;

fragment HASHHASH
    : '#' ~[#]+ '#'
    ;


mode CFEXPRESSION_MODE;
TAG_CLOSE1
    : '>' -> type(TAG_CLOSE) , popMode,popMode
    ;

EXPRESSION      : (~[/>'"]+ | SQSTR | DQSTR)+ -> popMode;

fragment SQSTR  : '\'' ~[']*? '\'' ;
fragment DQSTR  : '"' ~["]*? '"' ;
