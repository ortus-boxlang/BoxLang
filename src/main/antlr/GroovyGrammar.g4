/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
parser grammar GroovyGrammar;

// Phase 1 of the Groovy parser/transpiler effort. See GroovyLexer.g4 header for scope notes.

options {
    tokenVocab = GroovyLexer;
    superClass = GroovyParserControl;
}

@header {
	import ortus.boxlang.compiler.parser.GroovyParserControl;
}

@members {
	public static DFA[] getParseCache() {
		return GroovyGrammar._decisionToDFA;
	}

	public static ATN getStaticATN() {
		return _ATN;
	}
}

// A run of one or more newline/semicolon separators. Newlines are only emitted as real
// tokens outside of ( ) and [ ] - see GroovyLexer's paren/bracket depth tracking.
sep: ( NL | SEMI )+
    ;

compilationUnit: sep? packageDeclaration? importStatement* topLevelDeclarations? EOF
    ;

topLevelDeclarations: topLevelDeclaration ( sep topLevelDeclaration )* sep?
    ;

topLevelDeclaration: classDeclaration
    | enumDeclaration
    | methodDeclaration
    | statement
    ;

// A bounded enum implementation - see GroovyVisitor#visitEnumDeclaration for exactly what this
// desugars to and what real Groovy enum semantics (ordinal(), values(), true type identity) are
// NOT modeled. Top-level only; nesting an enum inside a class body isn't supported.
enumDeclaration: ENUM IDENTIFIER LBRACE sep? IDENTIFIER sep? ( COMMA sep? IDENTIFIER sep? )* COMMA? sep? RBRACE
    ;

packageDeclaration: PACKAGE qualifiedName sep?
    ;

importStatement: IMPORT STATIC? qualifiedName ( DOT STAR )? ( AS IDENTIFIER )? sep?
    ;

qualifiedName: IDENTIFIER ( DOT IDENTIFIER )*
    ;

// -----------------------------------------------------------------------------------------
// Classes / interfaces

classDeclaration:
    classModifier* ( CLASS | INTERFACE | TRAIT ) IDENTIFIER
    ( EXTENDS typeName )? ( IMPLEMENTS typeList )?
    LBRACE sep? classBody? RBRACE
;

classModifier: PUBLIC
    | PRIVATE
    | PROTECTED
    | STATIC
    | FINAL
    | ABSTRACT
    ;

classBody: classMember ( sep classMember )* sep?
    ;

classMember: constructorDeclaration
    | methodDeclaration
    | fieldDeclaration
    ;

fieldDeclaration: classModifier* ( typeName | DEF ) IDENTIFIER ( ASSIGN expression )?
    ;

// block is optional to allow abstract methods and interface method signatures, which have
// no body (e.g. "def area()" inside an interface, or "abstract def area()" in a class).
methodDeclaration:
    classModifier* ( typeName | DEF | VOID )? IDENTIFIER LPAREN parameterList? RPAREN
    ( THROWS typeList )? block?
;

constructorDeclaration: classModifier* IDENTIFIER LPAREN parameterList? RPAREN block
    ;

parameterList: parameter ( COMMA parameter )*
    ;

// ELLIPSIS marks a variadic parameter, e.g. "int... nums" - only meaningful (and only checked)
// on the LAST parameter of a parameterList; see GroovyVisitor#buildVarargsPreamble for exactly
// what this desugars to. Grammar-wise it's allowed on any parameter, same as real Groovy leaves
// putting it somewhere else as a caller-beware situation rather than a hard parse error.
parameter: ( typeName | DEF )? ELLIPSIS? IDENTIFIER ( ASSIGN expression )?
    ;

typeList: typeName ( COMMA typeName )*
    ;

typeName: qualifiedName ( LT typeList GT )? ( LBRACKET RBRACKET )*
    ;

// -----------------------------------------------------------------------------------------
// Statements

block: LBRACE sep? blockStatements? RBRACE
    ;

blockStatements: statement ( sep statement )* sep?
    ;

