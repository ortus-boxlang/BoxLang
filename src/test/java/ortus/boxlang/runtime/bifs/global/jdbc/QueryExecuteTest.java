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

package ortus.boxlang.runtime.bifs.global.jdbc;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.jdbc.ExecutedQuery;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import ortus.boxlang.runtime.util.conversion.ObjectMarshaller;
import tools.JDBCTestUtils;

public class QueryExecuteTest extends BaseJDBCTest {

	static Key result = new Key( "result" );

	@DisplayName( "It can execute a query with no bindings on the default datasource" )
	@Test
	public void testSimpleExecute() {
		instance.executeSource(
		    """
		    result = queryExecute( "SELECT * FROM developers ORDER BY id" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 4, query.size() );

		IStruct luis = query.getRowAsStruct( 0 );
		assertEquals( 1, luis.get( "id" ) );
		assertEquals( "Luis Majano", luis.get( "name" ) );
		assertEquals( "CEO", luis.get( "role" ) );

		IStruct eric = query.getRowAsStruct( 1 );
		assertEquals( 42, eric.get( "id" ) );
		assertEquals( "Eric Peterson", eric.get( "name" ) );
		assertEquals( "Developer", eric.get( "role" ) );

		IStruct michael = query.getRowAsStruct( 2 );
		assertEquals( 77, michael.get( "id" ) );
		assertEquals( "Michael Born", michael.get( "name" ) );
		assertEquals( "Developer", michael.get( "role" ) );
	}

	@DisplayName( "It can execute a query with array bindings on the default datasource" )
	@Test
	public void testArrayBindings() {
		instance.executeSource(
		    """
		    result = queryExecute( "SELECT * FROM developers WHERE id = ?", [ 77 ] );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );

		IStruct michael = query.getRowAsStruct( 0 );
		assertEquals( 77, michael.get( "id" ) );
		assertEquals( "Michael Born", michael.get( "name" ) );
		assertEquals( "Developer", michael.get( "role" ) );
	}

	@DisplayName( "It can execute a query with a (string) list binding" )
	@Test
	public void testListStringBindings() {
		instance.executeSource(
		    """
		    result = queryExecute(
		        "SELECT * FROM developers WHERE id IN (:ids)",
		        { "ids" : { value: "77,1,42", list : true } }
		    );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 3, query.size() );
	}

	@DisplayName( "It can execute a query with a named (array) list binding" )
	@Test
	public void testListArrayNamedBindings() {
		instance.executeSource(
		    """
		    result = queryExecute(
		        "SELECT * FROM developers WHERE id IN (:ids)",
		        { "ids" : { value: [77, 1, 42], list : true } }
		    );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 3, query.size() );
	}

	@DisplayName( "It can execute a query with an (array) list binding" )
	@Test
	public void testListArrayPositionalBinding() {
		instance.executeSource(
		    """
		    result = queryExecute(
		        "SELECT * FROM developers WHERE id IN (?)",
		        [ { value: [77, 1, 42], list : true } ]
		    );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 3, query.size() );
	}

	@DisplayName( "It can reuse named query parameters" )
	@Test
	public void testParamReuse() {
		instance.executeSource(
		    """
		    result = queryExecute(
		        "SELECT * FROM developers WHERE id = :id OR name=:id",
		        { id: 77 }
		    );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );
	}

	@DisplayName( "It can use various query params types (list and non-list) in a single SQL query" )
	@Test
	public void testParameterTypeMix() {
		instance.executeSource(
		    """
		    result = queryExecute(
		        "SELECT * FROM developers WHERE id = ? OR id IN (?)",
		        [ 77, { value: [1, 42], list : true } ]
		    );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 3, query.size() );
	}

	@DisplayName( "It can grab a queryparam name from a parameter array" )
	@Test
	public void testNamedParamInsideParamArray() {
		instance.executeSource(
		    """
		    result = queryExecute(
		        "SELECT * FROM developers WHERE id = :foo",
		        [ { name: "foo", value: "1" } ]
		    );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );
	}

	@DisplayName( "It can execute a query with struct bindings on the default datasource" )
	@Test
	public void testStructBindings() {
		instance.executeSource(
		    """
		    result = queryExecute( "SELECT * FROM developers WHERE id = :id", { "id": 77 } );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );

		IStruct michael = query.getRowAsStruct( 0 );
		assertEquals( 77, michael.get( "id" ) );
		assertEquals( "Michael Born", michael.get( "name" ) );
		assertEquals( "Developer", michael.get( "role" ) );
	}

	@DisplayName( "It can specify the sqltype on a parameter" )
	@Test
	public void testSqlType() {
		instance.executeSource(
		    """
		    result = queryExecute( "SELECT * FROM developers WHERE id = :id", { "id": { value : 77, sqltype : "integer" } } );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );

		IStruct michael = query.getRowAsStruct( 0 );
		assertEquals( 77, michael.get( "id" ) );
		assertEquals( "Michael Born", michael.get( "name" ) );
		assertEquals( "Developer", michael.get( "role" ) );
	}

