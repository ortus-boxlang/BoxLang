parser grammar CFMLParser;

options {
	tokenVocab = CFMLLexer;
}

// Top-level template rule.  Consists of component or interface or statements.
template: component | interface | ( boxImport* statements) EOF?;

// <b>My Name is #qry.name#.</b>
textContent: (nonInterpolatedText | interpolatedExpression)+;

// ANYTHING
tagName: TAG_NAME;

// <cfANYTHING ... >
genericOpenTag: TAG_OPEN PREFIX tagName attribute* TAG_CLOSE;

// <cfANYTHING />
genericOpenCloseTag:
	TAG_OPEN PREFIX tagName attribute* TAG_SLASH_CLOSE;

// </cfANYTHING>
genericCloseTag: TAG_OPEN SLASH_PREFIX tagName TAG_CLOSE;

// #bar#
interpolatedExpression: ICHAR expression ICHAR;
// Any text to be directly output
nonInterpolatedText: (TAG_OPEN? CONTENT_TEXT)+;
// bar or 1+2. The lexer keeps strings together so it doesnt end the expression prematurely
expression: (EXPRESSION_PART | quotedString)+;

attribute:
	// foo="bar" foo=bar
	attributeName TAG_EQUALS attributeValue
	// foo (value will default to empty string)
	| attributeName;

// called TAG_NAME because the lexer doens't know the difference between a tag name and a variable name at lexing time
attributeName: TAG_NAME;

// foo or.... "foo" or... 'foo' or... "#foo#"
attributeValue: identifier | quotedString;

// foo
identifier: IDENTIFIER;

// "text#expression#text" or ... 'text#expression#text'
quotedString:
	OPEN_QUOTE (quotedStringPart | interpolatedExpression)* CLOSE_QUOTE;

quotedStringPart: STRING_LITERAL | HASHHASH;

statements: (statement | script | textContent)*;

statement:
	function
	// <cfANYTHING />
	| genericOpenCloseTag
	// <cfANYTHING ... >
	| genericOpenTag
	// </cfANYTHING>
	| genericCloseTag
	| set
	| argument
	| return
	| if
	| try
	| output
	| while;

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
	// <cfset expression> <cfset expression />
	TAG_OPEN PREFIX SET expression (TAG_SLASH_CLOSE | TAG_CLOSE);

scriptBody: SCRIPT_BODY*;
// <cfscript> statements... </cfscript>
script: SCRIPT_OPEN scriptBody SCRIPT_END_BODY;

return:
	// <cfreturn> or... <cfreturn expression> or... <cfreturn expression />
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

/*
 <cfimport taglib="..." prefix="...">
 <cfimport name="com.foo.Bar">
 <cfimport prefix="java"
 name="com.foo.*">
 <cfimport prefix="java" name="com.foo.Bar" alias="bradLib">
 */
boxImport:
	TAG_OPEN PREFIX IMPORT attribute* (
		TAG_CLOSE
		| TAG_SLASH_CLOSE
	);

while:
	// <cfwhile condition="" >
	TAG_OPEN PREFIX WHILE attribute* TAG_CLOSE
	// code inside while
	statements
	// </cfwhile>
	TAG_OPEN SLASH_PREFIX WHILE TAG_CLOSE;