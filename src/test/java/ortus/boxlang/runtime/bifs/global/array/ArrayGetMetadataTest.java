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
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;

public class ArrayGetMetadataTest {

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
}
