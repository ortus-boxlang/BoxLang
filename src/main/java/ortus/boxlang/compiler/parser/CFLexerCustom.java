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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.misc.Pair;

import ortus.boxlang.parser.antlr.CFLexer;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.services.ComponentService;

/**
 * I extend the generated ANTLR lexer to add some custom methods for getting unpopped modes
 * so we can perform better validation after parsing.
 */
@SuppressWarnings( "unchecked" )
public class CFLexerCustom extends CFLexer {

	public static boolean						debug					= false;

	/**
	 * If the last token was an elseif
	 */
	Boolean										inElseIf				= false;

	/**
	 * Track the special token COMPONENT_CLOSE_EQUAL that happens when we have >= ending a component like cfif and need to break this into two tokens.
	 */
	Boolean										inComponentCloseEqual	= false;

	/**
	 * Are we in the body of a switch statement
	 */
	Boolean										inSwitchBody			= false;

	/**
	 * Curly brace count in switch body
	 */
	int											switchCurlyCount		= 0;

	/**
	 * A reference to the last component close equal token
	 */
	Token										lastComponentCloseEqual	= null;

	/**
	 * A reference to the last elseif token
	 */
	Token										lastElseIf				= null;

	/**
	 * A reference to the last token
	 */
	Token										lastToken				= null;

	/**
	 * Tokens that end an operator. Instead of having "less than" or "less than or equal to" as a single token sequence,
	 * we'll just check for the ending token ("than", or "to")
	 */
	private static final Set<Integer>			operatorEndingTokens	= Set.of( AND, AMPAMP, EQ, EQUAL, EQEQ, GT, GTSIGN, GTE, GE, GTESIGN, LT, LTSIGN,
	    LTE,
	    LE, LTESIGN, NEQ, BANGEQUAL, LESSTHANGREATERTHAN, OR, PIPEPIPE, AMPERSAND, ELVIS, EQUALSIGN, ARROW_RIGHT, MINUS, PERCENT, POWER, QM, SLASH, STAR,
	    CONCATEQUAL, PLUSEQUAL, MINUSEQUAL, STAREQUAL, SLASHEQUAL, MODEQUAL, PLUS, /* PLUSPLUS, */TEQ, TENQ, BITWISE_OR, BITWISE_AND, BITWISE_XOR,
	    BITWISE_COMPLEMENT, BITWISE_SIGNED_LEFT_SHIFT, BITWISE_SIGNED_RIGHT_SHIFT, BITWISE_UNSIGNED_RIGHT_SHIFT, NOT, THAN, TO, INSTANCEOF, IN, CONTAINS, IS );

	/**
	 * Tokens that can be used as labeled loops
	 */
	private static final Set<Integer>			labeledLoopTokens		= Set.of( WHILE, FOR );

	private static final Map<Integer, Object>	operatorStartingChars	= new HashMap<>();

	static {
		List<String> operators = Arrays.asList(
		    "AND", "&&", "EQ", "EQUAL", "==", "GT", ">", "GTE", "GE", ">=", "LT", "<", "LTE", "LE", "<=",
		    "NEQ", "!=", "<>", "OR", "||", "&", "=", "?:", "=>", "-", "%", "^", "?", "/", "*", "&=", "+=", "-=", "*=", "/=", "%=", "+",
		    "++", "===", "!==", "b|", "b&", "b^", "b~", "b<<", "b>>", "b>>>", "IS", "LESS", "GREATER", "DOES", "INSTANCEOF", "IN", "CONTAINS"
		);

		for ( String op : operators ) {
			Map<Integer, Object> current = operatorStartingChars;
			for ( int i = 0; i < op.length(); i++ ) {
				int c = ( int ) op.charAt( i );
				if ( !current.containsKey( c ) ) {
					current.put( c, new HashMap<Integer, Object>() );
				}
				current = ( Map<Integer, Object> ) current.get( c );
				if ( i == op.length() - 1 ) {
					current.put( -99, null );
				}
			}
		}
	}

