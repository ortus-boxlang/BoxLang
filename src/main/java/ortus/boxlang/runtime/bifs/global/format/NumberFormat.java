
package ortus.boxlang.runtime.bifs.global.format;

import java.text.DecimalFormat;
import java.util.HashMap;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF
@BoxMember( type = BoxLangType.NUMERIC )

public class NumberFormat extends BIF {

	private static final Struct commonFormats = new Struct(
	    new HashMap<Key, String>() {

		    {
			    put( Key.of( "()" ), "0;(0)" );
			    put( Key.of( "_,9" ), "#.000000000" );
			    put( Key.of( "+" ), "+0;-0" );
			    put( Key.of( "-" ), " 0;-0" );
			    put( Key.of( "," ), "#,#00.#" );
			    put( Key.of( "$" ), "'$'#,#00.00;-'$'#,#00.00" );
			    put( Key.of( "ls$" ), "¤#,#00.00;-¤#,#00.00" );
		    }
	    }
	);

	/**
	 * Constructor
	 */
	public NumberFormat() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.number ),
		    new Argument( false, "string", Key.mask )
		};
	}

	/**
	 * Formats a number with an optional format mask
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number The number to be formatted
	 *
	 * @argument.mask The formatting mask to apply
	 *
	 * @argument.locale Note used by standard NumberFormat but used by LSNumberFormat
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		double	value	= DoubleCaster.cast( arguments.get( Key.number ) );
		String	format	= arguments.getAsString( Key.mask );
		String	locale	= arguments.getAsString( Key.locale );

		if ( format != null ) {
			Key formatKey = Key.of( format );
			if ( commonFormats.containsKey( formatKey ) ) {
				format = commonFormats.getAsString( formatKey );
			} else {
				format = format.replaceAll( "9", "0" )
				    .replaceAll( "_", "#" );
				if ( format.substring( 0, 1 ).equals( "L" ) ) {
					format = format.substring( 1, format.length() );
				} else if ( format.substring( 0, 1 ).equals( "C" ) ) {
					format = format.substring( 1, format.length() ).replace( "0", "#" );
				}
			}
		} else {
			format = commonFormats.getAsString( Key.of( "," ) );
		}

		java.text.NumberFormat formatter = null;
		if ( locale != null ) {
			formatter = DecimalFormat.getInstance( LocalizationUtil.parseLocale( locale ) );
			( ( DecimalFormat ) formatter ).applyLocalizedPattern( format );
		} else {
			formatter = new DecimalFormat( format );
		}
		return formatter.format( value );
	}

}