statement: block                                                                  # blockStatement
    | IDENTIFIER COLON statement                                                  # labeledStatement
    | DEF LPAREN IDENTIFIER ( COMMA IDENTIFIER )+ RPAREN ASSIGN expression        # tupleDeclStatement
    | ( typeName | DEF ) IDENTIFIER ( ASSIGN expression )? ( COMMA IDENTIFIER ( ASSIGN expression )? )* # varDeclStatement
    | IF LPAREN expression RPAREN statement ( ELSE statement )?                   # ifStatement
    | WHILE LPAREN expression RPAREN statement                                    # whileStatement
    | DO block WHILE LPAREN expression RPAREN                                     # doWhileStatement
    | FOR LPAREN forControl RPAREN statement                                      # forStatement
    | TRY block catchClause* finallyClause?                                       # tryStatement
    | THROW expression                                                            # throwStatement
    | RETURN expression?                                                          # returnStatement
    | BREAK IDENTIFIER?                                                           # breakStatement
    | CONTINUE IDENTIFIER?                                                        # continueStatement
    | SWITCH LPAREN expression RPAREN LBRACE sep? switchCase* RBRACE              # switchStatement
    | ASSERT expression ( COLON expression )?                                     # assertStatement
    | expression                                                                  # exprStatement
    ;

// Java/CF-style switch: cases fall through unless an explicit "break" statement ends the
// case body (matching real Groovy's own switch semantics, and CFVisitor's BoxSwitchCase
// model - not the arrow-style "case X -> expr" form some other languages/BoxLang's own
// switch grammar also support).
switchCase: CASE expression COLON sep? blockStatements?                          # caseClause
    | DEFAULT COLON sep? blockStatements?                                        # defaultClause
    ;

forControl: ( typeName | DEF )? IDENTIFIER IN expression                          # forInControl
    | forInit? SEMI expression? SEMI forUpdate?                                   # classicForControl
    ;

forInit: ( typeName | DEF ) IDENTIFIER ASSIGN expression ( COMMA IDENTIFIER ASSIGN expression )*
    | expression ( COMMA expression )*
    ;

forUpdate: expression ( COMMA expression )*
    ;

catchClause: CATCH LPAREN ( typeName ( BITOR typeName )* )? IDENTIFIER RPAREN block
    ;

finallyClause: FINALLY block
    ;

// -----------------------------------------------------------------------------------------
// Expressions (precedence expressed via alt order, ANTLR4 direct-left-recursion style)

expression: primary                                                               # primaryExpr
    | expression LBRACKET expression RBRACKET                                     # indexExpr
    | expression LPAREN argumentList? RPAREN closure                              # callWithTrailingClosureExpr
    | expression LPAREN argumentList? RPAREN                                      # callExpr
    | expression ( DOT | SAFE_DOT | SPREAD_DOT ) IDENTIFIER closure                # trailingClosureCallExpr
    | expression ( DOT | SAFE_DOT | SPREAD_DOT | METHOD_POINTER ) IDENTIFIER       # memberExpr
    | expression ( INC | DEC )                                                    # postfixExpr
    | ( INC | DEC | PLUS | MINUS | BANG | TILDE ) expression                      # unaryExpr
    | <assoc=right> expression POWER expression                                   # powerExpr
    | expression ( STAR | SLASH | PERCENT ) expression                            # multiplicativeExpr
    | expression ( PLUS | MINUS ) expression                                      # additiveExpr
    | expression ( LSHIFT | RSHIFT ) expression                                   # shiftExpr
    | expression ( RANGE_INCL | RANGE_EXCL ) expression                           # rangeExpr
    | expression INSTANCEOF typeName                                              # instanceofExpr
    | expression AS typeName                                                      # asExpr
    | expression ( LT | GT | LE | GE ) expression                                 # relationalExpr
    | expression IN expression                                                    # inExpr
    | expression ( EQUAL | NOTEQUAL | IDENTICAL | NOT_IDENTICAL | SPACESHIP ) expression # equalityExpr
    | expression ( REGEX_FIND | REGEX_MATCH ) expression                          # regexExpr
    | expression BITAND expression                                                # bitAndExpr
    | expression BITXOR expression                                                # bitXorExpr
    | expression BITOR expression                                                 # bitOrExpr
    | expression AND expression                                                   # logicalAndExpr
    | expression OR expression                                                    # logicalOrExpr
    | <assoc=right> expression ELVIS expression                                   # elvisExpr
    | <assoc=right> expression QUESTION expression COLON expression               # ternaryExpr
    | <assoc=right> expression assignOp expression                                # assignExpr
    ;

