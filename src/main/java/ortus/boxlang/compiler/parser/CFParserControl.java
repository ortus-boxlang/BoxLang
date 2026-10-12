package ortus.boxlang.compiler.parser;

import static ortus.boxlang.parser.antlr.CFGrammar.ABSTRACT;
import static ortus.boxlang.parser.antlr.CFGrammar.AND;
import static ortus.boxlang.parser.antlr.CFGrammar.AS;
import static ortus.boxlang.parser.antlr.CFGrammar.BREAK;
import static ortus.boxlang.parser.antlr.CFGrammar.CASE;
import static ortus.boxlang.parser.antlr.CFGrammar.CASTAS;
import static ortus.boxlang.parser.antlr.CFGrammar.CATCH;
import static ortus.boxlang.parser.antlr.CFGrammar.COLON;
import static ortus.boxlang.parser.antlr.CFGrammar.COMMA;
import static ortus.boxlang.parser.antlr.CFGrammar.COMPONENT;
import static ortus.boxlang.parser.antlr.CFGrammar.CONTAIN;
import static ortus.boxlang.parser.antlr.CFGrammar.CONTAINS;
import static ortus.boxlang.parser.antlr.CFGrammar.CONTINUE;
import static ortus.boxlang.parser.antlr.CFGrammar.DEFAULT;
import static ortus.boxlang.parser.antlr.CFGrammar.DO;
import static ortus.boxlang.parser.antlr.CFGrammar.DOES;
import static ortus.boxlang.parser.antlr.CFGrammar.DOT;
import static ortus.boxlang.parser.antlr.CFGrammar.ELLIPSIS;
import static ortus.boxlang.parser.antlr.CFGrammar.ELSE;
import static ortus.boxlang.parser.antlr.CFGrammar.ELSEIF;
import static ortus.boxlang.parser.antlr.CFGrammar.EQ;
import static ortus.boxlang.parser.antlr.CFGrammar.EQUAL;
import static ortus.boxlang.parser.antlr.CFGrammar.EQUALSIGN;
import static ortus.boxlang.parser.antlr.CFGrammar.EQV;
import static ortus.boxlang.parser.antlr.CFGrammar.FALSE;
import static ortus.boxlang.parser.antlr.CFGrammar.FINAL;
import static ortus.boxlang.parser.antlr.CFGrammar.FINALLY;
import static ortus.boxlang.parser.antlr.CFGrammar.FOR;
import static ortus.boxlang.parser.antlr.CFGrammar.FUNCTION;
import static ortus.boxlang.parser.antlr.CFGrammar.GE;
import static ortus.boxlang.parser.antlr.CFGrammar.GREATER;
import static ortus.boxlang.parser.antlr.CFGrammar.GT;
import static ortus.boxlang.parser.antlr.CFGrammar.GTE;
import static ortus.boxlang.parser.antlr.CFGrammar.IDENTIFIER;
import static ortus.boxlang.parser.antlr.CFGrammar.IF;
import static ortus.boxlang.parser.antlr.CFGrammar.IMP;
import static ortus.boxlang.parser.antlr.CFGrammar.IMPORT;
import static ortus.boxlang.parser.antlr.CFGrammar.IN;
import static ortus.boxlang.parser.antlr.CFGrammar.INCLUDE;
import static ortus.boxlang.parser.antlr.CFGrammar.INSTANCEOF;
import static ortus.boxlang.parser.antlr.CFGrammar.INTERFACE;
import static ortus.boxlang.parser.antlr.CFGrammar.IS;
import static ortus.boxlang.parser.antlr.CFGrammar.JAVA;
import static ortus.boxlang.parser.antlr.CFGrammar.LBRACE;
import static ortus.boxlang.parser.antlr.CFGrammar.LBRACKET;
import static ortus.boxlang.parser.antlr.CFGrammar.LE;
import static ortus.boxlang.parser.antlr.CFGrammar.LESS;
import static ortus.boxlang.parser.antlr.CFGrammar.LPAREN;
import static ortus.boxlang.parser.antlr.CFGrammar.LT;
import static ortus.boxlang.parser.antlr.CFGrammar.LTE;
import static ortus.boxlang.parser.antlr.CFGrammar.MOD;
import static ortus.boxlang.parser.antlr.CFGrammar.NEQ;
import static ortus.boxlang.parser.antlr.CFGrammar.NEW;
import static ortus.boxlang.parser.antlr.CFGrammar.NOT;
import static ortus.boxlang.parser.antlr.CFGrammar.NULL;
import static ortus.boxlang.parser.antlr.CFGrammar.OR;
import static ortus.boxlang.parser.antlr.CFGrammar.PACKAGE;
import static ortus.boxlang.parser.antlr.CFGrammar.PARAM;
import static ortus.boxlang.parser.antlr.CFGrammar.PREFIXEDIDENTIFIER;
import static ortus.boxlang.parser.antlr.CFGrammar.PRIVATE;
import static ortus.boxlang.parser.antlr.CFGrammar.PROPERTY;
import static ortus.boxlang.parser.antlr.CFGrammar.PUBLIC;
import static ortus.boxlang.parser.antlr.CFGrammar.RBRACE;
import static ortus.boxlang.parser.antlr.CFGrammar.RBRACKET;
import static ortus.boxlang.parser.antlr.CFGrammar.REMOTE;
import static ortus.boxlang.parser.antlr.CFGrammar.REQUIRED;
import static ortus.boxlang.parser.antlr.CFGrammar.RETHROW;
import static ortus.boxlang.parser.antlr.CFGrammar.RETURN;
import static ortus.boxlang.parser.antlr.CFGrammar.RPAREN;
import static ortus.boxlang.parser.antlr.CFGrammar.SEMICOLON;
import static ortus.boxlang.parser.antlr.CFGrammar.STATIC;
import static ortus.boxlang.parser.antlr.CFGrammar.THAN;
import static ortus.boxlang.parser.antlr.CFGrammar.THROW;
import static ortus.boxlang.parser.antlr.CFGrammar.TO;
import static ortus.boxlang.parser.antlr.CFGrammar.TRUE;
import static ortus.boxlang.parser.antlr.CFGrammar.TRY;
import static ortus.boxlang.parser.antlr.CFGrammar.VAR;
import static ortus.boxlang.parser.antlr.CFGrammar.WHEN;
import static ortus.boxlang.parser.antlr.CFGrammar.WHILE;
import static ortus.boxlang.parser.antlr.CFGrammar.XOR;
import static ortus.boxlang.runtime.BoxRuntime.getInstance;

