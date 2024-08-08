parser grammar BoxTemplateGrammar;

options {
	tokenVocab = BoxTemplateLexer;
}

// Top-level template rule.
template: statements EOF?;

// <b>My Name is #qry.name#.</b> We can match as much non interpolated text but we need each
// interpolated expression to be its own rule to ensure they output in the right order.
textContent: (nonInterpolatedText | comment)+
	| ( comment* interpolatedExpression comment*);

// <!--- comment ---> or <!--- comment <!--- nested comment ---> comment --->
comment:
	COMMENT_START (COMMENT_TEXT | COMMENT_START)* COMMENT_END;

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
	attributeName COMPONENT_EQUALS attributeValue?
	// foo (value will default to empty string)
	| attributeName;

// any attributes once we've gotten past the component name
attributeName: ATTRIBUTE_NAME;

// foo or.... "foo" or... 'foo' or... "#foo#" or... #foo#
attributeValue:
	unquotedValue
	| quotedString
	| interpolatedExpression;

// foo
unquotedValue: UNQUOTED_VALUE_PART+;

// "text#expression#text" or ... 'text#expression#text'
quotedString:
	OPEN_QUOTE (quotedStringPart | interpolatedExpression)* CLOSE_QUOTE;

quotedStringPart: STRING_LITERAL | HASHHASH;

// Normal set of statements that can be anywhere.  Doesn't include imports.
statements: (statement | script | textContent)*;

statement:
	boxImport
	| function
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

function:
	// <bx:function name="foo" >
	COMPONENT_OPEN PREFIX FUNCTION attribute* COMPONENT_CLOSE
	// zero or more <bx:argument ... >
	(whitespace | comment)* (argument (whitespace | comment)*)*
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
	COMPONENT_OPEN PREFIX BREAK label = attributeName? (
		COMPONENT_CLOSE
		| COMPONENT_SLASH_CLOSE
	);

// <bx:continue> or... <bx:continue />
continue:
	COMPONENT_OPEN PREFIX CONTINUE label = attributeName? (
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