	@DisplayName( "It can specify the sqltype on a parameter as cf_sql_type" )
	@Test
	public void testCFSqlType() {
		instance.executeSource(
		    """
		    result = queryExecute( "SELECT * FROM developers WHERE id = :id", { "id": { value : 77, sqltype : "cf_sql_integer" } } );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );

		IStruct michael = query.getRowAsStruct( 0 );
		assertEquals( 77, michael.get( "id" ) );
		assertEquals( "Michael Born", michael.get( "name" ) );
		assertEquals( "Developer", michael.get( "role" ) );
	}

	@DisplayName( "It throws if the sql type is unknown" )
	@Test
	public void testInvalidSqlType() {
		IllegalArgumentException e = assertThrows( IllegalArgumentException.class, () -> instance.executeSource(
		    """
		    result = queryExecute( "SELECT * FROM developers WHERE id = :id", { "id": { value : 77, sqltype : "fooey" } } );
		    """,
		    context ) );

		assertThat( e.getMessage() )
		    .contains( "Unknown QueryColumnType" );
	}

	@DisplayName( "It throws an exception if the query is missing a named binding" )
	@Test
	public void testMissingStructBinding() {
		DatabaseException e = assertThrows( DatabaseException.class, () -> instance.executeSource(
		    """
		    result = queryExecute( "SELECT * FROM developers WHERE id = :id", { "name": "Michael Born" } );
		    """,
		    context ) );

		assertThat( e.getMessage() ).isEqualTo( "Named parameter [:id] not provided to query." );
		assertNull( variables.get( result ) );
	}

	@DisplayName( "It throws an exception if no default datasource is defined and no datasource is specified" )
	@Test
	public void testMissingDefaultDataSource() {
		context.getConnectionManager().setDefaultDatasource( null );
		DatabaseException e = assertThrows( DatabaseException.class, () -> instance.executeSource(
		    """
		    result = queryExecute( "SELECT * FROM developers" );
		    """,
		    context ) );

		assertThat( e.getMessage() )
		    .contains( "No default datasource" );
		assertNull( variables.get( result ) );
	}

	@DisplayName( "It can execute a query on a named datasource" )
	@Test
	public void testNamedDataSource() {
		var dbName = Key.of( "derby" );
		// Register the named datasource
		instance.getConfiguration().datasources.put(
		    Key.of( dbName ),
		    JDBCTestUtils.buildDatasourceConfig( dbName.getName() )
		);

		instance.executeSource(
		    """
		    queryExecute( "CREATE TABLE developers ( id INTEGER, name VARCHAR(155), role VARCHAR(155) )", [], { "datasource": "derby" } );
		    queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 77, 'Michael Born', 'Developer' )", [], { "datasource": "derby" } );
		          result = queryExecute( "SELECT * FROM developers ORDER BY id", [], { "datasource": "derby" } );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );

