
package ortus.boxlang.runtime.bifs.global.struct;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.IStruct;

@BoxBIF
@BoxMember( type = BoxLangType.STRUCT )

public class StructIsOrdered extends BIF {

	/**
	 * Constructor
	 */
	public StructIsOrdered() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "struct", Key.struct )
		};
	}

	/**
	 * Tests whether a struct is ordered ( e.g. linked )
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.struct The struct to test for a linked type
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		return arguments.getAsStruct( Key.struct ).getType().equals( IStruct.TYPES.LINKED );
	}

}
