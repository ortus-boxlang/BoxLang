parser grammar CFGrammar;

// Please note that this is still a WIP, but is essentially complete

// $antlr-format alignTrailingComments true, columnLimit 150, minEmptyLines 1, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortBlocksOnASingleLine true, alignSemicolons hanging, alignColons hanging
// $antlr-format alignColons hanging, allowShortRulesOnASingleLine on, alignFirstTokens on

options {
    tokenVocab = CFLexer;
    superClass = CFParserControl;
}

@header {
	import ortus.boxlang.compiler.parser.CFParserControl;
 }

// foo
identifier: IDENTIFIER | reservedKeyword
    ;

componentName
    :
    // Ask the component service if the component exists and verify that this context is actually a component.
    { isComponent(_input) }? identifier
    ;

prefixedComponentName
    :
    // Ask the component service if the component exists and verify that this context is actually a component.
    { isComponent(_input) }? PREFIXEDIDENTIFIER
    ;

// These are reserved words in the lexer, but are allowed to be an indentifer (variable name, method name)
reservedKeyword
    : ABSTRACT
    | ANY
    | ARRAY
    | AS
    | BOOLEAN
    | BREAK
    | CASE
    | CASTAS
    | CATCH
    | COMPONENT
    | CONTAIN
    | CONTAINS
    | CONTINUE
    | DEFAULT
    | DO
    | DOES
    | ELSEIF
    | ELSE IF? // TODO: May have to special case this :(
    | FALSE
    | FINAL
    | FINALLY
    | FOR
    | FUNCTION
    | IF
    | IMPORT
    | IN
    | INCLUDE
    | INSTANCEOF
    | INTERFACE
    | JAVA
    | MESSAGE
    | NEW
    | NULL
    | NUMERIC
    | PACKAGE
    | PARAM
    | PRIVATE
    | PROPERTY
    | PUBLIC
    | QUERY
    | REMOTE
    | REQUEST
    | REQUIRED
    | RETHROW
    | RETURN
    | SERVER
    | SETTING
    | STATIC
    | STRING
    | STRUCT
    // | SWITCH --> Could possibly be a var name, but not a function/method name
    | THROW
    | TO
    | TRUE
    | TRY
    | TYPE
    | VAR
    | VARIABLES
    | WHEN
    | WHILE
    | XOR
    | PREFIXEDIDENTIFIER // cfSomething
    ;

reservedOperators
    : NOT
    | OR
    | EQ
    | EQUAL
    | NEQ
    | XOR
    | THAN
    | NEQ
    | MOD
    | IS
    | LE
    | LESS
    | LT
    | LTE
    | IMP
    | EQV
    | AND
    | GE
    | GREATER
    | GT
    | GTE
    ;

// ANY NEW LEXER RULES IN DEFAULT MODE FOR WORDS NEED ADDED HERE

// This is the top level rule for a class or an interface
// TODO: Should this not also end with EOF? Otherwise the parser will stop at the end of the class/interface even if junk follows
classOrInterface: SEMICOLON* (boxClass | interface)
    ;

// This is the top level rule for a script of statements.
script: SEMICOLON* functionOrStatement*
    ;

// Used for tests, to force the parser to look at all tokens and not just stop at the first expression
testExpression: expression EOF
    ;

// import java:foo.bar.Baz as myAlias;
importStatement: IMPORT importFQN SEMICOLON*
    ;

importFQN: stringLiteral | fqn (DOT STAR)?
    ;

// include "myFile.bxm";
include: INCLUDE expression
    ;

// class {}
boxClass: importStatement* ABSTRACT? FINAL? COMPONENT postAnnotation* LBRACE classBody RBRACE
    ;

classBody: classBodyStatement*
    ;

classBodyStatement: property | staticInitializer | functionOrStatement
    ;

staticInitializer: STATIC normalStatementBlock
    ;

// interface {}
interface: importStatement* INTERFACE postAnnotation* LBRACE ( function)* RBRACE
    ;

// UDF or abstractFunction
function: functionSignature postAnnotation* normalStatementBlock? SEMICOLON*
    ;

// public String myFunction( String foo, String bar )
functionSignature: modifier* returnType? FUNCTION identifier LPAREN functionParamList? RPAREN
    ;

modifier: accessModifier | DEFAULT | STATIC | ABSTRACT | FINAL
    ;

