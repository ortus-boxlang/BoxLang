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

package ortus.boxlang.runtime.bifs.global.array;

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
import ortus.boxlang.runtime.types.Array;

public class ArrayUniqueTest {

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

	@DisplayName( "It can remove duplicate values from an array" )
	@Test
	public void testCanRemoveDuplicateValues() {
		instance.executeSource(
		    """
		    	nums = [ 1, 2, 1, 1, 1, 4, 3, 4 ];

		    	result = ArrayUnique( nums );
		    """,
		    context );
		Array resultArray = ( Array ) variables.get( result );
		assertThat( resultArray.size() ).isEqualTo( 4 );
		assertThat( resultArray.get( 0 ) ).isEqualTo( 1 );
		assertThat( resultArray.get( 1 ) ).isEqualTo( 2 );
		assertThat( resultArray.get( 2 ) ).isEqualTo( 4 );
		assertThat( resultArray.get( 3 ) ).isEqualTo( 3 );
	}

	@DisplayName( "It treats different numeric types with the same value as duplicates" )
	@Test
	public void testMixedNumericTypesAreDeduplicated() {
		instance.executeSource(
		    """
		    	nums = [ 1, 1.0, 2, 2.00, 3 ];
		    	result = ArrayUnique( nums );
		    """,
		    context );
		Array resultArray = ( Array ) variables.get( result );
		assertThat( resultArray.size() ).isEqualTo( 3 );
		assertThat( resultArray.get( 0 ) ).isEqualTo( 1 );
		assertThat( resultArray.get( 1 ) ).isEqualTo( 2 );
		assertThat( resultArray.get( 2 ) ).isEqualTo( 3 );
	}

	@DisplayName( "It treats differently-cased strings as duplicates by default" )
	@Test
	public void testCaseInsensitiveStringDedup() {
		instance.executeSource(
		    """
		    	words = [ "Hello", "hello", "HELLO", "World", "world" ];
		    	result = ArrayUnique( words );
		    """,
		    context );
		Array resultArray = ( Array ) variables.get( result );
		assertThat( resultArray.size() ).isEqualTo( 2 );
		assertThat( resultArray.get( 0 ) ).isEqualTo( "Hello" );
		assertThat( resultArray.get( 1 ) ).isEqualTo( "World" );
	}

	@DisplayName( "It preserves differently-cased strings when caseSensitive=true" )
	@Test
	public void testCaseSensitiveStringDedup() {
		instance.executeSource(
		    """
		    	words = [ "Hello", "hello", "HELLO", "World", "world" ];
		    	result = ArrayUnique( words, true );
		    """,
		    context );
		Array resultArray = ( Array ) variables.get( result );
		assertThat( resultArray.size() ).isEqualTo( 5 );
	}

	@DisplayName( "It works as a member function" )
	@Test
	public void testMemberFunction() {
		instance.executeSource(
		    """
		    	result = [ 1, 1.0, "foo", "FOO", 2 ].unique();
		    """,
		    context );
		Array resultArray = ( Array ) variables.get( result );
		assertThat( resultArray.size() ).isEqualTo( 3 );
		assertThat( resultArray.get( 0 ) ).isEqualTo( 1 );
		assertThat( resultArray.get( 1 ) ).isEqualTo( "foo" );
		assertThat( resultArray.get( 2 ) ).isEqualTo( 2 );
	}
}
