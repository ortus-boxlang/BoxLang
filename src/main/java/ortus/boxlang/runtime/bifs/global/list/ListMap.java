
package ortus.boxlang.runtime.bifs.global.list;

import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.bifs.global.array.ArrayMap;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.ListUtil;

@BoxBIF
@BoxMember( type = BoxLangType.STRING, name = "listMap" )
public class ListMap extends ArrayMap {

	/**
	 * Constructor
	 */
	public ListMap() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.list ),
		    new Argument( true, "function", Key.callback ),
		    new Argument( false, "string", Key.delimiter, ListUtil.DEFAULT_DELIMITER ),
		    new Argument( false, "boolean", Key.includeEmptyFields, false ),
		};
	}

	/**
	 * Used to iterate over a delimited list and run the function closure for each item in the list and create a new list from the returned values.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list The delimited list to perform operations on
	 *
	 * @argument.callback The function to invoke for each item. The function will be passed 3 arguments: the value, the index, the array.
	 *
	 * @argument.delimiter string the list delimiter
	 *
	 * @argument.includeEmptyFields boolean whether to include empty fields in the returned result
	 */
	public Array invoke( IBoxContext context, ArgumentsScope arguments ) {
		arguments.put(
		    Key.array,
		    ListUtil.asList(
		        arguments.getAsString( Key.list ),
		        arguments.getAsString( Key.delimiter ),
		        arguments.getAsBoolean( Key.includeEmptyFields ),
		        false
		    )
		);
		return super.invoke( context, arguments );
	}

}
