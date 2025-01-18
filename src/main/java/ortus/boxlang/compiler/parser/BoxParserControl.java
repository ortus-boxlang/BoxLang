package ortus.boxlang.compiler.parser;

import static ortus.boxlang.parser.antlr.BoxGrammar.ABSTRACT;
import static ortus.boxlang.parser.antlr.BoxGrammar.AND;
import static ortus.boxlang.parser.antlr.BoxGrammar.ANY;
import static ortus.boxlang.parser.antlr.BoxGrammar.ARRAY;
import static ortus.boxlang.parser.antlr.BoxGrammar.AS;
import static ortus.boxlang.parser.antlr.BoxGrammar.ASSERT;
import static ortus.boxlang.parser.antlr.BoxGrammar.BOOLEAN;
import static ortus.boxlang.parser.antlr.BoxGrammar.BREAK;
import static ortus.boxlang.parser.antlr.BoxGrammar.CASE;
import static ortus.boxlang.parser.antlr.BoxGrammar.CASTAS;
import static ortus.boxlang.parser.antlr.BoxGrammar.CATCH;
import static ortus.boxlang.parser.antlr.BoxGrammar.CLASS;
import static ortus.boxlang.parser.antlr.BoxGrammar.CONTAIN;
import static ortus.boxlang.parser.antlr.BoxGrammar.CONTAINS;
import static ortus.boxlang.parser.antlr.BoxGrammar.CONTINUE;
import static ortus.boxlang.parser.antlr.BoxGrammar.DEFAULT;
import static ortus.boxlang.parser.antlr.BoxGrammar.DO;
import static ortus.boxlang.parser.antlr.BoxGrammar.DOES;
import static ortus.boxlang.parser.antlr.BoxGrammar.ELSE;
import static ortus.boxlang.parser.antlr.BoxGrammar.EQ;
import static ortus.boxlang.parser.antlr.BoxGrammar.EQUAL;
import static ortus.boxlang.parser.antlr.BoxGrammar.EQV;
import static ortus.boxlang.parser.antlr.BoxGrammar.FALSE;
import static ortus.boxlang.parser.antlr.BoxGrammar.FINAL;
import static ortus.boxlang.parser.antlr.BoxGrammar.FINALLY;
import static ortus.boxlang.parser.antlr.BoxGrammar.FOR;
import static ortus.boxlang.parser.antlr.BoxGrammar.FUNCTION;
import static ortus.boxlang.parser.antlr.BoxGrammar.GE;
import static ortus.boxlang.parser.antlr.BoxGrammar.GREATER;
import static ortus.boxlang.parser.antlr.BoxGrammar.GT;
import static ortus.boxlang.parser.antlr.BoxGrammar.GTE;
import static ortus.boxlang.parser.antlr.BoxGrammar.IDENTIFIER;
import static ortus.boxlang.parser.antlr.BoxGrammar.IF;
import static ortus.boxlang.parser.antlr.BoxGrammar.IMP;
import static ortus.boxlang.parser.antlr.BoxGrammar.IMPORT;
import static ortus.boxlang.parser.antlr.BoxGrammar.IN;
import static ortus.boxlang.parser.antlr.BoxGrammar.INCLUDE;
import static ortus.boxlang.parser.antlr.BoxGrammar.INSTANCEOF;
import static ortus.boxlang.parser.antlr.BoxGrammar.INTERFACE;
import static ortus.boxlang.parser.antlr.BoxGrammar.IS;
import static ortus.boxlang.parser.antlr.BoxGrammar.JAVA;
import static ortus.boxlang.parser.antlr.BoxGrammar.LE;
import static ortus.boxlang.parser.antlr.BoxGrammar.LESS;
import static ortus.boxlang.parser.antlr.BoxGrammar.LPAREN;
import static ortus.boxlang.parser.antlr.BoxGrammar.LT;
import static ortus.boxlang.parser.antlr.BoxGrammar.LTE;
import static ortus.boxlang.parser.antlr.BoxGrammar.MESSAGE;
import static ortus.boxlang.parser.antlr.BoxGrammar.MOD;
import static ortus.boxlang.parser.antlr.BoxGrammar.NEQ;
import static ortus.boxlang.parser.antlr.BoxGrammar.NEW;
import static ortus.boxlang.parser.antlr.BoxGrammar.NOT;
import static ortus.boxlang.parser.antlr.BoxGrammar.NULL;
import static ortus.boxlang.parser.antlr.BoxGrammar.NUMERIC;
import static ortus.boxlang.parser.antlr.BoxGrammar.OR;
import static ortus.boxlang.parser.antlr.BoxGrammar.PACKAGE;
import static ortus.boxlang.parser.antlr.BoxGrammar.PARAM;
import static ortus.boxlang.parser.antlr.BoxGrammar.PRIVATE;
import static ortus.boxlang.parser.antlr.BoxGrammar.PROPERTY;
import static ortus.boxlang.parser.antlr.BoxGrammar.PUBLIC;
import static ortus.boxlang.parser.antlr.BoxGrammar.QUERY;
import static ortus.boxlang.parser.antlr.BoxGrammar.REMOTE;
import static ortus.boxlang.parser.antlr.BoxGrammar.REQUEST;
import static ortus.boxlang.parser.antlr.BoxGrammar.REQUIRED;
import static ortus.boxlang.parser.antlr.BoxGrammar.RETHROW;
import static ortus.boxlang.parser.antlr.BoxGrammar.RETURN;
import static ortus.boxlang.parser.antlr.BoxGrammar.SERVER;
import static ortus.boxlang.parser.antlr.BoxGrammar.SETTING;
import static ortus.boxlang.parser.antlr.BoxGrammar.STATIC;
import static ortus.boxlang.parser.antlr.BoxGrammar.STRING;
import static ortus.boxlang.parser.antlr.BoxGrammar.STRUCT;
import static ortus.boxlang.parser.antlr.BoxGrammar.THAN;
import static ortus.boxlang.parser.antlr.BoxGrammar.THROW;
import static ortus.boxlang.parser.antlr.BoxGrammar.TO;
import static ortus.boxlang.parser.antlr.BoxGrammar.TRUE;
import static ortus.boxlang.parser.antlr.BoxGrammar.TRY;
import static ortus.boxlang.parser.antlr.BoxGrammar.TYPE;
import static ortus.boxlang.parser.antlr.BoxGrammar.VAR;
import static ortus.boxlang.parser.antlr.BoxGrammar.VARIABLES;
import static ortus.boxlang.parser.antlr.BoxGrammar.WHEN;
import static ortus.boxlang.parser.antlr.BoxGrammar.WHILE;
import static ortus.boxlang.parser.antlr.BoxGrammar.XOR;

