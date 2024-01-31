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
@BoxMember( type = BoxLangType.STRING, name = "listChangeDelims" )
public class ListChangeDelims extends BIF {

	/**
	 * Constructor
	 */
	public ListChangeDelims() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.list ),
		    new Argument( true, "string", Key.newDelimiter ),
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
	 * Converts the delimiters of a list to the new delimiter.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list string list to convert the delimiters.
	 *
	 * @argument.newDelimiter string the new list delimiter
	 *
	 * @argument.delimiter string the old list delimiter
	 *
	 * @argument.includeEmptyFields boolean whether to include empty fields in the returned result
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		return ListUtil.asString(
		    ListUtil.asList(
		        arguments.getAsString( Key.list ),
		        arguments.getAsString( Key.delimiter ),
		        arguments.getAsBoolean( Key.includeEmptyFields ),
		        false
		    ),
		    arguments.getAsString( Key.newDelimiter )
		);
	}
}
