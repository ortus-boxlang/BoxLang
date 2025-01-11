
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
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;

import java.util.Objects;

public class HashTest {

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

	@DisplayName( "It tests the BIF Hash40 on a string" )
	@Test
	public void testHash40String() {
		instance.executeSource(
		    """
		    result = hash40( "Hash me baby!" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( String.class );
		assertThat( variables.getAsString( Key.of( "result" ) ).length() ).isEqualTo( 40 );
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

	@DisplayName( "Tests quick hash algorithm" )
	@Test
	public void testQuickHash() {
		instance.executeSource(
		    """
		    result = hash( "foo", "quick" );
		    """,
		    context );
		assertThat( variables.getAsString( Key.of( "result" ) ).length() ).isEqualTo( 16 );
		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "4d780c14822d4653" );
	}

	@DisplayName( "It will provide a consistent output and handle iterations correctly" )
	@Test
	public void testIterations() {
		instance.executeSource(
		    """
		    result = hash("hello world", "md5", "utf-8");
		    """,
		    context );
		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "5eb63bbbe01eeed093cb22bb8f5acdc3" );
		instance.executeSource(
		    """
		    result = hash("hello world", "md5", "utf-8", 1 );
		    """,
		    context );
		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "5eb63bbbe01eeed093cb22bb8f5acdc3" );
		instance.executeSource(
		    """
		    result = hash("hello world", "md5", "utf-8", 2 );
		    """,
		    context );
		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "241d8a27c836427bd7f04461b60e7359" );
		instance.executeSource(
		    """
		    result = hash("hello world", "md5", "utf-8", 3 );
		    """,
		    context );
		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "765ee851263ecc8bdb38f7f13bb137ea" );
		instance.executeSource(
		    """
		    result = hash("hello world", "md5", "utf-8", 4 );
		    """,
		    context );
		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "9074036bc3f46203468c06b09b303fb5" );
		instance.executeSource(
		    """
		    result = hash("hello world", "md5", "utf-8", 5 );
		    """,
		    context );
		assertThat( variables.getAsString( Key.of( "result" ) ) ).isEqualTo( "58113448b78dec752c533eba7f5eab99" );
	}

}
