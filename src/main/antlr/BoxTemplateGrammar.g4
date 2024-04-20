parser grammar BoxTemplateGrammar;

options {
	tokenVocab = BoxTemplateLexer;
}

// Top-level template rule.  Consists of imports and other statements.
template: topLevelStatements EOF?;

// Top-level class or interface rule.
classOrInterface: (component | interface) EOF?;

// <b>My Name is #qry.name#.</b>
textContent: (nonInterpolatedText | interpolatedExpression)+;

// ANYTHING
componentName: COMPONENT_NAME;

// <bx:ANYTHING ... >
genericOpenComponent:
	COMPONENT_OPEN PREFIX componentName attribute* COMPONENT_CLOSE;

// <bx:ANYTHING />
genericOpenCloseComponent:
	COMPONENT_OPEN PREFIX componentName attribute* COMPONENT_SLASH_CLOSE;

// </cfANYTHING>
genericCloseComponent:
	COMPONENT_OPEN SLASH_PREFIX componentName COMPONENT_CLOSE;

// #bar#
interpolatedExpression: ICHAR expression ICHAR;
// Any text to be directly output
nonInterpolatedText: (COMPONENT_OPEN | CONTENT_TEXT | whitespace)+;

whitespace: WS+;
// bar or 1+2. The lexer keeps strings together so it doesnt end the expression prematurely
expression: (EXPRESSION_PART | quotedString)+;

attribute:
	// foo="bar" foo=bar
	attributeName COMPONENT_EQUALS attributeValue
	// foo (value will default to empty string)
	| attributeName;

// called COMPONENT_NAME because the lexer doens't know the difference between a component name and a variable name at lexing time
attributeName:
	COMPONENT_NAME
	// These allow attributes inside a component to be any of these "reserved" words.
	| COMPONENT
	| INTERFACE
	| FUNCTION
	| ARGUMENT
	| SCRIPT
	| RETURN
	| IF
	| ELSE
	| ELSEIF
	| SET
	| TRY
	| CATCH
	| FINALLY
	| IMPORT
	| WHILE
	| BREAK
	| CONTINUE
	| INCLUDE
	| PROPERTY
	| RETHROW
	| THROW
	| SWITCH
	| CASE
	| DEFAULTCASE
	| PREFIX;

// foo or.... "foo" or... 'foo' or... "#foo#" or... #foo#
attributeValue:
	identifier
	| quotedString
	| interpolatedExpression;

// foo
identifier: IDENTIFIER;

// "text#expression#text" or ... 'text#expression#text'
quotedString:
	OPEN_QUOTE (quotedStringPart | interpolatedExpression)* CLOSE_QUOTE;

quotedStringPart: STRING_LITERAL | HASHHASH;

// These statements can be at the top level of a template file.  Includes imports.
topLevelStatements: (
		statement
		| script
		| textContent
		| boxImport
	)*;

// Normal set of statements that can be anywhere.  Doesn't include imports.
statements: (statement | script | textContent)*;

statement:
	function
	// <bx:ANYTHING />
	| genericOpenCloseComponent
	// <bx:ANYTHING ... >
	| genericOpenComponent
	// </cfANYTHING>
	| genericCloseComponent
	| set
	| return
	| if
	| try
	| output
	| while
	| break
	| continue
	| include
	| rethrow
	| throw
	| switch;

component:
	whitespace? (boxImport whitespace?)*
	// <bx:component ... >
	COMPONENT_OPEN PREFIX COMPONENT attribute* COMPONENT_CLOSE
	// <bx:property name="..."> (zero or more)
	(whitespace? property)*
	// code in pseudo-constructor
	statements
	// </cfcomponent>
	COMPONENT_OPEN SLASH_PREFIX COMPONENT COMPONENT_CLOSE;

// <bx:property name="..."> or... <bx:property name="..." />
property:
	COMPONENT_OPEN PREFIX PROPERTY attribute* (
		COMPONENT_CLOSE
		| COMPONENT_SLASH_CLOSE
	);

interface:
	whitespace? (boxImport whitespace?)*
	// <bx:interface ... >
	COMPONENT_OPEN PREFIX INTERFACE attribute* COMPONENT_CLOSE
	// Code in interface 
	statements
	// </cfinterface>
	COMPONENT_OPEN SLASH_PREFIX INTERFACE COMPONENT_CLOSE;

function:
	// <bx:function name="foo" >
	COMPONENT_OPEN PREFIX FUNCTION attribute* COMPONENT_CLOSE
	// zero or more <bx:argument ... >
	whitespace? (argument whitespace?)*
	// code inside function
	body = statements
	// </cffunction>
	COMPONENT_OPEN SLASH_PREFIX FUNCTION COMPONENT_CLOSE;

argument:
	// <bx:argument name="param">
	COMPONENT_OPEN PREFIX ARGUMENT attribute* (
		COMPONENT_SLASH_CLOSE
		| COMPONENT_CLOSE
	);

set:
	// <bx:set expression> <bx:set expression />
	COMPONENT_OPEN PREFIX SET expression (
		COMPONENT_SLASH_CLOSE
		| COMPONENT_CLOSE
	);

scriptBody: SCRIPT_BODY*;
// <bx:script> statements... </cfscript>
script: SCRIPT_OPEN scriptBody SCRIPT_END_BODY;

