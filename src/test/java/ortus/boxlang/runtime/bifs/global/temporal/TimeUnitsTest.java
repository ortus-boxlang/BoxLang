
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

import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;
import java.time.temporal.WeekFields;
import java.util.Locale;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.util.LocalizationUtil;

public class TimeUnitsTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );
	static Locale		locale;

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
		locale		= LocalizationUtil.parseLocaleFromContext( context, new ArgumentsScope() );
	}

	@DisplayName( "It tests the BIF Year" )
	@Test
	public void testBifYear() {
		Integer refYear = ZonedDateTime.now().getYear();
		instance.executeSource(
		    """
		    now = now();
		       result = year( now );
		       """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refYear );
	}

	@DisplayName( "It tests the DateTime Member function Year" )
	@Test
	public void testMemberYear() {
		Integer refYear = ZonedDateTime.now().getYear();
		instance.executeSource(
		    """
		    result = now().year();
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refYear );
	}

	@DisplayName( "It tests the String Member function Year" )
	@Test
	public void testMemberYearString() {
		instance.executeSource(
		    """
		    result = "2025-01-01".year();
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( 2025, result );
	}

	@DisplayName( "It tests the BIF Quarter" )
	@Test
	public void testBifQuarter() {
		Integer ref = ZonedDateTime.now().get( IsoFields.QUARTER_OF_YEAR );
		instance.executeSource(
		    """
		    result = Quarter( now() );
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, ref );
	}

	@DisplayName( "It tests the BIF Month" )
	@Test
	public void testBifMonth() {
		Integer ref = ZonedDateTime.now().getMonth().getValue();
		instance.executeSource(
		    """
		    result = month( now() );
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, ref );
	}

	@DisplayName( "It tests the DateTime Member function Month" )
	@Test
	public void testMembefMonth() {
		Integer refMonth = ZonedDateTime.now().getMonth().getValue();
		instance.executeSource(
		    """
		    result = now().month();
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refMonth );
	}

	@DisplayName( "It tests the String Member function Month" )
	@Test
	public void testMemberMonthString() {
		instance.executeSource(
		    """
		    result = "2025-01-01".month();
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( 1, result );
	}

	@DisplayName( "It tests the BIF MonthAsString" )
	@Test
	public void testBifMonthAsString() {
		String refMonthAsString = new DateTime().format( "MMMM" );
		instance.executeSource(
		    """
		    now = now();
		       result = monthAsString( now );
		       """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertEquals( result, refMonthAsString );
	}

	@DisplayName( "It tests the member function DateTime.monthAsString" )
	@Test
	public void testMemberMonthAsString() {
		String refMonthAsString = new DateTime().format( "MMMM" );
		instance.executeSource(
		    """
		    now = now();
		       result = now.monthAsString();
		       """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertEquals( result, refMonthAsString );
	}

	@DisplayName( "It tests the BIF MonthShortAsString" )
	@Test
	public void testBifMonthShortAsString() {
		String refMonthShortAsString = new DateTime().format( "MMM" );
		instance.executeSource(
		    """
		    now = now();
		       result = monthShortAsString( now );
		       """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertEquals( result, refMonthShortAsString );
	}

	@DisplayName( "It tests the member function DateTime.monthShortAsString" )
	@Test
	public void testMemberMonthShortAsString() {
		String refMonthShortAsString = new DateTime().format( "MMM" );
		instance.executeSource(
		    """
		    now = now();
		       result = now.monthShortAsString();
		       """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertEquals( result, refMonthShortAsString );
	}

	@DisplayName( "It tests the BIF Day" )
	@Test
	public void testBifDay() {
		Integer refDay = ZonedDateTime.now().getDayOfMonth();
		instance.executeSource(
		    """
		    now = now();
		       result = day( now );
		       """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refDay );
	}

	@DisplayName( "It tests the DateTime Member function Day" )
	@Test
	public void testMemberDay() {
		Integer refDay = ZonedDateTime.now().getDayOfMonth();
		instance.executeSource(
		    """
		    result = now().day();
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refDay );
	}

	@DisplayName( "It tests the String Member function Day" )
	@Test
	public void testMemberDayString() {
		instance.executeSource(
		    """
		    result = "2025-01-01".Day();
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( 1, result );
	}

	@DisplayName( "It tests the BIF DaysInMonth" )
	@Test
	public void testBifDaysInMonth() {
		DateTime	dateRef			= new DateTime();
		Integer		refDaysInMonth	= dateRef.getWrapped().getMonth().length( dateRef.isLeapYear() );
		instance.executeSource(
		    """
		    now = now();
		       result = daysInMonth( now );
		       """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refDaysInMonth );
	}

	@DisplayName( "It tests the DateTime Member function DaysInMonth" )
	@Test
	public void testMemberDaysInMonth() {
		DateTime	dateRef			= new DateTime();
		Integer		refDaysInMonth	= dateRef.getWrapped().getMonth().length( dateRef.isLeapYear() );
		instance.executeSource(
		    """
		    result = now().DaysInMonth();
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refDaysInMonth );
	}

	@DisplayName( "It tests the BIF DaysInYear" )
	@Test
	public void testBifDaysInYear() {
		DateTime	dateRef			= new DateTime();
		Integer		refDaysInYear	= Year.of( dateRef.getWrapped().getYear() ).length();
		instance.executeSource(
		    """
		    now = now();
		       result = daysInYear( now );
		       """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refDaysInYear );
	}

	@DisplayName( "It tests the DateTime Member function DaysInYear" )
	@Test
	public void testMemberDaysInYear() {
		DateTime	dateRef			= new DateTime();
		Integer		refDaysInYear	= Year.of( dateRef.getWrapped().getYear() ).length();
		instance.executeSource(
		    """
		    result = now().DaysInYear();
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refDaysInYear );
	}

	@DisplayName( "It tests the BIF DayOfWeek with system Locale" )
	@Test
	public void testBifDayOfWeekWithSystemLocale() {
		Integer refDayOfWeek = ZonedDateTime.now().get( WeekFields.of( locale ).dayOfWeek() );
		instance.executeSource(
		    """
		    now = now();
		       result = dayOfWeek( now );
		       """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refDayOfWeek );
	}

	@DisplayName( "It tests the BIF DayOfWeek with parse date/time" )
	@Test
	public void testBifDayOfWeekWithParseDateTime() {
		// Integer refDayOfWeek = LocalDate.of( 2024, Month.APRIL, 7 ).getDayOfWeek().getValue();
		instance.executeSource(
		    """
		    now = parseDateTime( "2024-04-07" );
		       result = dayOfWeek( now );
		       """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( 1, result );
	}

	@DisplayName( "It tests the DateTime Member function DayOfWeek" )
	@Test
	public void testMemberDayOfWeek() {
		Integer refDayOfWeek = ZonedDateTime.now().get( WeekFields.of( locale ).dayOfWeek() );
		instance.executeSource(
		    """
		    result = now().dayOfWeek();
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refDayOfWeek );
	}

	@DisplayName( "It tests the BIF DayOfWeekAsString" )
	@Test
	public void testBifDayOfWeekAsString() {
		String refDayOfWeekAsString = new DateTime().format( "eeee" );
		instance.executeSource(
		    """
		    now = now();
		       result = dayOfWeekAsString( now );
		       """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertEquals( result, refDayOfWeekAsString );
	}

	@DisplayName( "It tests the DateTime Member function DayOfWeekAsString" )
	@Test
	public void testMemberDayOfWeekAsString() {
		String refDayOfWeekAsString = new DateTime().format( "eeee" );
		instance.executeSource(
		    """
		    result = now().dayOfWeekAsString();
		    """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertEquals( result, refDayOfWeekAsString );
	}

	@DisplayName( "It tests the BIF DayOfWeekShortAsString" )
	@Test
	public void testBifDayOfWeekShortAsString() {
		String refDayOfWeekShortAsString = new DateTime().format( "eee" );
		instance.executeSource(
		    """
		    now = now();
		       result = dayOfWeekShortAsString( now );
		       """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertEquals( result, refDayOfWeekShortAsString );
	}

	@DisplayName( "It tests the DateTime Member function DayOfWeekShortAsString" )
	@Test
	public void testMemberDayOfWeekShortAsString() {
		String refDayOfWeekShortAsString = new DateTime().format( "eee" );
		instance.executeSource(
		    """
		    result = now().dayOfWeekShortAsString();
		    """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertEquals( result, refDayOfWeekShortAsString );
	}

	@DisplayName( "It tests the BIF DayOfYear" )
	@Test
	public void testBifDayOfYear() {
		Integer refDayOfYear = ZonedDateTime.now().getDayOfYear();
		instance.executeSource(
		    """
		    now = now();
		       result = dayOfYear( now );
		       """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refDayOfYear );
	}

	@DisplayName( "It tests the DateTime Member function DayOfYear" )
	@Test
	public void testMemberDayOfYear() {
		Integer refDayOfYear = ZonedDateTime.now().getDayOfYear();
		instance.executeSource(
		    """
		    result = now().dayOfYear();
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refDayOfYear );
	}

	@DisplayName( "It tests the BIF FirstDayOfMonth" )
	@Test
	public void testBifFirstDayOfMonth() {
		DateTime	dateRef				= new DateTime();
		Integer		refFirstDayOfMonth	= dateRef.getWrapped().withDayOfMonth( ( int ) 1 ).getDayOfYear();
		instance.executeSource(
		    """
		    now = now();
		       result = FirstDayOfMonth( now );
		       """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refFirstDayOfMonth );
	}

	@DisplayName( "It tests the DateTime Member function FirstDayOfMonth" )
	@Test
	public void testMemberFirstDayOfMonth() {
		DateTime	dateRef				= new DateTime();
		Integer		refFirstDayOfMonth	= dateRef.getWrapped().withDayOfMonth( ( int ) 1 ).getDayOfYear();
		instance.executeSource(
		    """
		    result = now().FirstDayOfMonth();
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refFirstDayOfMonth );
	}

	@DisplayName( "It tests the BIF Week" )
	@Test
	public void testBifWeek() {
		Integer refWeekOfYear = ZonedDateTime.now().get( WeekFields.of( instance.getConfiguration().locale ).weekOfWeekBasedYear() );
		instance.executeSource(
		    """
		    now = now();
		       result = week( now );
		       """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refWeekOfYear );
	}

	@DisplayName( "It tests the DateTime Member function Week" )
	@Test
	public void testMemberWeek() {
		Integer refWeekOfYear = ZonedDateTime.now().get( WeekFields.of( instance.getConfiguration().locale ).weekOfWeekBasedYear() );
		instance.executeSource(
		    """
		    result = now().week();
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refWeekOfYear );
	}

	@DisplayName( "It tests the BIF Hour" )
	@Test
	public void testBifHour() {
		Integer refHour = ZonedDateTime.now().getHour();
		instance.executeSource(
		    """
		    now = now();
		       result = hour( now );
		       """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refHour );
	}

	@DisplayName( "It tests the DateTime Member function Hour" )
	@Test
	public void testMemberHour() {
		Integer refHour = ZonedDateTime.now().getHour();
		instance.executeSource(
		    """
		    result = now().hour();
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refHour );
	}

	@DisplayName( "It tests the BIF Minute" )
	@Test
	public void testBifMinute() {
		Integer refMinute = ZonedDateTime.now().getMinute();
		instance.executeSource(
		    """
		    now = now();
		       result = minute( now );
		       """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertThat( result ).isAtLeast( refMinute );
	}

	@DisplayName( "It tests the DateTime Member function Minute" )
	@Test
	public void testMemberMinute() {
		Integer refMinute = ZonedDateTime.now().getMinute();
		instance.executeSource(
		    """
		    result = now().minute();
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertThat( result ).isAtLeast( refMinute );
	}

	@DisplayName( "It tests the BIF Second" )
	@Test
	public void testBifSecond() {
		DateTime	ref			= new DateTime();
		Integer		refSecond	= ref.getWrapped().getSecond();
		variables.put( Key.of( "date" ), ref );
		instance.executeSource(
		    """
		    result = second( date );
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertThat( result ).isAtLeast( refSecond );
	}

	@DisplayName( "It tests the DateTime Member function Second" )
	@Test
	public void testMemberSecond() {
		DateTime	ref			= new DateTime();
		Integer		refSecond	= ref.getWrapped().getSecond();
		variables.put( Key.of( "date" ), ref );
		instance.executeSource(
		    """
		    result = date.second();
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertThat( result ).isAtLeast( refSecond );
	}

	@DisplayName( "It tests the BIF Millisecond" )
	@Test
	public void testBifMillisecond() {
		DateTime	ref				= new DateTime();
		Integer		refMillisecond	= ref.getWrapped().getNano() / 1000000;
		variables.put( Key.of( "date" ), ref );
		instance.executeSource(
		    """
		    result = millisecond( date );
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refMillisecond );
	}

	@DisplayName( "It tests the DateTime Member function Millisecond" )
	@Test
	public void testMemberMillisecond() {
		DateTime	ref				= new DateTime();
		Integer		refMillisecond	= ref.getWrapped().getNano() / 1000000;
		variables.put( Key.of( "date" ), ref );
		instance.executeSource(
		    """
		    result = date.millisecond();
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refMillisecond );
	}

	@DisplayName( "It tests the BIF Nanosecond" )
	@Test
	public void testBifNanosecond() {
		DateTime	ref				= new DateTime();
		Integer		refNanosecond	= ref.getWrapped().getNano();
		variables.put( Key.of( "date" ), ref );
		instance.executeSource(
		    """
		    result = Nanosecond( date );
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refNanosecond );
	}

	@DisplayName( "It tests the DateTime Member function Nanosecond" )
	@Test
	public void testMemberNanosecond() {
		DateTime	ref				= new DateTime();
		Integer		refNanosecond	= ref.getWrapped().getNano();
		variables.put( Key.of( "date" ), ref );
		instance.executeSource(
		    """
		    result = date.Nanosecond();
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refNanosecond );
	}

	@DisplayName( "It tests the BIF Offset" )
	@Test
	public void testBifOffset() {
		String refOffset = new DateTime().format( "xxxx" );
		instance.executeSource(
		    """
		    now = now();
		       result = offset( now );
		       """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertEquals( result, refOffset );
	}

	@DisplayName( "It tests the BIF Offset with a timezone argument" )
	@Test
	public void testBifOffsetTZ() {
		String refOffset = new DateTime( ZoneId.of( "America/Los_Angeles" ) ).format( "xxxx" );
		instance.executeSource(
		    """
		    now = now();
		       result = offset( now, "America/Los_Angeles" );
		       """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertEquals( result, refOffset );
	}

	@DisplayName( "It tests the DateTime Member function Offset" )
	@Test
	public void testMemberOffset() {
		String refOffset = new DateTime().format( "xxxx" );
		instance.executeSource(
		    """
		    result = now().offset();
		    """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertEquals( result, refOffset );
	}

	@DisplayName( "It tests the BIF GetTimeZone" )
	@Test
	public void testBifGetTimeZone() {
		String refTimeZone = new DateTime( LocalizationUtil.parseZoneId( null, context ) ).format( "v" );
		instance.executeSource(
		    """
		    now = now();
		       result = GetTimeZone( now );
		       """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertEquals( result, refTimeZone );
		instance.executeSource(
		    """
		    result = GetTimeZone();
		    """,
		    context );
		result = variables.getAsString( Key.of( "result" ) );
		assertEquals( result, refTimeZone );
	}

	@DisplayName( "It tests the BIF GetTimeZone with a specified timezone" )
	@Test
	public void testBifGetTimeZoneWithTZ() {
		String refTimeZone = new DateTime( ZoneId.of( "America/Los_Angeles" ) ).format( "v" );
		instance.executeSource(
		    """
		    now = now();
		       result = GetTimeZone( now, "America/Los_Angeles" );
		       """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertEquals( result, refTimeZone );
	}

	@DisplayName( "It tests the DateTime Member function DateTime.timeZone" )
	@Test
	public void testMemberGetTimeZone() {
		String refTimeZone = new DateTime( LocalizationUtil.parseZoneId( null, context ) ).format( "v" );
		instance.executeSource(
		    """
		    result = now().timeZone();
		    """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertEquals( result, refTimeZone );
	}

	@DisplayName( "It tests the BIF GetNumericDate" )
	@Test
	public void testBifGetNumericDays() {
		DateTime	refDate			= new DateTime().setTimezone( "UTC" );
		Double		refNumericDate	= refDate.toEpochMillis().doubleValue() / LongCaster.cast( 86400000l ).doubleValue();
		variables.put( Key.of( "date" ), refDate );
		instance.executeSource(
		    """
		    result = GetNumericDate( date );
		    """,
		    context );
		Double result = variables.getAsDouble( Key.of( "result" ) );
		assertEquals( result, refNumericDate );

		instance.executeSource(
		    """
		    result = GetNumericDate( "2018-01-01T12:00:00" );
		    """,
		    context );
		result = variables.getAsDouble( Key.of( "result" ) );
		assertEquals( 17532.5d, result );

	}

	@DisplayName( "It tests the member function getTime" )
	@Test
	public void testMemberGetTime() {
		DateTime	refDate	= new DateTime( LocalizationUtil.parseZoneId( null, context ) );
		Long		refTime	= refDate.toEpochMillis();
		variables.put( Key.of( "date" ), refDate );
		instance.executeSource(
		    """
		    result = date.getTime();
		    """,
		    context );
		Long result = variables.getAsLong( Key.of( "result" ) );
		assertEquals( result, refTime );
	}

	/**
	 * Localized tests
	 */

	@DisplayName( "It tests the BIF Week" )
	@Test
	public void testLocalizedWeek() {
		Locale	locale			= LocalizationUtil.parseLocale( "es-SV" );
		Integer	refWeekOfYear	= ZonedDateTime.now().get( WeekFields.of( locale ).weekOfWeekBasedYear() );
		instance.executeSource(
		    """
		    now = now();
		    result = Week( date=now, locale="es-SV" );
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refWeekOfYear );

	}

	@DisplayName( "It tests the member function DateTime.Week" )
	@Test
	public void testMemberLocalizedWeek() {
		Locale	locale			= LocalizationUtil.parseLocale( "es-SV" );
		Integer	refWeekOfYear	= ZonedDateTime.now().get( WeekFields.of( locale ).weekOfWeekBasedYear() );
		instance.executeSource(
		    """
		    now = now();
		    result = now.Week( locale="es-SV" );
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refWeekOfYear );

	}

	@DisplayName( "It tests the BIF DayOfWeek" )
	@Test
	public void testLocalizedDayOfWeek() {
		Locale	locale			= LocalizationUtil.parseLocale( "es-SV" );
		Integer	refWeekOfYear	= ZonedDateTime.now().get( WeekFields.of( locale ).dayOfWeek() );
		instance.executeSource(
		    """
		    now = now();
		    result = DayOfWeek( date=now, locale="es-SV" );
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refWeekOfYear );

	}

	@DisplayName( "It tests the Member function DateTime.DayOfWeek" )
	@Test
	public void testMemberLocalizedDayOfWeek() {
		Locale	locale			= LocalizationUtil.parseLocale( "es-SV" );
		Integer	refWeekOfYear	= ZonedDateTime.now().get( WeekFields.of( locale ).dayOfWeek() );
		instance.executeSource(
		    """
		    now = now();
		    result = now.DayOfWeek( locale="es-SV" );
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refWeekOfYear );

	}

	@DisplayName( "It tests the BIF Month on a date that could be interpreted with a different mask" )
	@Test
	public void testBifMonthWithAltMaskPossible() {
		instance.executeSource(
		    """
		    setLocale( "en_US" );
		       result = month( '7/4/2021' );
		       """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( 7, result );
	}
}
