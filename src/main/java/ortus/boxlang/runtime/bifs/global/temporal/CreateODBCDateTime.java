
package ortus.boxlang.runtime.bifs.global.temporal;

import java.util.HashMap;

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
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF
@BoxBIF( alias = "CreateODBCDate" )
@BoxBIF( alias = "CreateODBCTime" )
@BoxMember( type = BoxLangType.DATETIME, name = "toODBCDateTime" )
@BoxMember( type = BoxLangType.DATETIME, name = "toODBCDate" )
@BoxMember( type = BoxLangType.DATETIME, name = "toODBCTime" )
public class CreateODBCDateTime extends BIF {

	private static final Struct formatters = new Struct(
	    new HashMap<Key, String>() {

		    {
			    put( Key.of( "CreateODBCDateTime" ), DateTime.ODBC_DATE_TIME_FORMAT_MASK );
			    put( Key.of( "CreateODBCDate" ), DateTime.ODBC_DATE_FORMAT_MASK );
			    put( Key.of( "CreateODBCTime" ), DateTime.ODBC_TIME_FORMAT_MASK );
			    put( Key.of( "toODBCDateTime" ), DateTime.ODBC_DATE_TIME_FORMAT_MASK );
			    put( Key.of( "toODBCDate" ), DateTime.ODBC_DATE_FORMAT_MASK );
			    put( Key.of( "toODBCTime" ), DateTime.ODBC_TIME_FORMAT_MASK );
		    }
	    }
	);

	/**
	 * Constructor
	 */
	public CreateODBCDateTime() {
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.date ),
		    new Argument( false, "string", Key.timezone )
		};
	}

	/**
	 * Creates a DateTime object with the format set to ODBC Implicit format
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.date The date string or object
	 * 
	 * @argument.timezone An optional timezone to apply
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		DateTime dateRef = DateTimeCaster.cast(
		    arguments.get( Key.date ),
		    true,
		    LocalizationUtil.parseZoneId( arguments.getAsString( Key.timezone ), context )
		);
		return new DateTime( dateRef.getWrapped() ).setFormat( formatters.getAsString( arguments.getAsKey( BIF.__functionName ) ) );
	}

}
