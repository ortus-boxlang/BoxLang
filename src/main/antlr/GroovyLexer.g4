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
		return !_modeStack.isEmpty() && ( _modeStack.peek() == gstringMode || _modeStack.peek() == tripleGstringMode );
	}

	// Classic regex-literal-vs-division disambiguation (the same problem JavaScript's "/" has):
	// a '/' means division only when it immediately follows something that already produced a
	// value - an identifier, a literal, a closing ")"/"]"/quote, this/super, or postfix ++/--.
	// Every other position (start of file, after an operator, after "(", ",", "return", etc.) is
	// a slashy regex literal's opening delimiter. Tracking just the previous DEFAULT-channel
	// token type (updated in nextToken() below) is enough to decide this without backtracking.
	private int lastTokenType = -1;

	@Override
	public Token nextToken() {
		Token t = super.nextToken();
		if ( t.getChannel() == Token.DEFAULT_CHANNEL ) {
			lastTokenType = t.getType();
		}
		return t;
	}

	private boolean isDivisionContext() {
		switch ( lastTokenType ) {
			case IDENTIFIER:
			case INT_LITERAL:
			case FLOAT_LITERAL:
			case RPAREN:
			case RBRACKET:
			case CLOSE_QUOTE:
			case CLOSE_TRIPLE_QUOTE:
			case SQUOTE_STRING:
			case THIS:
			case SUPER:
			case TRUE:
			case FALSE:
			case NULL_LIT:
			case INC:
			case DEC:
				return true;
			default:
				return false;
		}
	}

	// Groovy's common "fluent chain" formatting idiom puts the "."/"?."/"*." on the FOLLOWING
	// line, e.g.:
	//   list.findAll { it > 1 }
	//       .collect { it * 2 }
	// Without this, the newline after "{ it > 1 }" would end the statement right there, breaking
	// on the leading ".". Peeks past any run of plain horizontal/vertical whitespace (spaces,
	// tabs, blank lines) directly on the raw character stream - independent of how that
	// whitespace would otherwise be tokenized - to see whether a continuation dot follows. A
	// comment between the chained calls (e.g. a "// ..." line before the ".collect") is NOT
	// skipped over and will still end the statement early - a narrow, documented gap rather than
	// the common case this exists for.
	private boolean isLeadingDotContinuation() {
		int i = 1;
		int c;
		while ( ( c = _input.LA( i ) ) == ' ' || c == '\t' || c == '\r' || c == '\n' ) {
			i++;
		}
		if ( c == '.' ) {
			// Also covers METHOD_POINTER (".&"), which starts with the same character.
			return true;
		}
		int next = _input.LA( i + 1 );
		return ( c == '?' || c == '*' ) && next == '.';
	}
}

// -----------------------------------------------------------------------------------------
// Keywords
CLASS:      'class';
INTERFACE:  'interface';
TRAIT:      'trait';
ENUM:       'enum';
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
SWITCH:     'switch';
CASE:       'case';
DEFAULT:    'default';
ASSERT:     'assert';
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

// Must come before RANGE_INCL: both start with '..', and ANTLR4's lexer already prefers the
// longest match regardless of declaration order, but declaring the longer literal first here
// keeps the "which one wins on a tie" reasoning simple to read at a glance.
ELLIPSIS:   '...';
RANGE_INCL: '..';
RANGE_EXCL: '..<';

POWER:        '**';
POWER_ASSIGN: '**=';

PLUS:  '+';
MINUS: '-';

// Spread-map entry inside a map literal, e.g. [*: m1, *: m2] - must be declared before STAR so
// ANTLR's longest-match prefers it over a bare STAR followed by a separate COLON token.
SPREAD_MAP: '*:';
STAR:  '*';

// Slashy string literal, e.g. /foo\d+/ - Groovy's alternate string syntax mainly used for
// regex patterns with =~/==~. Declared before SLASH so it wins whenever its guard passes (a true
// predicate makes ANTLR prefer this alternative for the same input; the predicate itself is what
// keeps ordinary division working - see isDivisionContext()). Non-interpolated (no ${}/$name
// support, unlike GStrings) and single-line only - both are bounded simplifications, not real
// Groovy's full slashy-string feature set (which also has a "$/.../$" multi-line dollar-slashy
// form this doesn't implement). "\/" is the only recognized escape (a literal "/" that doesn't
// end the token); any other backslash sequence (e.g. "\d") is passed through untouched so regex
// metacharacters survive into the resulting string value as-is.
SLASHY_STRING: { !isDivisionContext() }? '/' ( '\\' . | ~[/\r\n\\] )* '/';

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
// Regex match operators - '==~' (3 chars) and '=~' (2 chars) are unambiguous against '==' and
// '=' purely by length: ANTLR4's lexer always prefers the longest match across all rules.
REGEX_MATCH:  '==~';
REGEX_FIND:   '=~';
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

