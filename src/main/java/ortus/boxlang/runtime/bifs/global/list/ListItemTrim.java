package ortus.boxlang.runtime.bifs.global.list;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.ListUtil;

@BoxBIF
@BoxMember( type = BoxLangType.STRING, name = "listItemTrim" )
public class ListItemTrim extends BIF {

	/**
	 * Constructor
	 */
	public ListItemTrim() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.list ),
		    new Argument(
		        false,
		        "string",
		        Key.delimiter,
		        ListUtil.DEFAULT_DELIMITER
		    ),
		    new Argument( false, "boolean", Key.includeEmptyFields, false ),
		};
	}

	/**
	 * Trims each item in the list.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list string list to trim each item
	 *
	 * @argument.delimiter string the list delimiter
	 *
	 * @argument.includeEmptyFields boolean whether to include empty fields in the returned result
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String delimiter = arguments.getAsString( Key.delimiter );

		return ListUtil.asString(
		    ArrayCaster.cast(
		        ListUtil
		            .asList(
		                arguments.getAsString( Key.list ),
		                delimiter,
		                arguments.getAsBoolean( Key.includeEmptyFields ),
		                false
		            )
		            .stream()
		            .map( s -> ( String ) s )
		            .map( s -> s.trim() )
		            .toArray()
		    ),
		    delimiter
		);
	}
}
