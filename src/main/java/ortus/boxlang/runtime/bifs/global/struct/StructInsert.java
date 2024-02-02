
package ortus.boxlang.runtime.bifs.global.struct;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.IStruct;

@BoxBIF
@BoxMember( type = BoxLangType.STRUCT )

public class StructInsert extends BIF {

	/**
	 * Constructor
	 */
	public StructInsert() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "struct", Key.struct ),
		    new Argument( true, "string", Key.key ),
		    new Argument( true, "any", Key.value ),
		    new Argument( false, "boolean", Key.overwrite, false )
		};
	}

	/**
	 * Inserts a key/value pair in to a struct - with an optional overwrite argument
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.struct The target struct
	 *
	 * @argument.key The struct key
	 *
	 * @argument.value The value to assign for the specified key
	 *
	 * @argument.overwrite Whether to overwrite the existing value if the key exists ( default: false )
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		IStruct	struct		= arguments.getAsStruct( Key.struct );
		Key		key			= Key.of( arguments.getAsString( Key.key ) );
		Boolean	overwrite	= arguments.getAsBoolean( Key.overwrite );
		Boolean	isMember	= arguments.getAsBoolean( __isMemberExecution );
		if ( !overwrite && struct.containsKey( key ) ) {
			return isMember
			    ? struct
			    : false;
		}
		struct.put( key, arguments.get( Key.value ) );

		return isMember
		    ? struct
		    : true;
	}

}
