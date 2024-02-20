
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

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Ignore;
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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class DateDiffTest {

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

	@DisplayName( "It tests the BIF DateDiff in Years" )
	@Test
	public void testDateDiffYears() {
		variables.put( "date1", new DateTime( "2024-01-20T00:00:00Z" ) );
		variables.put( "date2", new DateTime( "2023-01-20T00:00:00Z" ) );
		instance.executeSource(
		    """
		    result = dateDiff( "yyyy", date1, date2 );
		    """,
		    context );
		Long result = variables.getAsLong( Key.of( "result" ) );
		assertEquals( result, -1l );

	}

	@DisplayName( "It tests the member DateTime.diff in years" )
	@Test
	@Ignore
	public void testDateDiffMemberYears() {
		variables.put( "date1", new DateTime( "2024-01-20T00:00:00Z" ) );
		variables.put( "date2", new DateTime( "2023-01-20T00:00:00Z" ) );
		instance.executeSource(
		    """
		    result = date1.diff( "yyyy", date2 );
		    """,
		    context );
		Long result = variables.getAsLong( Key.of( "result" ) );
		assertEquals( result, -1l );

	}

	@DisplayName( "It tests the BIF DateDiff in Months" )
	@Test
	public void testDateDiffMonths() {
		variables.put( "date1", new DateTime( "2024-01-20T00:00:00Z" ) );
		variables.put( "date2", new DateTime( "2023-01-20T00:00:00Z" ) );
		instance.executeSource(
		    """
		    result = dateDiff( "m", date1, date2 );
		    """,
		    context );
		Long result = variables.getAsLong( Key.of( "result" ) );
		assertEquals( result, -12l );

	}

	@DisplayName( "It tests the member DateTime.diff in Months" )
	@Test
	@Ignore
	public void testDateDiffMemberMonths() {
		variables.put( "date1", new DateTime( "2024-01-20T00:00:00Z" ) );
		variables.put( "date2", new DateTime( "2023-01-20T00:00:00Z" ) );
		instance.executeSource(
		    """
		    result = date1.diff( "m", date2 );
		    """,
		    context );
		Long result = variables.getAsLong( Key.of( "result" ) );
		assertEquals( result, -12l );

	}

	@DisplayName( "It tests the BIF DateDiff in Days" )
	@Test
	public void testDateDiffDays() {
		variables.put( "date1", new DateTime( "2024-01-20T00:00:00Z" ) );
		variables.put( "date2", new DateTime( "2023-01-20T00:00:00Z" ) );
		instance.executeSource(
		    """
		    result = dateDiff( "d", date1, date2 );
		    """,
		    context );
		Long result = variables.getAsLong( Key.of( "result" ) );
		assertEquals( result, -365l );

	}

	@DisplayName( "It tests the member DateTime.diff in Days" )
	@Test
	@Ignore
	public void testDateDiffMemberDays() {
		variables.put( "date1", new DateTime( "2024-01-20T00:00:00Z" ) );
		variables.put( "date2", new DateTime( "2023-01-20T00:00:00Z" ) );
		instance.executeSource(
		    """
		    result = date1.diff( "d", date2 );
		    """,
		    context );
		Long result = variables.getAsLong( Key.of( "result" ) );
		assertEquals( result, -365l );

	}

	@DisplayName( "It tests the BIF DateDiff in WeekDays" )
	@Test
	public void testDateDiffWeekDays() {
		variables.put( "date1", new DateTime( "2024-01-20T00:00:00Z" ) );
		variables.put( "date2", new DateTime( "2024-01-15T00:00:00Z" ) );
		instance.executeSource(
		    """
		    result = dateDiff( "wd", date1, date2 );
		    """,
		    context );
		Long result = variables.getAsLong( Key.of( "result" ) );
		assertEquals( result, -5l );

	}

	@DisplayName( "It tests the member DateTime.diff in WeekDays" )
	@Test
	@Ignore
	public void testDateDiffMemberWeekDays() {
		variables.put( "date1", new DateTime( "2024-01-20T00:00:00Z" ) );
		variables.put( "date2", new DateTime( "2024-01-15T00:00:00Z" ) );
		instance.executeSource(
		    """
		    result = date1.diff( "wd", date2 );
		    """,
		    context );
		Long result = variables.getAsLong( Key.of( "result" ) );
		assertEquals( result, -5l );

	}

	@DisplayName( "It tests the BIF DateDiff in Weeks" )
	@Test
	public void testDateDiffWeeks() {
		variables.put( "date1", new DateTime( "2024-01-20T00:00:00Z" ) );
		variables.put( "date2", new DateTime( "2023-01-20T00:00:00Z" ) );
		instance.executeSource(
		    """
		    result = dateDiff( "w", date1, date2 );
		    """,
		    context );
		Long result = variables.getAsLong( Key.of( "result" ) );
		assertEquals( result, -52l );

	}

	@DisplayName( "It tests the member DateTime.diff in Weeks" )
	@Test
	@Ignore
	public void testDateDiffMemberWeeks() {
		variables.put( "date1", new DateTime( "2024-01-20T00:00:00Z" ) );
		variables.put( "date2", new DateTime( "2023-01-20T00:00:00Z" ) );
		instance.executeSource(
		    """
		    result = date1.diff( "w", date2 );
		    """,
		    context );
		Long result = variables.getAsLong( Key.of( "result" ) );
		assertEquals( result, -52l );

	}

	@DisplayName( "It tests the BIF DateDiff in Hours" )
	@Test
	public void testDateDiffHours() {
		variables.put( "date1", new DateTime( "2024-01-20T23:00:00Z" ) );
		variables.put( "date2", new DateTime( "2024-01-20T00:00:00Z" ) );
		instance.executeSource(
		    """
		    result = dateDiff( "h", date1, date2 );
		    """,
		    context );
		Long result = variables.getAsLong( Key.of( "result" ) );
		assertEquals( result, -23l );

	}

	@DisplayName( "It tests the member DateTime.diff in Hours" )
	@Test
	@Ignore
	public void testDateDiffMemberHours() {
		variables.put( "date1", new DateTime( "2024-01-20T23:00:00Z" ) );
		variables.put( "date2", new DateTime( "2024-01-20T00:00:00Z" ) );
		instance.executeSource(
		    """
		    result = date1.diff( "h", date2 );
		    """,
		    context );
		Long result = variables.getAsLong( Key.of( "result" ) );
		assertEquals( result, -23l );

	}

	@DisplayName( "It tests the BIF DateDiff in Minutes" )
	@Test
	public void testDateDiffMinutes() {
		variables.put( "date1", new DateTime( "2024-01-20T00:59:00Z" ) );
		variables.put( "date2", new DateTime( "2024-01-20T00:00:00Z" ) );
		instance.executeSource(
		    """
		    result = dateDiff( "n", date1, date2 );
		    """,
		    context );
		Long result = variables.getAsLong( Key.of( "result" ) );
		assertEquals( result, -59l );

	}

	@DisplayName( "It tests the member DateTime.diff in Minutes" )
	@Test
	@Ignore
	public void testDateDiffMemberMinutes() {
		variables.put( "date1", new DateTime( "2024-01-20T00:59:00Z" ) );
		variables.put( "date2", new DateTime( "2024-01-20T00:00:00Z" ) );
		instance.executeSource(
		    """
		    result = date1.diff( "n", date2 );
		    """,
		    context );
		Long result = variables.getAsLong( Key.of( "result" ) );
		assertEquals( result, -59l );

	}

	@DisplayName( "It tests the BIF DateDiff in Seconds" )
	@Test
	public void testDateDiffSeconds() {
		variables.put( "date1", new DateTime( "2024-01-20T00:01:00Z" ) );
		variables.put( "date2", new DateTime( "2024-01-20T00:00:00Z" ) );
		instance.executeSource(
		    """
		    result = dateDiff( "s", date1, date2 );
		    """,
		    context );
		Long result = variables.getAsLong( Key.of( "result" ) );
		assertEquals( result, -60l );

	}

	@DisplayName( "It tests the member DateTime.diff in Seconds" )
	@Test
	@Ignore
	public void testDateDiffMemberSeconds() {
		variables.put( "date1", new DateTime( "2024-01-20T00:01:00Z" ) );
		variables.put( "date2", new DateTime( "2024-01-20T00:00:00Z" ) );
		instance.executeSource(
		    """
		    result = date1.diff( "s", date2 );
		    """,
		    context );
		Long result = variables.getAsLong( Key.of( "result" ) );
		assertEquals( result, -60l );

	}

	@DisplayName( "It tests the BIF DateDiff in Milliseconds" )
	@Test
	public void testDateDiffMilliseconds() {
		variables.put( "date1", new DateTime( "2024-01-20T00:00:00.100Z" ) );
		variables.put( "date2", new DateTime( "2024-01-20T00:00:00.000Z" ) );
		instance.executeSource(
		    """
		    result = dateDiff( "l", date1, date2 );
		    """,
		    context );
		Long result = variables.getAsLong( Key.of( "result" ) );
		assertEquals( result, -100l );

	}

	@DisplayName( "It tests the member DateTime.diff in Milliseconds" )
	@Test
	@Ignore
	public void testDateDiffMemberMilliseconds() {
		variables.put( "date1", new DateTime( "2024-01-20T00:00:00.100Z" ) );
		variables.put( "date2", new DateTime( "2024-01-20T00:00:00.000Z" ) );
		instance.executeSource(
		    """
		    result = date1.diff( "l", date2 );
		    """,
		    context );
		Long result = variables.getAsLong( Key.of( "result" ) );
		assertEquals( result, -100l );

	}

	@DisplayName( "It tests the BIF DateDiff will throw an error with an invalid datePart" )
	@Test
	public void testDateDiffError() {
		variables.put( "date1", new DateTime( "2024-01-20T00:00:00.100Z" ) );
		variables.put( "date2", new DateTime( "2024-01-20T00:00:00.000Z" ) );
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = dateDiff( "blah", date1, date2 );
		        """,
		        context
		    )
		);

	}

}
