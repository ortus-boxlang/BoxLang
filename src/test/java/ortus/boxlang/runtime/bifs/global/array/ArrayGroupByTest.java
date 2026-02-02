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
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

import static com.google.common.truth.Truth.assertThat;

public class ArrayGroupByTest {

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

	@DisplayName( "It groups the value by the returned value of the predicate function" )
	@Test
	public void testGroupBy() {
		instance.executeSource(
		    """
		    	data = [ 1, 2, 3, 4 ];

		    	result = arrayGroupBy( data, ( n ) => n % 2 == 0 ? "even" : "odd" );
		    """,
		    context );
		assertThat( ( IStruct ) variables.get( result ) ).isEqualTo( Struct.of(
		    "odd", Array.of( 1, 3 ),
		    "even", Array.of( 2, 4 )
		) );
	}

	@DisplayName( "It groups the value by the returned value of the predicate function" )
	@Test
	public void testGroupByStruct() {
		instance.executeSource(
		    """
		    	data = [
		    	    { id = 1, name = "James T. Kirk", rank = "Captain", species = "Human" },
		    	    { id = 2, name = "Spock", rank = "Commander", species = "Vulcan" },
		    	    { id = 3, name = "Odo", rank = "Constable", species = "Changeling" },
		    	    { id = 4, name = "Jonathan Archer", rank = "Captain", species = "Human" }
		        ];

		    	result = arrayGroupBy( data, ( n ) => n.rank );
		    """,
		    context );
		assertThat( ( IStruct ) variables.get( result ) ).isEqualTo( Struct.of(
		    "Captain", Array.of(
		        Struct.of( "id", 1, "name", "James T. Kirk", "rank", "Captain", "species", "Human" ),
		        Struct.of( "id", 4, "name", "Jonathan Archer", "rank", "Captain", "species", "Human" )
		    ),
		    "Commander", Array.of(
		        Struct.of( "id", 2, "name", "Spock", "rank", "Commander", "species", "Vulcan" )
		    ),
		    "Constable", Array.of(
		        Struct.of( "id", 3, "name", "Odo", "rank", "Constable", "species", "Changeling" )
		    )
		) );
	}

}
