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
import java.math.BigInteger;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Locale;

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.XML;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;

/**
 * I handle casting anything to a string
 */
public class StringCaster implements IBoxCaster {

	/**
	 * Tests to see if the value can be cast to a string.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast to a string
	 *
	 * @return The string value
	 */
	public static CastAttempt<String> attempt( Object object ) {
		return attempt( object, null );
	}

	/**
	 * Tests to see if the value can be cast to a string.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast to a string
	 *
	 * @return The string value
	 */
	public static CastAttempt<String> attempt( Object object, String encoding ) {
		return CastAttempt.ofNullable( cast( object, encoding, false ) );
	}

	/**
	 * Used to cast anything to a string, throwing exception if we fail
	 *
	 * @param object The value to cast to a string
	 *
	 * @return The string value
	 */
	public static String cast( Object object, String encoding ) {
		return cast( object, encoding, true );
	}

	/**
	 * Used to cast anything to a string, throwing exception if we fail
	 *
	 * @param object The value to cast to a string
	 *
	 * @return The string value
	 */
	public static String cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Used to cast anything to a string
	 *
	 * @param object The value to cast to a string
	 * @param fail   True to throw exception when failing.
	 *
	 * @return The String value
	 */
	public static String cast( Object object, Boolean fail ) {
		return cast( object, null, fail );
	}

	/**
	 * Used to cast anything to a string
	 *
	 * @param object The value to cast to a string
	 * @param fail   True to throw exception when failing.
	 *
	 * @return The String value
	 */
	public static String cast( Object object, String encoding, Boolean fail ) {
		if ( object == null ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast null to a string." );
			} else {
				return null;
			}
		}
		object = DynamicObject.unWrap( object );
		if ( object instanceof Key key ) {
			return key.getName();
		}
		if ( object instanceof String str ) {
			return str;
		}
		if ( object instanceof Boolean bool ) {
			return bool ? "true" : "false";
		}
		if ( object instanceof Character chr ) {
			return chr.toString();
		}
		if ( object instanceof Character[] ca ) {
			return Arrays.toString( ca );
		}
		if ( object instanceof Path path ) {
			return path.toString();
		}
		if ( object instanceof java.util.Date date ) {
			return new DateTime( date ).toString();
		}
		if ( object instanceof Instant instant ) {
			return new DateTime( instant ).toString();
		}
		if ( object instanceof DateTime dt ) {
			return dt.toString();
		}
		if ( object instanceof Locale lc ) {
			return lc.toString();
		}
		if ( object instanceof java.util.UUID uuid ) {
			return uuid.toString();
		}
		if ( object instanceof ZoneId castedZone ) {
			return castedZone.getId();
		}
		if ( object instanceof StringBuilder sb ) {
			return sb.toString();
		}
		if ( object instanceof StringBuffer sb ) {
			return sb.toString();
		}
		if ( object instanceof Integer || object instanceof Long || object instanceof Short || object instanceof Byte || object instanceof BigInteger ) {
			return object.toString();
		}
		if ( object instanceof BigDecimal || object instanceof Float || object instanceof Double ) {
			String result = object.toString();
			if ( result.endsWith( ".0" ) ) {
				return result.substring( 0, result.length() - 2 );
			}
			return result;
		}
		if ( object instanceof Number ) {
			return object.toString();
		}
		if ( object instanceof byte[] b ) {
			if ( encoding != null && !encoding.isEmpty() ) {
				return new String( b, java.nio.charset.Charset.forName( encoding ) );
			} else {
				return new String( b );
			}
		}
		if ( object instanceof XML xml ) {
			return xml.asString();
		}

		// Do we throw?
		if ( fail ) {
			throw new BoxCastException( "Can't cast " + object.getClass().getName() + " to a string." );
		}

		return null;
	}

}