	/**
	 * Try and track what tokens can't preceed operators. This prolly won't be an exhaustive list, but trying to cover as may bases as we can.
	 * operator words above, when followed by a `(` will be treated as an identifier ONLY if they are preceeded by a token in this list, or there is no previous token
	 */
	private static final Set<Integer>	tokensThatDoNoPreceedeOperators	= Set.of( LPAREN, LBRACE, RBRACE, LBRACKET, SEMICOLON, RETURN, COLON, DOT, COMMA, AND,
	    EQ, EQUAL, EQV, GE, GT, GTE, IMP, IS, LE, LT, LTE, MOD, NEQ, NOT, OR, THAN, XOR, AMPAMP, EQEQ, GTSIGN, GTESIGN, LTSIGN, LTESIGN, BANGEQUAL,
	    LESSTHANGREATERTHAN, BANG, PIPEPIPE, AMPERSAND, ARROW, BACKSLASH, COLONCOLON, ELVIS, EQUALSIGN, ARROW_RIGHT, MINUS, MINUSMINUS, PIPE, PERCENT, POWER,
	    QM, SLASH, STAR, CONCATEQUAL, PLUSEQUAL, MINUSEQUAL, STAREQUAL, SLASHEQUAL, MODEQUAL, PLUS, PLUSPLUS, TEQ, TENQ, TEMPLATE_IF, TEMPLATE_ELSEIF,
	    TEMPLATE_SET, TEMPLATE_RETURN, SWITCH );

	/**
	 * These are tokens which can be the end of an operator, or maybe a function call!
	 */
	private static final Set<Integer>	endingOperatorWords				= Set.of( AND, EQ, EQUAL, EQV, GE, GT, GTE, IMP, IS, LE, LT, LTE, MOD, NEQ, NOT, OR,
	    THAN, XOR );

	/**
	 * Keywords that may be identifiers
	 */
	private static final Set<Integer>	keywordsThatMayBeIdentifiers	= Set.of( ABSTRACT, AS, BREAK, CASE, CASTAS, CATCH, COMPONENT,
	    CONTAIN, CONTAINS, CONTINUE, DEFAULT, DO, DOES, ELSE, FINAL, FINALLY, FOR, FUNCTION, IF, IMPORT, IN, INCLUDE, INSTANCEOF, INTERFACE, JAVA,
	    NEW, NULL, PACKAGE, PARAM, PRIVATE, PROPERTY, PUBLIC, REMOTE, REQUIRED, RETHROW, RETURN, STATIC,
	    THROW, TO, TRY, VAR, WHEN, WHILE, TRUE, FALSE, SWITCH, PREFIXEDIDENTIFIER,
	    // operator words
	    AND, EQ, EQUAL, EQV, GE, GREATER, GT, GTE, IMP, IS, LE, LESS, LT, LTE, MOD, NEQ, NOT, OR, THAN, XOR );

	/**
	 * Keywords that legtimatley have trailing (
	 */
	private static final Set<Integer>	keywordsThatComeBeforeLParen	= Set.of( CATCH, FOR, FUNCTION, IF, WHILE, SWITCH, NOT, AND, EQ, EQUAL, EQV, GE, GT,
	    GTE, IMP, IS, LE, LT, LTE, MOD, NEQ, OR, THAN, XOR ); // , ASSERT -without assert here, you can't use an expression wrapped in parens

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

	private final ComponentService		componentService				= BoxRuntime.getInstance().getComponentService();

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
		Token	nextToken		= super.nextToken();
		int		nextTokenType	= nextToken.getType();