// String function foo() or MyClass function foo()
returnType: type | identifier
    ;

// private String function foo()
accessModifier: PUBLIC | PRIVATE | REMOTE | PACKAGE
    ;

// Declared arguments for a function
functionParamList: functionParam (COMMA functionParam)* COMMA?
    ;

// required String param1="default" inject="something"
functionParam: REQUIRED? type? identifier (EQUALSIGN expression)? postAnnotation*
    ;

// @MyAnnotation "value". This is BL specific, so it's disabled in the CF grammar, but defined here
// in the base grammar for better rule reuse.
preAnnotation: AT fqn annotation*
    ;

arrayLiteral: LBRACKET expressionList? RBRACKET
    ;

// foo=bar baz="bum"
postAnnotation: identifier ((EQUALSIGN | COLON) attributeSimple)?
    ;

// This allows [1, 2, 3], "foo", or foo Adobe allows more chars than an identifer, Lucee allows darn
// near anything, but ANTLR is incapable of matching any tokens until the next whitespace. The
// literalExpression is just a BoxLang flourish to allow for more flexible expressions.
attributeSimple: annotation | identifier | fqn
    ;

annotation: atoms | stringLiteral | structExpression | arrayLiteral
    ;

type
    : (NUMERIC | STRING | BOOLEAN | COMPONENT | INTERFACE | ARRAY | STRUCT | QUERY | fqn | ANY) (
        LBRACKET RBRACKET
    )?
    ;

// Allow any statement or a function.
// TODO: This may need to be changed if functions are allowed inside of functions
functionOrStatement: function | statement
    ;

// property name="foo" type="string" default="bar" inject="something";
// Because a property is not seen as a normal statement, we have to add SEMICOLON here :(
property: PROPERTY postAnnotation* SEMICOLON+
    ;

// function() {} or () => {} or () -> {}
anonymousFunction
    :
    // function( param, param ) {}
    FUNCTION LPAREN functionParamList? RPAREN (postAnnotation)* normalStatementBlock # closureFunc
    // ( param, param ) => {}, param => {} (param, param) -> {}, param -> {}
    | (LPAREN functionParamList? RPAREN | identifier) (postAnnotation)* op = (ARROW | ARROW_RIGHT) statementOrBlock # lambdaFunc
    ;

// { statement; statement; }
statementBlock: LBRACE statement+ RBRACE SEMICOLON*
    ;

// { statement; statement; }
emptyStatementBlock: LBRACE RBRACE SEMICOLON*
    ;

normalStatementBlock: LBRACE statement* RBRACE
    ;

statementOrBlock: emptyStatementBlock | statement
    ;

// Any top-level statement that can be in a block.
statement
    : SEMICOLON* (
        importStatement
        | if
        | switch
        | try
        | while
        | for
        | do
        // throw would parser as a component or a simple statement, but the `throw new
        // java:com.foo.Bar();` case needs checked PRIOR to the component case, which needs checked
        // prior to simple statements due to its ambiguity
        | throw
        // include is really a component or a simple statement, but the `include expression;` case
        // needs checked PRIOR to the compnent case, which needs checked prior to expression
        | include
        | statementBlock
        | component
        | simpleStatement
        | expressionStatement // Allows for statements like complicated.thing.foo.bar--
        | emptyStatementBlock
        | componentIsland
    ) SEMICOLON*
    ;

// op=(VAR | FINAL) etc
assignmentModifier: op = ( VAR | FINAL | STATIC)
    ;

// Simple statements have no body
simpleStatement: break | continue | rethrow | param | return | not
    ;

// NOT ( expression ) is a special case when a statement as everything else should
// be seen as a function call. A quirk of the language, but easy to cater for
not: NOT expression
    ;

// http url="google.com" {}?
component
    : prefixedComponentName LPAREN (componentAttribute COMMA?)* RPAREN (
        normalStatementBlock
        | SEMICOLON?
    )
    | componentName componentAttribute* (normalStatementBlock | SEMICOLON)
    ;

componentAttribute: identifier ((EQUALSIGN | COLON) expression)?
    ;

// Arguments are zero or more named args, or zero or more positional args, but not both (validated in the AST-building stage).
argumentList: argument (COMMA argument)* COMMA?
    ;

argument: (namedArgument | positionalArgument)
    ;

