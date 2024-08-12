
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

public class DateAddTest {

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

	@DisplayName( "It tests the BIF DateAdd" )
	@Test
	public void testBif() {
		instance.executeSource(
		    """
		    result = dateAdd( "d", 1, createDate( 2023, 12, 31 ) );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( DateTime.class );
		assertThat( result.toString() ).isInstanceOf( String.class );
		assertThat( IntegerCaster.cast( result.format( "yyyy" ) ) ).isEqualTo( 2024 );
		assertThat( IntegerCaster.cast( result.format( "M" ) ) ).isEqualTo( 1 );
		assertThat( IntegerCaster.cast( result.format( "d" ) ) ).isEqualTo( 1 );

	}

	@DisplayName( "It tests the BIF DateAdd with a string as the date argument" )
	@Test
	public void testBifWithDateString() {
		instance.executeSource(
		    """
		    result = dateAdd( "d", 1, "2023-12-31" );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( DateTime.class );
		assertThat( result.toString() ).isInstanceOf( String.class );
		assertThat( IntegerCaster.cast( result.format( "yyyy" ) ) ).isEqualTo( 2024 );
		assertThat( IntegerCaster.cast( result.format( "M" ) ) ).isEqualTo( 1 );
		assertThat( IntegerCaster.cast( result.format( "d" ) ) ).isEqualTo( 1 );

	}

	@DisplayName( "It tests the member function DateTime.add( datepart, number )" )
	@Test
	public void testMemberFunction() {
		instance.executeSource(
		    """
		    ref = createDate( 2023, 12, 31 )
		       result = ref.add( datepart="d", number=1 );
		       """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( DateTime.class );
		assertThat( result.toString() ).isInstanceOf( String.class );
		assertThat( IntegerCaster.cast( result.format( "yyyy" ) ) ).isEqualTo( 2024 );
		assertThat( IntegerCaster.cast( result.format( "M" ) ) ).isEqualTo( 1 );
		assertThat( IntegerCaster.cast( result.format( "d" ) ) ).isEqualTo( 1 );
	}

	@DisplayName( "It does not modify the original argument" )
	@Test
	public void testValueSemantics() {
		instance.executeSource(
		    """
		    			original = createDate( 2023, 12, 31 );
		    modified = dateAdd( "d", 1, original );
		    """,
		    context );
		DateTime modified = ( DateTime ) variables.get( Key.of( "modified" ) );
		assertThat( modified ).isInstanceOf( DateTime.class );
		assertThat( modified.toString() ).isInstanceOf( String.class );
		assertThat( IntegerCaster.cast( modified.format( "yyyy" ) ) ).isEqualTo( 2024 );
		assertThat( IntegerCaster.cast( modified.format( "M" ) ) ).isEqualTo( 1 );
		assertThat( IntegerCaster.cast( modified.format( "d" ) ) ).isEqualTo( 1 );

		DateTime original = ( DateTime ) variables.get( Key.of( "original" ) );
		assertThat( original ).isInstanceOf( DateTime.class );
		assertThat( original.toString() ).isInstanceOf( String.class );
		assertThat( IntegerCaster.cast( original.format( "yyyy" ) ) ).isEqualTo( 2023 );
		assertThat( IntegerCaster.cast( original.format( "M" ) ) ).isEqualTo( 12 );
		assertThat( IntegerCaster.cast( original.format( "d" ) ) ).isEqualTo( 31 );
	}

}
