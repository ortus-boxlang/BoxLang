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

package ortus.boxlang.runtime.bifs.global.stream;

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
import ortus.boxlang.runtime.types.Query;

public class ToBXQueryTest {

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

	@DisplayName( "It can collect a stream into a query" )
	@Test
	public void testCanCollect() {
		instance.executeSource(
		    """
		    qry = queryNew( "name,title", "varchar,varchar" );
		    [
		    	{ name: "Brad", title: "Developer" },
		    	{ name: "Luis", title: "CEO" },
		    	{ name: "Jorge", title: "PM" }
		    ].stream().toBXQuery( qry );

		    result = qry;
		                 """,
		    context );
		assertTrue( variables.get( result ) instanceof Query );
		assertThat( ( variables.getAsQuery( result ) ).size() ).isEqualTo( 3 );
		assertThat( ( variables.getAsQuery( result ) ).getRowAsStruct( 0 ).get( "name" ) ).isEqualTo( "Brad" );
		assertThat( ( variables.getAsQuery( result ) ).getRowAsStruct( 0 ).get( "title" ) ).isEqualTo( "Developer" );
		assertThat( ( variables.getAsQuery( result ) ).getRowAsStruct( 1 ).get( "name" ) ).isEqualTo( "Luis" );
		assertThat( ( variables.getAsQuery( result ) ).getRowAsStruct( 1 ).get( "title" ) ).isEqualTo( "CEO" );
		assertThat( ( variables.getAsQuery( result ) ).getRowAsStruct( 2 ).get( "name" ) ).isEqualTo( "Jorge" );
		assertThat( ( variables.getAsQuery( result ) ).getRowAsStruct( 2 ).get( "title" ) ).isEqualTo( "PM" );
	}

}