import java.util.Set;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;

import ortus.boxlang.runtime.services.ComponentService;

public abstract class CFParserControl extends Parser {

	private static final Set<Integer>	identifiers			= Set.of( IDENTIFIER, PREFIXEDIDENTIFIER, ABSTRACT, AND, AS, BREAK, CASE,
	    CASTAS, CATCH,
	    COMPONENT, CONTAIN, CONTAINS, CONTINUE, DEFAULT, DO, DOES, ELSEIF, ELSE, EQ, EQUAL, EQV, FALSE, FINAL, FINALLY, FOR, FUNCTION, GE, GREATER, GT, GTE, IF,
	    IMP,
	    IMPORT, IN, INCLUDE, INSTANCEOF, INTERFACE, IS, JAVA, LE, LESS, LT, LTE, MOD, NEQ, NEW, NOT, NULL, OR, PACKAGE, PARAM, PRIVATE,
	    PROPERTY, PUBLIC, REMOTE, REQUIRED, RETHROW, RETURN, STATIC, THAN, THROW, TO, TRUE, TRY, VAR,
	    WHEN, WHILE, XOR );

	private static final Set<Integer>	types				= Set.of( COMPONENT, INTERFACE, FUNCTION );
	private final ComponentService		componentService	= getInstance().getComponentService();

	public CFParserControl( TokenStream input ) {
		super( input );
	}

	/**
	 * Determines if the given token type represents a type class in the language
	 *
	 * @param type the token type to test
	 *
	 * @return true if the token is a type class
	 */
	private boolean isType( int type ) {
		return types.contains( type );
	}

	/**
	 * Determines if the given token type represents a component in the BoxScript language.
	 *
	 * @param input the current token stream
	 *
	 * @return true if the stream represents a component
	 */
	protected boolean isComponent( TokenStream input ) {
		var	nextToken	= input.LT( 1 );
		var	tokText		= nextToken.getText();

		// If it starts with cf, the name follows
		if ( nextToken.getType() == PREFIXEDIDENTIFIER ) {
			tokText = tokText.substring( 2 );
			// If the code is cf_something( then it's a CF-style script custom tag/component
			// It's worth noting, you can't have a function called cf_myFunc() in CF because it will be
			// taken as a script custom tag every time, even if it doesn't exist. Typical ambiguous CFML :/
			if ( tokText.startsWith( "_" ) && input.LT( 2 ).getType() == LPAREN ) {
				return true;
			}
		} else if ( identifiers.contains( nextToken.getType() ) ) {
			// Since we know it's not the ACF syntax, we can throw out function calls
			if ( input.LT( 2 ).getType() == LPAREN )
				return false;
		} else {
			// short circuit if it is not an identifier
			return false;
		}

		// It is not a component if it is not registered in the component service
		if ( !componentService.hasComponent( tokText ) && !tokText.equalsIgnoreCase( "module" ) ) {
			return false;
		}

		// If array access, then [ will be next so reject the component
		if ( input.LT( 2 ).getType() == LBRACKET )
			return false;

		// Some components accept a type parameter, such as PARAM and if so we let them got through
		// the standard rules and not component
		//
		// PARAM String ....
		//
		// But we also see components where teh first annotation is a keyword used as an id, so
		// we have to assume that they are components
		//
		// SomeComp CLASS="dfdsffds"
		//
		// NB: It is possible that we could just check LT(1) == "PARAM" - but it is not clear to
		// me that PARAM always should be parsed using its own rule. If so, you can simplify this
		// method by just checking for PARAM.

		if ( isType( input.LT( 2 ).getType() ) ) {
			// If what looks like a type is actually assigned to, then it is in fact a component
			return input.LT( 3 ).getType() == EQUALSIGN;
		}

		// Sill looks like a component but components can't be named x.access, so it is a FQN of some sort if that is the name
		if ( input.LT( 2 ).getType() == DOT ) {
			return false;
		}
		// param x.y - component attributes cannot be FQN, so this is param or something similar
		return input.LT( 3 ).getType() != DOT;

		// Having eliminated all possible ways that this is not a component,
		// we know it must be a component
	}

