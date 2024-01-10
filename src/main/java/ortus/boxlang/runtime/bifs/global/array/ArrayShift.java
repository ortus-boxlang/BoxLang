
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
public class ArrayShift extends BIF {

	/**
	 * Constructor
	 */
	public ArrayShift() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "modifiablearray", Key.array ),
		};
	}

	/**
	 * Removes the first element from an array and returns the removed element. This method changes the length of the array. If used on an empty array, an
	 * exception will be thrown.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to shift
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array actualObj = arguments.getAsArray( Key.array );

		return actualObj.removeAt( 0 );
	}

}