import java.util.Set;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;

public abstract class BoxParserControl extends Parser {

	private static final Set<Integer>	identifiers	= Set.of( IDENTIFIER, ABSTRACT, AND, ANY, ARRAY, AS, ASSERT, BOOLEAN, BREAK, CASE, CASTAS, CATCH,
	    CLASS,
	    CONTAIN, CONTAINS, CONTINUE, DEFAULT, DO, DOES, ELSE, EQ, EQUAL, EQV, FALSE, FINAL, FINALLY, FOR, FUNCTION, GE, GREATER, GT, GTE, IF, IMP, IMPORT,
	    IN, INCLUDE, INSTANCEOF, INTERFACE, IS, JAVA, LE, LESS, LT, LTE, MESSAGE, MOD, NEQ, NEW, NOT, NULL, NUMERIC, OR, PACKAGE, PARAM, PRIVATE, PROPERTY,
	    PUBLIC, QUERY, REMOTE, REQUEST, REQUIRED, RETHROW, RETURN, SERVER, SETTING, STATIC, STRING, STRUCT, THAN, THROW, TO, TRUE, TRY, TYPE, VAR, VARIABLES,
	    WHEN, WHILE, XOR );

	private static final Set<Integer>	types		= Set.of(
	    NUMERIC, STRING, BOOLEAN, CLASS, INTERFACE, ARRAY, STRUCT, QUERY, ANY, FUNCTION
	);

	// private final ComponentService componentService = getInstance().getComponentService();

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
		// The bx: prefix should remove this ambiguity
		return true;
		/*
		 * var nextToken = input.LT( 1 );
		 * 
		 * // Short circuit if not an identifier
		 * if ( !identifiers.contains( nextToken.getType() ) ) {
		 * return false;
		 * }
		 * 
		 * var tokText = input.LT( 1 ).getText();
		 * 
		 * // It is not a component if it is not registered in the component service
		 * if ( !componentService.hasComponent( tokText ) ) {
		 * return false;
		 * }
		 * 
		 * // If a function call, then ( will be next so reject the component
		 * if ( input.LT( 2 ).getType() == LPAREN )
		 * return false;
		 * 
		 * // If array access, then [ will be next so reject the component
		 * if ( input.LT( 2 ).getType() == LBRACKET )
		 * return false;
		 * 
		 * // Some components accept a type parameter, such as PARAM and if so we let them got through
		 * // the standard rules and not component
		 * //
		 * // PARAM String ....
		 * //
		 * // But we also see components where teh first annotation is a keyword used as an id, so
		 * // we have to assume that they are components
		 * //
		 * // SomeComp CLASS="dfdsffds"
		 * //
		 * // NB: It is possible that we could just check LT(1) == "PARAM" - but it is not clear to
		 * // me that PARAM always should be parsed using its own rule. If so, you can simplify this
		 * // method by just checking for PARAM.
		 * 
		 * if ( isType( input.LT( 2 ).getType() ) ) {
		 * // If what looks like a type is actually assigned to, then it is in fact a component
		 * return input.LT( 3 ).getType() == EQUALSIGN;
		 * }
		 * 
		 * // Sill looks like a component but components can't be named x.access, so it is a FQN of some sort if that is the name
		 * if ( input.LT( 2 ).getType() == DOT ) {
		 * return false;
		 * }
		 * // param x.y - component attributes cannot be FQN, so this is param or something similar
		 * return input.LT( 3 ).getType() != DOT;
		 * 
		 * // Having elimnated all possible ways that this is not a component,
		 * // we know it must be a component
		 */
	}

	/**
	 * Determines if the VAR or final that follows this gate is a variable rather than a var expression = or var id
	 *
	 * @param input the token input stream
	 *
	 * @return true if this should be seen as a VAR expression and not an identifier
	 */
	protected boolean isAssignmentModifier( TokenStream input ) {
		System.out.println( "isAssignmentModifier" );

		int thisType = input.LT( 1 ).getType();
		System.out.println( "thisType: " + thisType );
		System.out.println( "LT(2): " + input.LT( 2 ).getType() );
		System.out
		    .println( "( thisType == VAR || thisType == FINAL || thisType == STATIC ): " + ( thisType == VAR || thisType == FINAL || thisType == STATIC ) );
		System.out.println( "identifiers.contains( input.LT( 2 ).getType() ): " + identifiers.contains( input.LT( 2 ).getType() ) );
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
