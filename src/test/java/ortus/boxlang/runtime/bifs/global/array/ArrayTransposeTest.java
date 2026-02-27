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

public class ArrayTransposeTest {

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

	@DisplayName( "It can transpose arrays together of the same length" )
	@Test
	public void testCanTransposeArraysOfTheSameLength() {
		instance.executeSource(
		    """
		    	names = [ "James T. Kirk", "Spock", "Odo", "Jonathan Archer" ];
		    	ranks = [ "Captain", "Commander", "Constable", "Captain" ];
		    	species = [ "Human", "Vulcan", "Changeling", "Human" ];

		    	result = arrayTranspose( names, ranks, species );
		    """,
		    context );
		assertThat( ( Array ) variables.get( result ) ).isEqualTo(
		    Array.of(
		        Array.of( "James T. Kirk", "Captain", "Human" ),
		        Array.of( "Spock", "Commander", "Vulcan" ),
		        Array.of( "Odo", "Constable", "Changeling" ),
		        Array.of( "Jonathan Archer", "Captain", "Human" )
		    )
		);
	}
}
