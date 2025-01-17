/**
 * [BoxLang]
 * <p>
 * Copyright [2023] [Ortus Solutions, Corp]
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.compiler.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;

import ortus.boxlang.parser.antlr.SQLLexer;

/**
 * I extend the generated ANTLR lexer to add some custom methods for getting unpopped modes
 * so we can perform better validation after parsing.
 */
public class SQLLexerCustom extends SQLLexer {

	/**
	 * The error listener
	 */
	ErrorListener						errorListener;

	/**
	 * The parser
	 */
	SQLParser							parser;

	/**
	 * A reference to the last token
	 */
	Token								lastToken			= null;

	/**
	 * A flag to check if we are in a cast
	 */
	int									inCast				= 0;

	/**
	 * These tokens are not function names. cast and convert have a special rule to match them, so they don't use the FUNCTION_NAME token type
	 */
	private static final Set<Integer>	notFunctionNames	= Set.of( NOT_, AND_, WHERE_, HAVING_, FROM_, IN_, ON_, CAST_, CONVERT_ );

	/**
	 * ASCII Character code for left parenthesis
	 */
	private int							LPAREN_Char_Code	= 40;

	/**
	 * Constructor
	 *
	 * @param input input stream
	 */
	public SQLLexerCustom( CharStream input, ErrorListener errorListener, SQLParser parser ) {
		super( input );
		this.errorListener	= errorListener;
		this.parser			= parser;
		reset();
	}

	/**
	 * Check if there are unpopped modes on the Lexer's mode stack
	 *
	 * @return true if there are unpopped modes
	 */
	public boolean hasUnpoppedModes() {
		return !_modeStack.isEmpty();
	}

	/**
	 * Get the unpopped modes on the Lexer's mode stack
	 *
	 * @return list of unpopped modes
	 */
	public List<Integer> getUnpoppedModesInts() {
		List<Integer> results = new ArrayList<Integer>();
		for ( int mode : _modeStack.toArray() ) {
			results.add( 0, mode );
		}
		results.add( 0, _mode );
		return results;
	}

	/**
	 * Get the unpopped modes on the Lexer's mode stack
	 *
	 * @return list of unpopped modes
	 */
	public List<String> getUnpoppedModes() {
		return getUnpoppedModesInts().stream().map( mode -> modeNames[ mode ] ).toList();
	}

	/**
	 * Check if the last mode was a specific mode
	 *
	 * @param mode mode to check
	 *
	 * @return true if the last mode was the specified mode
	 */
	public boolean lastModeWas( int mode ) {
		if ( !hasUnpoppedModes() ) {
			return false;
		}
		return getUnpoppedModesInts().get( 0 ) == mode;
	}

	/**
	 * Get the last token of a specific type
	 *
	 * @param type type of token to find
	 *
	 * @return the last token of the specified type
	 */
	public Token findPreviousToken( int type ) {
		reset();
		var tokens = getAllTokens();
		for ( int i = tokens.size() - 1; i >= 0; i-- ) {
			Token t = tokens.get( i );
			if ( t.getType() == type ) {
				return t;
			}
		}
		return null;
	}

	/**
	 * Back up to the closest unclosed brace
	 * Return null if none found
	 * *
	 *
	 * @return the unmatched opening brace
	 */
	public Token findUnclosedToken( int start, int end ) {
		int count = 0;
		reset();
		var tokens = getAllTokens();
		for ( int i = tokens.size() - 1; i >= 0; i-- ) {
			Token t = tokens.get( i );
			if ( t.getType() == start ) {
				count--;
			} else if ( t.getType() == end ) {
				count++;
			}
			if ( count < 0 ) {
				return t;
			}
		}
		return null;
	}

	/**
	 * Check if a specific mode is on the stack
	 *
	 * @param mode mode to check
	 *
	 * @return true if the mode is on the stack
	 */
	public boolean hasMode( int mode ) {
		if ( !hasUnpoppedModes() ) {
			return false;
		}
		return getUnpoppedModesInts().contains( mode );
	}

	/**
	 * Get the last token of a specific type and the next x siblings
	 * Returns empty list if not found
	 * 
	 * @param type  type of token to find
	 * @param count number of siblings to find
	 * 
	 * @return the list of tokens starting from the specified type
	 */
	public List<Token> findPreviousTokenAndXSiblings( int type, int count ) {
		reset();
		List<Token>	results	= new ArrayList<Token>();
		var			tokens	= getAllTokens();
		for ( int i = tokens.size() - 1; i >= 0; i-- ) {
			Token t = tokens.get( i );
			if ( t.getType() == type ) {
				results.add( t );
				for ( int j = 1; j <= count; j++ ) {
					results.add( tokens.get( i + j ) );
				}
				return results;
			}
		}
		return results;
	}

	public Token nextToken() {
		Token nextToken = super.nextToken();

		if ( lastToken == null ) {
			return setLastToken( nextToken );
		}

		if ( nextToken.getType() == OPEN_PAR && lastToken.getType() == CAST_ || inCast > 0 ) {
			inCast++;
		}

		if ( nextToken.getType() == CLOSE_PAR && inCast > 0 ) {
			inCast--;
		}

		// Any token after AS is an identifier
		if ( lastToken.getType() == SQLLexer.AS_ && nextToken.getType() != SQLLexer.SPACES && inCast == 0 ) {
			( ( CommonToken ) nextToken ).setType( IDENTIFIER );
			return setLastToken( nextToken );
		}

		// detect function calls and set the token type to FUNCTION_NAME
		if ( getInputStream().LA( 1 ) == LPAREN_Char_Code && !notFunctionNames.contains( nextToken.getType() ) ) {
			( ( CommonToken ) nextToken ).setType( SQLLexer.FUNCTION_NAME );
			return setLastToken( nextToken );
		}

		return setLastToken( nextToken );
	}

	private Token setLastToken( Token token ) {
		if ( token.getChannel() != HIDDEN ) {
			lastToken = token;
		}
		return token;
	}

}