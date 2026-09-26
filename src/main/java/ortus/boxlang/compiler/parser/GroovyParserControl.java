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
package ortus.boxlang.compiler.parser;

import static ortus.boxlang.parser.antlr.GroovyGrammar.BINARY_LITERAL;
import static ortus.boxlang.parser.antlr.GroovyGrammar.COLON;
import static ortus.boxlang.parser.antlr.GroovyGrammar.FALSE;
import static ortus.boxlang.parser.antlr.GroovyGrammar.FLOAT_LITERAL;
import static ortus.boxlang.parser.antlr.GroovyGrammar.HEX_LITERAL;
import static ortus.boxlang.parser.antlr.GroovyGrammar.IDENTIFIER;
import static ortus.boxlang.parser.antlr.GroovyGrammar.INT_LITERAL;
import static ortus.boxlang.parser.antlr.GroovyGrammar.LBRACE;
import static ortus.boxlang.parser.antlr.GroovyGrammar.LBRACKET;
import static ortus.boxlang.parser.antlr.GroovyGrammar.NEW;
import static ortus.boxlang.parser.antlr.GroovyGrammar.NULL_LIT;
import static ortus.boxlang.parser.antlr.GroovyGrammar.OPEN_QUOTE;
import static ortus.boxlang.parser.antlr.GroovyGrammar.OPEN_TRIPLE_QUOTE;
import static ortus.boxlang.parser.antlr.GroovyGrammar.SLASHY_STRING;
import static ortus.boxlang.parser.antlr.GroovyGrammar.SQUOTE_STRING;
import static ortus.boxlang.parser.antlr.GroovyGrammar.SUPER;
import static ortus.boxlang.parser.antlr.GroovyGrammar.THIS;
import static ortus.boxlang.parser.antlr.GroovyGrammar.TRUE;

import java.util.Set;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;

/**
 * Base class for the generated {@code GroovyGrammar} parser (see {@code GroovyGrammar.g4}'s
 * {@code superClass} option). Mirrors {@link CFParserControl}'s role for the CF grammar.
 */
public abstract class GroovyParserControl extends Parser {

	// The set of token types a command-style call's first argument is allowed to start with -
	// see isCommandStyleCallStart's own header for why this set is deliberately narrow. STAR is
	// deliberately NOT included here even though it marks a spread argument in the ordinary
	// "argument" rule: "IDENTIFIER * expr" is ordinary multiplication (e.g. a closure body's
	// "it * 2"), which is vastly more common than a paren-less command-style call whose sole
	// argument is a spread - confirmed the hard way, as a real regression caught by this branch's
	// own test suite (every closure using "it * 2"/similar was being misparsed as a command call
	// with a spread argument). A spread-only command-style call is simply unsupported.
	private static final Set<Integer>	commandArgStartTokens	= Set.of(
	    SQUOTE_STRING, OPEN_QUOTE, OPEN_TRIPLE_QUOTE, SLASHY_STRING,
	    INT_LITERAL, FLOAT_LITERAL, HEX_LITERAL, BINARY_LITERAL,
	    TRUE, FALSE, NULL_LIT, THIS, SUPER,
	    LBRACE, LBRACKET, NEW );

	// This grammar has no dedicated keyword token for most Java primitive type names (only VOID
	// is a real keyword - see GroovyLexer.g4) - "int", "long", etc. lex as plain IDENTIFIER and
	// reach typeName via qualifiedName exactly like any class name. Since they're also
	// lowercase, the naming-convention heuristic in isLikelyMethodName below would otherwise
	// wrongly treat "int x" as a command-style call - this is the finite, closed set that must be
	// excluded from that heuristic. Unlike a curated list of call NAMES (which would need
	// updating for every new user function), this is a fixed set defined by the Java/Groovy
	// language itself and never grows.
	private static final Set<String>	primitiveTypeNames		= Set.of(
	    "int", "long", "short", "byte", "char", "float", "double", "boolean" );

	public GroovyParserControl( TokenStream input ) {
		super( input );
	}

