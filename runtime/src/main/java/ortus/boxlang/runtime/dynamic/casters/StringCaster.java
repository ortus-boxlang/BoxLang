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
import java.text.DecimalFormat;

/**
 * I handle casting anything to a string
 */
public class StringCaster {

	/**
	 * Tests to see if the value can be cast to a string.
	 * Returns a CastAttempt<T> which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param value The value to cast to a string
	 * 
	 * @return The string value
	 */
	public static CastAttempt<String> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Used to cast anything to a string, throwing exception if we fail
	 *
	 * @param value The value to cast to a string
	 * 
	 * @return The string value
	 */
	public static String cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Used to cast anything to a string
	 *
	 * @param value The value to cast to a string
	 * @param fail  True to throw exception when failing.
	 * 
	 * @return The String value
	 */
	public static String cast( Object object, Boolean fail ) {
		if ( object == null ) {
			return "";
		}
		if ( object instanceof BigDecimal || object instanceof Float ) {
			String result = object.toString();
			if ( result.endsWith( ".0" ) ) {
				return result.substring( 0, result.length() - 2 );
			}
			return result;
		}
		if ( object instanceof Double ) {
			double	dObject	= ( Double ) object;
			long	lObject	= ( long ) dObject;
			if ( dObject == lObject || Math.abs( dObject - lObject ) < 0.000000000001 ) {
				return new DecimalFormat( "#.############" ).format( object );
			}
		}
		if ( object instanceof Number ) {
			return object.toString();
		}

		if ( object instanceof byte[] ) {
			return new String( ( byte[] ) object );
		}
		// TODO: Figure out which types need specific casting
		// For any casting failures, return null if the fail param is set to false!!
		return object.toString();
	}

}
