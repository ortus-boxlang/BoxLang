package ortus.boxlang.runtime.bifs.global.conversion;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.DateTime;

@BoxBIF
public class ToScript extends BIF {

	public ToScript() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.cfvar ),
		    new Argument( true, "string", Key.javascriptvar )
		};
	}

	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String f = "";
		f.isBlank();

		Object					runtimeVar		= arguments.get( Key.cfvar );
		String					jsVar			= arguments.getAsString( Key.javascriptvar );

		CastAttempt<DateTime>	dateCastAttempt	= DateTimeCaster.attempt( runtimeVar, context );
		if ( dateCastAttempt.wasSuccessful() ) {
			return jsVar + " = new Date('" + dateCastAttempt.get().toISOString() + "');";
		}

		Object result = context.invokeFunction( Key.of( "JSONSerialize" ), new Object[] { runtimeVar } );

		return jsVar + " = " + result + ";";
	}
}