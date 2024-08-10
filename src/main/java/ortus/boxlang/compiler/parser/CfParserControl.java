package ortus.boxlang.compiler.parser;

import org.antlr.v4.runtime.TokenStream;
import ortus.boxlang.runtime.services.ComponentService;

import static ortus.boxlang.parser.antlr.CFScriptGrammar.*;
import static ortus.boxlang.runtime.BoxRuntime.getInstance;

public class CfParserControl {

	private final ComponentService componentService = getInstance().getComponentService();

	/**
	 * Determines if the given token type represents a type class in the language
	 *
	 * @param type the token type to test
	 *
	 * @return true if the token is a type class
	 */
	private boolean isType( int type ) {
		return type == NUMERIC || type == STRING || type == BOOLEAN || type == INTERFACE || type == ARRAY || type == STRUCT || type == QUERY
		    || type == ANY;
	}

	/**
	 * Determines if the given token type represents a component in the BoxScript language.
	 *
	 * @param input the current token stream
	 * 
	 * @return true if the stream represents a component
	 */
	private boolean isComponent( TokenStream input ) {

		var tokText = input.LT( 1 ).getText();

		// It is not a component if it is not registered in the component service
		if ( !componentService.hasComponent( tokText ) ) {
			return false;
		}

		// If a function call, then ( will be next so reject the component, unless it is a cfTag()
		// which is a component even though it looks like a function call/
		if ( tokText.startsWith( "cf" ) )
			return true;
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
}
