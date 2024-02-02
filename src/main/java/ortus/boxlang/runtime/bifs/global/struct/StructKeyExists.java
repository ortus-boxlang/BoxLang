
package ortus.boxlang.runtime.bifs.global.struct;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxMember( type = BoxLangType.STRUCT )

public class StructKeyExists extends BIF {

	/**
	 * Constructor
	 */
	public StructKeyExists() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "struct", Key.struct ),
		    new Argument( true, "string", Key.key )
		};
	}

	/**
	 * Tests whether a key exists in a struct and returns a boolean value
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.struct The struct to test
	 *
	 * @argument.key The key within the struct to test for existence
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		return arguments.getAsStruct( Key.struct ).containsKey( Key.of( arguments.getAsString( Key.key ) ) );
	}

}
