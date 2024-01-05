
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
public class ArrayResize extends BIF {

	/**
	 * Constructor
	 */
	public ArrayResize() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "modifiablearray", Key.array ),
		    new Argument( true, "any", Key.size )
		};
	}

	/**
	 * Resets an array to a specified minimum number of elements.
	 * This can improve performance, if used to size an array to its
	 * expected maximum. For more than 500 elements, use arrayResize
	 * immediately after using the ArrayNew tag.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to resize
	 * 
	 * @argument.size The new minimum size of the array
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	actualObj	= arguments.getAsArray( Key.array );
		Integer	size		= arguments.getAsInteger( Key.size );

		if ( actualObj.size() < size ) {
			for ( int i = actualObj.size(); i < size; i++ ) {
				actualObj.add( null );
			}
		}

		return true;
	}

}
