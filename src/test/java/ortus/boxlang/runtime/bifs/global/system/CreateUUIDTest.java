/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.system;

import static com.google.common.truth.Truth.assertThat;

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

public class CreateUUIDTest {

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

	@DisplayName( "It creates a standard/valid UUID" )
	@Test
	public void isStandardUUIDFormat() {
		instance.executeSource(
		    """
		    result = createUUID();
		    """,
		    context );
		String uuid = variables.getAsString( result );
		assertThat( uuid.length() ).isEqualTo( 36 );
		String[] splitUUID = uuid.split( "-" );
		assertThat( splitUUID.length ).isEqualTo( 5 );
		assertThat( splitUUID[ 0 ].length() ).isEqualTo( 8 );
		assertThat( splitUUID[ 1 ].length() ).isEqualTo( 4 );
		assertThat( splitUUID[ 2 ].length() ).isEqualTo( 4 );
		assertThat( splitUUID[ 3 ].length() ).isEqualTo( 4 );
		assertThat( splitUUID[ 4 ].length() ).isEqualTo( 12 );
	}

	@DisplayName( "It uses the hexadecimal character set" )
	@Test
	public void testIsHexadecimal() {
		instance.executeSource(
		    """
		    result = createUUID();
		    """,
		    context );

		String uuid = variables.getAsString( result );
		assertThat( uuid.matches( "[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}" ) ).isEqualTo( true );
	}

	@DisplayName( "It does not SHOUT" )
	@Test
	public void testIsNotForcedUppercase() {
		instance.executeSource(
		    """
		    result = createUUID();
		    """,
		    context );

		String uuid = variables.getAsString( result );
		assertThat( uuid.matches( ".*[a-f].*" ) ).isEqualTo( true );
	}
}