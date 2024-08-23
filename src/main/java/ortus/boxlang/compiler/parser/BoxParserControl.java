package ortus.boxlang.compiler.parser;

import static ortus.boxlang.parser.antlr.BoxScriptGrammar.ABSTRACT;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.AND;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.ANY;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.ARRAY;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.AS;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.ASSERT;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.BOOLEAN;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.BREAK;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.CASE;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.CASTAS;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.CATCH;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.CLASS;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.CONTAIN;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.CONTAINS;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.CONTINUE;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.DEFAULT;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.DO;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.DOES;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.DOT;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.ELIF;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.ELSE;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.EQ;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.EQUAL;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.EQUALSIGN;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.EQV;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.FALSE;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.FINAL;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.FINALLY;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.FOR;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.FUNCTION;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.GE;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.GREATER;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.GT;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.GTE;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.IDENTIFIER;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.IF;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.IMP;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.IMPORT;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.IN;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.INCLUDE;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.INSTANCEOF;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.INTERFACE;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.IS;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.JAVA;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.LBRACKET;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.LE;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.LESS;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.LPAREN;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.LT;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.LTE;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.MESSAGE;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.MOD;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.NEQ;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.NEW;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.NOT;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.NULL;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.NUMERIC;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.OR;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.PACKAGE;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.PARAM;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.PRIVATE;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.PROPERTY;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.PUBLIC;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.QUERY;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.REMOTE;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.REQUEST;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.REQUIRED;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.RETHROW;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.RETURN;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.SERVER;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.SETTING;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.STATIC;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.STRING;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.STRUCT;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.THAN;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.THROW;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.TO;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.TRUE;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.TRY;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.TYPE;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.VAR;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.VARIABLES;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.WHEN;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.WHILE;
import static ortus.boxlang.parser.antlr.BoxScriptGrammar.XOR;
import static ortus.boxlang.runtime.BoxRuntime.getInstance;

import java.util.Set;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;

import ortus.boxlang.runtime.services.ComponentService;

public abstract class BoxParserControl extends Parser {

	private static final Set<Integer>	identifiers			= Set.of( IDENTIFIER, ABSTRACT, AND, ANY, ARRAY, AS, ASSERT, BOOLEAN, BREAK, CASE, CASTAS, CATCH,
	    CLASS,
	    CONTAIN, CONTAINS, CONTINUE, DEFAULT, DO, DOES, ELIF, ELSE, EQ, EQUAL, EQV, FALSE, FINAL, FINALLY, FOR, FUNCTION, GE, GREATER, GT, GTE, IF, IMP, IMPORT,
	    IN, INCLUDE, INSTANCEOF, INTERFACE, IS, JAVA, LE, LESS, LT, LTE, MESSAGE, MOD, NEQ, NEW, NOT, NULL, NUMERIC, OR, PACKAGE, PARAM, PRIVATE, PROPERTY,
	    PUBLIC, QUERY, REMOTE, REQUEST, REQUIRED, RETHROW, RETURN, SERVER, SETTING, STATIC, STRING, STRUCT, THAN, THROW, TO, TRUE, TRY, TYPE, VAR, VARIABLES,
	    WHEN, WHILE, XOR );

	private static final Set<Integer>	types				= Set.of(
	    NUMERIC, STRING, BOOLEAN, CLASS, INTERFACE, ARRAY, STRUCT, QUERY, ANY, FUNCTION
	);

	private final ComponentService		componentService	= getInstance().getComponentService();

	public BoxParserControl( TokenStream input ) {
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

		var nextToken = input.LT( 1 );

		// Short circuit if not an identifier
		if ( !identifiers.contains( nextToken.getType() ) ) {
			return false;
		}

		var tokText = input.LT( 1 ).getText();

		// It is not a component if it is not registered in the component service
		if ( !componentService.hasComponent( tokText ) ) {
			return false;
		}

		// If a function call, then ( will be next so reject the component
		if ( input.LT( 2 ).getType() == LPAREN )
			return false;

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

		// Having elimnated all possible ways that this is not a component,
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
}