		switch ( nextTokenType ) {

			case CFLexer.ELSEIF :
				// If this is actually in the context of if(){} elseif(){} then we need to return an else token
				if ( lastTokenWas( RBRACE ) && nextNonWhiteSpaceCharIs( '(' ) ) {
					// if the next token is elseif, then return if instead of elseif and set a flag that tells us on the next
					// call to nextToken(), we need to return an else token.
					inElseIf	= true;
					lastElseIf	= nextToken;
					CommonToken elseToken = new CommonToken( new Pair<TokenSource, CharStream>( this, this._input ), CFLexer.ELSE, DEFAULT_TOKEN_CHANNEL,
					    nextToken.getStartIndex(), nextToken.getStopIndex() - 2 );
					elseToken.setText( "else" );
					return setLastToken( elseToken );
				} else {
					// If this was just elseif = "brad" etc then just make it an identifer
					( ( CommonToken ) nextToken ).setType( IDENTIFIER );
					return setLastToken( nextToken );
				}

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

				if ( keywordsThatMayBeIdentifiers.contains( nextTokenType ) ) {
					// Let's run a series of tests to determine if this keyword is being used as an identifier (variable, function name) and is NOT a keyword
					boolean isIdentifier = false;

					if ( nextTokenType == NULL && !nextNonWhiteSpaceCharIs( '(' )
					    && !nextNonWhiteSpaceCharIs( '.' )
					    && !nextNonWhiteSpaceCharIs( '=' )
					    && !lastTokenWas( DOT )
					    && ( !nextNonWhiteSpaceCharIs( ':' ) || lastTokenOneOf( new int[] { CASE, QM } ) ) ) {
						// any null but foo.null, null.foo, and null() and null = foo and null : (unless it's in a case statement or ternary operator)
						isIdentifier = false;
					} else if ( ( nextTokenType == FALSE || nextTokenType == TRUE ) && !nextNonWhiteSpaceCharIs( '(' )
					    && !lastTokenWas( DOT )
					    && ( !nextNonWhiteSpaceCharIs( ':' ) || lastTokenOneOf( new int[] { CASE, QM } ) ) ) {
						// any true is the keyword except foo.true, and true() and true : (unless it's in a case statement or ternary operator)
						// any false is the keyword except foo.false, and false() and false : (unless it's in a case statement or ternary operator)
						isIdentifier = false;
					} else if ( endingOperatorWords.contains( nextTokenType ) && nextTokenType != NOT && nextNonWhiteSpaceCharIs( '(' )
					    && tokensThatDoNoPreceedeOperators.contains( lastToken.getType() ) ) {
						// <bx:if and( param ) >
						// but ignore <bx:if not( param ) >
						if ( debug )
							System.out.println( "Switching [" + nextToken.getText()
							    + "] token to identifer because it is a binary operator name which appears to be a function call" );
						isIdentifier = true;
					} else if ( ( nextTokenType == BREAK || nextTokenType == CASE ) && inSwitchBody ) {
						// switch( foo ) { case 1: break; }
						isIdentifier = false;
					} else if ( nextTokenType == RETURN && !lastTokenWas( DOT ) &&
					    ( nextNonWhiteSpaceCharIsOneOf( new int[] { '(', '{', '[', ';',
					        '}', '\'', '"' } )
					        || nextNonWhiteSpaceIsAnyChar() || nextNonWhiteSpaceIsAnyDigit() || nextNonWhiteSpaceCharIs( '-' ) ) ) {
						// return foo;
						// return 42
						// return (foo)
						// return []
						// return {}
						// return -4
						// { return }
						isIdentifier = false;
					} else if ( nextTokenType == PARAM && ( ( lastTokenWas( DOT ) || lastTokenWas( LPAREN ) || nextNonWhiteSpaceCharIs( ')' ) )
					    || ! ( nextNonWhiteSpaceCharIsOneOf( new int[] { '\'', '"' } ) || nextNonWhiteSpaceIsAnyChar() ) ) ) {
						// param foo="bar"
						// return 'foo'="bar"
						// return "foo"="bar"
						if ( debug )
							System.out.println( "Switching [" + nextToken.getText() + "] token to identifer because it's not an actual param statement" );
						isIdentifier = true;
					} else if ( nextTokenType == FUNCTION && !lastTokenWas( DOT ) && nextNonWhiteSpaceCharIs( '(' ) ) {
						// foo = function() {}
						// foo( value, function(){} )
						isIdentifier = false;
					} else if ( nextTokenType == REQUIRED && !lastTokenWas( DOT ) && getParenCount() > 0 && nextNonWhiteSpaceIsAnyChar() ) {
						// function foo( required string bar )
						isIdentifier = false;
					} else if ( nextTokenType == VAR && !lastTokenWas( DOT )
					    && ( nextNonWhiteSpaceIsAnyChar() || nextNonWhiteSpaceCharIs( '\'' ) || nextNonWhiteSpaceCharIs( '"' ) )
					    && !nextNonWhiteSpaceCharsAre( operatorStartingChars )
					    && ( lastToken != null && !operatorEndingTokens.contains( lastToken.getType() ) ) ) {
						// var foo = "bar"
						isIdentifier = false;
					} else if ( nextTokenType == NEW && !lastTokenWas( DOT )
					    && ( nextNonWhiteSpaceIsAnyChar() || nextNonWhiteSpaceCharIs( '\'' ) || nextNonWhiteSpaceCharIs( '"' ) )
					    && !nextNonWhiteSpaceCharsAre( operatorStartingChars ) ) {
						// foo = new Bar() is fine
						// but ignore "new is 1" because there is an operator after new
						isIdentifier = false;
					} else if ( nextNonWhiteSpaceCharIs( ':' ) && ! ( nextTokenType == DEFAULT && inSwitchBody ) ) {
						// left side of a : which is usually { foo : bar }
						// however, ignore default: in a switch body.
						if ( debug )
							System.out.println( "Switching [" + nextToken.getText() + "] token to identifer because next char is a colon" );
						isIdentifier = true;
					} else if ( lastToken != null && operatorEndingTokens.contains( lastToken.getType() )
					    && ! ( nextTokenType == CONTAIN && lastTokenWas( NOT ) )
					    && ! ( nextTokenType == EQUAL && lastTokenWas( NOT ) )
					    && ! ( nextTokenType == TO && lastTokenWas( EQUAL ) )
					    && ! ( nextTokenType == OR && lastTokenWas( THAN ) )
					    && ! ( nextTokenType == EQUAL && lastTokenWas( OR ) )
					    && nextTokenType != NOT ) {
						// right side of an operator token like foo == switch
						// but NOT CONTAINS and EQUAL TO are fine
						// also leave 5 AND NOT false alone
						// also leave greater than or equal to alone
						if ( debug )
							System.out.println( "Switching [" + nextToken.getText() + "] token to identifer because last token was the end of an operator" );

						isIdentifier = true;
					} else if ( nextNonWhiteSpaceCharIs( ',' ) ) {
						// followed by a ,
						if ( debug )
							System.out.println( "Switching [" + nextToken.getText() + "] token to identifer because next char is a comma" );
						isIdentifier = true;
					} else if ( lastTokenWas( COMMA )
					    && ! ( nextTokenType == NOT && ( nextNonWhiteSpaceCharIs( '(' ) || nextNonWhiteSpaceIsAnyChar() || nextNonWhiteSpaceIsAnyDigit() ) ) ) {
						// preceeded by a ,
						// ignore func( arg, NOT arg2 )
						if ( debug )
							System.out.println( "Switching [" + nextToken.getText() + "] token to identifer because last token was a comma" );
						isIdentifier = true;
					} else if ( lastTokenWas( COLON ) && nextTokenType != DO
					    && ! ( labeledLoopTokens.contains( nextTokenType ) && nextNonWhiteSpaceCharIs( '(' ) )
					    && ! ( inSwitchBody
					        && ( ( ( nextTokenType == IF || nextTokenType == PREFIXEDIDENTIFIER || nextTokenType == SWITCH ) && nextNonWhiteSpaceCharIs( '(' ) )
					            || ( nextTokenType == TRY && nextNonWhiteSpaceCharIs( '{' ) )
					            || ( nextTokenType == INCLUDE || nextTokenType == THROW || nextTokenType == VAR || nextTokenType == DEFAULT ) ) ) ) {

						// preceeded by a :
						// but myLabel : for() is fine
						// and myLabel : while()
						// and myLabel : do {}
						// and not case: if()
						// but not case: try {} catch(){}
						// and not case: include "foo"
						if ( debug )
							System.out.println( "Switching [" + nextToken.getText() + "] token to identifer because last token was a colon" );
						isIdentifier = true;
					} else if ( nextNonWhiteSpaceCharIs( '(' )
					    && ( ( !keywordsThatComeBeforeLParen.contains( nextTokenType ) || ( nextTokenType == CATCH && !lastTokenWas( RBRACE ) ) ) )
					    && ! ( nextTokenType == PREFIXEDIDENTIFIER && componentService.hasComponent( nextToken.getText().substring( 2 ) ) ) ) {
						// next char is a (
						// but some tokens like function() or if() are exceptions
						// catch() is allowed only if the previous token wasn't } which implies it's not actually part of a try/catch block
						// ignore cfhttp() and other component calls
						if ( debug )
							System.out.println( "Switching [" + nextToken.getText() + "] token to identifer because next char is a left parenthesis" );
						isIdentifier = true;
					} else if ( nextNonWhiteSpaceCharIs( '[' ) && nextTokenType != IN ) {
						// var[ 'foo' ]
						// but ignore for( item in [ 1, 2, 3 ] )
						if ( debug )
							System.out.println( "Switching [" + nextToken.getText() + "] token to identifer because next char is a right bracket" );
						isIdentifier = true;
					} else if ( lastTokenWas( DOT ) ) {
						// last token was a period (.)
						if ( debug )
							System.out.println( "Switching [" + nextToken.getText() + "] token to identifer because last token was a period" );
						isIdentifier = true;
					} else if ( nextNonWhiteSpaceCharIs( '.' ) && nextTokenType != RETURN ) {
						// next char is a period (.)
						// but allow return .functionMemberWrapper();
						if ( debug )
							System.out.println( "Switching [" + nextToken.getText() + "] token to identifer because next char is a period" );
						isIdentifier = true;
					} else if ( ! ( nextTokenType == THAN && lastTokenOneOf( new int[] { LESS, GREATER } ) )
					    && ! ( nextTokenType == OR && lastTokenWas( THAN ) )
					    && nextNonWhiteSpaceCharsAre( operatorStartingChars )
					    && !isFunctionDeclaration( nextToken )
					    && ! ( operatorEndingTokens.contains( nextTokenType ) && nextNonWhiteSpaceCharIsOneOf( new int[] { '-', '+' } ) )
					    && ! ( nextTokenType == NOT && nextCharsAreWord( "EQUAL" ) ) ) {
						// The next chars are the start of an operator
						// ignore THAN if it's GREATER THAN or LESS THAN
						// ignore OR if it's GREATER|LESS THAN OR ...
						// Ignore operators that are followed by a - or + (e.g. 5 EQ -3)
						if ( debug )
							System.out.println( "Switching [" + nextToken.getText() + "] token to identifer because next chars are the start of an operator" );
						isIdentifier = true;
					} else if ( lastTokenWas( ICHAR ) && nextNonWhiteSpaceCharIs( '#' ) && hasMode( hashMode ) ) {
						// The token is encased in #hash# signs
						if ( debug )
							System.out.println( "Switching [" + nextToken.getText() + "] token to identifer because it is encased in #hash# signs" );
						isIdentifier = true;
					} else if ( lastTokenWas( LBRACKET ) && nextNonWhiteSpaceCharIs( ']' ) ) {
						// The token is encased in brackets
						// foo[ package ]
						if ( debug )
							System.out.println( "Switching [" + nextToken.getText() + "] token to identifer because it is encased in brackets" );
						isIdentifier = true;
					} else if ( nextNonWhiteSpaceCharIs( ')' ) ) {
						// The token is enclosed in parens or just at the end of parens
						// ( var )
						// ( foo, bar, property )
						if ( debug )
							System.out.println( "Switching [" + nextToken.getText() + "] token to identifer because it is up against an ending )" );
						isIdentifier = true;
					} else if ( lastTokenWas( RETURN ) && nextTokenType != NEW ) {
						// The token is a varible being returned
						// return package;
						if ( debug )
							System.out.println( "Switching [" + nextToken.getText() + "] token to identifer because it is a variable being returned" );
						isIdentifier = true;
					} else if ( nextTokenType == PREFIXEDIDENTIFIER
					    && ( !nextNonWhiteSpaceCharIs( '(' )
					        || !componentService.hasComponent( nextToken.getText().substring( 2 ) ) ) ) {
						// The token is not an a CF component call
						// cfsomething function() {}
						if ( debug )
							System.out.println( "Switching [" + nextToken.getText() + "] token to identifer because it is not a CF component call" );
						isIdentifier = true;
					}

					if ( isIdentifier ) {
						( ( CommonToken ) nextToken ).setType( IDENTIFIER );
						return setLastToken( nextToken );
					} else {

						if ( debug )
							System.out.println( "%%%%%%%%%%%% Not switching [" + nextToken.getText() + "] token to identifer" );
					}
				}
				return setLastToken( nextToken );
		}
	}

	private Token setLastToken( Token token ) {
		if ( token.getChannel() != HIDDEN ) {
			lastToken = token;
		}
		if ( token.getType() == SWITCH ) {
			inSwitchBody = true;
		}
		if ( inSwitchBody && token.getType() == LBRACE ) {
			switchCurlyCount++;
		}
		if ( inSwitchBody && token.getType() == RBRACE ) {
			switchCurlyCount--;
			if ( switchCurlyCount == 0 ) {
				inSwitchBody = false;
			}
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
		int nextChar = getInputStream().LA( skipWhiteSpace( 1 ) );
		return nextChar == charCode;
	}

	private boolean nextNonWhiteSpaceCharIsOneOf( int[] charCodes ) {
		int nextChar = getInputStream().LA( skipWhiteSpace( 1 ) );

		for ( int c : charCodes ) {
			if ( nextChar == c ) {
				return true;
			}
		}
		return false;
	}

	private boolean nextNonWhiteSpaceIsAnyChar() {
		int nextChar = getInputStream().LA( skipWhiteSpace( 1 ) );
		return Character.isAlphabetic( nextChar );
	}

	private boolean nextNonWhiteSpaceIsAnyDigit() {
		int nextChar = getInputStream().LA( skipWhiteSpace( 1 ) );
		return Character.isDigit( nextChar );
	}

	private boolean nextNonWhiteSpaceCharsAre( Map<Integer, Object> charMap ) {
		int	pos			= skipWhiteSpace( 1 );
		int	nextChar	= Character.toUpperCase( getInputStream().LA( pos++ ) );

		// Start matching from the first non-whitespace character
		return matchOperator( getInputStream(), pos, charMap, nextChar, false );
	}

	private boolean matchOperator( CharStream input, int pos, Map<Integer, Object> current, int nextChar, boolean isTextOperator ) {
		if ( current == null || current.containsKey( -99 ) && isValidNextChar( nextChar, isTextOperator ) ) {
			// We've reached a leaf node, so we need to verify the next character
			return true;
		}

		if ( !current.containsKey( nextChar ) ) {
			return false;
		}

		// Determine if we're dealing with a text-based operator (letters only)
		boolean					nextIsText	= isTextOperator || Character.isLetter( nextChar );

		// Fetch the next part of the operator
		Map<Integer, Object>	nextMap		= ( Map<Integer, Object> ) current.get( nextChar );
		nextChar = Character.toUpperCase( input.LA( pos ) );

		return matchOperator( input, pos + 1, nextMap, nextChar, nextIsText );
	}

	private boolean isValidNextChar( int nextChar, boolean isTextOperator ) {
		if ( Character.isWhitespace( nextChar ) ) {
			return true;
		}

		if ( isTextOperator ) {
			return nextChar == '(' || nextChar == '[' || nextChar == '{';
		} else {
			return true;// nextChar == '(' || nextChar == '[' || nextChar == '{' || !isPunctuation( nextChar );
		}
	}

	private boolean isPunctuation( int c ) {
		return "&*()-_=+[]{};:'<>?/".indexOf( c ) != -1;
	}

	private boolean lastTokenWas( int type ) {
		return lastToken != null && lastToken.getType() == type;
	}

	private boolean lastTokenOneOf( int[] type ) {
		if ( lastToken == null ) {
			return false;
		}
		for ( int t : type ) {
			if ( lastToken.getType() == t ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if the next characters are the start of a function declaration
	 * That means the function token is followed by a valid function name and then a left parenthesis
	 * 
	 * @param nextToken the function token
	 * 
	 * @return true if the next characters are a function declaration
	 */
	private boolean isFunctionDeclaration( Token nextToken ) {
		if ( nextToken.getType() != FUNCTION ) {
			return false;
		}

		int	pos			= skipWhiteSpace( 1 );
		int	nextChar	= getInputStream().LA( pos++ );
		if ( !Character.isAlphabetic( nextChar ) ) {
			return false;
		}
		while ( Character.isAlphabetic( nextChar ) || Character.isDigit( nextChar ) || nextChar == '_' ) {
			nextChar = getInputStream().LA( pos++ );
		}
		nextChar = getInputStream().LA( skipWhiteSpace( pos - 1 ) );
		return nextChar == '(';
	}

	/**
	 * Check if the next sequence of characters matches a specific word
	 * 
	 * @param word the word to match. Pass as upper case
	 * 
	 * @return true if the next sequence of characters matches the word
	 */
	private boolean nextCharsAreWord( String word ) {
		int pos = skipWhiteSpace( 1 );
		for ( int i = 0; i < word.length(); i++ ) {
			int nextChar = Character.toUpperCase( getInputStream().LA( pos++ ) );
			if ( nextChar != word.charAt( i ) ) {
				return false;
			}
		}
		// now that we've matched the word, the next char must be a non-character or the EOF
		int nextChar = getInputStream().LA( pos );
		return !Character.isAlphabetic( nextChar ) && !Character.isDigit( nextChar );
	}

	/**
	 * Given a position in the input stream, skip over any whitespace characters
	 * and return the position of the next non-whitespace character.
	 * 
	 * @param pos the position in the input stream
	 * 
	 * @return the position of the next non-whitespace character
	 */
	private int skipWhiteSpace( int pos ) {
		int nextChar = getInputStream().LA( pos );

		// skip whitespace
		while ( Character.isWhitespace( nextChar ) ) {
			nextChar = getInputStream().LA( ++pos );
		}

		// skip single line comments
		if ( nextChar == '/' && getInputStream().LA( pos + 1 ) == '/' ) {
			pos++;
			while ( nextChar != '\n' && nextChar != '\r' && nextChar != CharStream.EOF ) {
				nextChar = getInputStream().LA( ++pos );
			}
			return skipWhiteSpace( pos );
		}

		// skip multi-line comments
		if ( nextChar == '/' && getInputStream().LA( pos + 1 ) == '*' ) {
			pos++;
			nextChar = getInputStream().LA( ++pos );
			while ( nextChar != CharStream.EOF ) {
				nextChar = getInputStream().LA( ++pos );
				if ( nextChar == '*' && getInputStream().LA( pos + 1 ) == '/' ) {
					return skipWhiteSpace( pos + 2 );
				}
			}
		}

		// CF only. Skip multi-line <!--- tag comments --->
		if ( nextChar == '<' && getInputStream().LA( pos + 1 ) == '!' && getInputStream().LA( pos + 2 ) == '-' && getInputStream().LA( pos + 3 ) == '-' ) {
			pos			+= 3;
			nextChar	= getInputStream().LA( pos );
			while ( nextChar != CharStream.EOF ) {
				nextChar = getInputStream().LA( ++pos );
				if ( nextChar == '-' && getInputStream().LA( pos + 1 ) == '-' && getInputStream().LA( pos + 2 ) == '>' ) {
					return skipWhiteSpace( pos + 3 );
				}
			}
		}

		return pos;
	}

}