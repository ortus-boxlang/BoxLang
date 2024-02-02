
package ortus.boxlang.runtime.bifs.global.struct;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.util.DuplicationUtil;

@BoxBIF
@BoxMember( type = BoxLangType.STRUCT )

public class StructCopy extends BIF {

	/**
	 * Constructor
	 */
	public StructCopy() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "struct", Key.struct ),
		};
	}

	/**
	 * Creates a shallow copy of a struct. Copies top-level keys, values, and arrays in the structure by value; copies nested structures by reference.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.struct The struct to copy
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		return DuplicationUtil.duplicateStruct( arguments.getAsStruct( Key.struct ), false );
	}

}
