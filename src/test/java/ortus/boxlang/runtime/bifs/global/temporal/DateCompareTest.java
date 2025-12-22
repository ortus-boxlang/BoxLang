
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
import static org.junit.Assert.assertNotEquals;

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

public class DateCompareTest {

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

	@DisplayName( "It tests the BIF DateCompare" )
	@Test
	public void testBif() {
		var	date1	= new DateTime();
		var	date2	= new DateTime().modify( "d", 2l );
		assertNotEquals( date1.getWrapped().toInstant(), date2.getWrapped().toInstant() );
		variables.put( Key.of( "date1" ), date1 );
		variables.put( Key.of( "date2" ), date2 );
		instance.executeSource(
		    """
		    result = dateCompare( date1, date2 );
		    """,
		    context );
		Integer comparison = variables.getAsInteger( Key.of( "result" ) );
		assertThat( comparison ).isEqualTo( -1 );
	}

	@DisplayName( "It tests the BIF DateCompare - date 1 greater" )
	@Test
	public void testBifGreater1() {
		var	date1	= new DateTime();
		var	date2	= new DateTime().modify( "d", 2l );
		assertNotEquals( date1.getWrapped().toInstant(), date2.getWrapped().toInstant() );
		variables.put( Key.of( "date1" ), date1 );
		variables.put( Key.of( "date2" ), date2 );
		instance.executeSource(
		    """
		    result = dateCompare( date2, date1 );
		    """,
		    context );
		Integer comparison = variables.getAsInteger( Key.of( "result" ) );
		assertThat( comparison ).isEqualTo( 1 );
	}

	@DisplayName( "It tests the BIF DateCompare - equal dates" )
	@Test
	public void testBifEqual() {
		var date1 = new DateTime();
		variables.put( Key.of( "date1" ), date1 );
		variables.put( Key.of( "date2" ), date1 );
		instance.executeSource(
		    """
		    result = dateCompare( date2, date1 );
		    """,
		    context );
		Integer comparison = variables.getAsInteger( Key.of( "result" ) );
		assertThat( comparison ).isEqualTo( 0 );
	}

	@DisplayName( "It tests the BIF DateCompare with string dates" )
	@Test
	public void testBifWithStrings() {
		var	date1	= "2024-01-19";
		var	date2	= "2024-01-22";
		variables.put( Key.of( "date1" ), date1 );
		variables.put( Key.of( "date2" ), date2 );
		instance.executeSource(
		    """
		    result = dateCompare( date1, date2 );
		    """,
		    context );
		Integer comparison = variables.getAsInteger( Key.of( "result" ) );
		assertThat( comparison ).isEqualTo( -1 );
	}

	@DisplayName( "It tests the BIF DateCompare with a datepart seconds" )
	@Test
	public void testBifWithDatepartSeconds() {
		var	date1	= new DateTime();
		var	date2	= new DateTime().modify( "s", 2l );
		variables.put( Key.of( "date1" ), date1 );
		variables.put( Key.of( "date2" ), date2 );
		instance.executeSource(
		    """
		    result = dateCompare( date1, date2, "s" );
		    """,
		    context );
		Integer comparison = variables.getAsInteger( Key.of( "result" ) );
		assertThat( comparison ).isEqualTo( -1 );
	}

	@DisplayName( "It tests the BIF DateCompare with a datepart minutes" )
	@Test
	public void testBifWithDatepartMinutes() {
		var	date1	= new DateTime();
		var	date2	= new DateTime().modify( "m", 2l );
		variables.put( Key.of( "date1" ), date1 );
		variables.put( Key.of( "date2" ), date2 );
		instance.executeSource(
		    """
		    result = dateCompare( date1, date2, "n" );
		    """,
		    context );
		Integer comparison = variables.getAsInteger( Key.of( "result" ) );
		assertThat( comparison ).isEqualTo( -1 );
	}