/*
 func( foo = bar, baz = qux )
 func( foo : bar, baz : qux )
 func( "foo" = bar, "baz" = qux )
 func(
 'foo' : bar, 'baz' : qux )
 */
namedArgument: (identifier | stringLiteral) (EQUALSIGN | COLON) expression
    ;

// func( foo, bar, baz )
positionalArgument: expression
    ;

// The generic component syntax won't capture all access expressions so we need this rule too param
/*
 param String foo.bar="baz";
 param foo.bar="baz";
 param String foo.bar;
 */
param: PARAM type? expressionStatement
    ; // Expression will capture x=y

// We support if blocks with or without else blocks, and if statements without else blocks. That's
// it - no other valid if constructs.
if: IF LPAREN expression RPAREN ifStmt = statementOrBlock (ELSE elseStmt = statementOrBlock)?
    ;

/*
 for( var i = 0; i < 10; i++ ) {}
 or...
 for( var i = 0; i < 10; i++ ) echo(i)
 or...
 for( var
 foo
 in bar ) {}
 or...
 for( var foo in bar ) echo(i)
 */
for
    : preFix? FOR LPAREN (
        VAR? expression IN expression
        | intializer = expression? SEMICOLON condition = expression? SEMICOLON increment = expression?
    ) RPAREN statementOrBlock
    ;

/*
 do {
 statement;
 } while( expression );
 */
do: preFix? DO statementOrBlock WHILE LPAREN expression RPAREN
    ;

/*
 while( expression ) {
 statement;
 }
 */
while: preFix? WHILE LPAREN expression RPAREN statementOrBlock
    ;

// break label;
break: BREAK identifier?
    ;

// continue label
continue: CONTINUE identifier?
    ;

/*
 return;
 return foo;
 */
return: RETURN expression?
    ;

// rethrow;
rethrow: RETHROW
    ;

// throw Exception;
throw: { isThrow(_input) }? THROW expression
    ;

/*
 switch( expression ) {
 case 1:
 statement;
 break;
 default: {
 statement;
 }
 }
 */
switch: SWITCH LPAREN expression RPAREN LBRACE case* RBRACE
    ;

/*
 case 1:
 statement;
 break;
 */
case: (CASE expression | DEFAULT) COLON statementOrBlock*
    ;

/*
 ```
 <bx:set components="here">
 ```
 */
componentIsland: COMPONENT_ISLAND_START template COMPONENT_ISLAND_END
    ;

/*
 try {

 } catch( e ) {
 } finally {
 }
 */
try: TRY normalStatementBlock catches* finallyBlock?
    ;

// catch( e ) {}
catches
    : CATCH LPAREN ct += expression? (PIPE ct += expression)* VAR? ex = expression RPAREN normalStatementBlock
    ;

// finally {}
finallyBlock: FINALLY normalStatementBlock
    ;

/*
 "foo"
 or...
 'foo'
 */
stringLiteral
    : OPEN_QUOTE (stringLiteralPart | ICHAR (expression | reservedOperators) ICHAR)* CLOSE_QUOTE
    ;

stringLiteralPart: STRING_LITERAL | HASHHASH
    ;

// { foo: "bar", baz = "bum" }
structExpression
    : LBRACE structMembers? RBRACE
    | LBRACKET structMembers RBRACKET
    | LBRACKET (COLON | EQUALSIGN) RBRACKET
    ;

structMembers: structMember (COMMA structMember)* COMMA?
    ;

/*
 foo : bar
 42 : bar
 "foo" : bar
 */
structMember: structKey (COLON | EQUALSIGN) expression
    ;

structKey
    : identifier
    | stringLiteral
    | reservedOperators
    | INTEGER_LITERAL
    | ILLEGAL_IDENTIFIER
    | fqn
    ;

new: NEW preFix? (fqn | stringLiteral) LPAREN argumentList? RPAREN
    ;

// foo.bar.Baz
fqn: (identifier DOT)* identifier
    ;

expressionStatement
    : anonymousFunction # exprStatAnonymousFunction // function() {} or () => {} or () -> {}
    | el2               # exprStatInvocable
    ;

// This is used to allow for headless access to a component, such as .foo.bar.baz, which is not allowed
// as standalone statement. Without this separation there is too much ambiguity in things like
// param foo.bar = "baz";
// which will allow .bar = baz to be a separate statement and think param foo is a component
expression
    : anonymousFunction                             # exprAnonymousFunction // function() {} or () => {} or () -> {}
    | el2                                           # invocable
    | DOT identifier (LPAREN argumentList? RPAREN)? # exprHeadless
    ;

