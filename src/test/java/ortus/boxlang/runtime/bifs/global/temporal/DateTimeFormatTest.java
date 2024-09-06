
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
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.DateTime;

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
		       setTimezone( "America/Los_Angeles" );
		          result2 = dateTimeFormat( ref, "v" );
		          resultHours = dateTimeFormat( ref, "HH" );
		          """,
		    context );
		DateTime	dateRef	= variables.getAsDateTime( Key.of( "ref" ) );
		String		result1	= variables.getAsString( Key.of( "result1" ) );
		String		result2	= variables.getAsString( Key.of( "result2" ) );
		assertNotEquals( result1, result2 );
		assertNotEquals( variables.getAsString( Key.of( "resultHours" ) ), StringCaster.cast( dateRef.getWrapped().getHour() ) );
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

}
