
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
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

@BoxBIF
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayToStruct extends BIF {

	/**
	 * Constructor
	 */
	public ArrayToStruct() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "array", Key.array )
		};
	}

	/**
	 * Transform the array to a struct, the index of the array is the key of the struct
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to convert
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	array		= arguments.getAsArray( Key.array );
		IStruct	toReturn	= new Struct();

		for ( int i = 0; i < array.size(); i++ ) {
			toReturn.put( Key.of( i + 1 ), array.get( i ) );
		}

		return toReturn;
	}

}
