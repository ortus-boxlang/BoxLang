
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
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.DateTime;

public class LSParseDateTimeTest {

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

	@DisplayName( "It tests the BIF LSParseDateTime with a full ISO including offset" )
	@Test
	public void testLSParseDateTimeFullISO() {
		instance.executeSource(
		    """
		    result = lsParseDateTime( "2024-01-14T00:00:01.0001Z" );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( DateTime.class );
		assertThat( result.toString() ).isInstanceOf( String.class );
		assertThat( IntegerCaster.cast( result.format( "yyyy" ) ) ).isEqualTo( 2024 );
		assertThat( IntegerCaster.cast( result.format( "M" ) ) ).isEqualTo( 1 );
		assertThat( IntegerCaster.cast( result.format( "d" ) ) ).isEqualTo( 14 );
		assertThat( IntegerCaster.cast( result.format( "H" ) ) ).isEqualTo( 0 );
		assertThat( IntegerCaster.cast( result.format( "m" ) ) ).isEqualTo( 0 );
		assertThat( IntegerCaster.cast( result.format( "s" ) ) ).isEqualTo( 1 );
		assertThat( IntegerCaster.cast( result.format( "n" ) ) ).isEqualTo( 100000 );
	}

	@DisplayName( "It tests the BIF LSParseDateTime with a full ISO without offset" )
	@Test
	public void testLSParseDateTimeNoOffset() {
		instance.executeSource(
		    """
		    result = lsParseDateTime( "2024-01-14T00:00:01.0001" );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( DateTime.class );
		assertThat( result.toString() ).isInstanceOf( String.class );
		assertThat( IntegerCaster.cast( result.format( "yyyy" ) ) ).isEqualTo( 2024 );
		assertThat( IntegerCaster.cast( result.format( "M" ) ) ).isEqualTo( 1 );
		assertThat( IntegerCaster.cast( result.format( "d" ) ) ).isEqualTo( 14 );
		assertThat( IntegerCaster.cast( result.format( "H" ) ) ).isEqualTo( 0 );
		assertThat( IntegerCaster.cast( result.format( "m" ) ) ).isEqualTo( 0 );
		assertThat( IntegerCaster.cast( result.format( "s" ) ) ).isEqualTo( 1 );
		assertThat( IntegerCaster.cast( result.format( "n" ) ) ).isEqualTo( 100000 );
	}

	@DisplayName( "It tests the BIF LSParseDateTime with without any time" )
	@Test
	public void testLSParseDateTimeNoTime() {
		instance.executeSource(
		    """
		    result = lsParseDateTime( "2024-01-14" );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( DateTime.class );
		assertThat( result.toString() ).isInstanceOf( String.class );
		assertThat( IntegerCaster.cast( result.format( "yyyy" ) ) ).isEqualTo( 2024 );
		assertThat( IntegerCaster.cast( result.format( "M" ) ) ).isEqualTo( 1 );
		assertThat( IntegerCaster.cast( result.format( "d" ) ) ).isEqualTo( 14 );
		assertThat( IntegerCaster.cast( result.format( "H" ) ) ).isEqualTo( 0 );
		assertThat( IntegerCaster.cast( result.format( "m" ) ) ).isEqualTo( 0 );
		assertThat( IntegerCaster.cast( result.format( "s" ) ) ).isEqualTo( 0 );
		assertThat( IntegerCaster.cast( result.format( "n" ) ) ).isEqualTo( 0 );
	}

	@DisplayName( "It tests the BIF LSParseDateTime with a full ISO including offset and locale argument" )
	@Test
	public void testLSParseDateTimeFullISOLocale() {
		instance.executeSource(
		    """
		    result = lsParseDateTime( "2024-01-14T00:00:01.0001Z", "en-US" );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( DateTime.class );
		assertThat( result.toString() ).isInstanceOf( String.class );
		assertThat( IntegerCaster.cast( result.format( "yyyy" ) ) ).isEqualTo( 2024 );
		assertThat( IntegerCaster.cast( result.format( "M" ) ) ).isEqualTo( 1 );
		assertThat( IntegerCaster.cast( result.format( "d" ) ) ).isEqualTo( 14 );
		assertThat( IntegerCaster.cast( result.format( "H" ) ) ).isEqualTo( 0 );
		assertThat( IntegerCaster.cast( result.format( "m" ) ) ).isEqualTo( 0 );
		assertThat( IntegerCaster.cast( result.format( "s" ) ) ).isEqualTo( 1 );
		assertThat( IntegerCaster.cast( result.format( "n" ) ) ).isEqualTo( 100000 );
	}

	@DisplayName( "It tests the BIF LSParseDateTime using a localized russian format" )
	@Test
	public void testLSParseDateTimeRussian() {
		instance.executeSource(
		    """
		    result = lsParseDateTime( "14.01.2024", "ru-RU", "dd.MM.yyyy" );
		    """,
		    context );
		DateTime result = ( DateTime ) variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( DateTime.class );
		assertThat( result.toString() ).isInstanceOf( String.class );
		assertThat( IntegerCaster.cast( result.format( "yyyy" ) ) ).isEqualTo( 2024 );
		assertThat( IntegerCaster.cast( result.format( "M" ) ) ).isEqualTo( 1 );
		assertThat( IntegerCaster.cast( result.format( "d" ) ) ).isEqualTo( 14 );
		assertThat( IntegerCaster.cast( result.format( "H" ) ) ).isEqualTo( 0 );
		assertThat( IntegerCaster.cast( result.format( "m" ) ) ).isEqualTo( 0 );
		assertThat( IntegerCaster.cast( result.format( "s" ) ) ).isEqualTo( 0 );
		assertThat( IntegerCaster.cast( result.format( "n" ) ) ).isEqualTo( 0 );
	}

}
