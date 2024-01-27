
package ortus.boxlang.runtime.bifs.global.list;

import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.bifs.global.array.ArrayFind;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.util.ArrayUtil;

@BoxBIF
@BoxBIF( alias = "ListFindNoCase" )
@BoxMember( type = BoxLangType.STRING, name = "listFind" )
@BoxMember( type = BoxLangType.STRING, name = "listFindNoCase" )

public class ListFind extends ArrayFind {

	/**
	 * Constructor
	 */
	public ListFind() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.list ),
		    new Argument( true, "string", Key.value ),
		    new Argument( false, "string", Key.delimiter, "," ),
		    new Argument( false, "boolean", Key.includeEmptyFields, false )
		};
	}

	/**
	 * Return int position of value in delimited list, case sensitive or case-insenstive variations
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list The list to be searched.
	 *
	 * @argument.value The value to locale
	 *
	 * @argument.delimiter The list delimiter(s)
	 *
	 * @argument.includeEmptyFields Whether to include empty fields in the search
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {

		arguments.put(
		    Key.array,
		    ArrayUtil.ofList(
		        arguments.getAsString( Key.list ),
		        arguments.getAsString( Key.delimiter ),
		        arguments.getAsBoolean( Key.includeEmptyFields ),
		        false
		    )
		);
		return super.invoke( context, arguments );
	}

}
