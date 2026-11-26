package ortus.boxlang.runtime.bifs.global.math;

import java.math.BigDecimal;
import java.math.BigInteger;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF( description = "Convert a decimal number to specified base" )
@BoxMember( type = BoxLangType.NUMERIC )
public class FormatBaseN extends BIF {

	/**
	 * Constructor
	 */
	public FormatBaseN() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "numeric", Key.number ),
		    new Argument( true, "integerTruncate", Key.radix )
		};
	}

	/**
	 * Converts a number to its string representation in the given base (radix). Only the integer part of the number is used and
	 * the result uses lowercase letters for digits above 9. Numbers that are not BigDecimal values are limited to their lower 32 bits, so negative
	 * numbers appear in two's complement form. An exception is thrown if the radix is outside the range 2-36.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number The number to convert.
	 *
	 * @argument.radix The base to convert the number to, in the range 2-36.
	 *
	 * @return The string representation of the number in the specified base.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Number	number	= arguments.getAsNumber( Key.number );
		int		radix	= arguments.getAsInteger( Key.radix );

		// Validate radix
		if ( radix < Character.MIN_RADIX || radix > Character.MAX_RADIX ) {
			throw new BoxRuntimeException( "Radix out of valid range" );
		}

		if ( number instanceof BigDecimal bd ) {
			BigInteger bigInt = bd.toBigInteger();

			return bigInt.toString( radix );
		}

		return Long.toString( LongCaster.cast( number ) & 0xffffffffL, radix );
	}
}
