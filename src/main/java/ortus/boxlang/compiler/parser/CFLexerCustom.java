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
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.misc.Pair;

import ortus.boxlang.parser.antlr.CFLexer;

/**
 * I extend the generated ANTLR lexer to add some custom methods for getting unpopped modes
 * so we can perform better validation after parsing.
 */
public class CFLexerCustom extends CFLexer {

	/**
	 * If the last token was an elseif
	 */
	Boolean								inElseIf						= false;

	/**
	 * Track the special token COMPONENT_CLOSE_EQUAL that happens when we have >= ending a component like cfif and need to break this into two tokens.
	 */
	Boolean								inComponentCloseEqual			= false;

	/**
	 * A reference to the last component close equal token
	 */
	Token								lastComponentCloseEqual			= null;

	/**
	 * A reference to the last elseif token
	 */
	Token								lastElseIf						= null;

	/**
	 * A reference to the last token
	 */
	Token								lastToken						= null;

	/**
	 * Reserved words that are operators
	 */
	private static final Set<Integer>	operatorWords					= Set.of( AND, EQ, EQUAL, EQV, GE, GREATER, GT, GTE, IMP, IS, LE, LESS, LT, LTE, MOD,
	    NEQ, NOT, OR, THAN, XOR );

	/**
	 * Try and track what tokens can't preceed operators. This prolly won't be an exhaustive list, but trying to cover as may bases as we can.
	 * operator words above, when followed by a `(` will be treated as an identifier ONLY if they are preceeded by a token in this list, or there is no previous token
	 */
	private static final Set<Integer>	tokensThatDoNoPreceedeOperators	= Set.of( LPAREN, LBRACE, RBRACE, LBRACKET, SEMICOLON, RETURN, COLON, DOT, COMMA, AND,
	    EQ, EQUAL, EQV, GE, GT, GTE, IMP, IS, LE, LT, LTE, MOD, NEQ, NOT, OR, THAN, XOR, AMPAMP, EQEQ, GTSIGN, GTESIGN, LTSIGN, LTESIGN, BANGEQUAL,
	    LESSTHANGREATERTHAN, BANG, PIPEPIPE, AMPERSAND, ARROW, BACKSLASH, COLONCOLON, ELVIS, EQUALSIGN, ARROW_RIGHT, MINUS, MINUSMINUS, PIPE, PERCENT, POWER,
	    QM, SLASH, STAR, CONCATEQUAL, PLUSEQUAL, MINUSEQUAL, STAREQUAL, SLASHEQUAL, MODEQUAL, PLUS, PLUSPLUS, TEQ, TENQ, TEMPLATE_IF, TEMPLATE_ELSEIF,
	    TEMPLATE_SET, TEMPLATE_RETURN );

	/**
	 * A flag to track if the last token was a dot
	 */
	private boolean						dotty							= false;

	/**
	 * ASCII Character code for left parenthesis
	 */
	private int							LPAREN_Char_Code				= 40;

	/**
	 * The mode for the lexer to start in
	 */
	private int							defaultMode;

	/**
	 * The error listener
	 */
	ErrorListener						errorListener;

	/**
	 * The parser
	 */
	CFParser							parser;