// Universal expression rule. This is the top level rule for all expressions. It's left recursive, covers
// precedence, implements precedence climbing, and handles all other expressions. This is the only rule needed for
// all expressions.
//
// Precedence is implemented here by placing the highest precedence expressions at the top of the rule, and the very
// lowest at the bottom. This is a form of precedence climbing, which is what ANTLR ends up genmerating, but
// it saves us from teh tedious manual expansion required of LL grammars and looks more like the LALR grammars
// that yacc/bison process.
//
// Note the use of labels allows our visitor to know what it is visiting without complicated token checking etc
el2
    : ILLEGAL_IDENTIFIER                                                    # exprIllegalIdentifier // 50foo
    | LPAREN expression RPAREN                                              # exprPrecedence        // ( foo )
    | new                                                                   # exprNew               // new foo.bar.Baz()
    | el2 LPAREN argumentList? RPAREN                                       # exprFunctionCall      // foo(bar, baz)
    | el2 QM? DOT DOT? el2                                                  # exprDotAccess         // xc.y?.z recursive and Adobe's stupid foo..bar bug they allow
    | el2 QM? DOT? DOT_FLOAT_LITERAL                                        # exprDotFloat          // foo.50
    | el2 QM? DOT? DOT_NUMBER_PREFIXED_IDENTIFIER                           # exprDotFloatID        // foo.50bar
    | el2 LBRACKET expression RBRACKET                                      # exprArrayAccess       // foo[bar]
    | <assoc = right> op = (NOT | BANG | MINUS | PLUS) el2                  # exprUnary             //  !foo, -foo, +foo
    | <assoc = right> op = (PLUSPLUS | MINUSMINUS | BITWISE_COMPLEMENT) el2 # exprPrefix            // ++foo, --foo, ~foo
    | el2 op = (PLUSPLUS | MINUSMINUS)                                      # exprPostfix           // foo++, bar--
    | el2 COLONCOLON el2                                                    # exprStaticAccess      // foo::bar
    | el2 POWER el2                                                         # exprPower             // foo ^ bar
    | el2 op = (STAR | SLASH | PERCENT | MOD | BACKSLASH) el2               # exprMult              // foo * bar
    | el2 op = (PLUS | MINUS) el2                                           # exprAdd               // foo + bar
    | el2 XOR el2                                                           # exprXor               // foo XOR bar
    | el2 AMPERSAND el2                                                     # exprCat               // foo & bar - string concatenation
    | el2 binOps el2                                                        # exprBinary            // foo eqv bar
    | el2 relOps el2                                                        # exprRelational        // foo > bar
    | el2 (EQ | EQUAL | EQEQ | IS) el2                                      # exprEqual             // foo == bar
    | el2 ELVIS el2                                                         # exprElvis             // Elvis operator
    | el2 DOES NOT CONTAIN el2                                              # exprNotContains       // foo DOES NOT CONTAIN bar
    | el2 (AND | AMPAMP) el2                                                # exprAnd               // foo AND bar
    | el2 (OR | PIPEPIPE) el2                                               # exprOr                // foo OR bar

    // Ternary operations are right associative, which means that if they are nested,
    // the rightmost operation is evaluated first.
    | <assoc = right> el2 QM el2 COLON el2 # exprTernary // foo ? bar : baz
    | atoms                                # exprAtoms   // foo, 42, true, false, null, [1,2,3], {foo:bar}

    // el2 elements that have no operators so will be selected in order other than LL(*) solving
    | ICHAR el2 ICHAR # exprOutString    // #el2# not within a string literal
    | literals        # exprLiterals     // "bar", [1,2,3], {foo:bar}
    | arrayLiteral    # exprArrayLiteral // [1,2,3]
    // Evaluate assign here so that we can assign the result of an el2 to a variable
    | el2 op = (
        EQUALSIGN
        | PLUSEQUAL
        | MINUSEQUAL
        | STAREQUAL
        | SLASHEQUAL
        | MODEQUAL
        | CONCATEQUAL
    ) expression                                                       # exprAssign     // foo = bar
    | { isAssignmentModifier(_input) }? assignmentModifier+ expression # exprVarDecl    // var foo = bar or final foo = bar
    | identifier                                                       # exprIdentifier // foo
    ;