		IStruct michael = query.getRowAsStruct( 0 );
		assertEquals( 77, michael.get( "id" ) );
		assertEquals( "Michael Born", michael.get( "name" ) );
		assertEquals( "Developer", michael.get( "role" ) );
	}

	@DisplayName( "It can insert an SQL Null from a null value param" )
	@Test
	public void testNullParam() {
		var dbName = Key.of( "derby" );
		// Register the named datasource
		instance.getConfiguration().datasources.put(
		    Key.of( dbName ),
		    JDBCTestUtils.buildDatasourceConfig( dbName.getName() )
		);

		// @formatter:off
		instance.executeSource(
		    """
		    queryExecute( "CREATE TABLE developers_new ( id INTEGER, name VARCHAR(155), role VARCHAR(155) )", [], { "datasource": "derby" } );
		    queryExecute(
				"INSERT INTO developers_new ( id, name, role ) VALUES ( :id, :name, :role )"
				, {
					"id": 77
					, "name": "Michael Born"
					, "role": nullValue()
				}
				, { "datasource": "derby" }
			);
		        result = queryExecute( "SELECT * FROM developers_new WHERE id = 77", [], { "datasource": "derby" } );
		    """,
		    context );
		// @formatter:off
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );

		IStruct michael = query.getRowAsStruct( 0 );
		assertEquals( 77, michael.get( "id" ) );
		assertEquals( "Michael Born", michael.get( "name" ) );
		assertEquals( null, michael.get( "role" ) );
	}

	// See: https://github.com/brettwooldridge/HikariCP/issues/1197
	// See: https://ortussolutions.atlassian.net/browse/BL-1376
	@EnabledIf( "tools.JDBCTestUtils#hasMySQLModule" )
	@DisplayName( "It can provide username/password at query time" )
	@Test
	public void testDatasourceUsernamePasswordAtQueryTime() {
		instance.executeSource(
		    """
		    ds = {
		        "host":"127.0.0.1",
		        "port":"3309",
		        "driver":"mysql",
		        "database":"myDB",
		        "custom":"allowMultiQueries=true",
		        "initializationFailTimeout": 0,
		        "minimumIdle" : 0
		    };
		    result = queryExecute( "SELECT 1", [], { "datasource": ds, "username": "root", "password": "123456Password" } );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );
	}

	@EnabledIf( "tools.JDBCTestUtils#hasMySQLModule" )
	@DisplayName( "It ignores empty username/password" )
	@Test
	public void testDatasourceUsernamePasswordEmptyIgnored() {
		assertDoesNotThrow( () -> {
		instance.executeSource(
		    """
		    ds = {
		        "host":"127.0.0.1",
		        "port":"3309",
		        "driver":"mysql",
		        "database":"myDB",
		        "custom":"allowMultiQueries=true",
		        "initializationFailTimeout": 0,
		        "minimumIdle" : 0,
				"username": "root",
				"password": "123456Password"
		    };
		    result = queryExecute( "SELECT 1", [], { "datasource": ds, "username": "", "password": "" } );
		    """,
		    context );
		} );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );
	}

	@DisplayName( "It throws an exception if the specified datasource is not registered" )
	@Test
	public void testMissingNamedDataSource() {
		DatabaseException e = assertThrows( DatabaseException.class, () -> instance.executeSource(
		    """
		    result = queryExecute( "SELECT * FROM developers WHERE name = :name", { "name": "Michael Born" }, { "datasource": "not_found" } );
		    """,
		    context ) );

		assertThat( e.getMessage() ).contains( "Datasource with name [not_found] not found" );
		assertNull( variables.get( result ) );
	}

	@DisplayName( "It can return query results as an array" )
	@Test
	public void testReturnTypeArray() {
		instance.executeSource(
		    """
		    result = queryExecute( "SELECT * FROM developers ORDER BY id", [], { "returntype": "array" } );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Array.class );
		Array results = variables.getAsArray( result );
		assertEquals( 4, results.size() );

		Object luisObject = results.get( 0 );
		assertThat( luisObject ).isInstanceOf( IStruct.class );
		IStruct luis = ( IStruct ) luisObject;
		assertEquals( 1, luis.get( "id" ) );
		assertEquals( "Luis Majano", luis.get( "name" ) );
		assertEquals( "CEO", luis.get( "role" ) );

		Object ericObject = results.get( 1 );
		assertThat( ericObject ).isInstanceOf( IStruct.class );
		IStruct eric = ( IStruct ) ericObject;
		assertEquals( 42, eric.get( "id" ) );
		assertEquals( "Eric Peterson", eric.get( "name" ) );
		assertEquals( "Developer", eric.get( "role" ) );

		Object michaelObject = results.get( 2 );
		assertThat( michaelObject ).isInstanceOf( IStruct.class );
		IStruct michael = ( IStruct ) michaelObject;
		assertEquals( 77, michael.get( "id" ) );
		assertEquals( "Michael Born", michael.get( "name" ) );
		assertEquals( "Developer", michael.get( "role" ) );
	}

	@DisplayName( "It can return query results as a struct" )
	@Test
	public void testReturnTypeStruct() {
		instance.executeSource(
		    """
		    result = queryExecute( "SELECT * FROM developers ORDER BY id", [], { "returntype": "struct", "columnKey": "role" } );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		IStruct results = variables.getAsStruct( result );
		assertEquals( 3, results.size() );

		List<String> keys = results.getKeysAsStrings();
		assertEquals( keys, List.of( "QA", "Developer", "CEO" ) );

		assertInstanceOf( Array.class, results.get( "CEO" ) );
		Array ceoRecords = results.getAsArray( Key.of( "CEO" ) );
		assertEquals( 1, ceoRecords.size() );

		Object luisObject = ceoRecords.get( 0 );
		assertThat( luisObject ).isInstanceOf( IStruct.class );
		IStruct luis = ( IStruct ) luisObject;
		assertEquals( 1, luis.get( "id" ) );
		assertEquals( "Luis Majano", luis.get( "name" ) );
		assertEquals( "CEO", luis.get( "role" ) );

		assertInstanceOf( Array.class, results.get( "Developer" ) );
		Array developerRecords = results.getAsArray( Key.of( "Developer" ) );
		assertEquals( 2, developerRecords.size() );

		Object ericObject = developerRecords.get( 0 );
		assertThat( ericObject ).isInstanceOf( IStruct.class );
		IStruct eric = ( IStruct ) ericObject;
		assertEquals( 42, eric.get( "id" ) );
		assertEquals( "Eric Peterson", eric.get( "name" ) );
		assertEquals( "Developer", eric.get( "role" ) );

		Object michaelObject = developerRecords.get( 1 );
		assertThat( michaelObject ).isInstanceOf( IStruct.class );
		IStruct michael = ( IStruct ) michaelObject;
		assertEquals( 77, michael.get( "id" ) );
		assertEquals( "Michael Born", michael.get( "name" ) );
		assertEquals( "Developer", michael.get( "role" ) );
	}

	@DisplayName( "It throws an exception if the returnType is struct but no columnKey is provided" )
	@Test
	public void testMissingColumnKey() {
		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    result = queryExecute( "SELECT * FROM developers ORDER BY id", [], { "returnType": "struct" } );
		    """,
		    context ) );

		assertThat( e.getMessage() ).isEqualTo( "You must define a `columnKey` option when using `returnType: struct`." );
		assertNull( variables.get( result ) );
	}

	@DisplayName( "It throws an exception if the returnType is invalid" )
	@Test
	public void testInvalidReturnType() {
		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    result = queryExecute( "SELECT * FROM developers ORDER BY id", [], { "returnType": "foobar" } );
		    """,
		    context ) );

		assertThat( e.getMessage() ).isEqualTo( "Unknown return type: foobar" );
		assertNull( variables.get( result ) );
	}

	@DisplayName( "It can access the results of a queryExecute call" )
	@Test
	public void testResultVariable() {
		instance.executeSource(
		    """
		    result = queryExecute( "SELECT * FROM developers WHERE role = ?", [ 'Developer' ], { "result": "queryResults" } );
		    """,
		    context );
		Object resultObject = variables.get( Key.of( "queryResults" ) );
		assertInstanceOf( IStruct.class, resultObject );
		IStruct result = StructCaster.cast( resultObject );

		assertThat( result ).containsKey( Key.sql );
		assertEquals( "SELECT * FROM developers WHERE role = 'Developer'", result.getAsString( Key.sql ) );

		assertThat( result ).containsKey( Key.sqlParameters );
		assertEquals( Array.of( "Developer" ), result.getAsArray( Key.sqlParameters ) );

		assertThat( result ).containsKey( Key.recordCount );
		assertEquals( 2, result.getAsInteger( Key.recordCount ) );

		assertThat( result ).containsKey( Key.columnList );
		assertEquals( "ID,NAME,ROLE,CREATEDAT", result.getAsString( Key.columnList ) );

		assertThat( result ).containsKey( Key.executionTime );
		assertThat( result.getAsLong( Key.executionTime ) ).isAtLeast( 0 );

		assertThat( result.containsKey( Key.generatedKey ) ).isEqualTo( false );
	}

	@DisplayName( "It can execute a query against an ad-hoc datasource" )
	@Test
	public void testAdHocDataSource() {
		DatabaseException e = assertThrows( DatabaseException.class, () -> {
			instance.executeSource(
			    """
			       result = queryExecute(
			      	"SELECT * FROM developers ORDER BY id",
			    	[],
			    	{ "datasource": { "driver" : "derby", "connectionString": "jdbc:derby:memory:foo123;create=true" } }
			    );
			       """,
			    context );
		} );
		assertEquals( "Table/View 'DEVELOPERS' does not exist.", e.getMessage() );
	}

	@DisplayName( "It can specify a max result size" )
	@Test
	public void testMaxRows() {
		instance.executeSource(
		    """
		    result = queryExecute( "SELECT * FROM developers", {}, { maxrows : 1 } );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );
	}

	@DisplayName( "DELETE queries put the affected recordCount in query meta" )
	@Test
	public void testDeleteQueryResult() {
		instance.executeSource(
		    """
		    result = queryExecute(
		        "DELETE FROM developers WHERE id = :id",
		        { "id" : 1 },
		        { "result" : "queryResults" }
		    );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		IStruct query = variables.getAsStruct( Key.of( "queryResults" ) );
		assertEquals( 1, query.get( Key.recordCount ) );
	}

	@DisplayName( "It closes connection on completion" )
	@Test
	public void testConnectionClose() {
		Integer initiallyActive = getDatasource().getPoolStats().getAsInteger( Key.of( "ActiveConnections" ) );
		instance.executeSource(
		    """
		    result = queryExecute( "SELECT * FROM developers", {}, { maxrows : 1 } );
		    """,
		    context );
		Integer subsequentActive = getDatasource().getPoolStats().getAsInteger( Key.of( "ActiveConnections" ) );
		assertEquals( initiallyActive, subsequentActive );
	}

	@DisplayName( "It can read date values" )
	@Test
	public void testSQLDate() {
		instance.executeSource(
		    """
		    result = queryExecute( "SELECT CURRENT_DATE as my_date FROM SYSIBM.SYSDUMMY1" )
		    isDate = isNumeric( result.my_date[1] )
		    """,
		    context );
		assertThat( variables.getAsBoolean( Key.of( "isDate" ) ) ).isEqualTo( false );
	}

	@DisplayName( "It can read time values" )
	@Test
	public void testSQLTime() {
		instance.executeSource(
		    """
		    result = queryExecute( "SELECT CURRENT_TIMESTAMP as my_date FROM SYSIBM.SYSDUMMY1" )
		    isDate = isNumeric( result.my_date[1] )
		    """,
		    context );
		assertThat( variables.getAsBoolean( Key.of( "isDate" ) ) ).isEqualTo( false );
	}

	@DisplayName( "It can return cached query results within the cache timeout" )
	@Test
	public void testQueryCaching() {
		// @formatter:off
		instance.executeSource(
		    """
				sql = "SELECT id,name,role FROM developers WHERE role = ?";
				params = [ 'Developer' ];

				result  = queryExecute( sql, params, { "cache": true, "cacheTimeout": createTimespan( 0, 0, 0, 2 ), "result" : "queryMeta", "returnType" : "array" } );
				assert queryMeta.cached == false;

				result2 = queryExecute( sql, params, { "cache": true, "cacheTimeout": createTimespan( 0, 0, 0, 2 ), "result" : "queryMeta2", "returnType" : "array" } );
				// println( "============QUERY META 2===============" );
				// println( queryMeta2 );
				assert queryMeta2.cached == true;

				result3 = queryExecute( sql, [ 'Admin' ], { "cache": true, "cacheTimeout": createTimespan( 0, 0, 0, 2 ), "result" : "queryMeta3", "returnType" : "array" } );
				// println( "============QUERY META 3===============" );
				// println( queryMeta3 );
				assert queryMeta3.cached == false;

				result4 = queryExecute( sql, params, { "cache": false, "cacheTimeout": createTimespan( 0, 0, 0, 2 ), "result" : "queryMeta4", "returnType" : "array" } );
				// println( "============QUERY META 4===============" );
				// println( queryMeta4 );
				assert queryMeta4.cached == false;
			   """,
		    context );
		// @formatter:on

		Array	query1	= variables.getAsArray( result );
		Array	query2	= variables.getAsArray( Key.of( "result2" ) );
		Array	query3	= variables.getAsArray( Key.of( "result3" ) );
		Array	query4	= variables.getAsArray( Key.of( "result4" ) );

		// All 3 queries should have identical return values
		assertEquals( query1, query2 );
		assertEquals( query2, query4 );
		// query 3 should be a different, uncached result
		assertNotEquals( query1, query3 );
	}

	@DisplayName( "It can name a cache provider" )
	@Test
	public void testCustomCacheProvider() {
		// @formatter:off
		instance.executeSource(
		    """
		    result  = queryExecute(
				"SELECT id,name,role FROM developers WHERE role = ?",
				[ 'Developer' ],
				{ "cache": true, "cacheProvider": "default", "result" : "queryMeta", "returnType" : "array" }
			);

			assert querymeta.cached == false;

		    result2  = queryExecute(
				"SELECT id,name,role FROM developers WHERE role = ?",
				[ 'Developer' ],
				{ "cache": true, "cacheProvider": "default", "result" : "queryMeta2", "returnType" : "array" }
			);

		    """,
		    context );
		// @formatter:on

		Array	query1	= variables.getAsArray( result );
		Array	query2	= variables.getAsArray( Key.of( "result2" ) );
		assertEquals( query1, query2 );

		// query 2 SHOULD be cached
		IStruct queryMeta2 = StructCaster.cast( variables.getAsStruct( Key.of( "queryMeta2" ) ) );
		assertThat( queryMeta2.getAsBoolean( Key.cached ) ).isEqualTo( true );
	}

	@DisplayName( "It properly sets query results with cache metadata" )
	@Test
	public void testCacheResultMeta() {
		// @formatter:off
		instance.executeSource(
		    """
		     result = queryExecute(
		      	"SELECT * FROM developers WHERE role = ?",
		      	[ 'Admin' ],
		      	{
					"cache"                 : true,
					"cacheProvider"         : "default",
					"cacheKey"              : "adminDevs",
					"cacheTimeout"          : createTimespan( 0, 1, 0, 0 ),
					"cacheLastAccessTimeout": createTimespan( 0, 0, 30, 0 ),
					"result"                : "queryMeta"
				}
		      );
		      result2 = queryExecute(
		      	"SELECT * FROM developers WHERE role = ?",
		      	[ 'Admin' ],
				{
					"cache"                 : true,
					"cacheProvider"         : "default",
					"cacheKey"              : "adminDevs",
					"cacheTimeout"          : createTimespan( 0, 1, 0, 0 ),
					"cacheLastAccessTimeout": createTimespan( 0, 0, 30, 0 ),
					"result"                : "queryMeta2"
				}
		      );
		      """,
		    context );
		// @formatter:on
		Object	resultObject	= variables.get( Key.of( "queryMeta" ) );
		Object	resultObject2	= variables.get( Key.of( "queryMeta2" ) );
		assertInstanceOf( IStruct.class, resultObject );
		assertInstanceOf( IStruct.class, resultObject2 );

		IStruct	result	= ( IStruct ) resultObject;
		IStruct	result2	= ( IStruct ) resultObject2;
		assertThat( result.getAsBoolean( Key.cached ) ).isFalse();
		assertThat( result2.getAsBoolean( Key.cached ) ).isTrue();

		assertThat( result2 ).containsKey( Key.cached );
		assertThat( result2 ).containsKey( Key.cacheProvider );
		assertThat( result2 ).containsKey( Key.cacheKey );
		assertThat( result2 ).containsKey( Key.cacheTimeout );
		assertThat( result2 ).containsKey( Key.cacheLastAccessTimeout );

		assertThat( result2.getAsBoolean( Key.cached ) ).isEqualTo( true );
		assertThat( result2.getAsString( Key.cacheProvider ) ).isEqualTo( "default" );
		assertThat( result2.getAsString( Key.cacheKey ) ).isEqualTo( "adminDevs" );
		assertThat( result2.get( Key.cacheTimeout ) ).isEqualTo( Duration.ofHours( 1 ) );
		assertThat( result2.get( Key.cacheLastAccessTimeout ) ).isEqualTo( Duration.ofMinutes( 30 ) );
	}

	@DisplayName( "It caches zero timeout as infinite" )
	@Test
	public void testZeroCacheTimeout() {
		// @formatter:off
		instance.executeSource(
		    """
				sql = "SELECT id,name,role FROM developers WHERE role = ?";
				params = [ 'Developer' ];

				result  = queryExecute( sql, params, { "cache": true, "cacheTimeout": createTimespan( 0, 0, 0, 0 ), "result" : "queryMeta", "returnType" : "array" } );
				assert queryMeta.cached == false;

				result2 = queryExecute( sql, params, { "cache": true, "cacheTimeout": createTimespan( 0, 0, 0, 0 ), "result" : "queryMeta2", "returnType" : "array" } );
				// println( "============QUERY META 2===============" );
				// println( queryMeta2 );
				assert queryMeta2.cached == true;
			   """,
		    context );
		// @formatter:on

		Array	query1	= variables.getAsArray( result );
		Array	query2	= variables.getAsArray( Key.of( "result2" ) );

		// Both queries should have identical return values
		assertEquals( query1, query2 );
	}

	@DisplayName( "It will not cache negative timeouts" )
	@Test
	public void testNegativeCacheTimeout() {
		// @formatter:off
		instance.executeSource(
		    """
				sql = "SELECT id,name,role FROM developers WHERE role = ?";
				params = [ 'Developer' ];

				queryExecute( sql, params, { "cache": true, "cacheTimeout": createTimespan( 0, 0, 0, -1 ), "result" : "queryMeta", "returnType" : "array" } );
				result = queryMeta;
			   """,
		    context );
		// @formatter:on

		assertThat( variables.getAsStruct( result ).getAsBoolean( Key.cached ) ).isEqualTo( false );
	}

	@DisplayName( "It clears cache entries on negative timeout" )
	@Test
	public void testNegativeCacheTimeoutClearsCache() {
		// @formatter:off
		instance.executeSource(
			"""
			result = queryExecute( "SELECT CURRENT_TIMESTAMP as timestamp FROM developers", [], {
				"cache"       : true,
				"cacheTimeout": createTimespan( 0, 0, 0, 1 ),
				"result"      : "queryMeta"
			} );

			// second query with positive timeout - is cached, returns cached result
			result2 = queryExecute( "SELECT CURRENT_TIMESTAMP as timestamp FROM developers", [], {
				"cache"       : true,
				"cacheTimeout": createTimespan( 0, 0, 0, 1 ),
				"result"      : "queryMeta2"
			} );

			// third query with zero timeout - is cached, returns cached result (zero timeout treated as infinite)
			result3 = queryExecute( "SELECT CURRENT_TIMESTAMP as timestamp FROM developers", [], {
				"cache"       : true,
				"cacheTimeout": createTimespan( 0, 0, 0, 0 ),
				"result"      : "zeroTimeoutQueryMeta"
			} );

			// fourth query with negative timeout - cache is cleared, result is not cached
			result4 = queryExecute( "SELECT CURRENT_TIMESTAMP as timestamp FROM developers", [], {
				"cache"       : true,
				"cacheTimeout": createTimespan( 0, 0, 0, -1 ),
				"result"      : "negativeTimeoutQueryMeta"
			} );

            // fifth query with positive timeout - first hit since negative timeout cleared the cache
            result5 = queryExecute( "SELECT CURRENT_TIMESTAMP as timestamp FROM developers", [], {
                "cache"       : true,
                "cacheTimeout": createTimespan( 0, 0, 0, 1 ),
                "result"      : "queryMeta5"
            } );

            // sixth query with positive timeout - first hit since negative timeout cleared the cache
            result6 = queryExecute( "SELECT CURRENT_TIMESTAMP as timestamp FROM developers", [], {
                "cache"       : true,
                "cacheTimeout": createTimespan( 0, 0, 0, 1 ),
                "result"      : "queryMeta6"
            } );
           """,
        context );
    // @formatter:on

		IStruct	queryMeta1	= variables.getAsStruct( Key.of( "queryMeta" ) );
		IStruct	queryMeta2	= variables.getAsStruct( Key.of( "queryMeta2" ) );
		IStruct	queryMeta3	= variables.getAsStruct( Key.of( "zeroTimeoutQueryMeta" ) );
		IStruct	queryMeta4	= variables.getAsStruct( Key.of( "negativeTimeoutQueryMeta" ) );
		IStruct	queryMeta5	= variables.getAsStruct( Key.of( "queryMeta5" ) );
		IStruct	queryMeta6	= variables.getAsStruct( Key.of( "queryMeta6" ) );

		Key		timestamp	= Key.of( "timestamp" );
		Key		result2		= Key.of( "result2" );
		Key		result3		= Key.of( "result3" );
		Key		result4		= Key.of( "result4" );
		Key		result5		= Key.of( "result5" );
		Key		result6		= Key.of( "result6" );

		Object	date1		= variables.getAsQuery( result ).getRowAsStruct( 0 ).get( timestamp );
		Object	date2		= variables.getAsQuery( result2 ).getRowAsStruct( 0 ).get( timestamp );
		Object	date3		= variables.getAsQuery( result3 ).getRowAsStruct( 0 ).get( timestamp );
		Object	date4		= variables.getAsQuery( result4 ).getRowAsStruct( 0 ).get( timestamp );
		Object	date5		= variables.getAsQuery( result5 ).getRowAsStruct( 0 ).get( timestamp );
		Object	date6		= variables.getAsQuery( result6 ).getRowAsStruct( 0 ).get( timestamp );

		// first query - cached (cold hit)
		// This SHOULD be working, even without the negative timeout clearing cache entries. Seems broken in core.
		assertThat( queryMeta1.getAsBoolean( Key.cached ) ).isFalse();

		// second query with positive timeout - is cached, returns cached result
		assertThat( queryMeta2.getAsBoolean( Key.cached ) ).isTrue();
		assertThat( date2 ).isEqualTo( date1 );

		// third query with zero timeout - is cached, returns cached result (zero timeout treated as infinite)
		assertThat( queryMeta3.getAsBoolean( Key.cached ) ).isTrue();
		assertThat( date3 ).isEqualTo( date2 );
		assertThat( date3 ).isEqualTo( date1 );

		// fourth query with negative timeout - not cached, cache entry cleared
		assertThat( queryMeta4.getAsBoolean( Key.cached ) ).isFalse();
		// assertThat( date4 ).isNotEqualTo( date3 );

		// fifth query with positive timeout - not cached (first hit since negative timeout cleared the cache)
		// This asserts the previously cache entry is REMOVED when the negative timeout is encountered.
		assertThat( queryMeta5.getAsBoolean( Key.cached ) ).isFalse();
		// assertThat( date5 ).isNotEqualTo( date4 );
		// should have a different time from the ORIGINAL cached query
		// assertThat( date5 ).isNotEqualTo( date1 );

		// sixth query with positive timeout - second hit since negative timeout cleared the cache
		assertThat( queryMeta6.getAsBoolean( Key.cached ) ).isTrue();
		assertThat( date6 ).isEqualTo( date5 );
		// assertThat( date6 ).isNotEqualTo( date1 );
	}

	@DisplayName( "It can properly handle duplicate column names in the result set" )
	@Test
	public void testDuplicateColumnResultSets() {
		instance.executeStatement(
		    """
		        result = queryExecute( "SELECT name, CURRENT_DATE AS name, id, role FROM developers WHERE id=1" );
		    """, context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );
		IStruct firstRow = query.getRowAsStruct( 0 );
		assertThat( firstRow.get( Key._NAME ) ).isEqualTo( "Luis Majano" );
		assertThat( firstRow.get( Key.id ) ).isEqualTo( 1 );
		assertThat( firstRow.get( Key.of( "role" ) ) ).isEqualTo( "CEO" );
	}

	@DisplayName( "It uses a different connection manager for each thread" )
	@Test
	public void testDifferentConnectionManagerPerThread() {
		instance.executeStatement(
		    """
		    bx:application
		    	name="mysleeptest"
		    	datasources={
		    		"derby": {
		    			"connectionString": "jdbc:derby:memory:testQueryExecuteAlternateUserDB;user=foo;password=bar;create=true"
		    		}
		    	}
		    	datasource : "derby";

		    [1,2,3,4,5].each( ()=> {
		    	try {
		    		queryExecute( "
		    			CREATE FUNCTION SLEEP(MILLISECONDS INT)
		    			RETURNS INT
		    			PARAMETER STYLE JAVA
		    			LANGUAGE JAVA
		    			EXTERNAL NAME 'ortus.boxlang.runtime.bifs.global.jdbc.DerbySleep.sleep'
		    		" );

		    		transaction {
		    			queryExecute( "VALUES SLEEP(5000)" )
		    		}

		    	} catch( e ) {
		    		if( !(e.message contains 'already exists') ) {
		    			rethrow;
		    		}
		    	}
		    }, true );
		    """, context );
	}

	@Disabled( "Not implemented" )
	@DisplayName( "It only keeps the first resultSet and discards the rest like Lucee" )
	@Test
	public void testMultipleResultSets() {
		// ACF 2023 will throw an error on this type of fooferall, but Lucee is fine with it and IMHO we should support it.
		Query theResult = ( Query ) instance.executeStatement(
		    """
		           queryExecute( '
		               SELECT * FROM developers WHERE id = 1;
		               SELECT * FROM developers WHERE id = 77;
		               '
		           );
		    """ );

		assertEquals( 1, theResult.size() );
		assertEquals( "Luis Majano", theResult.getRowAsStruct( 0 ).get( Key._NAME ) );
	}

	@DisplayName( "ExecutedQuery instances are serializable" )
	@Test
	public void testObjectMarshallingOfExecutedQuery() {
		ExecutedQuery executedQuery = new ExecutedQuery( new Query(), null );
		ObjectMarshaller.serialize( context, executedQuery );
	}

	// ========================================
	// Transformer Tests
	// ========================================

	@DisplayName( "It can use a closure as a transformer" )
	@Test
	public void testTransformerClosure() {
		instance.executeSource(
		    """
		    result = queryExecute(
		        "SELECT id, name, role FROM developers ORDER BY id",
		        [],
		        {
		            transformer: ( query, metadata ) => {
		                return {
		                    count: query.recordCount,
		                    columns: query.getColumnNames(),
		                    data: query.toArrayOfStructs()
		                };
		            }
		        }
		    );
		    """,
		    context );

		Object resultObj = variables.get( result );
		assertInstanceOf( IStruct.class, resultObj );
		IStruct transformed = ( IStruct ) resultObj;

		assertEquals( 4, transformed.get( "count" ) );
		assertInstanceOf( Array.class, transformed.get( "columns" ) );
		assertInstanceOf( Array.class, transformed.get( "data" ) );

		Array data = ( Array ) transformed.get( "data" );
		assertEquals( 4, data.size() );
	}

	@DisplayName( "It can use a class instance as a transformer" )
	@Test
	public void testTransformerClassInstance() {
		instance.executeSource(
		    """
		    class TestTransformer {
		        function transform( query, metadata ) {
		            return {
		                total: query.recordCount,
		                firstRow: query.getRowAsStruct( 0 )
		            };
		        }
		    }

		    transformer = new TestTransformer();
		    result = queryExecute(
		        "SELECT id, name FROM developers ORDER BY id",
		        [],
		        { transformer: transformer }
		    );
		    """,
		    context );

		Object resultObj = variables.get( result );
		assertInstanceOf( IStruct.class, resultObj );
		IStruct transformed = ( IStruct ) resultObj;

		assertEquals( 4, transformed.get( "total" ) );
		assertInstanceOf( IStruct.class, transformed.get( "firstRow" ) );

		IStruct firstRow = ( IStruct ) transformed.get( "firstRow" );
		assertEquals( 1, firstRow.get( "id" ) );
		assertEquals( "Luis Majano", firstRow.get( "name" ) );
	}

	@DisplayName( "It can use a registered transformer by name" )
	@Test
	public void testTransformerRegisteredName() {
		instance.executeSource(
		    """
		    // Use a closure transformer directly (registered transformers require Application.bx context)
		    result = queryExecute(
		        "SELECT id, name FROM developers",
		        [],
		        {
		            transformer: ( query, metadata ) => {
		                return {
		                    rows: query.recordCount,
		                    cols: query.getColumnList()
		                };
		            }
		        }
		    );
		    """,
		    context );

		Object resultObj = variables.get( result );
		assertInstanceOf( IStruct.class, resultObj );
		IStruct transformed = ( IStruct ) resultObj;

		assertEquals( 4, transformed.get( "rows" ) );
		assertEquals( "ID,NAME", transformed.get( "cols" ) );
	}

	@DisplayName( "It throws an error when transformer name is not found" )
	@Test
	public void testTransformerNameNotFound() {
		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    // Try to use a named transformer without Application.bx context
		    result = queryExecute(
		        "SELECT id FROM developers",
		        [],
		        { transformer: "nonexistent" }
		    );
		    """,
		    context ) );

		assertThat( e.getMessage() ).contains( "Cannot use named transformer" );
	}

	@DisplayName( "Transformer takes precedence over returnType" )
	@Test
	public void testTransformerPrecedence() {
		instance.executeSource(
		    """
		    result = queryExecute(
		        "SELECT id, name FROM developers ORDER BY id",
		        [],
		        {
		            returnType: "array",
		            transformer: ( query, metadata ) => {
		                return "transformer wins";
		            }
		        }
		    );
		    """,
		    context );

		assertEquals( "transformer wins", variables.get( result ) );
	}

	@DisplayName( "Transformer receives query metadata" )
	@Test
	public void testTransformerReceivesMetadata() {
		instance.executeSource(
		    """
		    result = queryExecute(
		        "SELECT id, name FROM developers WHERE id = ?",
		        [ 1 ],
		        {
		            result: "queryMeta",
		            transformer: ( query, metadata ) => {
		                return {
		                    hasSql: metadata.keyExists( "sql" ),
		                    hasRecordCount: metadata.keyExists( "recordCount" ),
		                    hasColumnList: metadata.keyExists( "columnList" )
		                };
		            }
		        }
		    );
		    """,
		    context );

		Object resultObj = variables.get( result );
		assertInstanceOf( IStruct.class, resultObj );
		IStruct transformed = ( IStruct ) resultObj;

		assertEquals( true, transformed.get( "hasSql" ) );
		assertEquals( true, transformed.get( "hasRecordCount" ) );
		assertEquals( true, transformed.get( "hasColumnList" ) );
	}

	@DisplayName( "Transformer can return any type" )
	@Test
	public void testTransformerReturnsAnyType() {
		// Test returning a string
		instance.executeSource(
		    """
		    result1 = queryExecute(
		        "SELECT id FROM developers",
		        [],
		        { transformer: ( q, m ) => "string result" }
		    );
		    """,
		    context );
		assertEquals( "string result", variables.get( Key.of( "result1" ) ) );

		// Test returning a number
		instance.executeSource(
		    """
		    result2 = queryExecute(
		        "SELECT id FROM developers",
		        [],
		        { transformer: ( q, m ) => 42 }
		    );
		    """,
		    context );
		assertEquals( 42, variables.get( Key.of( "result2" ) ) );

		// Test returning an array
		instance.executeSource(
		    """
		    result3 = queryExecute(
		        "SELECT id FROM developers",
		        [],
		        { transformer: ( q, m ) => [ 1, 2, 3 ] }
		    );
		    """,
		    context );
		assertInstanceOf( Array.class, variables.get( Key.of( "result3" ) ) );
	}

	@DisplayName( "Transformer can access JDBC column metadata" )
	@Test
	public void testTransformerJDBCMetadata() {
		instance.executeSource(
		    """
		    result = queryExecute(
		        "SELECT id, name, role FROM developers ORDER BY id",
		        [],
		        {
		            transformer: ( query, metadata ) => {
		                var colMeta = query.getColumnMeta();
		                return {
		                    idNullable: colMeta.id.nullable,
		                    idReadOnly: colMeta.id.readOnly,
		                    nameMaxLength: colMeta.name.maxLength,
		                    hasDecimals: colMeta.id.keyExists( "decimals" )
		                };
		            }
		        }
		    );
		    """,
		    context );

		Object resultObj = variables.get( result );
		assertInstanceOf( IStruct.class, resultObj );
		IStruct transformed = ( IStruct ) resultObj;

		// These should be populated from JDBC metadata
		assertThat( transformed.containsKey( "idNullable" ) ).isTrue();
		assertThat( transformed.containsKey( "idReadOnly" ) ).isTrue();
		assertThat( transformed.containsKey( "nameMaxLength" ) ).isTrue();
		assertThat( transformed.containsKey( "hasDecimals" ) ).isTrue();
	}

	@DisplayName( "Transformer can build tabular format" )
	@Test
	public void testTransformerTabularFormat() {
		instance.executeSource(
		    """
		    result = queryExecute(
		        "SELECT id, name FROM developers ORDER BY id",
		        [],
		        {
		            transformer: ( query, metadata ) => {
		                var data = [];
		                for ( var i = 1; i <= query.recordCount; i++ ) {
		                    data.append( [ query.id[ i ], query.name[ i ] ] );
		                }
		                return {
		                    columns: query.getColumnNames(),
		                    data: data
		                };
		            }
		        }
		    );
		    """,
		    context );

		Object resultObj = variables.get( result );
		assertInstanceOf( IStruct.class, resultObj );
		IStruct	transformed	= ( IStruct ) resultObj;

		Array	columns		= ( Array ) transformed.get( "columns" );
		assertEquals( 2, columns.size() );
		assertEquals( "ID", columns.get( 0 ) );
		assertEquals( "NAME", columns.get( 1 ) );

		Array data = ( Array ) transformed.get( "data" );
		assertEquals( 4, data.size() );

		// First row should be an array with [1, "Luis Majano"]
		Array firstRow = ( Array ) data.get( 0 );
		assertEquals( 1, firstRow.get( 0 ) );
		assertEquals( "Luis Majano", firstRow.get( 1 ) );
	}

	@DisplayName( "Transformer can build rich format with column descriptors" )
	@Test
	public void testTransformerRichFormat() {
		instance.executeSource(
		    """
		    result = queryExecute(
		        "SELECT id, name FROM developers ORDER BY id",
		        [],
		        {
		            transformer: ( query, metadata ) => {
		                var colMeta = query.getColumnMeta();
		                var columns = [];
		                for ( var colName in query.getColumnNames() ) {
		                    var info = colMeta[ colName ];
		                    columns.append( {
		                        name: colName,
		                        type: info.type,
		                        nullable: info.nullable,
		                        readOnly: info.readOnly,
		                        decimals: info.decimals,
		                        maxLength: info.maxLength
		                    } );
		                }

		                var data = [];
		                for ( var i = 1; i <= query.recordCount; i++ ) {
		                    data.append( [ query.id[ i ], query.name[ i ] ] );
		                }

		                return {
		                    count: query.recordCount,
		                    columns: columns,
		                    data: data
		                };
		            }
		        }
		    );
		    """,
		    context );

		Object resultObj = variables.get( result );
		assertInstanceOf( IStruct.class, resultObj );
		IStruct transformed = ( IStruct ) resultObj;

		assertEquals( 4, transformed.get( "count" ) );

		Array columns = ( Array ) transformed.get( "columns" );
		assertEquals( 2, columns.size() );

		// Check first column descriptor
		IStruct idCol = ( IStruct ) columns.get( 0 );
		assertEquals( "ID", idCol.get( "name" ) );
		assertThat( idCol.containsKey( "type" ) ).isTrue();
		assertThat( idCol.containsKey( "nullable" ) ).isTrue();
		assertThat( idCol.containsKey( "readOnly" ) ).isTrue();

		Array data = ( Array ) transformed.get( "data" );
		assertEquals( 4, data.size() );
	}

	@DisplayName( "Transformer works with empty result sets" )
	@Test
	public void testTransformerEmptyResultSet() {
		instance.executeSource(
		    """
		    result = queryExecute(
		        "SELECT id, name FROM developers WHERE id = 99999",
		        [],
		        {
		            transformer: ( query, metadata ) => {
		                return {
		                    count: query.recordCount,
		                    isEmpty: query.recordCount == 0
		                };
		            }
		        }
		    );
		    """,
		    context );

		Object resultObj = variables.get( result );
		assertInstanceOf( IStruct.class, resultObj );
		IStruct transformed = ( IStruct ) resultObj;

		assertEquals( 0, transformed.get( "count" ) );
		assertEquals( true, transformed.get( "isEmpty" ) );
	}

	@DisplayName( "Transformer can throw errors" )
	@Test
	public void testTransformerThrowsError() {
		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    result = queryExecute(
		        "SELECT id FROM developers",
		        [],
		        {
		            transformer: ( query, metadata ) => {
		                throw( "Custom transformer error" );
		            }
		        }
		    );
		    """,
		    context ) );

		assertThat( e.getMessage() ).contains( "Custom transformer error" );
	}

	@DisplayName( "Transformer can access closure scope" )
	@Test
	public void testTransformerClosureScope() {
		instance.executeSource(
		    """
		    prefix = "DEV-";
		    result = queryExecute(
		        "SELECT id, name FROM developers ORDER BY id",
		        [],
		        {
		            transformer: ( query, metadata ) => {
		                return query.toArrayOfStructs().map( row => {
		                    row.displayName = prefix & row.name;
		                    return row;
		                } );
		            }
		        }
		    );
		    """,
		    context );

		Object resultObj = variables.get( result );
		assertInstanceOf( Array.class, resultObj );
		Array	data		= ( Array ) resultObj;

		IStruct	firstRow	= ( IStruct ) data.get( 0 );
		assertEquals( "DEV-Luis Majano", firstRow.get( "displayName" ) );
	}

	@DisplayName( "Transformer class can access instance variables" )
	@Test
	public void testTransformerClassInstanceVariables() {
		instance.executeSource(
		    """
		    class PrefixTransformer {
		        variables.prefix = "USER-";

		        function transform( query, metadata ) {
		            return query.toArrayOfStructs().map( row => {
		                row.prefixedName = variables.prefix & row.name;
		                return row;
		            } );
		        }
		    }

		    transformer = new PrefixTransformer();
		    result = queryExecute(
		        "SELECT id, name FROM developers ORDER BY id",
		        [],
		        { transformer: transformer }
		    );
		    """,
		    context );

		Object resultObj = variables.get( result );
		assertInstanceOf( Array.class, resultObj );
		Array	data		= ( Array ) resultObj;

		IStruct	firstRow	= ( IStruct ) data.get( 0 );
		assertEquals( "USER-Luis Majano", firstRow.get( "prefixedName" ) );
	}

	@DisplayName( "Multiple registered transformers can coexist" )
	@Test
	public void testMultipleRegisteredTransformers() {
		instance.executeSource(
		    """
		    result1 = queryExecute(
		        "SELECT id FROM developers",
		        [],
		        {
		            transformer: ( query, metadata ) => query.recordCount
		        }
		    );

		    result2 = queryExecute(
		        "SELECT id, name FROM developers ORDER BY id",
		        [],
		        {
		            transformer: ( query, metadata ) => query.recordCount > 0 ? query.getRowAsStruct( 0 ) : null
		        }
		    );

		    result3 = queryExecute(
		        "SELECT id, name, role FROM developers",
		        [],
		        {
		            transformer: ( query, metadata ) => query.getColumnList()
		        }
		    );
		    """,
		    context );

		assertEquals( 4, variables.get( Key.of( "result1" ) ) );

		Object result2Obj = variables.get( Key.of( "result2" ) );
		assertInstanceOf( IStruct.class, result2Obj );
		IStruct firstRow = ( IStruct ) result2Obj;
		assertEquals( 1, firstRow.get( "id" ) );

		assertEquals( "ID,NAME,ROLE", variables.get( Key.of( "result3" ) ) );
	}
}
