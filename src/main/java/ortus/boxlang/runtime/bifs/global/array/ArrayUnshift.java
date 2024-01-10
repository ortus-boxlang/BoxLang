
package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayUnshift extends BIF {

	/**
	 * Constructor
	 */
	public ArrayUnshift() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "modifiablearray", Key.array ),
		    new Argument( true, "any", Key.object )
		};
	}

	/**
	 * This function adds one or more elements to the beginning of the original array and returns the length of the modified array.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to add an item to
	 * 
	 * @argument.object The value to add
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array actualObj = arguments.getAsArray( Key.array );

		actualObj.add( 0, arguments.get( Key.object ) );

		return actualObj.size();
	}

}
