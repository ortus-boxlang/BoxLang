package ortus.boxlang.runtime.bifs;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.types.Argument;

public abstract class BIF {

	public static final Key		__isMemberExectution	= new Key( "__isMemberExectution" );
	protected Argument[]		arguments				= new Argument[] {};
	protected FunctionService	functionService			= BoxRuntime.getInstance().getFunctionService();

	public abstract Object invoke( IBoxContext context, ArgumentsScope arguments );

	public Argument[] getArguments() {
		return arguments;
	}

}
