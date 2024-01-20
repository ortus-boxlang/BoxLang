
package ortus.boxlang.runtime.bifs.global.temporal;

import java.time.ZoneId;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF
public class DateConvert extends BIF {

	private static final Key utc2Local = Key.of( "utc2Local" );

	/**
	 * Constructor
	 */
	public DateConvert() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.conversionType ),
		    new Argument( true, "any", Key.date )
		};
	}

	/**
	 * Describe what the invocation of your bif function does
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.foo Describe any expected arguments
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key			conversion	= Key.of( arguments.getAsString( Key.conversionType ) );
		ZoneId		localZone	= LocalizationUtil.parseZoneId( null, context );
		DateTime	dateRef		= DateTimeCaster.cast(
		    arguments.get( Key.date ),
		    true,
		    localZone
		);

		return dateRef.convertToZone( conversion.equals( utc2Local ) ? localZone : ZoneId.of( "UTC" ) );

	}

}
