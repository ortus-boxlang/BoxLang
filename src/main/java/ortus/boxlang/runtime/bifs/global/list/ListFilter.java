
package ortus.boxlang.runtime.bifs.global.list;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.ListUtil;

@BoxBIF
@BoxMember( type = BoxLangType.STRING, name = "filter" )

public class ListFilter extends BIF {

	/**
	 * Constructor
	 */
	public ListFilter() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.list ),
		    new Argument( true, "function", Key.filter ),
		    new Argument( false, "string", Key.delimiter, ListUtil.DEFAULT_DELIMITER ),
		    new Argument( false, "boolean", Key.includeEmptyFields, false ),
		    new Argument( false, "boolean", Key.multiCharacterDelimiter, true ),
		    new Argument( false, "boolean", Key.parallel ),
		    new Argument( false, "numeric", Key.maxThreads, 20 )
		};
	}

	/**
	 * Filters a delimted list and returns the values from the callback test
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list string list to filter entries from
	 *
	 * @argument.filter function closure filter test
	 *
	 * @argument.delimiter string the list delimiter
	 *
	 * @argument.includeEmptyFields boolean whether to include empty fields in the returned result
	 *
	 * @argument.multiCharacterDelimiter boolean whether the delimiter is multi-character
	 *
	 * @argument.parallel boolean whether to execute the filter in parallel
	 *
	 * @argument.maxThreads number the maximum number of threads to use in the parallel filter
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		return ListUtil.asString(
		    ListUtil.filter(
		        ListUtil.asList(
		            arguments.getAsString( Key.list ),
		            arguments.getAsString( Key.delimiter ),
		            arguments.getAsBoolean( Key.includeEmptyFields ),
		            arguments.getAsBoolean( Key.multiCharacterDelimiter )
		        ),
		        arguments.getAsFunction( Key.filter ),
		        context,
		        BooleanCaster.cast( arguments.getOrDefault( "parallel", false ) ),
		        IntegerCaster.cast( arguments.getOrDefault( "maxThreads", 20 ) )
		    ),
		    arguments.getAsString( Key.delimiter )
		);
	}

}
