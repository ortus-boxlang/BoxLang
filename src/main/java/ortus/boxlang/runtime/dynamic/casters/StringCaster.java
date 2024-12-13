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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.XML;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;
import ortus.boxlang.runtime.util.FileSystemUtil;

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
		Charset charset = null;
		if ( encoding != null ) {
			charset = Charset.forName( encoding );
		}
		if ( object instanceof Key key ) {
			return key.getName();
		}
		if ( object instanceof String str ) {
			return str;
		}
		if ( object instanceof InputStream is ) {
			try (
			    BOMInputStream inputStream = BOMInputStream.builder()
			        .setInputStream( is )
			        .setByteOrderMarks( ByteOrderMark.UTF_8, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_32BE,
			            ByteOrderMark.UTF_32LE )
			        .setInclude( false )
			        .get() ) {
				InputStreamReader inputReader = null;
				if ( charset != null ) {
					inputReader = new InputStreamReader( inputStream, charset );
				} else {
					inputReader = new InputStreamReader( inputStream );
				}
				try ( BufferedReader reader = new BufferedReader( inputReader ) ) {
					return reader.lines().collect( Collectors.joining( FileSystemUtil.LINE_SEPARATOR ) );
				}

			} catch ( Exception e ) {
				throw new BoxCastException( "Failed to read input stream as a string.", e );
			}
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

		// Date classes

		// This check needs to run BEFORE the next one since a java.sql.Date IS a java.util.Date, but the toInstance() method will throw an unchecked exception
		if ( object instanceof java.sql.Date sDate ) {
			return sDate.toString();
		}
		if ( object instanceof java.util.Date date ) {
			return date.toString();
		}
		if ( object instanceof Instant instant ) {
			return instant.toString();
		}
		if ( object instanceof DateTime dt ) {
			return dt.toString();
		}
		if ( object instanceof Locale lc ) {
			return lc.toString();
		}
		if ( object instanceof ZoneId castedZone ) {
			return castedZone.getId();
		}
		if ( object instanceof LocalTime targetTimestamp ) {
			return targetTimestamp.toString();
		}
		if ( object instanceof java.time.LocalDateTime targetLocalDateTime ) {
			return targetLocalDateTime.toString();
		}
		if ( object instanceof java.time.LocalDate targetLocalDate ) {
			return targetLocalDate.toString();
		}
		if ( object instanceof java.sql.Timestamp targetTimestamp ) {
			return targetTimestamp.toString();
		}
		if ( object instanceof java.time.ZonedDateTime targetZonedDateTime ) {
			return targetZonedDateTime.toString();
		}
		if ( object instanceof java.util.Calendar targetCalendar ) {
			return targetCalendar.getTime().toString();
		}
		// End date classes

		if ( object instanceof java.util.UUID uuid ) {
			return uuid.toString();
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
			if ( charset != null ) {
				return new String( b, charset );
			} else {
				return new String( b );
			}
		}

		if ( object instanceof XML xml ) {
			return xml.asString();
		}
		if ( object instanceof URI uri ) {
			return uri.toString();
		}
		if ( object instanceof URL url ) {
			return url.toString();
		}

		if ( object instanceof InetSocketAddress inet ) {
			return inet.toString();
		}

		// Do we throw?
		if ( fail ) {
			throw new BoxCastException( "Can't cast " + object.getClass().getName() + " to a string." );
		}

		return null;
	}

}
