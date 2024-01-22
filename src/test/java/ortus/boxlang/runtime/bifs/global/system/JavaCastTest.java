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
import static org.junit.jupiter.api.Assertions.assertThrows;

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

public class JavaCastTest {

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

	@DisplayName( "It casts a number to double" )
	@Test
	public void testItCastsNumberToDouble() {
		instance.executeSource(
		    """
		    result = javaCast('double', 42);
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 42.0 );
	}

	@DisplayName( "It casts a string to boolean" )
	@Test
	public void testItCastsStringToBoolean() {
		instance.executeSource(
		    """
		    result = javaCast('boolean', 'true');
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "It casts null to string" )
	@Test
	public void testItCastsNullToString() {
		instance.executeSource(
		    """
		    result = javaCast('string', null);
		    """,
		    context );
		assertThat( variables.get( result ) ).isNull();
	}

	@DisplayName( "It casts a number to boolean" )
	@Test
	public void testItCastsNumberToBoolean() {
		instance.executeSource(
		    """
		    result = javaCast('boolean', 0);
		    result2 = javaCast('boolean', 1);
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( true );
	}

	@DisplayName( "It casts a string to double" )
	@Test
	public void testItCastsStringToDouble() {
		instance.executeSource(
		    """
		    result = javaCast('double', '3.14159265359');
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 3.14159265359 );
	}

	@DisplayName( "It casts a boolean to string" )
	@Test
	public void testItCastsBooleanToString() {
		instance.executeSource(
		    """
		    result = javaCast('string', true);
		    result2 = javaCast('string', false);
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "true" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "false" );
	}

	@DisplayName( "It casts a string to int" )
	@Test
	public void testItCastsStringToInt() {
		instance.executeSource(
		    """
		    result = javaCast('int', '42');
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 42 );
	}

	@DisplayName( "It casts a string to long" )
	@Test
	public void testItCastsStringToLong() {
		instance.executeSource(
		    """
		    result = javaCast('long', '42');
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 42L );
	}

	@DisplayName( "It casts a string to float" )
	@Test
	public void testItCastsStringToFloat() {
		instance.executeSource(
		    """
		    result = javaCast('float', '3.14159265359');
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 3.14159265359f );
	}

	@DisplayName( "It casts 'null' to null" )
	@Test
	public void testItCastsNullToNull() {
		instance.executeSource(
		    """
		    result = javaCast('null', 'null');
		    """,
		    context );
		assertThat( variables.get( result ) ).isNull();
	}

	@DisplayName( "It casts a number to byte" )
	@Test
	public void testItCastsNumberToByte() {
		instance.executeSource(
		    """
		    result = javaCast('byte', 42);
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( ( byte ) 42 );
	}

	@DisplayName( "It casts to string to bigdecimal" )
	@Test
	public void testItCastsStringToBigDecimal() {
		instance.executeSource(
		    """
		    result = javaCast('bigdecimal', '3.14159265359');
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( new java.math.BigDecimal( "3.14159265359" ) );
	}

	@DisplayName( "It can casts to a char" )
	@Test
	public void testItCastsToChar() {
		instance.executeSource(
		    """
		    result = javaCast('char', 'a');
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( ( char ) 'a' );
	}

	@DisplayName( "It thrwos an exception when casting to an invalid type" )
	@Test
	public void testItThrowsExceptionWhenCastingToInvalidType() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = javaCast( 'invalid', 42 );
		        """,
		        context )
		);
	}
	// Add more test cases for different casting scenarios here
}
