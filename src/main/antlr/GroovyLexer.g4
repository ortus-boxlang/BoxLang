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
lexer grammar GroovyLexer;

// Phase 1 of the Groovy parser/transpiler effort (see project plan). Covers a deliberately
// scoped core of Groovy syntax: classes/interfaces, methods, closures, GString interpolation
// (including nested braces), and newline-significant statement separation. Not yet covered:
// traits, AST transform annotations, static typing, multi-line/slashy strings, number literal
// separators (1_000), hex/octal/binary literals, and fully general command-style method calls
// (only the dotted trailing-closure form, e.g. `items.each { it }`, is supported).

options {
    caseInsensitive = false;
}

@members {
	// Per-mode-frame counters, mirroring CFLexer/BoxLexer's existing expressionCountStack
	// pattern (see CFLexer.g4). counts[0] = paren/bracket depth, used to suppress newline
	// significance inside ( ) and [ ]. counts[1] = brace depth *within this frame*, used to
	// find the ${ ... } GString interpolation's matching closing brace even when the
	// interpolated expression contains its own nested braces (e.g. a closure argument).
	protected java.util.ArrayDeque<int[]> counts = new java.util.ArrayDeque<int[]>();
	{
		counts.push( new int[] { 0, 0 } );
	}

	public void pushMode( int m ) {
		counts.push( new int[] { 0, 0 } );
		super.pushMode( m );
	}

	public int popMode() {
		counts.pop();
		return super.popMode();
	}

	private void incParenBracket() {
		counts.peek()[ 0 ]++;
	}

	private void decParenBracket() {
		if ( counts.peek()[ 0 ] > 0 ) {
			counts.peek()[ 0 ]--;
		}
	}

	private boolean inParenOrBracket() {
		return counts.peek()[ 0 ] > 0;
	}

	private void incBrace() {
		counts.peek()[ 1 ]++;
	}

	private void decBrace() {
		if ( counts.peek()[ 1 ] > 0 ) {
			counts.peek()[ 1 ]--;
		}
	}

	private boolean atZeroBraceDepth() {
		return counts.peek()[ 1 ] == 0;
	}

	private boolean insideGStringInterpolation() {
		return !_modeStack.isEmpty() && _modeStack.peek() == gstringMode;
	}
}

// -----------------------------------------------------------------------------------------
// Keywords
CLASS:      'class';
INTERFACE:  'interface';
TRAIT:      'trait';
DEF:        'def';
PUBLIC:     'public';
PRIVATE:    'private';
PROTECTED:  'protected';
STATIC:     'static';
FINAL:      'final';
ABSTRACT:   'abstract';
EXTENDS:    'extends';
IMPLEMENTS: 'implements';
THROWS:     'throws';
IMPORT:     'import';
PACKAGE:    'package';
RETURN:     'return';
IF:         'if';
ELSE:       'else';
WHILE:      'while';
FOR:        'for';
IN:         'in';
DO:         'do';
TRY:        'try';
CATCH:      'catch';
FINALLY:    'finally';
THROW:      'throw';
BREAK:      'break';
CONTINUE:   'continue';
NEW:        'new';
TRUE:       'true';
FALSE:      'false';
NULL_LIT:   'null';
THIS:       'this';
SUPER:      'super';
INSTANCEOF: 'instanceof';
AS:         'as';
VOID:       'void';

// -----------------------------------------------------------------------------------------
// Punctuation / operators
LBRACE: '{' { incBrace(); };
// Must be declared before the generic RBRACE alternative below: ANTLR prefers the first
// declared rule whose predicate (if any) succeeds when multiple rules match the same text.
GSTRING_CLOSE_RBRACE:
    '}' { insideGStringInterpolation() && atZeroBraceDepth() }? -> type( RBRACE ), popMode
;
RBRACE: '}' { decBrace(); };

LPAREN:   '(' { incParenBracket(); };
RPAREN:   ')' { decParenBracket(); };
LBRACKET: '[' { incParenBracket(); };
RBRACKET: ']' { decParenBracket(); };

SEMI:  ';';
COMMA: ',';
DOT:   '.';
ARROW: '->';

SPREAD_DOT:     '*.';
SAFE_DOT:       '?.';
METHOD_POINTER: '.&';

RANGE_INCL: '..';
RANGE_EXCL: '..<';

POWER:        '**';
POWER_ASSIGN: '**=';

PLUS:  '+';
MINUS: '-';
STAR:  '*';
SLASH: '/';
PERCENT: '%';

INC: '++';
DEC: '--';

ASSIGN:        '=';
PLUS_ASSIGN:   '+=';
MINUS_ASSIGN:  '-=';
STAR_ASSIGN:   '*=';
SLASH_ASSIGN:  '/=';
PERCENT_ASSIGN: '%=';
AND_ASSIGN:    '&=';
OR_ASSIGN:     '|=';
XOR_ASSIGN:    '^=';
LSHIFT_ASSIGN: '<<=';
RSHIFT_ASSIGN: '>>=';

EQUAL:        '==';
NOTEQUAL:     '!=';
IDENTICAL:    '===';
NOT_IDENTICAL: '!==';
SPACESHIP:    '<=>';
LE:           '<=';
GE:           '>=';
LT:           '<';
GT:           '>';

AND: '&&';
OR:  '||';
BANG: '!';

BITAND: '&';
BITOR:  '|';
BITXOR: '^';
TILDE:  '~';
LSHIFT: '<<';
RSHIFT: '>>';

QUESTION: '?';
ELVIS:    '?:';
COLON:    ':';
DOUBLE_COLON: '::';

AT: '@';

// -----------------------------------------------------------------------------------------
// Literals / identifiers
IDENTIFIER: [a-zA-Z_$][a-zA-Z0-9_$]*;

FLOAT_LITERAL: [0-9]+ DOT [0-9]+ ( [eE] [+-]? [0-9]+ )? | [0-9]+ [eE] [+-]? [0-9]+;
INT_LITERAL:   [0-9]+;

// Single-quoted strings are always plain (non-interpolated) per Groovy semantics.
SQUOTE_STRING: '\'' ( ~['\\] | '\\' . )* '\'';

// Double-quoted strings are GStrings: they may contain ${ expr } or $identifier
// interpolations. Pushes gstringMode so we lex the string body character-by-character.
OPEN_QUOTE: '"' -> pushMode( gstringMode );

NL:
    [\r\n]+ { inParenOrBracket() }? -> channel( HIDDEN )
;
NL_SIGNIFICANT:
    [\r\n]+ -> type( NL )
;

WS: [ \t]+ -> channel( HIDDEN );

LINE_COMMENT:  '//' ~[\r\n]* -> channel( HIDDEN );
BLOCK_COMMENT: '/*' .*? '*/' -> channel( HIDDEN );

// -----------------------------------------------------------------------------------------
mode gstringMode;

CLOSE_QUOTE: '"' -> popMode;

GSTRING_DOLLAR_LBRACE: '${' -> pushMode( DEFAULT_MODE );

// Shorthand interpolation: "Hello $name" - identifier immediately after '$', no braces.
GSTRING_DOLLAR_IDENTIFIER: '$' [a-zA-Z_][a-zA-Z0-9_]* ( '.' [a-zA-Z_][a-zA-Z0-9_]* )*;

GSTRING_TEXT: ( ~["$\\] | '\\' . )+;

// A '$' not followed by an identifier or '{' is just literal text (e.g. a lone '$' or '$$').
GSTRING_DOLLAR_LITERAL: '$' -> type( GSTRING_TEXT );
