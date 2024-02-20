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

public class ArrayFindAllTest {

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

	@DisplayName( "It should match numbers" )
	@Test
	public void testMatchNumber() {
		instance.executeSource(
		    """
		        nums = [ 1, 2, 3, 4, 5 ];
		        result = nums.findAll( 3 );
		    """,
		    context );
		Array found = ( Array ) variables.get( result );
		assertThat( found.size() ).isEqualTo( 1 );
		assertThat( found.get( 0 ) ).isEqualTo( 3 );
	}

	@DisplayName( "It should match doubles" )
	@Test
	public void testMatchDoubles() {
		instance.executeSource(
		    """
		        nums = [ 1, 2, 3, 4, 5 ];
		        result = nums.findAll( 3.0 );
		    """,
		    context );
		Array found = ( Array ) variables.get( result );
		assertThat( found.size() ).isEqualTo( 1 );
		assertThat( found.get( 0 ) ).isEqualTo( 3 );
	}

	@DisplayName( "It should match numbers and strings" )
	@Test
	public void testMatchNumberAndString() {
		instance.executeSource(
		    """
		        nums = [ 1, 2, 3, 4, 5, "3" ];
		        result = nums.findAll( 3 );
		    """,
		    context );
		Array found = ( Array ) variables.get( result );
		assertThat( found.size() ).isEqualTo( 2 );
		assertThat( found.get( 0 ) ).isEqualTo( 3 );
		assertThat( found.get( 1 ) ).isEqualTo( 6 );
	}

	@DisplayName( "It should find strings in a case sensitive manner" )
	@Test
	public void testMatchStringCaseSensitive() {
		instance.executeSource(
		    """
		        nums = [ "red", "blue", "orange" ];
		        result = nums.findAll( "bluE" );
		    """,
		    context );
		Array found = ( Array ) variables.get( result );
		assertThat( found.size() ).isEqualTo( 0 );
	}

	@DisplayName( "It should match arrays" )
	@Test
	public void testMatchArrays() {
		instance.executeSource(
		    """
		        nums = [ 1, [3] ];
		        result = nums.findAll( [3] );
		    """,
		    context );
		Array found = ( Array ) variables.get( result );
		assertThat( found.size() ).isEqualTo( 1 );
		assertThat( found.get( 0 ) ).isEqualTo( 2 );
	}

	@DisplayName( "It should match structs" )
	@Test
	public void testMatchStructs() {
		instance.executeSource(
		    """
		        nums = [ 1, [3], { test: true } ];
		        result = nums.findAll( { test: true } );
		    """,
		    context );
		Array found = ( Array ) variables.get( result );
		assertThat( found.size() ).isEqualTo( 1 );
		assertThat( found.get( 0 ) ).isEqualTo( 3 );
	}

	@DisplayName( "It should find all matches" )
	@Test
	public void testMatchAll() {
		instance.executeSource(
		    """
		        nums = [ 1, [3], true, { test: true }, "1", 1.0, 4, "1.0" ];
		        result = nums.findAll( 1 );
		    """,
		    context );
		Array found = ( Array ) variables.get( result );
		assertThat( found.size() ).isEqualTo( 5 );
		assertThat( found.get( 0 ) ).isEqualTo( 1 );
		assertThat( found.get( 1 ) ).isEqualTo( 3 );
		assertThat( found.get( 2 ) ).isEqualTo( 5 );
		assertThat( found.get( 3 ) ).isEqualTo( 6 );
		assertThat( found.get( 4 ) ).isEqualTo( 8 );
	}

	@DisplayName( "It should match every number variation when compared with an int" )
	@Test
	public void testMatchNumberVariationsInt() {
		instance.executeSource(
		    """
		        nums = [ 1, "1", 1.0, "1.0", true, "true", "yes" ];
		        result = nums.findAll( 1 );
		    """,
		    context );
		Array found = ( Array ) variables.get( result );
		assertThat( found.size() ).isEqualTo( 7 );
	}

	@DisplayName( "It should match every number variation when compared with a string int" )
	@Test
	public void testMatchNumberVariationsStringInt() {
		instance.executeSource(
		    """
		        nums = [ 1, "1", 1.0, "1.0", true, "true", "yes" ];
		        result = nums.findAll( "1" );
		    """,
		    context );
		Array found = ( Array ) variables.get( result );
		assertThat( found.size() ).isEqualTo( 7 );
	}

	@DisplayName( "It should match every number variation when compared with a double" )
	@Test
	public void testMatchNumberVariationsDouble() {
		instance.executeSource(
		    """
		        nums = [ 1, "1", 1.0, "1.0", true, "true", "yes" ];
		        result = nums.findAll( 1.0 );
		    """,
		    context );
		Array found = ( Array ) variables.get( result );
		assertThat( found.size() ).isEqualTo( 7 );
	}

	@DisplayName( "It should match every number variation when compared with a string double" )
	@Test
	public void testMatchNumberVariationsStringDouble() {
		instance.executeSource(
		    """
		        nums = [ 1, "1", 1.0, "1.0", true, "true", "yes" ];
		        result = nums.findAll( "1.0" );
		    """,
		    context );
		Array found = ( Array ) variables.get( result );
		assertThat( found.size() ).isEqualTo( 7 );
	}

	@DisplayName( "It should match every number variation when compared with a boolean" )
	@Test
	public void testMatchNumberVariationsBoolean() {
		instance.executeSource(
		    """
		        nums = [ 1, "1", 1.0, "1.0", true, "true", "yes" ];
		        result = nums.findAll( true );
		    """,
		    context );
		Array found = ( Array ) variables.get( result );
		assertThat( found.size() ).isEqualTo( 7 );
	}

	@DisplayName( "It should match every number variation when compared with a string boolean" )
	@Test
	public void testMatchNumberVariationsStringBoolean() {
		instance.executeSource(
		    """
		        nums = [ 1, "1", 1.0, "1.0", true, "true", "yes" ];
		        result = nums.findAll( "true" );
		    """,
		    context );
		Array found = ( Array ) variables.get( result );
		assertThat( found.size() ).isEqualTo( 7 );
	}

	@DisplayName( "It should match every number variation when compared with yes" )
	@Test
	public void testMatchNumberVariationsYes() {
		instance.executeSource(
		    """
		        nums = [ 1, "1", 1.0, "1.0", true, "true", "yes" ];
		        result = nums.findAll( "yes" );
		    """,
		    context );
		Array found = ( Array ) variables.get( result );
		assertThat( found.size() ).isEqualTo( 7 );
	}

	@DisplayName( "It should match using a provided UDF" )
	@Test
	public void testMatchUsingUDF() {
		instance.executeSource(
		    """
		        nums = [ 1, "1", 1.0, "1.0", "red", "red", true, "true", "yes" ];
		        result = nums.findAll( value -> value == "red" );
		    """,
		    context );
		Array found = ( Array ) variables.get( result );
		assertThat( found.size() ).isEqualTo( 2 );
		assertThat( found.get( 0 ) ).isEqualTo( 5 );
		assertThat( found.get( 1 ) ).isEqualTo( 6 );
	}

	@DisplayName( "It should find strings in a case insensitive manner when using nocase" )
	@Test
	public void testMatchStringCaseInSensitive() {
		instance.executeSource(
		    """
		        nums = [ "red", "blue", "orange" ];
		        result = nums.findAllNoCase( "bluE" );
		    """,
		    context );
		Array found = ( Array ) variables.get( result );
		assertThat( found.size() ).isEqualTo( 1 );
		assertThat( found.get( 0 ) ).isEqualTo( 2 );
	}
}