// Use this instead of redoing it as arrayValues, arguments etc.
expressionList: expression (COMMA expression)* COMMA?
    ;

atoms: a = (NULL | TRUE | FALSE | INTEGER_LITERAL | FLOAT_LITERAL | DOT_FLOAT_LITERAL)
    ;

// All literal expressions
literals: stringLiteral | structExpression
    ;

// Relational operators as their own rule so we can have the visitor generate theAST
relOps
    : LESS THAN OR (EQ | EQUAL) TO
    | GREATER THAN OR (EQ | EQUAL) TO
    | GREATER THAN
    | GT
    | GTSIGN
    | GTE
    | GE
    | GTESIGN
    | TEQ
    | TENQ
    | LTE
    | LE
    | LTESIGN
    | LT
    | LTSIGN
    | LESS THAN
    | NEQ
    | IS NOT
    | BANGEQUAL
    | LESSTHANGREATERTHAN
    ;

binOps: EQV | IMP | CONTAINS | NOT CONTAINS
    ;

preFix: identifier COLON
    ;

// ##################################
// ###      TEMPLATE RULES        ###
// ##################################

// Top-level template rule.  Consists of imports and other statements.
template: template_statements EOF?
    ;

// Top-level class or interface rule.
template_classOrInterface
    :
    // Leading text will be ignored
    template_textContent* (
        template_component
        | template_interface
        | ( template_whitespace? template_script template_whitespace?)
    ) EOF?
    ;

// <b>My Name is #qry.name#.</b> We can match as much non interpolated text but we need each
// interpolated expression to be its own rule to ensure they output in the right order.
template_textContent
    : (template_nonInterpolatedText | template_comment)+
    | ( template_comment* template_interpolatedExpression template_comment*)
    ;

// <!--- comment ---> or <!--- comment <!--- nested comment ---> comment --->
template_comment: COMMENT_START (COMMENT_TEXT | COMMENT_START)* COMMENT_END
    ;

// ANYTHING
template_componentName: COMPONENT_NAME
    ;

// <cfANYTHING ... >
template_genericOpenComponent
    : COMPONENT_OPEN PREFIX template_componentName template_attribute* COMPONENT_CLOSE
    ;

// <cfANYTHING />
template_genericOpenCloseComponent
    : COMPONENT_OPEN PREFIX template_componentName template_attribute* COMPONENT_SLASH_CLOSE
    ;

// </cfANYTHING>
template_genericCloseComponent
    : COMPONENT_OPEN SLASH_PREFIX template_componentName COMPONENT_CLOSE
    ;

template_interpolatedExpression: ICHAR expression ICHAR
    ;

// Any text to be directly output
template_nonInterpolatedText: ( COMPONENT_OPEN | CONTENT_TEXT | template_whitespace)+
    ;

template_whitespace: TEMPLATE_WS+
    ;

template_attribute
    :
    // foo="bar" foo=bar
    template_attributeName COMPONENT_EQUALS template_attributeValue?
    // foo (value will default to empty string)
    | template_attributeName
    ;

// any attributes once we've gotten past the component name
template_attributeName: ATTRIBUTE_NAME
    ;

// foo or.... "foo" or... 'foo' or... "#foo#" or... #foo#
template_attributeValue: template_unquotedValue | ICHAR el2 ICHAR | stringLiteral
    ;

// foo
template_unquotedValue: UNQUOTED_VALUE_PART+
    ;

// Normal set of statements that can be anywhere.  Doesn't include imports.
template_statements: ( template_statement | template_script | template_textContent)*
    ;

template_statement
    : template_boxImport
    | template_function
    // <cfANYTHING />
    | template_genericOpenCloseComponent
    // <cfANYTHING ... >
    | template_genericOpenComponent
    // </cfANYTHING>
    | template_genericCloseComponent
    | template_set
    | template_return
    | template_if
    | template_try
    | template_output
    | template_while
    | template_break
    | template_continue
    | template_include
    | template_rethrow
    | template_throw
    | template_switch
    ;

