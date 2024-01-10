
package ortus.boxlang.runtime.bifs.global.array;

import java.util.Comparator;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.operators.Compare;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
@BoxMember( type = BoxLangType.ARRAY )
public class ArraySort extends BIF {

	/**
	 * Constructor
	 */
	public ArraySort() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "modifiablearray", Key.array ),
		    new Argument( false, "any", Key.sortType ),
		    new Argument( false, "string", Key.sortOrder, "asc" ),
		    new Argument( false, "boolean", Key.localeSensitive ),
		    new Argument( false, "any", Key.callback )
		};
	}

	/**
	 * Sorts array elements.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to sort
	 * 
	 * @argument.sortType Options are text, numeric, or textnocase
	 * 
	 * @argument.sortOrder Options are asc or desc
	 * 
	 * @argument.localeSensitive Sort based on local rules
	 * 
	 * @argument.callback Function to sort by
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array		array		= arguments.getAsArray( Key.array );
		Function	callback	= arguments.getAsFunction( Key.callback );
		Object		sortType	= arguments.get( Key.sortType );
		String		sortOrder	= arguments.getAsString( Key.sortOrder );
		Comparator	compareFunc	= getCompareFunc( context, sortType, sortOrder, callback );

		array.sort( compareFunc );

		return true;
	}

	private Comparator getCompareFunc( IBoxContext context, Object sortType, String sortOrder, Function callback ) {
		if ( callback == null && sortType == null ) {
			throw new BoxRuntimeException( "You must supply either a sortOrder or callback" );
		}

		// use provided callback
		if ( callback != null ) {
			return ( a, b ) -> IntegerCaster.cast( context.invokeFunction( callback, new Object[] { a, b } ) );
		} else if ( sortType instanceof Function sortFunc ) {
			return ( a, b ) -> {
				Integer val = IntegerCaster.cast( context.invokeFunction( sortFunc, new Object[] { a, b } ) );
				return val;
			};
		}

		// String sortType argument
		String	sort			= ( String ) sortType;
		int		sortModifier	= sortOrder.equalsIgnoreCase( "asc" ) ? 1 : -1;

		if ( sort.equalsIgnoreCase( "text" ) ) {
			return ( a, b ) -> Compare.invoke( a, b, true ) * sortModifier;
		} else if ( sort.equalsIgnoreCase( "numeric" ) ) {
			return ( a, b ) -> Compare.invoke( a, b, true ) * sortModifier;
		} else if ( sort.equalsIgnoreCase( "textnocase" ) ) {
			return ( a, b ) -> Compare.invoke( a, b, false ) * sortModifier;
		}

		throw new BoxRuntimeException( "You must supply either a sortOrder or callback" );
	}

}