// Hex/binary literals - unambiguous by their "0x"/"0b" prefix, so they never compete with plain
// decimal INT_LITERAL parsing (which never starts with "0" followed by "x"/"b"). Octal literals
// (a bare leading zero, e.g. "010" meaning 8) are deliberately NOT supported - real Groovy's own
// octal syntax is a well-known footgun and rare in practice, and supporting it would require a new
// OCTAL_LITERAL rule to out-rank the existing INT_LITERAL rule on a same-length tie (e.g. "010"
// would match both equally), a fragile ordering dependency not worth taking on for such a rarely-
// used form. "_" digit separators (Groovy's 1_000_000) and the L/G/F/D/I type suffixes are
// supported on all integer-literal forms and on FLOAT_LITERAL - see GroovyExpressionVisitor for
// exactly how each is interpreted (the suffix is recognized and stripped so the literal parses,
// but does not force a distinct runtime type beyond what BoxLang's own length-based int/long/
// BigDecimal selection already produces - a documented, bounded simplification).
HEX_LITERAL:    '0' [xX] [0-9a-fA-F] ( '_'? [0-9a-fA-F] )* [lLgGiI]?;
BINARY_LITERAL: '0' [bB] [01] ( '_'? [01] )* [lLgGiI]?;

FLOAT_LITERAL: [0-9] ( '_'? [0-9] )* DOT [0-9] ( '_'? [0-9] )* ( [eE] [+-]? [0-9]+ )? [fFdDgG]?
    | [0-9] ( '_'? [0-9] )* [eE] [+-]? [0-9]+ [fFdDgG]?
    | [0-9] ( '_'? [0-9] )* [fFdDgG];
INT_LITERAL:   [0-9] ( '_'? [0-9] )* [lLiI]?;

// Single-quoted strings are always plain (non-interpolated) per Groovy semantics.
SQUOTE_STRING: '\'' ( ~['\\] | '\\' . )* '\'';

// Triple-quoted strings are multi-line GStrings: same ${ expr }/$identifier interpolation, but
// the body can span multiple physical lines and contain literal (non-triple) '"' characters.
// Declared before OPEN_QUOTE - ANTLR4's lexer always prefers the longest match across ALL rules
// regardless of declaration order, so a genuine `"""` is matched here even though OPEN_QUOTE
// could also start matching a single `"`; order only matters for equal-length ties.
OPEN_TRIPLE_QUOTE: '"""' -> pushMode( tripleGstringMode );

// Double-quoted strings are GStrings: they may contain ${ expr } or $identifier
// interpolations. Pushes gstringMode so we lex the string body character-by-character.
OPEN_QUOTE: '"' -> pushMode( gstringMode );

NL:
    [\r\n]+ { inParenOrBracket() || isLeadingDotContinuation() }? -> channel( HIDDEN )
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

// -----------------------------------------------------------------------------------------
// Triple-quoted string body - same interpolation forms as gstringMode (reusing its token types
// via -> type(...) so the parser grammar/visitor only need to deal with one set of GSTRING_*
// tokens), but newlines are ordinary content here (never suppressed/significant - we're inside
// a string, not between statements), and a '"' or '""' that isn't the closing '"""' is literal
// text rather than an error.
mode tripleGstringMode;

CLOSE_TRIPLE_QUOTE: '"""' -> popMode;

TRIPLE_GSTRING_DOLLAR_LBRACE: '${' -> type( GSTRING_DOLLAR_LBRACE ), pushMode( DEFAULT_MODE );

TRIPLE_GSTRING_DOLLAR_IDENTIFIER: '$' [a-zA-Z_][a-zA-Z0-9_]* ( '.' [a-zA-Z_][a-zA-Z0-9_]* )* -> type( GSTRING_DOLLAR_IDENTIFIER );

TRIPLE_GSTRING_TEXT: ( ~["$\\] | '\\' . )+ -> type( GSTRING_TEXT );

TRIPLE_GSTRING_DOLLAR_LITERAL: '$' -> type( GSTRING_TEXT );

// A lone '"' or a '""' run shorter than three quotes isn't the closing delimiter - just text.
// ANTLR's longest-match already prefers CLOSE_TRIPLE_QUOTE above whenever 3 are actually there.
TRIPLE_GSTRING_QUOTE: '"' -> type( GSTRING_TEXT );
