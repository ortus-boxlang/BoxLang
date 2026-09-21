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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.DateTime;

public class DateSetTest {

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

	@DisplayName( "It tests the BIF DateSetDay" )
	@Test
	public void testBifDay() {
		instance.executeSource(
		    """
		    result = dateSetDay( createDate( 2023, 1, 15 ), 20 );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( DateTime.class );
		assertThat( IntegerCaster.cast( result.format( "d" ) ) ).isEqualTo( 20 );
	}

	@DisplayName( "It tests the DateTime member function setDay" )
	@Test
	public void testMemberDay() {
		instance.executeSource(
		    """
		    result = createDate( 2023, 1, 15 ).setDay( 20 );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( IntegerCaster.cast( result.format( "d" ) ) ).isEqualTo( 20 );
	}

	@DisplayName( "It tests the BIF DateSetHour" )
	@Test
	public void testBifHour() {
		instance.executeSource(
		    """
		    result = dateSetHour( createDateTime( 2023, 1, 15, 12, 30, 45 ), 5 );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( IntegerCaster.cast( result.format( "H" ) ) ).isEqualTo( 5 );
	}

	@DisplayName( "It tests the DateTime member function setHour" )
	@Test
	public void testMemberHour() {
		instance.executeSource(
		    """
		    result = createDateTime( 2023, 1, 15, 12, 30, 45 ).setHour( 5 );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( IntegerCaster.cast( result.format( "H" ) ) ).isEqualTo( 5 );
	}

	@DisplayName( "It tests the BIF DateSetMinute" )
	@Test
	public void testBifMinute() {
		instance.executeSource(
		    """
		    result = dateSetMinute( createDateTime( 2023, 1, 15, 12, 30, 45 ), 15 );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( IntegerCaster.cast( result.format( "m" ) ) ).isEqualTo( 15 );
	}

	@DisplayName( "It tests the DateTime member function setMinute" )
	@Test
	public void testMemberMinute() {
		instance.executeSource(
		    """
		    result = createDateTime( 2023, 1, 15, 12, 30, 45 ).setMinute( 15 );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( IntegerCaster.cast( result.format( "m" ) ) ).isEqualTo( 15 );
	}

	@DisplayName( "It tests the BIF DateSetMonth" )
	@Test
	public void testBifMonth() {
		instance.executeSource(
		    """
		    result = dateSetMonth( createDate( 2023, 1, 15 ), 6 );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( IntegerCaster.cast( result.format( "M" ) ) ).isEqualTo( 6 );
	}

	@DisplayName( "It tests the DateTime member function setMonth" )
	@Test
	public void testMemberMonth() {
		instance.executeSource(
		    """
		    result = createDate( 2023, 1, 15 ).setMonth( 6 );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( IntegerCaster.cast( result.format( "M" ) ) ).isEqualTo( 6 );
	}

	@DisplayName( "It tests the BIF DateSetSecond" )
	@Test
	public void testBifSecond() {
		instance.executeSource(
		    """
		    result = dateSetSecond( createDateTime( 2023, 1, 15, 12, 30, 45 ), 10 );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( IntegerCaster.cast( result.format( "s" ) ) ).isEqualTo( 10 );
	}

	@DisplayName( "It tests the DateTime member function setSecond" )
	@Test
	public void testMemberSecond() {
		instance.executeSource(
		    """
		    result = createDateTime( 2023, 1, 15, 12, 30, 45 ).setSecond( 10 );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( IntegerCaster.cast( result.format( "s" ) ) ).isEqualTo( 10 );
	}

	@DisplayName( "It tests the BIF DateSetYear" )
	@Test
	public void testBifYear() {
		instance.executeSource(
		    """
		    result = dateSetYear( createDate( 2023, 1, 15 ), 2030 );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( IntegerCaster.cast( result.format( "yyyy" ) ) ).isEqualTo( 2030 );
	}

	@DisplayName( "It tests the DateTime member function setYear" )
	@Test
	public void testMemberYear() {
		instance.executeSource(
		    """
		    result = createDate( 2023, 1, 15 ).setYear( 2030 );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( IntegerCaster.cast( result.format( "yyyy" ) ) ).isEqualTo( 2030 );
	}

	@DisplayName( "It clamps setDay( 0 ) to the first day of the month" )
	@Test
	public void testDayZeroClampsToFirstDay() {
		instance.executeSource(
		    """
		    result = createDate( 2023, 1, 15 ).setDay( 0 );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( IntegerCaster.cast( result.format( "M" ) ) ).isEqualTo( 1 );
		assertThat( IntegerCaster.cast( result.format( "d" ) ) ).isEqualTo( 1 );
	}

	@DisplayName( "It rolls over setDay( 50 ) into the following month" )
	@Test
	public void testDayRollover() {
		instance.executeSource(
		    """
		    result = createDate( 2023, 1, 15 ).setDay( 50 );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( IntegerCaster.cast( result.format( "M" ) ) ).isEqualTo( 2 );
		assertThat( IntegerCaster.cast( result.format( "d" ) ) ).isEqualTo( 19 );
	}

	@DisplayName( "It rolls over setMonth( 13 ) into the following year" )
	@Test
	public void testMonthRollover() {
		instance.executeSource(
		    """
		    result = createDate( 2023, 1, 15 ).setMonth( 13 );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( IntegerCaster.cast( result.format( "yyyy" ) ) ).isEqualTo( 2024 );
		assertThat( IntegerCaster.cast( result.format( "M" ) ) ).isEqualTo( 1 );
		assertThat( IntegerCaster.cast( result.format( "d" ) ) ).isEqualTo( 15 );
	}

	@DisplayName( "It rolls over setHour( 24 ) into the following day" )
	@Test
	public void testHourRollover() {
		instance.executeSource(
		    """
		    result = createDateTime( 2023, 1, 15, 12, 30, 45 ).setHour( 24 );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( IntegerCaster.cast( result.format( "d" ) ) ).isEqualTo( 16 );
		assertThat( IntegerCaster.cast( result.format( "H" ) ) ).isEqualTo( 0 );
	}

	@DisplayName( "It clamps negative hour to zero" )
	@Test
	public void testNegativeHourClampsToZero() {
		instance.executeSource(
		    """
		    result = createDateTime( 2023, 1, 15, 12, 30, 45 ).setHour( -5 );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( IntegerCaster.cast( result.format( "H" ) ) ).isEqualTo( 0 );
	}

	@DisplayName( "It clamps setYear( 0 ) to year 1" )
	@Test
	public void testYearZeroClampsToOne() {
		instance.executeSource(
		    """
		    result = createDate( 2023, 1, 15 ).setYear( 0 );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( IntegerCaster.cast( result.format( "yyyy" ) ) ).isEqualTo( 1 );
	}

	@DisplayName( "It mutates the original DateTime in place" )
	@Test
	public void testValueSemantics() {
		instance.executeSource(
		    """
		    original = createDate( 2023, 1, 15 );
		    result = original.setDay( 20 );
		    """,
		    context );
		DateTime	original	= ( DateTime ) variables.get( Key.of( "original" ) );
		DateTime	result		= ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( result ).isSameInstanceAs( original );
		assertThat( IntegerCaster.cast( original.format( "d" ) ) ).isEqualTo( 20 );
	}

}
