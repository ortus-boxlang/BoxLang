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
import ortus.boxlang.runtime.types.IStruct;

public class ArrayGetMetadataTest {

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

	@DisplayName( "It should return a struct containing information about the array" )
	@Test
	public void testReturnValue() {
		instance.executeSource(
		    """
		    arr = [ 1, 2, 3 ];
		    result = ArrayGetMetadata( arr );
		    """,
		    context );
		IStruct meta = ( IStruct ) variables.get( result );
		assertThat( meta.containsKey( "type" ) ).isEqualTo( true );
		assertThat( meta.containsKey( "datatype" ) ).isEqualTo( true );
		assertThat( meta.containsKey( "dimensions" ) ).isEqualTo( true );
		assertThat( meta.get( Key.of( "type" ) ) ).isEqualTo( "synchronized" );
		assertThat( meta.get( Key.of( "dimensions" ) ) ).isEqualTo( 1 );
	}

	@DisplayName( "It should be invocable as a member function" )
	@Test
	public void testMemberInvocation() {
		instance.executeSource(
		    """
		    arr = [ 1, 2, 3 ];
		    result = arr.getMetadata();
		    """,
		    context );
		IStruct meta = ( IStruct ) variables.get( result );
		assertThat( meta.containsKey( "type" ) ).isEqualTo( true );
		assertThat( meta.containsKey( "datatype" ) ).isEqualTo( true );
		assertThat( meta.containsKey( "dimensions" ) ).isEqualTo( true );
	}

	@DisplayName( "It should report synchronized for ArrayNew with default args" )
	@Test
	public void testArrayNewDefaultIsSynchronized() {
		instance.executeSource(
		    """
		    arr = ArrayNew();
		    result = ArrayGetMetadata( arr );
		    """,
		    context );
		IStruct meta = ( IStruct ) variables.get( result );
		assertThat( meta.get( Key.of( "type" ) ) ).isEqualTo( "synchronized" );
	}

	@DisplayName( "It should report synchronized when isSynchronized is true" )
	@Test
	public void testArrayNewSynchronized() {
		instance.executeSource(
		    """
		    arr = ArrayNew( 1, true );
		    result = ArrayGetMetadata( arr );
		    """,
		    context );
		IStruct meta = ( IStruct ) variables.get( result );
		assertThat( meta.get( Key.of( "type" ) ) ).isEqualTo( "synchronized" );
	}

	@DisplayName( "It should report normal when isSynchronized is false" )
	@Test
	public void testArrayNewUnsynchronized() {
		instance.executeSource(
		    """
		    arr = ArrayNew( 1, false );
		    result = ArrayGetMetadata( arr );
		    """,
		    context );
		IStruct meta = ( IStruct ) variables.get( result );
		assertThat( meta.get( Key.of( "type" ) ) ).isEqualTo( "unsynchronized" );
	}

	@DisplayName( "It should report the correct dimensions" )
	@Test
	public void testArrayNewDimensions() {
		instance.executeSource(
		    """
		    arr1 = ArrayNew( 1 );
		    arr2 = ArrayNew( 2 );
		    arr3 = ArrayNew( 3 );
		    result1 = ArrayGetMetadata( arr1 );
		    result2 = ArrayGetMetadata( arr2 );
		    result3 = ArrayGetMetadata( arr3 );
		    """,
		    context );
		IStruct	meta1	= ( IStruct ) variables.get( Key.of( "result1" ) );
		IStruct	meta2	= ( IStruct ) variables.get( Key.of( "result2" ) );
		IStruct	meta3	= ( IStruct ) variables.get( Key.of( "result3" ) );
		assertThat( meta1.get( Key.of( "dimensions" ) ) ).isEqualTo( 1 );
		assertThat( meta2.get( Key.of( "dimensions" ) ) ).isEqualTo( 2 );
		assertThat( meta3.get( Key.of( "dimensions" ) ) ).isEqualTo( 3 );
	}
}