template_component
    : (template_whitespace | template_comment)* (
        template_boxImport ( template_whitespace | template_comment)*
    )*
    // <cfcomponent ... >
    COMPONENT_OPEN PREFIX TEMPLATE_COMPONENT template_attribute* COMPONENT_CLOSE
    // <cfproperty name="..."> (zero or more)
    ((template_whitespace | template_comment)* template_property)*
    // code in pseudo-constructor
    template_statements
    // </cfcomponent>
    COMPONENT_OPEN SLASH_PREFIX TEMPLATE_COMPONENT COMPONENT_CLOSE template_textContent*
    ;

// <cfproperty name="..."> or... <cfproperty name="..." />
template_property
    : COMPONENT_OPEN PREFIX TEMPLATE_PROPERTY template_attribute* (
        COMPONENT_CLOSE
        | COMPONENT_SLASH_CLOSE
    )
    ;

template_interface
    : template_whitespace? (template_boxImport template_whitespace?)*
    // <cfinterface ... >
    COMPONENT_OPEN PREFIX TEMPLATE_INTERFACE template_attribute* COMPONENT_CLOSE
    // Code in interface
    (template_whitespace | template_function | template_comment)*
    // </cfinterface>
    COMPONENT_OPEN SLASH_PREFIX TEMPLATE_INTERFACE COMPONENT_CLOSE template_textContent*
    ;

template_function
    :
    // <cffunction name="foo" >
    COMPONENT_OPEN PREFIX TEMPLATE_FUNCTION template_attribute* COMPONENT_CLOSE
    // zero or more <cfargument ... >
    ((template_nonInterpolatedText | template_comment)* template_argument)* template_whitespace?
    // code inside function
    body = template_statements
    // </cffunction>
    COMPONENT_OPEN SLASH_PREFIX TEMPLATE_FUNCTION COMPONENT_CLOSE
    ;

template_argument
    :
    // <cfargument name="param">
    COMPONENT_OPEN PREFIX TEMPLATE_ARGUMENT template_attribute* (
        COMPONENT_SLASH_CLOSE
        | COMPONENT_CLOSE
    )
    ;

template_set
    :
    // <cfset expression> <cfset expression />
    COMPONENT_OPEN PREFIX TEMPLATE_SET expression (COMPONENT_SLASH_CLOSE | COMPONENT_CLOSE)
    ;

// <cfscript> statements... </cfscript>
template_script: SCRIPT_OPEN (classOrInterface | script) SCRIPT_END_BODY
    ;

/*
 <cfreturn>
 <cfreturn />
 <cfreturn expression>
 <cfreturn expression />
 <cfreturn 10/5 >
 <cfreturn 20 / 7 />
 */
template_return
    : COMPONENT_OPEN PREFIX TEMPLATE_RETURN expression? (COMPONENT_SLASH_CLOSE | COMPONENT_CLOSE)
    ;

template_if
    :
    // <cfif ... >`
    COMPONENT_OPEN PREFIX TEMPLATE_IF ifCondition = expression COMPONENT_CLOSE thenBody = template_statements
    // Any number of <cfelseif ... >
    (
        COMPONENT_OPEN PREFIX TEMPLATE_ELSEIF elseIfCondition += expression elseIfComponentClose += COMPONENT_CLOSE elseThenBody +=
            template_statements
    )*
    // One optional <cfelse>
    (
        COMPONENT_OPEN PREFIX TEMPLATE_ELSE (COMPONENT_CLOSE | COMPONENT_SLASH_CLOSE) elseBody = template_statements
    )?
    // Closing </cfif>
    COMPONENT_OPEN SLASH_PREFIX TEMPLATE_IF COMPONENT_CLOSE
    ;

template_try
    :
    // <cftry>
    COMPONENT_OPEN PREFIX TEMPLATE_TRY COMPONENT_CLOSE
    // code inside try
    template_statements
    // <cfcatch> (zero or more)
    (template_catchBlock template_statements)*
    // <cffinally> (zero or one)
    template_finallyBlock? template_statements
    // </cftry>
    COMPONENT_OPEN SLASH_PREFIX TEMPLATE_TRY COMPONENT_CLOSE
    ;

/*
 <cfcatch type="..."> ... </cfcatch>
 <cfcatch type="..." />
 */
template_catchBlock
    : (
        // <cfcatch type="...">
        COMPONENT_OPEN PREFIX TEMPLATE_CATCH template_attribute* COMPONENT_CLOSE
        // code in catch
        template_statements
        // </cfcatch>
        COMPONENT_OPEN SLASH_PREFIX TEMPLATE_CATCH COMPONENT_CLOSE
    )
    | COMPONENT_OPEN PREFIX TEMPLATE_CATCH template_attribute* COMPONENT_SLASH_CLOSE
    ;