	/**
	 * Constructor
	 *
	 * @param input input stream
	 */
	public CFLexerCustom( CharStream input, int defaultMode, ErrorListener errorListener, CFParser parser ) {
		super( input );
		this.defaultMode	= defaultMode;
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
		return !_modeStack.isEmpty()
		    && ! ( _modeStack.peek() == DEFAULT_MODE && _mode == DEFAULT_SCRIPT_MODE )
		    && ! ( _modeStack.peek() == DEFAULT_MODE && _mode == DEFAULT_TEMPLATE_MODE );
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
	 * Supporting silly behavior of
	 * if( condition ) {
	 * } elseif( condition ) {
	 * }
	 * but it totally messed with the grammar to return a single elseif token
	 * so we're changing any elseif tokens to be two tokens.
	 */
	public Token nextToken() {
		// If we're in the middle of an elseif workaround, then return an else token
		if ( inElseIf ) {
			inElseIf = false;
			CommonToken ifToken = new CommonToken( new Pair<TokenSource, CharStream>( this, this._input ), CFLexer.IF, DEFAULT_TOKEN_CHANNEL,
			    lastElseIf.getStartIndex() + 4, lastElseIf.getStopIndex() );
			ifToken.setText( "if" );
			lastToken = ifToken;
			return setLastToken( ifToken );
		}
		if ( inComponentCloseEqual ) {
			inComponentCloseEqual = false;
			CommonToken equalToken = new CommonToken( new Pair<TokenSource, CharStream>( this, this._input ), CFLexer.CONTENT_TEXT, DEFAULT_TOKEN_CHANNEL,
			    lastComponentCloseEqual.getStartIndex() + 1, lastComponentCloseEqual.getStopIndex() );
			equalToken.setText( "=" );
			return setLastToken( equalToken );
		}
		Token nextToken = super.nextToken();

		switch ( nextToken.getType() ) {

			case CFLexer.ELSEIF :
				// if the next token is elseif, then return if instead of elseif and set a flag that tells us on the next
				// call to nextToken(), we need to return an else token.
				inElseIf = true;
				lastElseIf = nextToken;
				CommonToken elseToken = new CommonToken( new Pair<TokenSource, CharStream>( this, this._input ), CFLexer.ELSE, DEFAULT_TOKEN_CHANNEL,
				    nextToken.getStartIndex(), nextToken.getStopIndex() - 2 );
				elseToken.setText( "else" );
				return setLastToken( elseToken );

			case CFLexer.DOT :
				dotty = true;
				return setLastToken( nextToken );

			case CFLexer.UNEXPECTED_EXPRESSION_END :
				errorListener.semanticError( "Unexpected end of expression", parser.getPosition( nextToken ) );
				( ( CommonToken ) nextToken ).setType( COMPONENT_OPEN );
				return setLastToken( nextToken );

			case CFLexer.COMPONENT_CLOSE_EQUAL :
				inComponentCloseEqual = true;
				lastComponentCloseEqual = nextToken;
				CommonToken componentCloseToken = new CommonToken( new Pair<TokenSource, CharStream>( this, this._input ), CFLexer.COMPONENT_CLOSE,
				    DEFAULT_TOKEN_CHANNEL,
				    nextToken.getStartIndex(), nextToken.getStopIndex() - 1 );
				componentCloseToken.setText( ">" );
				return setLastToken( componentCloseToken );

			default :
				// reserved operators after a dot are just identifiers
				// foo.var
				// bar.GT()
				if ( dotty && ( operatorWords.contains( nextToken.getType() ) || nextToken.getType() == CFLexer.SWITCH ) ) {
					( ( CommonToken ) nextToken ).setType( IDENTIFIER );
					// reserved operators (other than NOT) before an open parenthesis are just identifiers
					// LT()
					// GT()
				} else if ( nextToken.getType() != CFLexer.NOT && operatorWords.contains( nextToken.getType() )
				    && nextNonWhiteSpaceCharIs( LPAREN_Char_Code )
				    && ( lastToken == null || tokensThatDoNoPreceedeOperators.contains( lastToken.getType() ) ) ) {
					( ( CommonToken ) nextToken ).setType( IDENTIFIER );
				}
				dotty = false;
				return setLastToken( nextToken );
		}
	}

	private Token setLastToken( Token token ) {
		if ( token.getChannel() != HIDDEN ) {
			lastToken = token;
		}
		return token;
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

	public void reset() {
		super.reset();
		pushMode( defaultMode );
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

	private boolean nextNonWhiteSpaceCharIs( int charCode ) {
		int	pos			= 1;
		int	nextChar	= getInputStream().LA( pos++ );
		while ( Character.isWhitespace( nextChar ) ) {
			nextChar = getInputStream().LA( pos++ );
		}
		return nextChar == charCode;
	}

}