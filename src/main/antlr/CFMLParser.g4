parser grammar CFMLParser;

options {
	tokenVocab = CFMLLexer;
}

template: component | interface | statements EOF?;

textContent: (nonInterpolatedText | interpolatedExpression)+;

genericOpenTag: TAG_OPEN PREFIX tagName attribute* TAG_CLOSE;
genericOpenCloseTag:
	TAG_OPEN PREFIX tagName attribute* TAG_SLASH_CLOSE;
genericCloseTag: TAG_OPEN SLASH_PREFIX tagName TAG_CLOSE;

interpolatedExpression: ICHAR expression ICHAR;
nonInterpolatedText: (TAG_OPEN? CONTENT_TEXT)+;
expression: (EXPRESSION_PART | quotedString)+;

attribute:
	attributeName TAG_EQUALS attributeValue
	| attributeName;

attributeName: TAG_NAME;

attributeValue: identifier | quotedString;

identifier: IDENTIFIER;

quotedString:
	OPEN_QUOTE (quotedStringPart | interpolatedExpression)* CLOSE_QUOTE;

quotedStringPart: STRING_LITERAL | HASHHASH;

tagName: TAG_NAME;
statements: (statement | textContent)*;

statement:
	function
	| genericOpenCloseTag
	| genericOpenTag
	| genericCloseTag
	| set
	| script
	| argument
	| return
	| if
	| param
	| try
	| catchBlock
	| output;

component:
	TAG_OPEN PREFIX COMPONENT attribute* (
		TAG_SLASH_CLOSE
		| TAG_CLOSE
	) statements TAG_OPEN SLASH_PREFIX COMPONENT TAG_CLOSE;
interface:
	TAG_OPEN PREFIX INTERFACE attribute* (
		TAG_SLASH_CLOSE
		| TAG_CLOSE
	) statements TAG_OPEN SLASH_PREFIX INTERFACE TAG_CLOSE;

function:
	TAG_OPEN PREFIX FUNCTION attribute* (
		TAG_SLASH_CLOSE
		| TAG_CLOSE
	) statements TAG_OPEN SLASH_PREFIX FUNCTION TAG_CLOSE;

set:
	TAG_OPEN PREFIX SET expression (TAG_SLASH_CLOSE | TAG_CLOSE);

script: SCRIPT_OPEN SCRIPT_BODY;

code: CONTENT_TEXT;

argument:
	TAG_OPEN PREFIX ARGUMENT attribute* (
		TAG_SLASH_CLOSE
		| TAG_CLOSE
	);

return:
	TAG_OPEN PREFIX RETURN expression? (
		TAG_SLASH_CLOSE
		| TAG_CLOSE
	);

if:
	TAG_OPEN PREFIX IF expression (TAG_SLASH_CLOSE | TAG_CLOSE) statements (
		TAG_OPEN PREFIX ELSEIF expression (
			TAG_SLASH_CLOSE
			| TAG_CLOSE
		) statements
	)* (
		TAG_OPEN PREFIX ELSE (TAG_SLASH_CLOSE | TAG_CLOSE) statements
	)* TAG_OPEN SLASH_PREFIX IF TAG_CLOSE;

param:
	TAG_OPEN PREFIX PARAM attribute* (
		TAG_SLASH_CLOSE
		| TAG_CLOSE
	);

try:
	TAG_OPEN PREFIX TRY attribute* (TAG_SLASH_CLOSE | TAG_CLOSE) statements TAG_OPEN TAG_SLASH
		PREFIX TRY TAG_CLOSE;

catchBlock:
	TAG_OPEN PREFIX CATCH attribute* (
		TAG_SLASH_CLOSE
		| TAG_CLOSE
	) statements TAG_OPEN SLASH_PREFIX CATCH TAG_CLOSE;

output:
	TAG_OPEN PREFIX OUTPUT attribute* TAG_SLASH_CLOSE
	| TAG_OPEN PREFIX OUTPUT attribute* TAG_CLOSE statements TAG_OPEN SLASH_PREFIX OUTPUT;