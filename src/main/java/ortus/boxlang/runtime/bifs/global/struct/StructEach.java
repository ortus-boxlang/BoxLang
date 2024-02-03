
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
import ortus.boxlang.runtime.util.StructUtil;

@BoxBIF
@BoxMember( type = BoxLangType.STRUCT )

public class StructEach extends BIF {

	/**
	 * Constructor
	 */
	public StructEach() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "struct", Key.struct ),
		    new Argument( true, "function", Key.callback ),
		    new Argument( false, "boolean", Key.parallel, false ),
		    new Argument( false, "integer", Key.maxThreads ),
		    new Argument( false, "boolean", Key.ordered, false )
		};
	}

	/**
	 * Used to iterate over a struct and run the function closure for each key/value pair.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to reduce
	 *
	 * @argument.callback The function to invoke for each item. The function will be passed 3 arguments: the key, the value, the struct.
	 *
	 * @argument.parallel Specifies whether the items can be executed in parallel
	 *
	 * @argument.maxThreads The maximum number of threads to use when parallel = true
	 *
	 * @argument.ordered (BoxLang only) whether parallel operations should execute and maintain order
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		IStruct target = arguments.getAsStruct( Key.struct );

		StructUtil.each(
		    target,
		    arguments.getAsFunction( Key.callback ),
		    context,
		    arguments.getAsBoolean( Key.parallel ),
		    arguments.getAsInteger( Key.maxThreads ),
		    arguments.getAsBoolean( Key.ordered )
		);

		return arguments.getAsBoolean( __isMemberExecution )
		    ? target
		    : null;
	}

}