	/**
	 * Determines if the VAR or final that follows this gate is a variable rather than a var expression = or var id
	 *
	 * @param input the token input stream
	 *
	 * @return true if this should be seen as a VAR expression and not an identifier
	 */
	protected boolean isAssignmentModifier( TokenStream input ) {
		int	thisType	= input.LT( 1 ).getType();
		int	nextType	= input.LT( 2 ).getType();
		if ( thisType != VAR && thisType != FINAL && thisType != STATIC ) {
			return false;
		}
		// static[ expr ] is bracket access into the static scope unless the brackets hold a destructuring pattern
		if ( thisType == STATIC && nextType == LBRACKET ) {
			return isStaticArrayDestructuring( input );
		}
		return identifiers.contains( nextType ) || nextType == LBRACE || nextType == LBRACKET;
	}

	/**
	 * Gate for treating the STATIC keyword as the static scope in {@code static[ expr ]} bracket access (BL-2750).
	 *
	 * @param input the token input stream
	 *
	 * @return true if STATIC is followed by brackets that are scope access rather than a destructuring declaration
	 */
	protected boolean isStaticScopeAccess( TokenStream input ) {
		return input.LT( 1 ).getType() == STATIC && input.LT( 2 ).getType() == LBRACKET && !isStaticArrayDestructuring( input );
	}

	/**
	 * Decides whether {@code static [ ... ]} starts a static array destructuring declaration or is bracket access
	 * into the static scope, such as {@code static[ name ] = value}.
	 *
	 * It is a declaration only when the matching {@code ]} is followed by {@code =} and the brackets hold something
	 * that cannot be a single key expression: a top-level comma, a rest binding ({@code ...}), a default value
	 * ({@code =}), a nested pattern, or nothing at all. So {@code static [ a, b ] = data} and {@code static [ a, ] = data}
	 * destructure, while {@code static[ name ] = value}, {@code static [ name ] = value} and {@code static[ k ] += 1}
	 * assign into the static scope.
	 *
	 * @param input the token input stream, positioned on the STATIC token
	 *
	 * @return true if this is a static array destructuring declaration
	 */
	private boolean isStaticArrayDestructuring( TokenStream input ) {
		int		firstType	= input.LT( 3 ).getType();
		boolean	isPattern	= firstType == LBRACKET || firstType == RBRACKET || firstType == ELLIPSIS;
		int		depth		= 0;
		for ( int i = 2;; i++ ) {
			int type = input.LT( i ).getType();
			if ( type == Token.EOF ) {
				return false;
			}
			if ( type == LBRACKET || type == LPAREN || type == LBRACE ) {
				depth++;
			} else if ( type == RBRACKET || type == RPAREN || type == RBRACE ) {
				depth--;
				if ( depth == 0 ) {
					return isPattern && input.LT( i + 1 ).getType() == EQUALSIGN;
				}
			} else if ( depth == 1 && ( type == COMMA || type == ELLIPSIS || type == EQUALSIGN ) ) {
				isPattern = true;
			}
		}
	}

	/**
	 * Provides a gate for the [throw expr] rule if the token after throw is `(` don't match since we'll assume it's the throw() BIF.
	 * This DOES rule out code like `throw (new Exception())` but that's a rare case.
	 *
	 * @param input the token input stream
	 *
	 * @return true if this should be seen as a throw
	 */
	protected boolean isThrow( TokenStream input ) {
		int	thisType	= input.LT( 1 ).getType();
		int	nextType	= input.LT( 2 ).getType();
		return thisType == THROW && nextType != LPAREN;
	}

	/**
	 * Return true if the next token is ) ] } : or , indicating that the expression continues
	 * 
	 * @param input
	 * 
	 * @return
	 */
	protected boolean isNextTokenExprContinuation( TokenStream input ) {
		int nextType = input.LT( 1 ).getType();
		// This predicate runs before the rule. If the next token isn't a semicolon, just stop now
		if ( nextType != SEMICOLON ) {
			return false;
		}
		// If it is a semicolon, check what comes after it
		nextType = input.LT( 2 ).getType();
		return nextType == RPAREN || nextType == COMMA || nextType == RBRACKET || nextType == RBRACE || nextType == COLON;
	}

}
