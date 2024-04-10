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

public class QueryFilterTest {

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

	@DisplayName( "It should filter a query according to the filter criteria" )
	@Test
	public void testFilter() {

		instance.executeSource(
		    """
		    news = queryNew("id,type,title", "integer,varchar,varchar");
		    queryAddRow(news,[{
		    	id: 1,
		    	type: "book",
		    	title: "Cloud Atlas"
		    },{
		    	id: 2,
		    	type: "book",
		    	title: "Lord of The Rings"
		    },{
		    	id: 3,
		    	type: "film",
		    	title: "Men in Black"
		    }]);
		    result = queryFilter(news,function(_news) {
		    	return _news.type is 'book';
		    });
		               """,
		    context );

		Query qry = variables.getAsQuery( result );

		assertThat( qry.size() ).isEqualTo( 2 );
		assertThat( qry.getRowAsStruct( 0 ).getAsInteger( Key.of( "id" ) ) ).isEqualTo( 1 );
		assertThat( qry.getRowAsStruct( 0 ).getAsString( Key.of( "type" ) ) ).isEqualTo( "book" );
		assertThat( qry.getRowAsStruct( 1 ).getAsInteger( Key.of( "id" ) ) ).isEqualTo( 2 );
		assertThat( qry.getRowAsStruct( 1 ).getAsString( Key.of( "type" ) ) ).isEqualTo( "book" );
	}

	@DisplayName( "It should return current row member" )
	@Test
	public void testFilterMember() {

		instance.executeSource(
		    """
		    news = queryNew("id,type,title", "integer,varchar,varchar");
		    queryAddRow(news,[{
		    	id: 1,
		    	type: "book",
		    	title: "Cloud Atlas"
		    },{
		    	id: 2,
		    	type: "book",
		    	title: "Lord of The Rings"
		    },{
		    	id: 3,
		    	type: "film",
		    	title: "Men in Black"
		    }]);
		    result = news.filter(function(_news) {
		    	return _news.type is 'book';
		    });
		               """,
		    context );

		Query qry = variables.getAsQuery( result );

		assertThat( qry.size() ).isEqualTo( 2 );
		assertThat( qry.getRowAsStruct( 0 ).getAsInteger( Key.of( "id" ) ) ).isEqualTo( 1 );
		assertThat( qry.getRowAsStruct( 0 ).getAsString( Key.of( "type" ) ) ).isEqualTo( "book" );
		assertThat( qry.getRowAsStruct( 1 ).getAsInteger( Key.of( "id" ) ) ).isEqualTo( 2 );
		assertThat( qry.getRowAsStruct( 1 ).getAsString( Key.of( "type" ) ) ).isEqualTo( "book" );
	}

}