assignOp: ASSIGN
    | PLUS_ASSIGN
    | MINUS_ASSIGN
    | STAR_ASSIGN
    | SLASH_ASSIGN
    | PERCENT_ASSIGN
    | POWER_ASSIGN
    | AND_ASSIGN
    | OR_ASSIGN
    | XOR_ASSIGN
    | LSHIFT_ASSIGN
    | RSHIFT_ASSIGN
    ;

argumentList: argument ( COMMA argument )*
    ;

// STAR-prefixed spread argument, e.g. foo(*list) - expands a list's elements as individual
// positional arguments at that call site (mirrors CFGrammar's own "...expr" spread argument,
// just with Groovy's own "*" token instead of CF's ellipsis). "name: value" named arguments
// (Groovy's map-literal-without-brackets call convention, e.g. foo(name: "x")) are collected
// into a single trailing map argument by the visitor - see GroovyExpressionVisitor#buildArguments.
argument: STAR expression                                                        # spreadArgument
    | IDENTIFIER COLON expression                                                # namedArgument
    | expression                                                                 # positionalArgument
    ;

primary: IDENTIFIER                                                               # identifierExpr
    | INT_LITERAL                                                                 # intLiteralExpr
    | FLOAT_LITERAL                                                               # floatLiteralExpr
    | TRUE                                                                        # trueLiteralExpr
    | FALSE                                                                       # falseLiteralExpr
    | NULL_LIT                                                                    # nullLiteralExpr
    | THIS                                                                        # thisExpr
    | SUPER                                                                       # superExpr
    | stringOrGString                                                             # stringExpr
    | listOrMapLiteral                                                            # collectionExpr
    | closure                                                                     # closureLiteralExpr
    | LPAREN expression RPAREN                                                    # parenExpr
    | NEW typeName LPAREN argumentList? RPAREN                                    # newInstanceExpr
    ;

stringOrGString: gstring
    | tripleGstring
    | SQUOTE_STRING
    | SLASHY_STRING
    ;

gstring: OPEN_QUOTE gstringPart* CLOSE_QUOTE
    ;

// Multi-line triple-quoted GString - same interpolation, but the lexer accepts embedded
// newlines and lone/double '"' characters in its body (see GroovyLexer's tripleGstringMode).
tripleGstring: OPEN_TRIPLE_QUOTE gstringPart* CLOSE_TRIPLE_QUOTE
    ;

gstringPart: GSTRING_TEXT
    | GSTRING_DOLLAR_IDENTIFIER
    | GSTRING_DOLLAR_LBRACE expression RBRACE
    ;

listOrMapLiteral: LBRACKET RBRACKET                                               # emptyListLiteral
    | LBRACKET COLON RBRACKET                                                     # emptyMapLiteral
    | LBRACKET mapEntry ( COMMA mapEntry )* RBRACKET                              # mapLiteral
    | LBRACKET expression ( COMMA expression )* RBRACKET                         # listLiteral
    ;

mapEntry: mapKey COLON expression
    ;

mapKey: IDENTIFIER
    | stringOrGString
    | LPAREN expression RPAREN
    ;

// -----------------------------------------------------------------------------------------
// Closures

closure: LBRACE sep? ( closureParams ARROW sep? )? blockStatements? RBRACE
    ;

closureParams: parameter ( COMMA parameter )*
    ;