	@DisplayName( "It tests the BIF DateCompare with a datepart day" )
	@Test
	public void testBifWithDatepartDay() {
		var	date1	= "2024-01-19T10:00:00Z";
		var	date2	= "2024-01-20T02:00:00Z";
		variables.put( Key.of( "date1" ), date1 );
		variables.put( Key.of( "date2" ), date2 );
		instance.executeSource(
		    """
		    result = dateCompare( date1, date2, "d" );
		    """,
		    context );
		Integer comparison = variables.getAsInteger( Key.of( "result" ) );
		assertThat( comparison ).isEqualTo( -1 );
	}

	@DisplayName( "It tests the BIF DateCompare with a datepart month" )
	@Test
	public void testBifWithDatepart() {
		var	date1	= "2024-01-19";
		var	date2	= "2024-02-19";
		variables.put( Key.of( "date1" ), date1 );
		variables.put( Key.of( "date2" ), date2 );
		instance.executeSource(
		    """
		    result = dateCompare( date1, date2, "m" );
		    """,
		    context );
		Integer comparison = variables.getAsInteger( Key.of( "result" ) );
		assertThat( comparison ).isEqualTo( -1 );
	}

	@DisplayName( "It tests the BIF DateCompare with a datepart year" )
	@Test
	public void testBifWithDatepartYear() {
		var	date1	= "2024-01-19";
		var	date2	= "2024-02-19";
		variables.put( Key.of( "date1" ), date1 );
		variables.put( Key.of( "date2" ), date2 );
		instance.executeSource(
		    """
		    result = dateCompare( date1, date2, "yyyy" );
		    """,
		    context );
		Integer comparison = variables.getAsInteger( Key.of( "result" ) );
		assertThat( comparison ).isEqualTo( 0 );
	}

	@DisplayName( "It tests the member function for DateTime.compare" )
	@Test
	public void testDateCompareMember() {
		var	date1	= new DateTime();
		var	date2	= new DateTime().modify( "d", 2l );
		assertNotEquals( date1.getWrapped().toInstant(), date2.getWrapped().toInstant() );
		variables.put( Key.of( "date1" ), date1 );
		variables.put( Key.of( "date2" ), date2 );
		instance.executeSource(
		    """
		    result = date1.compare( date2 );
		    """,
		    context );
		Integer comparison = variables.getAsInteger( Key.of( "result" ) );
		assertThat( comparison ).isEqualTo( -1 );
	}

	@DisplayName( "It tests the member function for DateTime.compareTo" )
	@Test
	public void testDateCompareToMember() {
		var	date1	= new DateTime();
		var	date2	= new DateTime().modify( "d", 2l );
		assertNotEquals( date1.getWrapped().toInstant(), date2.getWrapped().toInstant() );
		variables.put( Key.of( "date1" ), date1 );
		variables.put( Key.of( "date2" ), date2 );
		instance.executeSource(
		    """
		    result = date2.compare( date1 );
		    """,
		    context );
		Integer comparison = variables.getAsInteger( Key.of( "result" ) );
		assertThat( comparison ).isEqualTo( 1 );
	}

	@DisplayName( "It can compare two zero-value time strings" )
	@Test
	public void testCompareZeroTime() {
		instance.executeSource(
		    """
		    result = dateCompare( "00:00:00", "00:00:00" );
		    """,
		    context );
		Integer comparison = variables.getAsInteger( Key.of( "result" ) );
		assertThat( comparison ).isEqualTo( 0 );
	}

	@DisplayName( "It can compare similar date parts with different other parts" )
	@Test
	public void testCompareSimilar() {
		instance.executeSource(
		    """
		    assert dateCompare('2025-01-01', '2025-02-01', 'd' ) == -1;
		    assert dateCompare('2024-02-01', '2025-02-01', 'm' ) == -1;
		    assert dateCompare( '2025-02-01', '2024-02-01', 'm' ) == 1;
		    assert dateCompare('2025-01-01T00:00:01Z', '2025-01-01T00:10:00Z', 'd' ) == 0;
		    assert dateCompare('2025-01-02T00:00:01Z', '2025-01-15T00:10:00Z', 'm' ) == 0;
		    assert dateCompare('2025-01-01T00:00:00Z', '2025-01-15T00:00:00Z', 'y' ) == 0;
		    assert dateCompare('2025-01-01T00:00:00Z', '2025-01-15T00:00:00Z', 'yyyy' ) == 0;
		    """,
		    context );
	}

}