	/**
	 * Gates Groovy's paren-less "command-style" call statement (e.g. {@code println "hi"},
	 * {@code apply plugin: 'groovy'}) against the classic Groovy grammar ambiguity: a bare
	 * "IDENTIFIER IDENTIFIER" is genuinely indistinguishable, with no further context, from a
	 * Java-style typed local declaration ("Type varName" - already claimed by this grammar's own
	 * {@code varDeclStatement}), and a leading unary {@code +}/{@code -} on the argument would
	 * collide with reading the whole thing as a single additive/unary expression statement
	 * instead (e.g. "x + 1" could otherwise misparse as calling "x" with argument "+1").
	 * <p>
	 * Deliberately bounded rather than generally solved (real Groovy resolves this with full
	 * semantic predicates informed by symbol resolution neither this parser nor most hand-written
	 * single-pass Groovy grammars attempt): only recognized when the argument starts with
	 * something that can NEVER also be a second bare identifier or a unary-operator-prefixed
	 * expression - a literal (string/number/boolean/null), {@code this}/{@code super}, a
	 * closure/list/map literal, {@code new}, a spread argument, or a "name: value" named
	 * argument. A bare-identifier argument (e.g. {@code println x}) is additionally recognized,
	 * but only when the CALL name itself cannot plausibly be read as a type name: real Java/Groovy
	 * naming convention (enforced by every style guide and virtually universal in practice) is that
	 * type names are UpperCamelCase and method/variable names are lowerCamelCase, so a leading
	 * identifier starting with a lowercase letter (e.g. "println", "log", "doStuff") is never a
	 * declaration's type token in practice, while one starting with an uppercase letter (e.g.
	 * "String", "MyClass") always might be - with one closed exception this grammar must carve out
	 * explicitly: Java's own lowercase primitive type names ("int", "long", etc. - see
	 * {@link #primitiveTypeNames}), which lex as plain identifiers here and would otherwise collide.
	 * This is a convention-based heuristic, not true type/symbol resolution (which this single-pass
	 * parser doesn't have) - it can misfire only for code that deliberately violates the naming
	 * convention (e.g. a type literally named "myClass", or a method literally named "Foo"), which
	 * is accepted as an intentional, documented trade-off. Note also that {@link #commandArgStartTokens}
	 * checks above always take precedence for a non-bare-identifier argument, so this heuristic is
	 * only ever consulted for the narrower "IDENTIFIER IDENTIFIER" shape.
	 *
	 * @param input the token input stream
	 *
	 * @return true if this should be read as a command-style call statement
	 */
	protected boolean isCommandStyleCallStart( TokenStream input ) {
		if ( input.LT( 1 ).getType() != IDENTIFIER ) {
			return false;
		}
		int firstArgType = input.LT( 2 ).getType();
		if ( commandArgStartTokens.contains( firstArgType ) ) {
			return true;
		}
		if ( firstArgType != IDENTIFIER ) {
			return false;
		}
		// "name: value" named argument - the argument itself is a bare identifier, but only when
		// immediately followed by a colon, which a second declaration-target identifier never is.
		if ( input.LT( 3 ).getType() == COLON ) {
			return true;
		}
		// A plain bare-identifier argument (println x) - recognized whenever the call name itself
		// cannot plausibly be a type name (see this method's own header for the naming-convention
		// rationale).
		return isLikelyMethodName( input.LT( 1 ).getText() );
	}

	/**
	 * Naming-convention heuristic: true if the given identifier starts with a lowercase letter and
	 * isn't one of Java's own lowercase primitive type names, meaning it cannot plausibly be a
	 * Java/Groovy type name (UpperCamelCase by convention, primitives aside) and is therefore safe
	 * to read as a command-style call name rather than a declaration's type token.
	 *
	 * @param identifierText the identifier's raw text
	 *
	 * @return true if the identifier looks like a method/variable name rather than a type name
	 */
	private static boolean isLikelyMethodName( String identifierText ) {
		return !identifierText.isEmpty()
		    && Character.isLowerCase( identifierText.charAt( 0 ) )
		    && !primitiveTypeNames.contains( identifierText );
	}

}
