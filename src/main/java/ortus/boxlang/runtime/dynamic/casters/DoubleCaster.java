/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.dynamic.casters;

import java.math.BigDecimal;

import org.apache.commons.lang3.math.NumberUtils;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;

/**
 * I handle casting anything to a Double
 */
public class DoubleCaster implements IBoxCaster {

	/**
	 * Tests to see if the value can be cast to a Double.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast to a Double
	 *
	 * @return The Double value
	 */
	public static CastAttempt<Double> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Used to cast anything to a Double, throwing exception if we fail
	 *
	 * @param object The value to cast to a Double
	 *
	 * @return The Double value
	 */
	public static Double cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Used to cast anything to a double
	 *
	 * @param object The value to cast to a double
	 * @param fail   If true, throw exception if we fail
	 *
	 * @return The double value
	 */
	public static Double cast( Object object, Boolean fail ) {
		if ( object == null ) {
			return Double.valueOf( 0 );
		}

		object = DynamicObject.unWrap( object );

		if ( object instanceof Double d ) {
			return d;
		}
		if ( object instanceof BigDecimal bd ) {
			// TODO: remove this
			if ( BoxRuntime.getInstance().inDebugMode() ) {
				System.out.println( "Potential precision loss casting BigDecimal to double" );
				// Print out the top 10 stack trace lines
				StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
				for ( int i = 1; i < 11; i++ ) {
					// If we reach the end of the stack trace, break or of class starts with boxgenerated.
					if ( i == stackTrace.length - 1 || stackTrace[ i ].getClassName().startsWith( "boxgenerated." ) ) {
						break;
					}
					System.out.println( "  " + stackTrace[ i ] );
				}
				Array tagContext = ExceptionUtil.getTagContext();
				tagContext.forEach( i -> {
					Struct item = ( Struct ) i;
					System.out.println( "  " + item.getAsString( Key.template ) + ":" + item.getAsInteger( Key.line ) + " " + item.getAsString( Key.id ) );
				} );
			}
			return bd.doubleValue();
		}

		if ( object instanceof Number num ) {
			return num.doubleValue();
		}

		if ( object instanceof Boolean bool ) {
			return bool ? 1D : 0D;
		}

		if ( object instanceof String str ) {
			// String true and yes are truthy
			if ( str.equalsIgnoreCase( "true" ) || str.equalsIgnoreCase( "yes" ) ) {
				return 1D;
				// String false and no are truthy
			} else if ( str.equalsIgnoreCase( "false" ) || str.equalsIgnoreCase( "no" ) ) {
				return 0D;
			}
		}
		// Try to parse the string as a double
		String	stringValue	= StringCaster.cast( object, false );
		Double	result		= parseDouble( stringValue );
		if ( result != null ) {
			return result;
		}

		// Verify if we can throw an exception
		if ( fail ) {
			throw new BoxCastException( String.format( "Can't cast [%s] to a double.", object.toString() ) );
		} else {
			return null;
		}

	}

	/**
	 * Determine whether the provided string is castable to a Double.
	 *
	 * @param value A probably-hopefully double string value, with an optional plus/minus sign.
	 *
	 * @return Optional - parsed Double if all string characters are digits, with an optional sign and decimal point. Empty optional for empty string,
	 *         null, floats, alpha characters, etc.
	 */
	private static Double parseDouble( String value ) {
		if ( value == null )
			return null;
		if ( NumberUtils.isCreatable( value ) ) {
			try {
				return Double.parseDouble( value );
			} catch ( Exception e ) {
				return null;
			}
			// test for fractions
		}

		return null;
	}

}
