
package ortus.boxlang.runtime.bifs.global.list;

import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.bifs.global.array.ArraySort;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.ListUtil;

@BoxBIF
@BoxMember( type = BoxLangType.STRING, name = "listSort" )

public class ListSort extends ArraySort {

	/**
	 * Constructor
	 */
	public ListSort() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.list ),
		    new Argument( false, "any", Key.sortType ),
		    new Argument( false, "string", Key.sortOrder, "asc" ),
		    new Argument( false, "string", Key.delimiter, ListUtil.DEFAULT_DELIMITER ),
		    new Argument( false, "boolean", Key.includeEmptyFields, false ),
		    new Argument( false, "boolean", Key.multiCharacterDelimiter, false ),
		    new Argument( false, "boolean", Key.localeSensitive ),
		    new Argument( false, "any", Key.callback )
		};
	}

	/**
	 * Sorts a delimited list and returns the result
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
	 * @argument.delimiter string the list delimiter
	 *
	 * @argument.includeEmptyFields boolean whether to include empty fields in the returned result
	 *
	 * @argument.multiCharacterDelimiter boolean whether the delimiter is multi-character
	 *
	 * @argument.localeSensitive Sort based on local rules
	 *
	 * @argument.callback Optional function to use for sorting - if the sort type is a closure, it will be recognized as a callback
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array listArray = ListUtil.asList(
		    arguments.getAsString( Key.list ),
		    arguments.getAsString( Key.delimiter ),
		    arguments.getAsBoolean( Key.includeEmptyFields ),
		    arguments.getAsBoolean( Key.multiCharacterDelimiter )
		);
		arguments.put( Key.array, listArray );
		arguments.put( __isMemberExecution, true );
		return ListUtil.asString(
		    ArrayCaster.cast( super.invoke( context, arguments ) ),
		    arguments.getAsString( Key.delimiter )

		);

	}

}
