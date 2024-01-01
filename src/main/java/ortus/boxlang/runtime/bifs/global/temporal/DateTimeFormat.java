
package ortus.boxlang.runtime.bifs.global.temporal;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
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
		DateTime ref = null;
		try {
			ref = ( DateTime ) arguments.get( Key.of( "date" ) );
		} catch ( java.lang.ClassCastException e ) {
			ref = new DateTime( ( String ) arguments.get( Key.of( "date" ) ) );
		} catch ( Exception e ) {
			throw new RuntimeException( e );
		}

		Key		bifMethodKey	= ( Key ) arguments.get( Key.of( "__functionName" ) );
		String	bifMethod		= ( String ) bifMethodKey.getOriginalValue();
		String	format			= ( String ) arguments.get( Key.of( "mask" ) );

		if ( format == null && bifMethod.toLowerCase().equals( "dateformat" ) ) {
			format = "dd-MMM-yy";
		} else if ( format == null && bifMethod.toLowerCase().equals( "timeformat" ) ) {
			format = "HH:mm a";
		} else if ( format == null ) {
			format = "dd-MMM-yyyy HH:mm:ss";
		}

		String timezone = ( String ) arguments.get( Key.of( "timezone" ) );
		if ( timezone != null ) {
			ref.setTimezone( timezone );
		}

		return ref.format( format );

	}

}
