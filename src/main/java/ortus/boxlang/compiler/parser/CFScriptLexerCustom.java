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

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.misc.Pair;

import ortus.boxlang.parser.antlr.CFScriptLexer;

/**
 * I extend the generated ANTLR lexer to add some custom methods for getting unpopped modes
 * so we can perform better validation after parsing.
 */
public class CFScriptLexerCustom extends CFScriptLexer {

	Boolean	inElseIf	= false;
	Token	lastElseIf	= null;

	/**
	 * Constructor
	 * 
	 * @param input input stream
	 */
	public CFScriptLexerCustom( CharStream input ) {
		super( input );
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
	public List<String> getUnpoppedModes() {
		List<String> results = new ArrayList<String>();
		for ( int mode : _modeStack.toArray() ) {
			results.add( modeNames[ mode ] );
		}
		return results;
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
		return _modeStack.peek() == mode;
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
	 * Supporting silly behavior of
	 * if( condition ) {
	 * } elseif( condition ) {
	 * }
	 * but it totally messed with the grammer to return a single elseif token
	 * so we're changing any elseif tokens to be two tokens.
	 */
	public Token nextToken() {
		// If we're in the middle of an elseif workaround, then return an else token
		if ( inElseIf ) {
			inElseIf = false;
			CommonToken ifToken = new CommonToken( new Pair<TokenSource, CharStream>( this, this._input ), CFScriptLexer.IF, DEFAULT_TOKEN_CHANNEL,
			    lastElseIf.getStartIndex() + 4, lastElseIf.getStopIndex() );
			ifToken.setText( "if" );
			return ifToken;
		}
		Token nextToken = super.nextToken();
		// if the next token is elseif, then return if instead of elseif and set a flag that tells us on the next
		// call to nextToken(), we need to return an else token.
		if ( nextToken.getType() == CFScriptLexer.ELSEIF ) {
			inElseIf	= true;
			lastElseIf	= nextToken;
			CommonToken elseToken = new CommonToken( new Pair<TokenSource, CharStream>( this, this._input ), CFScriptLexer.ELSE, DEFAULT_TOKEN_CHANNEL,
			    nextToken.getStartIndex(), nextToken.getStopIndex() - 2 );
			elseToken.setText( "else" );
			return elseToken;
		}
		return nextToken;
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

}