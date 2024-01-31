
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
@BoxMember( type = BoxLangType.STRING, name = "listRemoveDuplicates" )

public class ListRemoveDuplicates extends BIF {

	/**
	 * Constructor
	 */
	public ListRemoveDuplicates() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.list ),
		    new Argument( false, "string", Key.delimiter, ListUtil.DEFAULT_DELIMITER ),
		    new Argument( false, "boolean", Key.ignoreCase, false )
		};
	}

	/**
	 * De-duplicates a delimited list - either case-sensitively or case-insenstively
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list The list to deduplicate
	 * 
	 * @argument.delimiter The delimiter of the list
	 * 
	 * @argument.ignoreCase Whether case should be ignored or not during deduplication - defaults to false
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		return ListUtil.removeDuplicates(
		    arguments.getAsString( Key.list ),
		    arguments.getAsString( Key.delimiter ),
		    !arguments.getAsBoolean( Key.ignoreCase )
		);
	}

}
