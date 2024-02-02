
package ortus.boxlang.runtime.bifs.global.struct;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxMember( type = BoxLangType.STRUCT, name = "getFromPath", objectArgument = "object" )

public class StructGet extends BIF {

	/**
	 * Constructor
	 */
	public StructGet() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.path ),
		    new Argument( false, "struct", Key.object )
		};
	}

	/**
	 * Retrieves the value from a struct using a path based expression
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.path The string path to the object requested in the struct
	 *
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String path = arguments.getAsString( Key.path );
		if ( arguments.getAsBoolean( __isMemberExecution ) ) {
			path = "arguments.object." + path;
			System.out.println( path );
		}
		return ExpressionInterpreter.getVariable( context, path, true );

	}

}
