
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.DateTime;

public class TimeUnitsTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
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

	@DisplayName( "It tests the BIF DayOfWeek" )
	@Test
	public void testBifDayOfWeek() {
		Integer refDayOfWeek = ZonedDateTime.now().getDayOfWeek().getValue();
		instance.executeSource(
		    """
		    now = now();
		       result = dayOfWeek( now );
		       """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refDayOfWeek );

	}

	@DisplayName( "It tests the DateTime Member function DayOfWeek" )
	@Test
	public void testMemberDayOfWeek() {
		Integer refDayOfWeek = ZonedDateTime.now().getDayOfWeek().getValue();
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
		assertEquals( result, refMinute );

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
		assertEquals( result, refMinute );

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
		assertEquals( result, refSecond );

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
		assertEquals( result, refSecond );

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
		String refTimeZone = new DateTime().format( "v" );
		instance.executeSource(
		    """
		    now = now();
		       result = GetTimeZone( now );
		       """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
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
		String refTimeZone = new DateTime().format( "v" );
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
		DateTime	refDate			= new DateTime();
		Double		refNumericDate	= refDate.toEpochMillis().doubleValue() / LongCaster.cast( 86400000l ).doubleValue();
		System.out.println( refNumericDate );
		variables.put( Key.of( "date" ), refDate );
		instance.executeSource(
		    """
		    result = GetNumericDate( date );
		    """,
		    context );
		Double result = variables.getAsDouble( Key.of( "result" ) );
		assertEquals( result, refNumericDate );

	}

}
