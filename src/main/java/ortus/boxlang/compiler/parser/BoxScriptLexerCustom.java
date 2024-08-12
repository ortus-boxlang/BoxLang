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
import org.antlr.v4.runtime.Token;

import ortus.boxlang.parser.antlr.BoxScriptLexer;

/**
 * I extend the generated ANTLR lexer to add some custom methods for getting unpopped modes
 * so we can perform better validation after parsing.
 */
public class BoxScriptLexerCustom extends BoxScriptLexer {

	/**
	 * Constructor
	 * 
	 * @param input input stream
	 */
	public BoxScriptLexerCustom( CharStream input ) {
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
	public List<Integer> getUnpoppedModesInts() {
		List<Integer> results = new ArrayList<Integer>();
		results.add( _mode );
		for ( int mode : _modeStack.toArray() ) {
			results.add( mode );
		}
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
}