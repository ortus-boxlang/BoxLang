
package ortus.boxlang.runtime.bifs.global.format;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import org.apache.commons.lang3.math.Fraction;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
@BoxMember( type = BoxLangType.NUMERIC )

public class DecimalFormat extends BIF {

	/**
	 * Constructor
	 */
	public DecimalFormat() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.number ),
		    new Argument( false, "integer", Key.length, 2 ),
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
		Locale			locale			= ( Locale ) context.getConfigItem( Key.locale, Locale.getDefault() );
		double			value			= DoubleCaster.cast( arguments.get( Key.number ) );
		int				decimalPlaces	= arguments.getAsInteger( Key.length );
		NumberFormat	formatter		= java.text.DecimalFormat.getInstance( locale );
		formatter.setMinimumFractionDigits( 2 );
		formatter.setMaximumFractionDigits( decimalPlaces );
		return formatter.format( value );
	}

}
