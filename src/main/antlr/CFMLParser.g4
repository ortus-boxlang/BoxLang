parser grammar CFMLParser;

options {
	tokenVocab = CFMLLexer;
}

template: component | interface | statements EOF?;

textContent: TAG_OPEN? CONTENT_TEXT | INTERPOLATION;

genericOpenTag: TAG_OPEN PREFIX tagName attribute* TAG_CLOSE;
genericOpenCloseTag:
	TAG_OPEN PREFIX tagName attribute* TAG_SLASH_CLOSE;
genericCloseTag: TAG_OPEN SLASH_PREFIX tagName TAG_CLOSE;

statements: (statement | textContent)*;

statement:
	function
	| genericOpenCloseTag
	| genericOpenTag
	| genericCloseTag
	| set
	| dump
	| script
	| argument
	| return
	| if
	| query
	| throw
	| loop
	| param
	| try
	| catchBlock
	| abort
	| lock
	| include
	| invoke
	| invoke1
	| invokeargument
	| file
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

dump:
	TAG_OPEN PREFIX DUMP attribute* (TAG_SLASH_CLOSE | TAG_CLOSE);
script: SCRIPT_OPEN SCRIPT_BODY;
// : TAG_OPEN SCRIPT attribute* (TAG_SLASH_CLOSE | TAG_CLOSE) code TAG_OPEN TAG_SLASH SCRIPT
// TAG_CLOSE ;

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

query:
	TAG_OPEN PREFIX QUERY attribute* (
		TAG_SLASH_CLOSE
		| TAG_CLOSE
	) sqlcode TAG_OPEN SLASH_PREFIX QUERY TAG_CLOSE;
sqlcode: CONTENT_TEXT;

throw:
	TAG_OPEN PREFIX THROW attribute* (
		TAG_SLASH_CLOSE
		| TAG_CLOSE
	);

loop:
	TAG_OPEN PREFIX LOOP attribute* (TAG_SLASH_CLOSE | TAG_CLOSE) statements TAG_OPEN TAG_SLASH
		PREFIX LOOP TAG_CLOSE;

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

abort:
	TAG_OPEN PREFIX ABORT attribute* (
		TAG_SLASH_CLOSE
		| TAG_CLOSE
	);

lock:
	TAG_OPEN PREFIX LOCK attribute* (TAG_SLASH_CLOSE | TAG_CLOSE) statements TAG_OPEN TAG_SLASH
		PREFIX LOCK TAG_CLOSE;

include:
	TAG_OPEN PREFIX INCLUDE attribute* (
		TAG_SLASH_CLOSE
		| TAG_CLOSE
	);

invoke:
	TAG_OPEN PREFIX INVOKE attribute* (
		TAG_SLASH_CLOSE
		| TAG_CLOSE
	);
invoke1:
	TAG_OPEN PREFIX INVOKE attribute* (
		TAG_SLASH_CLOSE
		| TAG_CLOSE
	) statements TAG_OPEN SLASH_PREFIX INVOKE TAG_CLOSE;
invokeargument:
	TAG_OPEN PREFIX INVOKEARGUMENT attribute* (
		TAG_SLASH_CLOSE
		| TAG_CLOSE
	);

file: TAG_OPEN PREFIX FILE attribute* TAG_CLOSE;

output:
	TAG_OPEN PREFIX OUTPUT attribute* TAG_SLASH_CLOSE
	| TAG_OPEN PREFIX OUTPUT attribute* TAG_CLOSE statements TAG_OPEN SLASH_PREFIX OUTPUT TAG_CLOSE;

expression: EXPRESSION;

attribute:
	attributeName TAG_EQUALS attributeValue
	| attributeName;

attributeName: TAG_NAME;

attributeValue: identifier | quotedString;

identifier: IDENTIFIER;
quotedString: DOUBLE_QUOTE_STRING | SINGLE_QUOTE_STRING;

tagName: TAG_NAME;