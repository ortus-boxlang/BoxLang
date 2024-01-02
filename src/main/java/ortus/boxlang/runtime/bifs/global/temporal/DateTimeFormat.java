
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
		    new Argument( true, "any", Key.of( "date" ) ),
		    new Argument( false, "string", Key.of( "mask" ) ),
		    new Argument( false, "string", Key.of( "timezone" ) )
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
		DateTime ref = DateTimeCaster.cast( arguments.get( Key.of( "date" ) ) );

		Key		bifMethodKey	= ( Key ) arguments.get( Key.of( "__functionName" ) );
		String	bifMethod		= ( String ) bifMethodKey.getOriginalValue();
		String	format			= ( String ) arguments.get( Key.of( "mask" ) );

		if ( format == null && bifMethod.toLowerCase().equals( "dateformat" ) ) {
			format = ref.DEFAULT_DATE_FORMAT_MASK;
		} else if ( format == null && bifMethod.toLowerCase().equals( "timeformat" ) ) {
			format = ref.DEFAULT_TIME_FORMAT_MASK;
		} else if ( format == null ) {
			format = ref.DEFAULT_DATETIME_FORMAT_MASK;
		}

		String timezone = ( String ) arguments.get( Key.of( "timezone" ) );
		if ( timezone != null ) {
			ref.setTimezone( timezone );
		}

		return ref.format( format );

	}

}
