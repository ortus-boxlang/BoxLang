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
statements: (statement | script | textContent)*;

statement:
	function
	| genericOpenCloseTag
	| genericOpenTag
	| genericCloseTag
	| set
	| argument
	| return
	| if
	| try
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
	// <cffunction name="foo" >
	TAG_OPEN PREFIX FUNCTION attribute* TAG_CLOSE
	// zero or more <cfargument ... >
	argument*
	// code inside function
	statements
	// </cffunction>
	TAG_OPEN SLASH_PREFIX FUNCTION TAG_CLOSE;

argument:
	// <cfargument name="param">
	TAG_OPEN PREFIX ARGUMENT attribute* (
		TAG_SLASH_CLOSE
		| TAG_CLOSE
	);

set:
	TAG_OPEN PREFIX SET expression (TAG_SLASH_CLOSE | TAG_CLOSE);

scriptBody: SCRIPT_BODY*;
script: SCRIPT_OPEN scriptBody SCRIPT_END_BODY;

code: CONTENT_TEXT;

return:
	TAG_OPEN PREFIX RETURN expression? (
		TAG_SLASH_CLOSE
		| TAG_CLOSE
	);

if:
	// <cfif ... >`
	TAG_OPEN PREFIX IF ifCondition = expression TAG_CLOSE thenBody = statements
	// Any number of <cfelseif ... >
	(
		TAG_OPEN PREFIX ELSEIF elseIfCondition += expression elseIfTagClose += TAG_CLOSE
			elseThenBody += statements
	)*
	// One optional <cfelse> 
	(TAG_OPEN PREFIX ELSE TAG_CLOSE elseBody = statements)?
	// Closing </cfif>
	TAG_OPEN SLASH_PREFIX IF TAG_CLOSE;

try:
	// <cftry>
	TAG_OPEN PREFIX TRY TAG_CLOSE
	// code inside try
	statements
	// <cfcatch> (zero or more)
	catchBlock*
	// <cffinally> (zero or one)
	finallyBlock?
	// </cftry>
	TAG_OPEN SLASH_PREFIX TRY TAG_CLOSE;

catchBlock:
	// <cfcatch type="...">
	TAG_OPEN PREFIX CATCH attribute* TAG_CLOSE
	// code in catch
	statements
	// </cfcatch>
	TAG_OPEN SLASH_PREFIX CATCH TAG_CLOSE;

finallyBlock:
	// <cffinally>
	TAG_OPEN PREFIX FINALLY TAG_CLOSE
	// code in finally 
	statements
	// </cffinally>
	TAG_OPEN SLASH_PREFIX FINALLY TAG_CLOSE;

output:
	// <cfoutput />
	TAG_OPEN PREFIX OUTPUT attribute* TAG_SLASH_CLOSE
	|
	// <cfoutput> ... 
	TAG_OPEN PREFIX OUTPUT attribute* TAG_CLOSE
	// code in output
	statements
	// </cfoutput>
	TAG_OPEN SLASH_PREFIX OUTPUT;