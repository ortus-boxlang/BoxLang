
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;

public class GetTimezoneInfoTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
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

	@DisplayName( "It tests the BIF GetTimezoneInfo with no arguments" )
	@Test
	public void testBif() {
		instance.executeSource(
		    """
		    result = getTimezoneInfo();
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( IStruct.class );
		IStruct infoStruct = StructCaster.cast( result );
		assertTrue( infoStruct.containsKey( "DSTOffset" ) );
		assertThat( infoStruct.get( "DSTOffset" ) ).isInstanceOf( Integer.class );
		assertTrue( infoStruct.containsKey( "id" ) );
		assertTrue( infoStruct.containsKey( "isDSTon" ) );
		assertTrue( infoStruct.containsKey( "name" ) );
		assertTrue( infoStruct.containsKey( "nameDST" ) );
		assertTrue( infoStruct.containsKey( "offset" ) );
		assertThat( infoStruct.get( "offset" ) ).isInstanceOf( Integer.class );
		assertTrue( infoStruct.containsKey( "shortName" ) );
		assertTrue( infoStruct.containsKey( "shortNameDST" ) );
		assertTrue( infoStruct.containsKey( "timezone" ) );
		assertTrue( infoStruct.containsKey( "utcHourOffset" ) );
		assertThat( infoStruct.get( "utcHourOffset" ) ).isInstanceOf( Integer.class );
		assertTrue( infoStruct.containsKey( "utcMinuteOffset" ) );
		assertThat( infoStruct.get( "utcMinuteOffset" ) ).isInstanceOf( Integer.class );
		assertTrue( infoStruct.containsKey( "utcSecondOffset" ) );
		assertThat( infoStruct.get( "utcSecondOffset" ) ).isInstanceOf( Integer.class );
		assertThat( infoStruct.getAsInteger( Key.of( "utcSecondOffset" ) ) ).isEqualTo( Math.abs( infoStruct.getAsInteger( Key.of( "offset" ) ) ) );
	}

	@DisplayName( "It tests the BIF GetTimezoneInfo with no a timezone argument" )
	@Test
	public void testBifWithTimezone() {
		instance.executeSource(
		    """
		    result = getTimezoneInfo( "UTC" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( IStruct.class );
		IStruct infoStruct = StructCaster.cast( result );
		assertTrue( infoStruct.containsKey( "DSTOffset" ) );
		assertTrue( infoStruct.containsKey( "id" ) );
		assertTrue( infoStruct.containsKey( "isDSTon" ) );
		assertTrue( infoStruct.containsKey( "name" ) );
		assertTrue( infoStruct.containsKey( "nameDST" ) );
		assertTrue( infoStruct.containsKey( "offset" ) );
		assertTrue( infoStruct.containsKey( "shortName" ) );
		assertTrue( infoStruct.containsKey( "shortNameDST" ) );
		assertTrue( infoStruct.containsKey( "timezone" ) );
		assertTrue( infoStruct.containsKey( "utcHourOffset" ) );
		assertTrue( infoStruct.containsKey( "utcMinuteOffset" ) );
		assertTrue( infoStruct.containsKey( "utcSecondOffset" ) );
		assertThat( infoStruct.getAsInteger( Key.of( "utcSecondOffset" ) ) ).isEqualTo( Math.abs( infoStruct.getAsInteger( Key.of( "offset" ) ) ) );
		assertThat( infoStruct.getAsInteger( Key.of( "offset" ) ) ).isEqualTo( 0 );
		assertThat( infoStruct.getAsString( Key.of( "shortName" ) ) ).isEqualTo( "UTC" );
		assertFalse( infoStruct.getAsBoolean( Key.of( "isDSTon" ) ) );
	}

	@DisplayName( "It tests the BIF GetTimezoneInfo with no a timezone and locale argument" )
	@Test
	public void testBifWithTimezoneAndLocale() {
		instance.executeSource(
		    """
		    result = getTimezoneInfo( "UTC", "es-SA" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( IStruct.class );
		IStruct infoStruct = StructCaster.cast( result );
		assertTrue( infoStruct.containsKey( "DSTOffset" ) );
		assertTrue( infoStruct.containsKey( "id" ) );
		assertTrue( infoStruct.containsKey( "isDSTon" ) );
		assertTrue( infoStruct.containsKey( "name" ) );
		assertTrue( infoStruct.containsKey( "nameDST" ) );
		assertTrue( infoStruct.containsKey( "offset" ) );
		assertTrue( infoStruct.containsKey( "shortName" ) );
		assertTrue( infoStruct.containsKey( "shortNameDST" ) );
		assertTrue( infoStruct.containsKey( "timezone" ) );
		assertTrue( infoStruct.containsKey( "utcHourOffset" ) );
		assertTrue( infoStruct.containsKey( "utcMinuteOffset" ) );
		assertTrue( infoStruct.containsKey( "utcSecondOffset" ) );
		assertThat( infoStruct.getAsInteger( Key.of( "utcSecondOffset" ) ) ).isEqualTo( Math.abs( infoStruct.getAsInteger( Key.of( "offset" ) ) ) );
		assertThat( infoStruct.getAsInteger( Key.of( "offset" ) ) ).isEqualTo( 0 );
		assertThat( infoStruct.getAsString( Key.of( "name" ) ) ).isEqualTo( "tiempo universal coordinado" );
		assertThat( infoStruct.getAsString( Key.of( "name" ) ) ).isEqualTo( "tiempo universal coordinado" );
		assertThat( infoStruct.getAsString( Key.of( "shortName" ) ) ).isEqualTo( "UTC" );
		assertFalse( infoStruct.getAsBoolean( Key.of( "isDSTon" ) ) );
	}

}
