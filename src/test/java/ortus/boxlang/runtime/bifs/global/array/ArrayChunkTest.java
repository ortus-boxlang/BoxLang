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

import org.junit.jupiter.api.*;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;

import static com.google.common.truth.Truth.assertThat;

public class ArrayChunkTest {

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

	@DisplayName( "It chunks an array given a size" )
	@Test
	public void testCanChunkAnArray() {
		instance.executeSource(
		    """
		    	data = [ 1, 2, 3, 4, 5, 6, 7, 8, 9 ];

		    	result = arrayChunk( data, 3 );
		    """,
		    context );
		assertThat( ( Array ) variables.get( result ) ).isEqualTo(
		    Array.of(
		        Array.of( 1, 2, 3 ),
		        Array.of( 4, 5, 6 ),
		        Array.of( 7, 8, 9 )
		    )
		);
	}

	@DisplayName( "It adds the remaining values to the last chunk even if it is not the full chunk size" )
	@Test
	public void testCanChunkAnArrayUnevenNumbers() {
		instance.executeSource(
		    """
		    	data = [ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 ];

		    	result = arrayChunk( data, 3 );
		    """,
		    context );
		assertThat( ( Array ) variables.get( result ) ).isEqualTo(
		    Array.of(
		        Array.of( 1, 2, 3 ),
		        Array.of( 4, 5, 6 ),
		        Array.of( 7, 8, 9 ),
		        Array.of( 10, 11 )
		    )
		);
	}
}
