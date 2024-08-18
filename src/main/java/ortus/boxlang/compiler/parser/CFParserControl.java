package ortus.boxlang.compiler.parser;

import static ortus.boxlang.parser.antlr.CFScriptGrammar.ABSTRACT;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.AND;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.ANY;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.ARRAY;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.AS;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.BOOLEAN;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.BREAK;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.CASE;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.CASTAS;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.CATCH;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.COMPONENT;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.CONTAIN;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.CONTAINS;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.CONTINUE;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.DEFAULT;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.DO;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.DOES;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.DOT;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.ELSE;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.ELSEIF;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.EQ;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.EQUAL;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.EQUALSIGN;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.EQV;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.FALSE;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.FINAL;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.FINALLY;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.FOR;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.FUNCTION;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.GE;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.GREATER;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.GT;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.GTE;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.IDENTIFIER;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.IF;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.IMP;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.IMPORT;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.IN;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.INCLUDE;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.INSTANCEOF;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.INTERFACE;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.IS;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.JAVA;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.LBRACKET;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.LE;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.LESS;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.LPAREN;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.LT;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.LTE;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.MESSAGE;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.MOD;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.NEQ;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.NEW;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.NOT;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.NULL;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.NUMERIC;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.OR;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.PACKAGE;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.PARAM;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.PREFIXEDIDENTIFIER;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.PRIVATE;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.PROPERTY;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.PUBLIC;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.QUERY;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.REMOTE;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.REQUEST;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.REQUIRED;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.RETHROW;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.RETURN;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.SERVER;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.SETTING;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.STATIC;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.STRING;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.STRUCT;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.THAN;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.THROW;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.TO;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.TRUE;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.TRY;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.TYPE;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.VAR;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.VARIABLES;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.WHEN;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.WHILE;
import static ortus.boxlang.parser.antlr.CFScriptGrammar.XOR;
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
	 * Determines if the VAR that follows this gate is a variable rather than a var expression = or var id
	 *
	 * @param input the token input stream
	 *
	 * @return true if this should be seen as a VAR expression and not an identifier
	 */
	protected boolean isVar( TokenStream input ) {
		return identifiers.contains( input.LT( 2 ).getType() );
	}
}
