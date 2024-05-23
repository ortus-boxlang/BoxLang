
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

import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.WeekFields;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.DateTime;

public class DatePartTest {

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

	@DisplayName( "It tests the DatePart Year" )
	@Test
	public void testDatePartYear() {
		Integer refYear = ZonedDateTime.now().getYear();
		instance.executeSource(
		    """
		    now = now();
		    result = datePart( "yyyy", now );
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refYear );

	}

	@DisplayName( "It tests the DatePart for Quarter" )
	@Test
	public void testDatePartQuarter() {
		Integer ref = ZonedDateTime.now().get( IsoFields.QUARTER_OF_YEAR );
		instance.executeSource(
		    """
		    result = DatePart( "q", now() );
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, ref );

	}

	@DisplayName( "It tests the DatePart for Month" )
	@Test
	public void testBifMonth() {
		Integer ref = ZonedDateTime.now().getMonth().getValue();
		instance.executeSource(
		    """
		    result = datePart( "m", now() );
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, ref );

	}

	@DisplayName( "It tests the DatePart for Day" )
	@Test
	public void testBifDay() {
		Integer refDay = ZonedDateTime.now().getDayOfMonth();
		instance.executeSource(
		    """
		    now = now();
		    result = datePart( "d", now );
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refDay );

	}

	@DisplayName( "It tests the DatePart for DayOfWeek" )
	@Test
	public void testBifDayOfWeek() {
		Integer refDayOfWeek = ZonedDateTime.now().get( WeekFields.of( instance.getConfiguration().runtime.locale ).dayOfWeek() );
		instance.executeSource(
		    """
		    now = now();
		    result = datePart( "w", now );
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refDayOfWeek );

	}

	@DisplayName( "It tests the DatePart for Week of year" )
	@Test
	public void testBifDayOfWeekAsString() {
		Integer refWeekOfYear = new DateTime().getWrapped().get( ChronoField.ALIGNED_WEEK_OF_YEAR );
		instance.executeSource(
		    """
		    now = now();
		    result = DatePart( "ww", now );
		    """,
		    context );
		Integer result = variables.getAsInteger( Key.of( "result" ) );
		assertEquals( result, refWeekOfYear );

	}

	@DisplayName( "It tests the DatePart for DayOfYear" )
	@Test
	public void testBifDayOfYear() {
		Integer refDayOfYear = ZonedDateTime.now().getDayOfYear();
		instance.executeSource(
		    """
		    now = now();
		    result = datePart( "y", now );
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refDayOfYear );

	}

	@DisplayName( "It tests the DatePart for Hour" )
	@Test
	public void testBifHour() {
		Integer refHour = ZonedDateTime.now().getHour();
		instance.executeSource(
		    """
		    now = now();
		    result = datePart( "h", now );
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refHour );

	}

	@DisplayName( "It tests the DatePart for Minute" )
	@Test
	public void testBifMinute() {
		DateTime	refDate		= new DateTime();
		Integer		refMinute	= refDate.getWrapped().getMinute();
		variables.put( Key.date, refDate );
		instance.executeSource(
		    """
		    result = datePart( "n", date );
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refMinute );

	}

	@DisplayName( "It tests the DatePart for Second" )
	@Test
	public void testBifSecond() {
		DateTime	ref			= new DateTime();
		Integer		refSecond	= ref.getWrapped().getSecond();
		variables.put( Key.of( "date" ), ref );
		instance.executeSource(
		    """
		    result = datePart( "s", date );
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refSecond );

	}

	@DisplayName( "It tests the DatePart for Millisecond" )
	@Test
	public void testBifMillisecond() {
		DateTime	ref				= new DateTime();
		Integer		refMillisecond	= ref.getWrapped().getNano() / 1000000;
		variables.put( Key.of( "date" ), ref );
		instance.executeSource(
		    """
		    result = datePart( "l", date );
		    """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refMillisecond );

	}

}