template_finallyBlock
    :
    // <cffinally>
    COMPONENT_OPEN PREFIX TEMPLATE_FINALLY COMPONENT_CLOSE
    // code in finally
    template_statements
    // </cffinally>
    COMPONENT_OPEN SLASH_PREFIX TEMPLATE_FINALLY COMPONENT_CLOSE
    ;

template_output
    :
    // <cfoutput> ...
    OUTPUT_START template_attribute* COMPONENT_CLOSE
    // code in output
    template_statements
    // </cfoutput>
    COMPONENT_OPEN SLASH_PREFIX OUTPUT_END
    |
    // <cfoutput />
    OUTPUT_START template_attribute* COMPONENT_SLASH_CLOSE
    ;

/*
 <cfimport componentlib="..." prefix="...">
 <cfimport name="com.foo.Bar">
 <cfimport prefix="java"
 name="com.foo.*">
 <cfimport prefix="java" name="com.foo.Bar" alias="bradLib">
 */
template_boxImport
    : COMPONENT_OPEN PREFIX TEMPLATE_IMPORT template_attribute* (
        COMPONENT_CLOSE
        | COMPONENT_SLASH_CLOSE
    )
    ;

template_while
    :
    // <cfwhile condition="" >
    COMPONENT_OPEN PREFIX TEMPLATE_WHILE template_attribute* COMPONENT_CLOSE
    // code inside while
    template_statements
    // </cfwhile>
    COMPONENT_OPEN SLASH_PREFIX TEMPLATE_WHILE COMPONENT_CLOSE
    ;

// <cfbreak> or... <cfbreak />
template_break
    : COMPONENT_OPEN PREFIX TEMPLATE_BREAK label = template_attributeName? (
        COMPONENT_CLOSE
        | COMPONENT_SLASH_CLOSE
    )
    ;

// <cfcontinue> or... <cfcontinue />
template_continue
    : COMPONENT_OPEN PREFIX TEMPLATE_CONTINUE label = template_attributeName? (
        COMPONENT_CLOSE
        | COMPONENT_SLASH_CLOSE
    )
    ;

// <cfinclude template="..."> or... <cfinclude template="..." />
template_include
    : COMPONENT_OPEN PREFIX TEMPLATE_INCLUDE template_attribute* (
        COMPONENT_CLOSE
        | COMPONENT_SLASH_CLOSE
    )
    ;

// <cfrethrow> or... <cfrethrow />
template_rethrow
    : COMPONENT_OPEN PREFIX TEMPLATE_RETHROW (COMPONENT_CLOSE | COMPONENT_SLASH_CLOSE)
    ;

// <cfthrow message="..." detail="..."> or... <cfthrow />
template_throw
    : COMPONENT_OPEN PREFIX TEMPLATE_THROW template_attribute* (
        COMPONENT_CLOSE
        | COMPONENT_SLASH_CLOSE
    )
    ;

template_switch
    :
    // <cfswitch expression="...">
    COMPONENT_OPEN PREFIX TEMPLATE_SWITCH template_attribute* COMPONENT_CLOSE
    // <cfcase> or <cfdefaultcase>
    template_switchBody
    // </cftry>
    COMPONENT_OPEN SLASH_PREFIX TEMPLATE_SWITCH COMPONENT_CLOSE
    ;

template_switchBody
    : (template_statement | template_script | template_textContent | template_case)*
    ;

template_case
    : (
        // <cfcase value="...">
        COMPONENT_OPEN PREFIX TEMPLATE_CASE template_attribute* COMPONENT_CLOSE
        // code in case
        template_statements
        // </cfcase>
        COMPONENT_OPEN SLASH_PREFIX TEMPLATE_CASE COMPONENT_CLOSE
    )
    | (
        // <cfdefaultcase>
        COMPONENT_OPEN PREFIX TEMPLATE_DEFAULTCASE COMPONENT_CLOSE
        // code in default case
        template_statements
        // </cfdefaultcase >
        COMPONENT_OPEN SLASH_PREFIX TEMPLATE_DEFAULTCASE COMPONENT_CLOSE
    )
    ;