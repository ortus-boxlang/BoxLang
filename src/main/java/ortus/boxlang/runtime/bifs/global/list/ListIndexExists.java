
package ortus.boxlang.runtime.bifs.global.list;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.ListUtil;

@BoxBIF
@BoxMember( type = BoxLangType.STRING, name = "listIndexExists" )
public class ListIndexExists extends BIF {

	/**
	 * Constructor
	 */
	public ListIndexExists() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.list ),
		    new Argument( true, "numeric", Key.index ),
		    new Argument( false, "string", Key.delimiter, ListUtil.DEFAULT_DELIMITER ),
		    new Argument( false, "boolean", Key.includeEmptyFields, false ),
		};
	}

	/**
	 * Checks if a list has a given index
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list string list to check for an index
	 *
	 * @argument.index numeric The index to check for
	 *
	 * @argument.delimiter string the list delimiter
	 *
	 * @argument.includeEmptyFields boolean whether to include empty fields in the returned result
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Double index = arguments.getAsDouble( Key.index );
		return index > 0 && ListUtil.asList(
		    arguments.getAsString( Key.list ),
		    arguments.getAsString( Key.delimiter ),
		    arguments.getAsBoolean( Key.includeEmptyFields ),
		    false
		).size() >= index;
	}
}
