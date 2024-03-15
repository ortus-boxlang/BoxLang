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
package ortus.boxlang.parser;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;

import ortus.boxlang.parser.antlr.CFMLLexer;

public class CFMLLexerCustom extends CFMLLexer {

	public CFMLLexerCustom( CharStream input ) {
		super( input );
	}

	public boolean hasUnpoppedModes() {
		return !_modeStack.isEmpty();
	}

	// get mode stack
	public List<String> getUnpoppedModes() {
		List<String> results = new ArrayList<String>();
		for ( int mode : _modeStack.toArray() ) {
			results.add( modeNames[ mode ] );
		}
		return results;
	}

	public boolean lastModeWas( int mode ) {
		if ( !hasUnpoppedModes() ) {
			return false;
		}
		return _modeStack.peek() == mode;
	}

	public Token findPreviousToken( int type ) {
		reset();
		var tokens = getAllTokens();
		for ( int i = tokens.size() - 1; i >= 0; i-- ) {
			Token t = tokens.get( i );
			System.out.println( "Token: " + t.getText() + " Type: " + t.getType() );
			if ( t.getType() == type ) {
				return t;
			}
		}
		return null;
	}
}