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

package ortus.boxlang.runtime.bifs.global.query;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import ortus.boxlang.runtime.types.Struct;

public class QueryGetResultTest {

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

	@DisplayName( "It should return the query metadata" )
	@Test
	public void testGetResult() {

		instance.executeSource(
		    """
		    myQuery = queryNew("id,name",  "integer,varchar", [ {"id":1,"name":"apple"}, {"id":2,"name":"banana"}, {"id":3,"name":"orange"}, {"id":4,"name":"peach"} ]);
		       result = queryGetResult(myQuery);
		       """,
		    context );

		assertTrue( variables.get( Key.result ) instanceof Struct );
		IStruct result = variables.getAsStruct( Key.result );
		assertTrue( result.containsKey( Key.cached ) );
		assertTrue( result.get( Key.cached ) instanceof Boolean );
		assertTrue( result.containsKey( Key.executionTime ) );
		assertTrue( result.containsKey( Key.recordCount ) );
		assertTrue( result.get( Key.recordCount ) instanceof Integer );
		assertThat( result.getAsInteger( Key.recordCount ) ).isEqualTo( 4 );
	}

	@DisplayName( "It should return the query metadata member" )
	@Test
	public void testGetResultMember() {

		instance.executeSource(
		    """
		    myQuery = queryNew("id,name",  "integer,varchar", [ {"id":1,"name":"apple"}, {"id":2,"name":"banana"}, {"id":3,"name":"orange"}, {"id":4,"name":"peach"} ]);
		       result = myQuery.getResult();
		       """,
		    context );

		assertTrue( variables.get( Key.result ) instanceof Struct );
		IStruct result = variables.getAsStruct( Key.result );
		assertTrue( result.containsKey( Key.cached ) );
		assertTrue( result.get( Key.cached ) instanceof Boolean );
		assertTrue( result.containsKey( Key.executionTime ) );
		assertTrue( result.containsKey( Key.recordCount ) );
		assertTrue( result.get( Key.recordCount ) instanceof Integer );
		assertThat( result.getAsInteger( Key.recordCount ) ).isEqualTo( 4 );
	}

}
