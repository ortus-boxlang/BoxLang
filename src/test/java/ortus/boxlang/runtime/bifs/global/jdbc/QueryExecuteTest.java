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

@EnabledIf( "tools.JDBCTestUtils#hasDerbyModule" )
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
		instance.executeSource(
		    """
		       sql = "SELECT id,name,role FROM developers WHERE role = ?";
		    params = [ 'Developer' ];
		       result  = queryExecute( sql, params, { "cache": true, "cacheTimeout": createTimespan( 0, 0, 0, 2 ), "result" : "queryMeta", "returnType" : "array" } );
		       result2 = queryExecute( sql, params, { "cache": true, "cacheTimeout": createTimespan( 0, 0, 0, 2 ), "result" : "queryMeta2", "returnType" : "array" } );
		       result3 = queryExecute( sql, [ 'Admin' ], { "cache": true, "cacheTimeout": createTimespan( 0, 0, 0, 2 ), "result" : "queryMeta3", "returnType" : "array" } );
		       result4 = queryExecute( sql, params, { "cache": false, "cacheTimeout": createTimespan( 0, 0, 0, 2 ), "result" : "queryMeta4", "returnType" : "array" } );
		       """,
		    context );
		Array	query1	= variables.getAsArray( result );
		Array	query3	= variables.getAsArray( Key.of( "result3" ) );
		Array	query2	= variables.getAsArray( Key.of( "result2" ) );
		Array	query4	= variables.getAsArray( Key.of( "result4" ) );

		// All 3 queries should have identical return values
		assertEquals( query1, query2 );
		assertEquals( query2, query4 );
		// query 3 should be a different, uncached result
		assertNotEquals( query1, query3 );

		// Query 1 should NOT be cached
		IStruct queryMeta = StructCaster.cast( variables.getAsStruct( Key.of( "queryMeta" ) ) );
		assertThat( queryMeta.getAsBoolean( Key.cached ) ).isEqualTo( false );

		// query 2 SHOULD be cached
		IStruct queryMeta2 = StructCaster.cast( variables.getAsStruct( Key.of( "queryMeta2" ) ) );
		assertThat( queryMeta2.getAsBoolean( Key.cached ) ).isEqualTo( true );

		// query 3 should NOT be cached because it has an additional param
		IStruct queryMeta3 = StructCaster.cast( variables.getAsStruct( Key.of( "queryMeta3" ) ) );
		assertThat( queryMeta3.getAsBoolean( Key.cached ) ).isEqualTo( false );

		// query 4 should NOT be cached because it strictly disallows it
		IStruct queryMeta4 = StructCaster.cast( variables.getAsStruct( Key.of( "queryMeta4" ) ) );
		assertThat( queryMeta4.getAsBoolean( Key.cached ) ).isEqualTo( false );
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

		// Query 1 should NOT be cached
		IStruct queryMeta = StructCaster.cast( variables.getAsStruct( Key.of( "queryMeta" ) ) );
		assertThat( queryMeta.getAsBoolean( Key.cached ) ).isEqualTo( false );

		// query 2 SHOULD be cached
		IStruct queryMeta2 = StructCaster.cast( variables.getAsStruct( Key.of( "queryMeta2" ) ) );
		assertThat( queryMeta2.getAsBoolean( Key.cached ) ).isEqualTo( true );
	}

	@DisplayName( "It properly sets query results with cache metadata" )
	@Test
	public void testCacheResultMeta() {
		instance.executeSource(
		    """
		    queryExecute(
		    	"SELECT * FROM developers WHERE role = ?",
		    	[ 'Admin' ],
		    	{ "cache" : true, "cacheProvider" : "default", "cacheKey": "adminDevs", "cacheTimeout": createTimespan( 0, 1, 0, 0 ), "cacheLastAccessTimeout": createTimespan( 0, 0, 30, 0 ) }
		    );
		    result = queryExecute(
		    	"SELECT * FROM developers WHERE role = ?",
		    	[ 'Admin' ],
		    	 { "result": "queryResults", "cache" : true, "cacheProvider" : "default", "cacheKey": "adminDevs", "cacheTimeout": createTimespan( 0, 1, 0, 0 ), "cacheLastAccessTimeout": createTimespan( 0, 0, 30, 0 ) }
		    );
		    """,
		    context );
		Object resultObject = variables.get( Key.of( "queryResults" ) );
		assertInstanceOf( IStruct.class, resultObject );
		IStruct result = ( IStruct ) resultObject;

		assertThat( result ).containsKey( Key.cached );
		assertThat( result ).containsKey( Key.cacheProvider );
		assertThat( result ).containsKey( Key.cacheKey );
		assertThat( result ).containsKey( Key.cacheTimeout );
		assertThat( result ).containsKey( Key.cacheLastAccessTimeout );

		assertThat( result.getAsBoolean( Key.cached ) ).isEqualTo( true );
		assertThat( result.getAsString( Key.cacheProvider ) ).isEqualTo( "default" );
		assertThat( result.getAsString( Key.cacheKey ) ).isEqualTo( "adminDevs" );
		assertThat( result.get( Key.cacheTimeout ) ).isEqualTo( Duration.ofHours( 1 ) );
		assertThat( result.get( Key.cacheLastAccessTimeout ) ).isEqualTo( Duration.ofMinutes( 30 ) );
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
		ExecutedQuery executedQuery = new ExecutedQuery( new Query(), null, null );
		ObjectMarshaller.serialize( context, executedQuery );
	}
}
