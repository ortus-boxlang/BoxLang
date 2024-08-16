package ortus.boxlang.compiler.parser;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;
import ortus.boxlang.runtime.services.ComponentService;

import java.util.Set;

import static ortus.boxlang.parser.antlr.CFScriptGrammar.*;
import static ortus.boxlang.runtime.BoxRuntime.getInstance;

public abstract class CFParserControl extends Parser {

	private static final Set<Integer>	identifiers			= Set.of( IDENTIFIER, PREFIXEDIDENTIFIER,  ABSTRACT, AND, ANY, ARRAY, AS,  BOOLEAN, BREAK, CASE, CASTAS, CATCH,
	    COMPONENT, CONTAIN, CONTAINS, CONTINUE, DEFAULT, DO, DOES, ELSEIF, ELSE, EQ, EQUAL, EQV, FALSE, FINAL, FINALLY, FOR, FUNCTION, GE, GREATER, GT, GTE, IF, IMP,
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

		var tokText = input.LT( 1 ).getText();

		// If it starts with cf, it is a component as all CF components start with cf
		if ( tokText.toLowerCase().startsWith( "cf" ) )
			return true;

		// It is not a component if it is not registered in the component service
		if ( !componentService.hasComponent( tokText ) ) {
			return false;
		}

		if ( input.LT( 2 ).getType() == LPAREN )
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
