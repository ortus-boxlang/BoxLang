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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.bifs.global.jdbc.BaseJDBCTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

public class QueryGetResultTest extends BaseJDBCTest {

	static Key result = new Key( "result" );

	@DisplayName( "It should return the query metadata" )
	@Test
	public void testGetResult() {

		getInstance().executeSource(
		    """
		    myQuery = queryNew("id,name",  "integer,varchar", [ {"id":1,"name":"apple"} ]);
		    result = queryGetResult(myQuery);
		    """,
		    getContext() );

		assertTrue( getVariables().get( Key.result ) instanceof Struct );
		IStruct result = getVariables().getAsStruct( Key.result );
		assertTrue( result.containsKey( Key.recordCount ) );
		assertTrue( result.containsKey( Key.columns ) );
		assertTrue( result.get( Key.recordCount ) instanceof Integer );
		assertThat( result.getAsInteger( Key.recordCount ) ).isEqualTo( 1 );
	}

	@DisplayName( "It should return the query metadata member" )
	@Test
	public void testGetResultMember() {

		getInstance().executeSource(
		    """
		    myQuery = queryNew("id,name","integer,varchar", [ {"id":1,"name":"apple"} ]);
		    result = myQuery.getResult();
		    """,
		    getContext() );

		assertTrue( getVariables().get( Key.result ) instanceof Struct );
		IStruct result = getVariables().getAsStruct( Key.result );
		assertTrue( result.containsKey( Key.recordCount ) );
		assertTrue( result.containsKey( Key.columns ) );
		assertTrue( result.get( Key.recordCount ) instanceof Integer );
		assertThat( result.getAsInteger( Key.recordCount ) ).isEqualTo( 1 );
	}

	@DisplayName( "It should return JDBC query metadata on JDBC queries" )
	@Test
	public void testJDBCQueryMeta() {
		getInstance().executeSource(
		    """
		       myQuery = queryExecute( "SELECT * FROM developers ORDER BY id" );
		       result = queryGetResult(myQuery);
		    """,
		    getContext() );

		assertTrue( getVariables().get( Key.result ) instanceof Struct );
		IStruct result = getVariables().getAsStruct( Key.result );
		assertTrue( result.containsKey( Key.recordCount ) );
		assertTrue( result.get( Key.recordCount ) instanceof Integer );
		assertThat( result.getAsInteger( Key.recordCount ) ).isEqualTo( 4 );
		assertTrue( result.containsKey( Key.cached ) );
		assertTrue( result.get( Key.cached ) instanceof Boolean );
		assertTrue( result.containsKey( Key.executionTime ) );

		assertTrue( result.containsKey( Key.sql ) );
		assertTrue( result.get( Key.sql ) instanceof String );
		assertTrue( result.containsKey( Key.sqlParameters ) );
		assertTrue( result.get( Key.sqlParameters ) instanceof Array );
		assertTrue( result.containsKey( Key.cacheProvider ) );
		assertTrue( result.containsKey( Key.cacheKey ) );
		assertTrue( result.containsKey( Key.cacheTimeout ) );
		assertTrue( result.containsKey( Key.cacheLastAccessTimeout ) );
	}

	@Disabled( "Disabled until QOQ implementation is complete" )
	@DisplayName( "It should return query metadata on QOQs" )
	@Test
	public void testQoQQueryMeta() {
		getInstance().executeSource(
		    """
		       fruit = queryNew("id,name",  "integer,varchar", [ {"id":1,"name":"apple"}, {"id":2,"name":"banana"}, {"id":3,"name":"orange"}, {"id":4,"name":"peach"} ]);
		       myQuery = queryExecute( "select * from fruit where id < 4",{},{dbtype="query"});
		       result = queryGetResult(myQuery);
		    """,
		    getContext() );

		assertTrue( getVariables().get( Key.result ) instanceof Struct );
		IStruct result = getVariables().getAsStruct( Key.result );
		assertTrue( result.containsKey( Key.recordCount ) );
		assertTrue( result.get( Key.recordCount ) instanceof Integer );
		assertThat( result.getAsInteger( Key.recordCount ) ).isEqualTo( 3 );
		assertTrue( result.containsKey( Key.cached ) );
		assertTrue( result.get( Key.cached ) instanceof Boolean );
		assertTrue( result.containsKey( Key.executionTime ) );
		assertTrue( result.containsKey( Key.sql ) );
		assertTrue( result.get( Key.sql ) instanceof String );
		assertTrue( result.containsKey( Key.sqlParameters ) );
		assertTrue( result.get( Key.sqlParameters ) instanceof Array );
		assertTrue( result.containsKey( Key.cacheProvider ) );
		assertTrue( result.containsKey( Key.cacheKey ) );
		assertTrue( result.containsKey( Key.cacheTimeout ) );
		assertTrue( result.containsKey( Key.cacheLastAccessTimeout ) );
	}

}
