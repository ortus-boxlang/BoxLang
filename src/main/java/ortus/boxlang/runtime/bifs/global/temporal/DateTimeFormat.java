
package ortus.boxlang.runtime.bifs.global.temporal;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.DateTime;

@BoxBIF
@BoxBIF( alias = "DateFormat" )
@BoxBIF( alias = "TimeFormat" )
@BoxMember( type = BoxLangType.DATETIME, name = "format" )
public class DateTimeFormat extends BIF {

	/**
	 * Constructor
	 */
	public DateTimeFormat() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.date ),
		    new Argument( false, "string", Key.mask ),
		    new Argument( false, "string", Key.timezone )
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
		DateTime	ref				= DateTimeCaster.cast( arguments.get( Key.date ) );

		Key			bifMethodKey	= arguments.getAsKey( __functionName );
		String		format			= arguments.getAsString( Key.mask );

		if ( format == null && bifMethodKey.equals( Key.dateFormat ) ) {
			format = DateTime.DEFAULT_DATE_FORMAT_MASK;
		} else if ( format == null && bifMethodKey.equals( Key.timeFormat ) ) {
			format = DateTime.DEFAULT_TIME_FORMAT_MASK;
		} else if ( format == null ) {
			format = DateTime.DEFAULT_DATETIME_FORMAT_MASK;
		}

		String timezone = arguments.getAsString( Key.timezone );
		if ( timezone != null ) {
			ref.setTimezone( timezone );
		}

		return ref.format( format );

	}

}
