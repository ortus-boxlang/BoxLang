
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

package ortus.boxlang.runtime.bifs.global.encryption;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class HashTest {

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

	@DisplayName( "It tests the BIF Hash on a string" )
	@Test
	public void testHashString() {
		instance.executeSource(
		    """
		    result = hash( "Hash me baby!" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( String.class );
		assertThat( variables.getAsString( Key.of( "result" ) ).length() ).isEqualTo( 32 );
	}

	@DisplayName( "It tests the BIF Hash on a struct" )
	@Test
	public void testHashStruct() {
		instance.executeSource(
		    """
		    result = hash( { "action" : "Hash me baby!" } );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( String.class );
		assertThat( variables.getAsString( Key.of( "result" ) ).length() ).isEqualTo( 32 );
	}

	@DisplayName( "It tests the BIF Hash on a array" )
	@Test
	public void testHashArray() {
		instance.executeSource(
		    """
		    result = hash( [ "Hash me baby!" ] );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( String.class );
		assertThat( variables.getAsString( Key.of( "result" ) ).length() ).isEqualTo( 32 );
	}

	@DisplayName( "It tests the BIF Hash on a string with a different algorithm" )
	@Test
	public void testHashStringAlgorithm() {
		instance.executeSource(
		    """
		    result = hash( "Hash me baby!", "sha512", "utf-16" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( String.class );
		assertThat( variables.getAsString( Key.of( "result" ) ).length() ).isEqualTo( 128 );
	}

	@DisplayName( "It tests that an invalid algorithm will throw an error" )
	@Test
	public void testHashAlgorithmError() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = hash( "Hash me baby!", "ickyalgorithm" );
		        """,
		        context )
		);

	}

	@DisplayName( "It tests the BIF Hash on a string with an encoding" )
	@Test
	public void testHashEncodedString() {
		instance.executeSource(
		    """
		    result = hash( "Hash me baby!", "md5", "utf-16" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( String.class );
		assertThat( variables.getAsString( Key.of( "result" ) ).length() ).isEqualTo( 32 );
	}

	@DisplayName( "It tests the BIF Hash on a string with a number of iterations" )
	@Test
	public void testHashIterationsString() {
		instance.executeSource(
		    """
		    result = hash( "Hash me baby!", "md5", "utf-16", 10 );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( String.class );
		assertThat( variables.getAsString( Key.of( "result" ) ).length() ).isEqualTo( 32 );
	}

	@DisplayName( "It tests the String.hash member function" )
	@Test
	public void testMemberString() {
		instance.executeSource(
		    """
		    result = "Hash me baby!".hash();
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( String.class );
		assertThat( variables.getAsString( Key.of( "result" ) ).length() ).isEqualTo( 32 );
	}

}
