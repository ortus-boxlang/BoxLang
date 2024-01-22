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

package ortus.boxlang.runtime.bifs.global.type;

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

public class ArrayLenTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );
	static Key			result2	= new Key( "result2" );

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

	@DisplayName( "It returns the length of the array" )
	@Test
	public void testItReturnsArrayLength() {
		instance.executeSource(
		    """
		    arr = [ 1, 2, 3 ];
		    result = arrayLen( arr );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 3 );

		instance.executeSource(
		    """
		       arr = [ 1, 2, 3 ];
		       result = arr.len();
		    result2 = [ 1, 2, 3 ].len();
		       """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 3 );
		assertThat( variables.get( result2 ) ).isEqualTo( 3 );
	}

	@DisplayName( "It returns the length of the struct" )
	@Test
	public void testItReturnsStructLength() {
		instance.executeSource(
		    """
		    str = { foo : "bar", brad : "wood" };
		    result = structCount( str );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 2 );

		instance.executeSource(
		    """
		    str = { foo : "bar", brad : "wood" };
		    result = str.len();
		    result2 = str.count();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( 2 );

		instance.executeSource(
		    """
		    result = { foo : "bar", brad : "wood" }.len();
		    result2 = { foo : "bar", brad : "wood" }.count();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( 2 );
	}

	@DisplayName( "It returns the length of the string" )
	@Test
	public void testItReturnsStringLength() {
		instance.executeSource(
		    """
		    str = "BoxLang";
		    result = len( str );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 7 );

		instance.executeSource(
		    """
		    str = "BoxLang";
		    result = str.len();
		    result2 = "BoxLang".len();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 7 );
		assertThat( variables.get( result2 ) ).isEqualTo( 7 );
	}

	@DisplayName( "It returns the length of the number" )
	@Test
	public void testItReturnsNumberLength() {
		instance.executeSource(
		    """
		    num = 123;
		    result = len( num );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 3 );

		instance.executeSource(
		    """
		    num = 123;
		    result = num.len();
		    result = (123).len();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 3 );
	}

}
