package ortus.boxlang.compiler.parser;

import static ortus.boxlang.parser.antlr.CFGrammar.ABSTRACT;
import static ortus.boxlang.parser.antlr.CFGrammar.AND;
import static ortus.boxlang.parser.antlr.CFGrammar.ANY;
import static ortus.boxlang.parser.antlr.CFGrammar.ARRAY;
import static ortus.boxlang.parser.antlr.CFGrammar.AS;
import static ortus.boxlang.parser.antlr.CFGrammar.BOOLEAN;
import static ortus.boxlang.parser.antlr.CFGrammar.BREAK;
import static ortus.boxlang.parser.antlr.CFGrammar.CASE;
import static ortus.boxlang.parser.antlr.CFGrammar.CASTAS;
import static ortus.boxlang.parser.antlr.CFGrammar.CATCH;
import static ortus.boxlang.parser.antlr.CFGrammar.COMPONENT;
import static ortus.boxlang.parser.antlr.CFGrammar.CONTAIN;
import static ortus.boxlang.parser.antlr.CFGrammar.CONTAINS;
import static ortus.boxlang.parser.antlr.CFGrammar.CONTINUE;
import static ortus.boxlang.parser.antlr.CFGrammar.DEFAULT;
import static ortus.boxlang.parser.antlr.CFGrammar.DO;
import static ortus.boxlang.parser.antlr.CFGrammar.DOES;
import static ortus.boxlang.parser.antlr.CFGrammar.DOT;
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
import static ortus.boxlang.parser.antlr.CFGrammar.LBRACKET;
import static ortus.boxlang.parser.antlr.CFGrammar.LE;
import static ortus.boxlang.parser.antlr.CFGrammar.LESS;
import static ortus.boxlang.parser.antlr.CFGrammar.LPAREN;
import static ortus.boxlang.parser.antlr.CFGrammar.LT;
import static ortus.boxlang.parser.antlr.CFGrammar.LTE;
import static ortus.boxlang.parser.antlr.CFGrammar.MESSAGE;
import static ortus.boxlang.parser.antlr.CFGrammar.MOD;
import static ortus.boxlang.parser.antlr.CFGrammar.NEQ;
import static ortus.boxlang.parser.antlr.CFGrammar.NEW;
import static ortus.boxlang.parser.antlr.CFGrammar.NOT;
import static ortus.boxlang.parser.antlr.CFGrammar.NULL;
import static ortus.boxlang.parser.antlr.CFGrammar.NUMERIC;
import static ortus.boxlang.parser.antlr.CFGrammar.OR;
import static ortus.boxlang.parser.antlr.CFGrammar.PACKAGE;
import static ortus.boxlang.parser.antlr.CFGrammar.PARAM;
import static ortus.boxlang.parser.antlr.CFGrammar.PREFIXEDIDENTIFIER;
import static ortus.boxlang.parser.antlr.CFGrammar.PRIVATE;
import static ortus.boxlang.parser.antlr.CFGrammar.PROPERTY;
import static ortus.boxlang.parser.antlr.CFGrammar.PUBLIC;
import static ortus.boxlang.parser.antlr.CFGrammar.QUERY;
import static ortus.boxlang.parser.antlr.CFGrammar.REMOTE;
import static ortus.boxlang.parser.antlr.CFGrammar.REQUEST;
import static ortus.boxlang.parser.antlr.CFGrammar.REQUIRED;
import static ortus.boxlang.parser.antlr.CFGrammar.RETHROW;
import static ortus.boxlang.parser.antlr.CFGrammar.RETURN;
import static ortus.boxlang.parser.antlr.CFGrammar.SERVER;
import static ortus.boxlang.parser.antlr.CFGrammar.SETTING;
import static ortus.boxlang.parser.antlr.CFGrammar.STATIC;
import static ortus.boxlang.parser.antlr.CFGrammar.STRING;
import static ortus.boxlang.parser.antlr.CFGrammar.STRUCT;
import static ortus.boxlang.parser.antlr.CFGrammar.THAN;
import static ortus.boxlang.parser.antlr.CFGrammar.THROW;
import static ortus.boxlang.parser.antlr.CFGrammar.TO;
import static ortus.boxlang.parser.antlr.CFGrammar.TRUE;
import static ortus.boxlang.parser.antlr.CFGrammar.TRY;
import static ortus.boxlang.parser.antlr.CFGrammar.TYPE;
import static ortus.boxlang.parser.antlr.CFGrammar.VAR;
import static ortus.boxlang.parser.antlr.CFGrammar.VARIABLES;
import static ortus.boxlang.parser.antlr.CFGrammar.WHEN;
import static ortus.boxlang.parser.antlr.CFGrammar.WHILE;
import static ortus.boxlang.parser.antlr.CFGrammar.XOR;
import static ortus.boxlang.runtime.BoxRuntime.getInstance;

import java.util.Set;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;

import ortus.boxlang.runtime.services.ComponentService;

public abstract class CFParserControl extends Parser {

	private static final Set<Integer>	identifiers			= Set.of( IDENTIFIER, PREFIXEDIDENTIFIER, ABSTRACT, AND, ANY, ARRAY, AS, BOOLEAN, BREAK, CASE,
	    CASTAS, CATCH,
	    COMPONENT, CONTAIN, CONTAINS, CONTINUE, DEFAULT, DO, DOES, ELSEIF, ELSE, EQ, EQUAL, EQV, FALSE, FINAL, FINALLY, FOR, FUNCTION, GE, GREATER, GT, GTE, IF,
	    IMP,
	    IMPORT, IN, INCLUDE, INSTANCEOF, INTERFACE, IS, JAVA, LE, LESS, LT, LTE, MESSAGE, MOD, NEQ, NEW, NOT, NULL, NUMERIC, OR, PACKAGE, PARAM, PRIVATE,
	    PROPERTY, PUBLIC, QUERY, REMOTE, REQUEST, REQUIRED, RETHROW, RETURN, SERVER, SETTING, STATIC, STRING, STRUCT, THAN, THROW, TO, TRUE, TRY, TYPE, VAR,
	    VARIABLES, WHEN, WHILE, XOR );

	private static final Set<Integer>	types				= Set.of(
	    NUMERIC, STRING, BOOLEAN, COMPONENT, INTERFACE, ARRAY, STRUCT, QUERY, ANY, FUNCTION
	);
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
		} else if ( identifiers.contains( nextToken.getType() ) ) {
			// Since we know it's not the ACF syntax, we can throw out function calls
			if ( input.LT( 2 ).getType() == LPAREN )
				return false;
		} else {
			// short circuit if it is not an identifier
			return false;
		}

		// It is not a component if it is not registered in the component service
		if ( !componentService.hasComponent( tokText ) ) {
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
		int thisType = input.LT( 1 ).getType();
		return ( thisType == VAR || thisType == FINAL || thisType == STATIC ) && identifiers.contains( input.LT( 2 ).getType() );
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
}
