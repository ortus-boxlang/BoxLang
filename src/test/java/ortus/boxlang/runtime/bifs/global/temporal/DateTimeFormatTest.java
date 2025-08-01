
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

package ortus.boxlang.runtime.bifs.global.temporal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.util.LocalizationUtil;

public class DateTimeFormatTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It tests the BIF DateFormat" )
	@Test
	public void testDateFormatBif() {
		String result = null;
		// Default Format
		instance.executeSource(
		    """
		    ref = createDate( 2023, 12, 31 );
		       result = dateFormat( ref );
		       """,
		    context );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "31-Dec-23" );
		// Custom Format
		instance.executeSource(
		    """
		    ref = createDate( 2023, 12, 31 );
		       result = dateFormat( ref, "yyyy-MM-dd" );
		       """,
		    context );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "2023-12-31" );
	}

	@DisplayName( "It tests the BIF DateFormat with the common format masks" )
	@Test
	public void testDateFormatCommonMasks() {
		String				result		= null;
		DateTime			refDate		= new DateTime( ZoneId.of( "UTC" ) );
		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "longDate" );
		String				refResult	= refDate.format( formatter );
		variables.put( Key.of( "refDate" ), refDate );
		instance.executeSource(
		    """
		    setTimezone( "UTC" );
		       result = dateFormat( refDate, "long" );
		       """,
		    context );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( refResult );

		formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "ISODate" );
		refResult	= refDate.format( formatter );
		instance.executeSource(
		    """
		    setTimezone( "UTC" );
		      result = dateFormat( refDate, "iso" );
		      """,
		    context );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( refResult );

		formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "ISO8601Date" );
		refResult	= refDate.format( formatter );
		instance.executeSource(
		    """
		    setTimezone( "UTC" );
		      result = dateFormat( refDate, "iso8601" );
		      """,
		    context );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( refResult );

		formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "shortDate" );
		refResult	= refDate.format( formatter );
		instance.executeSource(
		    """
		    setTimezone( "UTC" );
		      result = dateFormat( refDate, "short" );
		      """,
		    context );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( refResult );

	}

	@DisplayName( "It tests the output of the format will change with a timezone change" )
	@Test
	public void testDateTimeFormatTZChange() {
		instance.executeSource(
		    """
		       setTimezone( "America/New_York" );
		       ref = now();
		             result1 = dateTimeFormat( ref, "v" );
		    	  result1Hours = dateTimeFormat( ref, "HH" );
		    setTimezone( "America/Los_Angeles" );

		             result2 = dateTimeFormat( ref, "v" );
		             result2Hours = dateTimeFormat( ref, "HH" );
		             """,
		    context );
		DateTime	dateRef	= variables.getAsDateTime( Key.of( "ref" ) );
		String		result1	= variables.getAsString( Key.of( "result1" ) );
		String		result2	= variables.getAsString( Key.of( "result2" ) );
		assertNotEquals( result1, result2 );
		assertNotEquals( variables.getAsString( Key.of( "result2Hours" ) ), variables.getAsString( Key.of( "result1Hours" ) ) );
	}

	@DisplayName( "It tests the BIF will retain locale awareness" )
	@Test
	public void testLocaleAwareness() {
		instance.executeSource(
		    """
		    setLocale(  "de-DE" );
		    result = dateFormat( now(), "long" );
		            """,
		    context );
		System.out.println( variables.getAsString( result ) );
	}

	@DisplayName( "It tests the BIF DateTimeFormat" )
	@Test
	public void testDateTimeFormatBif() {
		String result = null;
		// Default Format
		instance.executeSource(
		    """
		    setTimezone( "UTC" );
		       ref = createDateTime( 2023, 12, 31, 12, 30, 30, 0, "UTC" );
		          result = dateTimeFormat( ref );
		          """,
		    context );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "31-Dec-2023 12:30:30" );
		// Custom Format
		instance.executeSource(
		    """
		    setTimezone( "UTC" );
		         ref = createDateTime( 2023, 12, 31, 12, 30, 30, 0, "UTC" );
		      result = dateTimeFormat( ref, "yyyy-MM-dd'T'HH:mm:ssXXX" );
		      """,
		    context );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "2023-12-31T12:30:30Z" );
	}

	@DisplayName( "It tests the BIF DateTimeFormat with the common format masks" )
	@Test
	public void testDateTimeFormatCommonMasks() {
		String				result		= null;
		DateTime			refDate		= new DateTime();
		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "longDateTime" );
		String				refResult	= refDate.format( formatter );
		variables.put( Key.of( "refDate" ), refDate );
		instance.executeSource(
		    """
		    result = dateTimeFormat( refDate, "long" );
		    """,
		    context );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( refResult );

		formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "ISODateTime" );
		refResult	= refDate.format( formatter );
		instance.executeSource(
		    """
		    result = dateTimeFormat( refDate, "iso" );
		    """,
		    context );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( refResult );

		formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "ISO8601DateTime" );
		refResult	= refDate.format( formatter );
		instance.executeSource(
		    """
		    result = dateTimeFormat( refDate, "iso8601" );
		    """,
		    context );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( refResult );

		formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "shortDateTime" );
		refResult	= refDate.format( formatter );
		instance.executeSource(
		    """
		    result = dateTimeFormat( refDate, "short" );
		    """,
		    context );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( refResult );

		formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "mediumDateTime" );
		refResult	= refDate.format( formatter );
		instance.executeSource(
		    """
		    result = dateTimeFormat( refDate, "medium" );
		    """,
		    context );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( refResult );

		Long refEpoch = refDate.toEpoch();
		instance.executeSource(
		    """
		    result = dateTimeFormat( refDate, "epoch" );
		    """,
		    context );
		assertThat( LongCaster.cast( variables.getAsLong( Key.of( "result" ) ) ) ).isEqualTo( refEpoch );

		refEpoch = refDate.toEpochMillis();
		instance.executeSource(
		    """
		    result = dateTimeFormat( refDate, "epochms" );
		    """,
		    context );
		assertThat( LongCaster.cast( variables.getAsLong( Key.of( "result" ) ) ) ).isEqualTo( refEpoch );
	}

	@DisplayName( "It tests the BIF TimeFormat" )
	@Test
	public void testTimeFormatBif() {
		String result = null;
		// Default Format
		instance.executeSource(
		    """
		    setTimezone( "UTC" );
		       ref = createDateTime( 2023, 12, 31, 12, 30, 30, 0 );
		          result = timeFormat( ref );
		          """,
		    context );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "12:30 PM" );
		// PM times
		instance.executeSource(
		    """
		    setTimezone( "UTC" );
		       ref = createDateTime( 2023, 12, 31, 13, 30, 30, 0 );
		    	  result = timeFormat( ref );
		    	  """,
		    context );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "01:30 PM" );
		// Custom Format
		instance.executeSource(
		    """
		    setTimezone( "UTC" );
		       ref = createDateTime( 2023, 12, 31, 12, 30, 30, 0, "UTC" );
		          result = timeFormat( ref, "HH:mm:ssXXX" );
		          """,
		    context );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "12:30:30Z" );

		instance.executeSource(
		    """
		    setTimezone( "UTC" );
		       ref = createDateTime( 2023, 12, 31, 12, 30, 30, 999, "UTC" );
		          result = timeFormat( ref, "HH:mm:ss.SSS" );
		          """,
		    context );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "12:30:30.999" );

	}

	@DisplayName( "It tests the BIF TimeFormat with the common format masks" )
	@Test
	public void testTimeFormatCommonMasks() {
		String				result		= null;
		DateTime			refTime		= new DateTime();
		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "longTime" );
		String				refResult	= refTime.format( formatter );
		variables.put( Key.of( "refTime" ), refTime );
		instance.executeSource(
		    """
		    result = timeFormat( refTime, "long" );
		    """,
		    context );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( refResult );

		formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "ISOTime" );
		refResult	= refTime.format( formatter );
		instance.executeSource(
		    """
		    result = timeFormat( refTime, "iso" );
		    """,
		    context );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( refResult );

		formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "ISO8601Time" );
		refResult	= refTime.format( formatter );
		instance.executeSource(
		    """
		    result = timeFormat( refTime, "iso8601" );
		    """,
		    context );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( refResult );

		formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "shortTime" );
		refResult	= refTime.format( formatter );
		instance.executeSource(
		    """
		    result = timeFormat( refTime, "short" );
		    """,
		    context );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( refResult );

	}

	@DisplayName( "It tests the BIF TimeFormat with string times" )
	@Test
	public void testTimeFormatString() {
		String result = null;
		// Default Format
		instance.executeSource(
		    """
		      setTimezone( "UTC" );
		    parsedTime = parseDateTime( "11:00" );
		            result = timeFormat( "11:00" , "hh:mm a" );
		            """,
		    context );
		DateTime parsedTime = DateTimeCaster.cast( variables.get( Key.of( "parsedTime" ) ) );
		assertEquals( parsedTime.format( "hh:mm a" ), variables.getAsString( Key.of( "result" ) ) );
		assertEquals( ZoneId.of( "UTC" ), parsedTime.getZone() );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "11:00 AM" );

	}

	@DisplayName( "It tests the member function DateTime.format( mask, [timezone] )" )
	@Test
	public void testMemberFunction() {
		String result = null;
		// Default Format
		instance.executeSource(
		    """
		    setTimezone( "UTC" );
		      ref = createDateTime( 2023, 12, 31, 12, 30, 30, 0 );
		         result = ref.format();
		         """,
		    context );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "31-Dec-2023 12:30:30" );
		// Custom Format
		instance.executeSource(
		    """
		    setTimezone( "UTC" );
		      ref = createDateTime( 2023, 12, 31, 12, 30, 30, 0, "UTC" );
		         result = ref.format( "yyyy-MM-dd'T'HH:mm:ssXXX" );
		         """,
		    context );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "2023-12-31T12:30:30Z" );

	}

	@DisplayName( "It can use common formatters even when they contain extra spaces" )
	@Test
	public void testCommonFormattersExtraSpaces() {
		String result = null;
		// Default Format
		instance.executeSource(
		    """
		    setTimezone( "UTC" );
		      ref = createDateTime( 2023, 12, 31, 12, 30, 30, 0 );
		    	 result = ref.format( " short" );
		    	 """,
		    context );
		result = ( String ) variables.get( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "12/31/23, 12:30\u202FPM" );

	}

	/**
	 * Locale-aware tests
	 */

	@DisplayName( "It tests the BIF DateTimeFormat using a localized, Spanish long-form format" )
	@Test
	public void testLocalizedDateTimeFormatSpain() {
		DateTime dateRef = new DateTime( ZoneId.of( "UTC" ) );
		variables.put( Key.date, dateRef );
		instance.executeSource(
		    """
		    result = DateTimeFormat( date, "long", "UTC", "es-ES" );
		    """,
		    context );
		String				result		= variables.getAsString( Key.of( "result" ) );
		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "longDateTime" );
		assertEquals( result, dateRef.format( formatter.withLocale( LocalizationUtil.parseLocale( "es-ES" ) ) ) );
	}

	@DisplayName( "It tests the Member function DateTime.DateTimeFormat" )
	@Test
	public void testLocalizedDateTimeFormatMember() {
		DateTime dateRef = new DateTime( ZoneId.of( "UTC" ) );
		variables.put( Key.date, dateRef );
		instance.executeSource(
		    """
		    result = date.DateTimeFormat( "long", "UTC", "es-ES" );
		    """,
		    context );
		String				result		= variables.getAsString( Key.of( "result" ) );
		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "longDateTime" );
		assertEquals( result, dateRef.format( formatter.withLocale( LocalizationUtil.parseLocale( "es-ES" ) ) ) );
	}

	@DisplayName( "It tests the Member function DateTime.DateTimeFormat with the common JS toString format" )
	@Test
	public void testJsToStringDateTimeFormatMember() {
		DateTime dateRef = new DateTime( ZoneId.of( "UTC" ) );
		variables.put( Key.date, dateRef );
		instance.executeSource(
		    """
		    result = date.DateTimeFormat( "javascript", "UTC", "en-US" );
		    """,
		    context );
		String				result		= variables.getAsString( Key.of( "result" ) );
		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "javascriptDateTime" );
		assertEquals( result, dateRef.format( formatter.withLocale( LocalizationUtil.parseLocale( "en-US" ) ) ) );
	}

	@DisplayName( "It tests the Member function String.DateTimeFormat" )
	@Test
	public void testLocalizedDateTimeFormatStringMember() {
		DateTime dateRef = DateTimeCaster.cast( "2025-01-01T12:00:00.000Z" );
		instance.executeSource(
		    """
		    setTimeZone( "Z" );
		       result = "2025-01-01T12:00:00.000Z".DateTimeFormat( format="long", locale="en-US" );
		       """,
		    context );
		String				result		= variables.getAsString( Key.of( "result" ) );
		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "longDateTime" );
		assertEquals( dateRef.format( formatter.withLocale( LocalizationUtil.parseLocale( "en-US" ) ) ), result );
	}

	@DisplayName( "It tests the BIF DateTimeFormat using a traditional chinese long-form format" )
	@Test
	public void testLocalizedDateTimeFormatChina() {
		DateTime dateRef = new DateTime( ZoneId.of( "UTC" ) );
		variables.put( Key.date, dateRef );
		instance.executeSource(
		    """
		    result = DateTimeFormat( date, "long", "UTC", "zh-CN" );
		    """,
		    context );
		String				result		= variables.getAsString( Key.of( "result" ) );
		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "longDateTime" );
		assertEquals( result, dateRef.format( formatter.withLocale( LocalizationUtil.parseLocale( "zh-CN" ) ) ) );
	}

	@DisplayName( "It tests the BIF DateFormat using a localized, Spanish long-form format" )
	@Test
	public void testLocalizedDateFormatSpain() {
		DateTime dateRef = new DateTime( ZoneId.of( "UTC" ) );
		variables.put( Key.date, dateRef );
		instance.executeSource(
		    """
		    result = DateFormat( date, "long", "UTC", "es-ES" );
		    """,
		    context );
		String				result		= variables.getAsString( Key.of( "result" ) );
		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "longDate" );
		assertEquals( result, dateRef.format( formatter.withLocale( LocalizationUtil.parseLocale( "es-ES" ) ) ) );
	}

	@DisplayName( "It tests the Member function DateTime.DateFormat" )
	@Test
	public void testLocalizedDateFormatMember() {
		DateTime dateRef = new DateTime( ZoneId.of( "UTC" ) );
		variables.put( Key.date, dateRef );
		instance.executeSource(
		    """
		    result = date.DateFormat( "long", "UTC", "es-ES" );
		    """,
		    context );
		String				result		= variables.getAsString( Key.of( "result" ) );
		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "longDate" );
		assertEquals( result, dateRef.format( formatter.withLocale( LocalizationUtil.parseLocale( "es-ES" ) ) ) );
	}

	@DisplayName( "It tests the Member function String.DateFormat" )
	@Test
	public void testLocalizedDateFormatStringMember() {
		DateTime dateRef = DateTimeCaster.cast( "2025-01-01T12:00:00.000Z" );
		instance.executeSource(
		    """
		    result = "2025-01-01T12:00:00.000Z".dateformat( format="long", locale="en-US" );
		    """,
		    context );
		String				result		= variables.getAsString( Key.of( "result" ) );
		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "longDate" );
		assertEquals( dateRef.format( formatter.withLocale( LocalizationUtil.parseLocale( "en-US" ) ) ), result );
	}

	@DisplayName( "It tests the BIF DateFormat using a traditional chinese long-form format" )
	@Test
	public void testLocalizedDateFormatChina() {
		DateTime dateRef = new DateTime( ZoneId.of( "UTC" ) );
		variables.put( Key.date, dateRef );
		instance.executeSource(
		    """
		    result = DateFormat( date, "long", "UTC", "zh-CN" );
		    """,
		    context );
		String				result		= variables.getAsString( Key.of( "result" ) );
		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "longDate" );
		assertEquals( result, dateRef.format( formatter.withLocale( LocalizationUtil.parseLocale( "zh-CN" ) ) ) );
	}

	@DisplayName( "It tests the BIF TimeFormat using a localized, Spanish long-form format" )
	@Test
	public void testLocalizedTimeFormatSpain() {
		DateTime timeRef = new DateTime( ZoneId.of( "UTC" ) );
		variables.put( Key.date, timeRef );
		instance.executeSource(
		    """
		    result = TimeFormat( date, "long", "UTC", "es-ES" );
		    """,
		    context );
		String				result		= variables.getAsString( Key.of( "result" ) );

		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "longTime" );
		assertEquals( result, timeRef.format( formatter.withLocale( LocalizationUtil.parseLocale( "es-ES" ) ) ) );
	}

	@DisplayName( "It tests the Member function DateTime.TimeFormat" )
	@Test
	public void testLocalizedTimeFormatMember() {
		DateTime timeRef = new DateTime( ZoneId.of( "UTC" ) );
		variables.put( Key.date, timeRef );
		instance.executeSource(
		    """
		    result = date.TimeFormat( "long", "UTC", "es-ES" );
		    """,
		    context );
		String				result		= variables.getAsString( Key.of( "result" ) );

		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "longTime" );
		assertEquals( result, timeRef.format( formatter.withLocale( LocalizationUtil.parseLocale( "es-ES" ) ) ) );
	}

	@DisplayName( "It tests the Member function String.TimeFormat" )
	@Test
	public void testLocalizedTimeFormatStringMember() {
		DateTime dateRef = DateTimeCaster.cast( "2025-01-01T12:00:00.000Z" );
		instance.executeSource(
		    """
		    setTimeZone( "Z" );
		       result = "2025-01-01T12:00:00.000Z".timeFormat( format="long", locale="en-US" );
		       """,
		    context );
		String				result		= variables.getAsString( Key.of( "result" ) );
		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "longTime" );
		assertEquals( dateRef.format( formatter.withLocale( LocalizationUtil.parseLocale( "en-US" ) ) ), result );
	}

	@DisplayName( "It tests the BIF TimeFormat using a traditional chinese long-form format" )
	@Test
	public void testLocalizedTimeFormatChina() {
		DateTime timeRef = new DateTime( ZoneId.of( "UTC" ) );
		variables.put( Key.date, timeRef );
		instance.executeSource(
		    """
		    result = TimeFormat( date, "long", "UTC", "zh-CN" );
		    """,
		    context );
		String				result		= variables.getAsString( Key.of( "result" ) );
		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "longTime" );
		System.out.println( result );
		assertEquals( result, timeRef.format( formatter.withLocale( LocalizationUtil.parseLocale( "zh-CN" ) ) ) );
	}

}