/*
 <bx:return>
 <bx:return />
 <bx:return expression>
 <bx:return expression />
 <bx:return 10/5 >
 <bx:return 20 / 7 />
 */
return:
	COMPONENT_OPEN PREFIX RETURN expression? (
		COMPONENT_SLASH_CLOSE
		| COMPONENT_CLOSE
	);

if:
	// <bx:if ... >`
	COMPONENT_OPEN PREFIX IF ifCondition = expression COMPONENT_CLOSE thenBody = statements
	// Any number of <bx:elseif ... >
	(
		COMPONENT_OPEN PREFIX ELSEIF elseIfCondition += expression elseIfComponentClose +=
			COMPONENT_CLOSE elseThenBody += statements
	)*
	// One optional <bx:else> 
	(
		COMPONENT_OPEN PREFIX ELSE (
			COMPONENT_CLOSE
			| COMPONENT_SLASH_CLOSE
		) elseBody = statements
	)?
	// Closing </cfif>
	COMPONENT_OPEN SLASH_PREFIX IF COMPONENT_CLOSE;

try:
	// <bx:try>
	COMPONENT_OPEN PREFIX TRY COMPONENT_CLOSE
	// code inside try
	statements
	// <bx:catch> (zero or more)
	(catchBlock statements)*
	// <bx:finally> (zero or one)
	finallyBlock? statements
	// </cftry>
	COMPONENT_OPEN SLASH_PREFIX TRY COMPONENT_CLOSE;

/*
 <bx:catch type="..."> ... </cfcatch>
 <bx:catch type="..." />
 */
catchBlock:
	(
		// <bx:catch type="...">
		COMPONENT_OPEN PREFIX CATCH attribute* COMPONENT_CLOSE
		// code in catch
		statements
		// </cfcatch>
		COMPONENT_OPEN SLASH_PREFIX CATCH COMPONENT_CLOSE
	)
	| COMPONENT_OPEN PREFIX CATCH attribute* COMPONENT_SLASH_CLOSE;

finallyBlock:
	// <bx:finally>
	COMPONENT_OPEN PREFIX FINALLY COMPONENT_CLOSE
	// code in finally 
	statements
	// </cffinally>
	COMPONENT_OPEN SLASH_PREFIX FINALLY COMPONENT_CLOSE;

output:
	// <bx:output />
	OUTPUT_START attribute* COMPONENT_SLASH_CLOSE
	|
	// <bx:output> ... 
	OUTPUT_START attribute* COMPONENT_CLOSE
	// code in output
	statements
	// </cfoutput>
	COMPONENT_OPEN SLASH_PREFIX OUTPUT_END;

/*
 <bx:import componentlib="..." prefix="...">
 <bx:import name="com.foo.Bar">
 <bx:import
 prefix="java"
 name="com.foo.*">
 <bx:import prefix="java" name="com.foo.Bar" alias="bradLib">
 */
boxImport:
	COMPONENT_OPEN PREFIX IMPORT attribute* (
		COMPONENT_CLOSE
		| COMPONENT_SLASH_CLOSE
	);

while:
	// <bx:while condition="" >
	COMPONENT_OPEN PREFIX WHILE attribute* COMPONENT_CLOSE
	// code inside while
	statements
	// </cfwhile>
	COMPONENT_OPEN SLASH_PREFIX WHILE COMPONENT_CLOSE;

// <bx:break> or... <bx:break />
break:
	COMPONENT_OPEN PREFIX BREAK (
		COMPONENT_CLOSE
		| COMPONENT_SLASH_CLOSE
	);

// <bx:continue> or... <bx:continue />
continue:
	COMPONENT_OPEN PREFIX CONTINUE (
		COMPONENT_CLOSE
		| COMPONENT_SLASH_CLOSE
	);

// <bx:include template="..."> or... <bx:include template="..." />
include:
	COMPONENT_OPEN PREFIX INCLUDE attribute* (
		COMPONENT_CLOSE
		| COMPONENT_SLASH_CLOSE
	);

// <bx:rethrow> or... <bx:rethrow />
rethrow:
	COMPONENT_OPEN PREFIX RETHROW (
		COMPONENT_CLOSE
		| COMPONENT_SLASH_CLOSE
	);

// <bx:throw message="..." detail="..."> or... <bx:throw />
throw:
	COMPONENT_OPEN PREFIX THROW attribute* (
		COMPONENT_CLOSE
		| COMPONENT_SLASH_CLOSE
	);

switch:
	// <bx:switch expression="...">
	COMPONENT_OPEN PREFIX SWITCH attribute* COMPONENT_CLOSE
	// <bx:case> or <bx:defaultcase> 
	switchBody
	// </cftry>
	COMPONENT_OPEN SLASH_PREFIX SWITCH COMPONENT_CLOSE;

switchBody: (statement | script | textContent | case)*;

case:
	(
		// <bx:case value="...">
		COMPONENT_OPEN PREFIX CASE attribute* COMPONENT_CLOSE
		// code in case
		statements
		// </cfcase>
		COMPONENT_OPEN SLASH_PREFIX CASE COMPONENT_CLOSE
	)
	| (
		// <bx:defaultcase>
		COMPONENT_OPEN PREFIX DEFAULTCASE COMPONENT_CLOSE
		// code in default case
		statements
		// </cfdefaultcase >
		COMPONENT_OPEN SLASH_PREFIX DEFAULTCASE COMPONENT_CLOSE
	);