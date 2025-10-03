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
package ortus.boxlang.runtime.types;

import static com.google.common.truth.Truth.assertThat;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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

public class DateTimeTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "Test Constructors" )
	@Test
	void testConstructors() {
		DateTime			defaultDateTime		= new DateTime();
		ZonedDateTime		referenceNow		= ZonedDateTime.of( LocalDateTime.now(), ZoneId.systemDefault() );
		DateTimeFormatter	referenceFormatter	= DateTimeFormatter.ISO_LOCAL_DATE;
		assertThat( defaultDateTime.setFormat( "yyyy-MM-dd" ).toString() ).isEqualTo( referenceFormatter.format( referenceNow ) );
		DateTime referenceZoned = new DateTime( referenceNow );
		assertThat( referenceZoned.setFormat( "yyyy-MM-dd" ).toString() ).isEqualTo( referenceFormatter.format( referenceNow ) );
		DateTime parsedDateTime = new DateTime( "2023-12-31 23:59:59", "yyyy-MM-dd HH:mm:ss" );
		assertThat( parsedDateTime.setFormat( "yyyy-MM-dd HH:mm:ss" ).toString() ).isEqualTo( "2023-12-31 23:59:59" );
		DateTime dateTimeFromParts = new DateTime( 2023, 12, 31, 12, 30, 30, 0, null );
		assertThat( dateTimeFromParts.setFormat( "yyyy-MM-dd HH:mm:ss" ).toString() ).isEqualTo( "2023-12-31 12:30:30" );
		DateTime dateFromParts = new DateTime( 2023, 12, 31 );
		assertThat( dateFromParts.setFormat( "yyyy-MM-dd HH:mm:ss" ).toString() ).isEqualTo( "2023-12-31 00:00:00" );
		DateTime dateFromSQLDate = new DateTime( Date.valueOf( "2023-12-31" ), context );
		assertThat( dateFromSQLDate.setFormat( "yyyy-MM-dd HH:mm:ss" ).toString() ).isEqualTo( "2023-12-31 00:00:00" );
		DateTime datefromSQLTime = new DateTime( Time.valueOf( "23:00:00" ) );
		assertThat( datefromSQLTime.setFormat( "yyyy-MM-dd HH:mm:ss" ).toString() ).isEqualTo( "1970-01-01 23:00:00" );
	}

	@DisplayName( "Tests various ISO formats in the constructor" )
	@Test
	void testVariousISOFormats() {
		DateTime reference = new DateTime( "2024-05-13T18:40:59.898284" );
		assertThat( reference.setFormat( "yyyy-MM-dd" ).toString() ).isEqualTo( "2024-05-13" );
		assertThat( reference.setFormat( "SSSSSS" ).toString() ).isEqualTo( "898284" );

		reference = new DateTime( "2024-05-13T18:40:59" );
		assertThat( reference.setFormat( "yyyy-MM-dd" ).toString() ).isEqualTo( "2024-05-13" );
		assertThat( reference.setFormat( "SSSSSS" ).toString() ).isEqualTo( "000000" );
	}

	@DisplayName( "Can create a DateTime with a specific timezone" )
	@Test
	void testTimezone() {
		DateTime defaultDateTime = new DateTime();
		assertThat( defaultDateTime.getWrapped().getZone() ).isEqualTo( ZoneId.systemDefault() );

		defaultDateTime = new DateTime( ZoneId.of( "UTC" ) );
		assertThat( defaultDateTime.getWrapped().getZone() ).isEqualTo( ZoneId.of( "UTC" ) );
	}

	@DisplayName( "Tests the default format method" )
	@Test
	void testDefaultFormat() {
		DateTime	defaultDateTime	= new DateTime();
		String		dateValue		= defaultDateTime.toString();
		assertThat( dateValue ).isEqualTo( defaultDateTime.setFormat( "'{ts '''yyyy-MM-dd HH:mm:ss'''}'" ).toString() );
	}

	@DisplayName( "Tests the setTimezone method" )
	@Test
	void testTimezoneMods() {
		DateTime	defaultDateTime	= new DateTime( "2023-12-31T00:00:00-06:00", "yyyy-MM-dd'T'HH:mm:ssXXX" );
		String		dateValue		= defaultDateTime.setFormat( "yyyy-MM-dd HH:mm:ss" ).toString();
		String		zonedValue		= defaultDateTime.toISOString();
		defaultDateTime.setTimezone( "UTC" );
		assertThat( dateValue ).isEqualTo( defaultDateTime.setFormat( "yyyy-MM-dd HH:mm:ss" ).toString() );
		assertThat( zonedValue ).isNotEqualTo( defaultDateTime.toISOString() );
	}

	@DisplayName( "Tests DateTime modifications" )
	@Test
	void testModifications() {
		DateTime	initialDateTime	= new DateTime( "2023-12-31 00:00:00", "yyyy-MM-dd HH:mm:ss" );
		Long		initialDay		= LongCaster.cast( initialDateTime.format( "d" ) );
		assertThat( initialDay ).isEqualTo( 31l );
		// Days
		assertThat( LongCaster.cast( initialDateTime.modify( "d", 1l ).format( "d" ) ) ).isEqualTo( 1l );
		assertThat( Long.signum( -1l ) ).isEqualTo( -1 );
		initialDateTime.modify( "d", -1l );
		assertThat( initialDay ).isEqualTo( LongCaster.cast( initialDateTime.format( "d" ) ) );
		// Years
		Long initialYear = LongCaster.cast( initialDateTime.format( "yyyy" ) );
		assertThat( LongCaster.cast( initialDateTime.modify( "yyyy", 1l ).format( "yyyy" ) ) ).isGreaterThan( initialYear );
		initialDateTime.modify( "yyyy", -1l );
		assertThat( initialYear ).isEqualTo( LongCaster.cast( initialDateTime.format( "yyyy" ) ) );
		// Months
		Long initialMonth = LongCaster.cast( initialDateTime.format( "M" ) );
		assertThat( LongCaster.cast( initialDateTime.modify( "m", 1l ).format( "M" ) ) ).isEqualTo( 1l );
		initialDateTime.modify( "m", -1l );
		assertThat( LongCaster.cast( initialDateTime.format( "M" ) ) ).isEqualTo( initialMonth );
		// Weeks
		Long initialWeek = LongCaster.cast( initialDateTime.format( "w" ) );
		// Our date is the first week in the new month
		assertThat( initialWeek ).isEqualTo( 1l );
		assertThat( LongCaster.cast( initialDateTime.modify( "ww", 1l ).format( "w" ) ) ).isEqualTo( 2l );
		initialDateTime.modify( "ww", -1l );
		assertThat( LongCaster.cast( initialDateTime.format( "w" ) ) ).isEqualTo( initialWeek );
		// Hours
		Long initialHour = LongCaster.cast( initialDateTime.format( "H" ) );
		assertThat( initialHour ).isEqualTo( 0l );
		assertThat( LongCaster.cast( initialDateTime.modify( "h", 1l ).format( "H" ) ) ).isEqualTo( 1l );
		initialDateTime.modify( "h", -1l );
		assertThat( LongCaster.cast( initialDateTime.format( "H" ) ) ).isEqualTo( initialHour );
		// Minutes
		Long initialMinute = LongCaster.cast( initialDateTime.format( "m" ) );
		assertThat( initialMinute ).isEqualTo( 0l );
		assertThat( LongCaster.cast( initialDateTime.modify( "n", 1l ).format( "m" ) ) ).isEqualTo( 1l );
		initialDateTime.modify( "n", -1l );
		assertThat( LongCaster.cast( initialDateTime.format( "m" ) ) ).isEqualTo( initialMinute );
		// Seconds
		Long initialSeconds = LongCaster.cast( initialDateTime.format( "s" ) );
		assertThat( initialSeconds ).isEqualTo( 0l );
		assertThat( LongCaster.cast( initialDateTime.modify( "s", 1l ).format( "s" ) ) ).isEqualTo( 1l );
		initialDateTime.modify( "s", -1l );
		assertThat( LongCaster.cast( initialDateTime.format( "s" ) ) ).isEqualTo( initialSeconds );
		// Milliseconds
		Long initialMillis = LongCaster.cast( initialDateTime.format( "A" ) );
		assertThat( initialMillis ).isEqualTo( 0l );
		assertThat( LongCaster.cast( initialDateTime.modify( "l", 1l ).format( "A" ) ) ).isEqualTo( 1l );
		initialDateTime.modify( "l", -1l );
		assertThat( LongCaster.cast( initialDateTime.format( "A" ) ) ).isEqualTo( initialMillis );
		// Week Day ( working )
		Long initialWeekDay = LongCaster.cast( initialDateTime.format( "e" ) );
		assertThat( LongCaster.cast( initialDateTime.modify( "w", 1l ).format( "e" ) ) ).isEqualTo( 2l );
		assertThat( Long.signum( -1l ) ).isEqualTo( -1 );
		initialDateTime.modify( "w", -1l );
		assertThat( initialWeekDay ).isEqualTo( LongCaster.cast( initialDateTime.format( "e" ) ) );
		// Quarter
		Long initialQuarter = LongCaster.cast( initialDateTime.format( "Q" ) );
		assertThat( LongCaster.cast( initialDateTime.modify( "q", 1l ).format( "Q" ) ) ).isEqualTo( 1l );
		assertThat( Long.signum( -1l ) ).isEqualTo( -1 );
		initialDateTime.modify( "q", -1l );
		assertThat( initialQuarter ).isEqualTo( LongCaster.cast( initialDateTime.format( "Q" ) ) );
	}

	@DisplayName( "Tests DateTime equality" )
	@Test
	void testEquality() {
		java.util.Date	epoch		= new java.util.Date( 0 );
		DateTime		dateTime1	= new DateTime( epoch, ZoneId.of( "UTC" ) );
		DateTime		dateTime2	= DateTimeCaster.cast( "1970-01-01T00:00:00Z" );
		assertThat( dateTime1.isEqual( dateTime2 ) ).isTrue();
		assertThat( dateTime1.equals( dateTime2 ) ).isTrue();
		assertThat( dateTime1.toISOString() ).isEqualTo( dateTime2.toISOString() );
		// @formatter:off
		instance.executeSource(
		"""
			utcBaseDate = createObject( 'java', 'java.util.Date' ).init( javacast( 'long', 0 ) );
			dateOne = dateAdd( 'l', 1567296000000, utcBaseDate );
			dateTwo = parseDateTime( '2019-09-01T00:00:00Z' );
			println( dateTimeFormat( dateOne, "iso8601" ) );
			println( dateFormat( dateTwo, "iso8601" ) );
		""", 
		context 
		);
		DateTime date1 = variables.getAsDateTime( Key.of( "dateOne" ) );
		DateTime date2 = variables.getAsDateTime( Key.of( "dateTwo" ) );
		assertThat( date1.toEpochMillis() ).isEqualTo( date2.toEpochMillis() );
		assertThat( date1.isEqual( date2 ) ).isTrue();
		assertThat( date1.equals( date2 ) ).isTrue();
		// @formatter:on
	}

	@DisplayName( "Test getTime() helper" )
	@Test
	void testGetTime() {
		DateTime defaultDateTime = new DateTime( "2023-12-31 00:00:00", "yyyy-MM-dd HH:mm:ss" );
		assertThat( defaultDateTime.getTime() ).isEqualTo( defaultDateTime.getWrapped().toInstant().toEpochMilli() );
	}

	@DisplayName( "Tests DateTime serialization and deserialization" )
	@Test
	void testSerializationDeserialization() {
		// Test that the original formatter value is maintained
		// @formatter:off
		instance.executeSource(
		"""
			obj = parseDateTime( "2023-12-31 00:00:00", "yyyy-MM-dd HH:mm:ss" );
			obj.setFormat( "yyyy-MM-dd HH:mm:ss" );
			a = objectSerialize( obj );
			result = objectDeserialize( a );
		""", 
		context 
		);
		assertThat( variables.get( Key.of( "result" ) ) ).isInstanceOf( DateTime.class );
		assertThat( variables.getAsDateTime( Key.of( "result" ) ).toString() ).isEqualTo( "2023-12-31 00:00:00" );
		// @formatter:on
	}

	@DisplayName( "Tests DateTime default format string results" )
	@Test
	void testConsistentToStringResult() {
		// Test that the original formatter value is maintained
		// @formatter:off
		instance.executeSource(
		"""
			result = parseDateTime( "2025-09-09T13:32:30Z" );       // {ts '2025-09-09 13:32:30'}
			result2 = parseDateTime( "2025-09-09T13:32:30-00:00" );  // 2025-09-09T13:32:30Z
			result3 = parseDateTime( "2025-09-09T13:32:30-06:00" );  // 2025-09-09T13:32:30-06:00
		""", 
		context 
		);
		assertThat( variables.get( Key.of( "result" ) ) ).isInstanceOf( DateTime.class );
		assertThat( variables.getAsDateTime( Key.of( "result" ) ).toString() ).isEqualTo( "{ts '2025-09-09 13:32:30'}" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isInstanceOf( DateTime.class );
		assertThat( variables.getAsDateTime( Key.of( "result2" ) ).toString() ).isEqualTo( "{ts '2025-09-09 13:32:30'}" );
		assertThat( variables.get( Key.of( "result3" ) ) ).isInstanceOf( DateTime.class );
		assertThat( variables.getAsDateTime( Key.of( "result3" ) ).toString() ).isEqualTo( "{ts '2025-09-09 13:32:30'}" );
		// @formatter:on
	}

}
