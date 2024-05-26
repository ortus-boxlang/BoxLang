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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.config.segments.DatasourceConfig;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
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
		assertEquals( 3, query.size() );

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
		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    result = queryExecute( "SELECT * FROM developers WHERE id = :id", { "name": "Michael Born" } );
		    """,
		    context ) );

		assertThat( e.getMessage() ).isEqualTo( "Missing param in query: [id]. SQL: SELECT * FROM developers WHERE id = :id" );
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
		instance.getConfiguration().runtime.datasources.put(
		    Key.of( dbName ),
		    DatasourceConfig.fromStruct( JDBCTestUtils.getDatasourceConfig( dbName.getName() ) )
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

	@DisplayName( "It throws an exception if the specified datasource is not registered" )
	@Test
	public void testMissingNamedDataSource() {
		DatabaseException e = assertThrows( DatabaseException.class, () -> instance.executeSource(
		    """
		    result = queryExecute( "SELECT * FROM developers WHERE id = :id", { "name": "Michael Born" }, { "datasource": "not_found" } );
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
		assertEquals( 3, results.size() );

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
		assertEquals( 2, results.size() );

		List<String> keys = results.getKeysAsStrings();
		assertEquals( keys, List.of( "Developer", "CEO" ) );

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

		assertThat( e.getMessage() ).isEqualTo( "You must defined a `columnKey` option when using `returnType: struct`." );
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

		assertTrue( result.containsKey( "sql" ) );
		assertEquals( "SELECT * FROM developers WHERE role = ?", result.getAsString( Key.sql ) );

		assertTrue( result.containsKey( "cached" ) );
		assertFalse( result.getAsBoolean( Key.cached ) );

		assertTrue( result.containsKey( "sqlParameters" ) );
		assertEquals( Array.of( "Developer" ), result.getAsArray( Key.sqlParameters ) );

		assertTrue( result.containsKey( "recordCount" ) );
		assertEquals( 2, result.getAsInteger( Key.recordCount ) );

		assertTrue( result.containsKey( "columnList" ) );
		assertEquals( "ID,NAME,ROLE", result.getAsString( Key.columnList ) );

		assertTrue( result.containsKey( "executionTime" ) );
		assertThat( result.getAsLong( Key.executionTime ) ).isAtLeast( 0 );

		assertFalse( result.containsKey( "generatedKey" ) );
	}

	@DisplayName( "It can execute a query against an ad-hoc datasource" )
	@Test
	public void testAdHocDataSource() {
		DatabaseException e = assertThrows( DatabaseException.class, () -> {
			instance.executeSource(
			    """
			      result = queryExecute( "SELECT * FROM developers ORDER BY id", [], { "datasource": {
			    "driver" : "derby",
			    "connectionString": "jdbc:derby:memory:anotherTestDB;create=true"
			    } } );
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

	@Disabled( "Not implemented" )
	@DisplayName( "It can execute multiple statements in a single queryExecute() call like Lucee" )
	@Test
	public void testMultipleStatements() {
		// ACF 2023 will throw an error on this type of fooferall, but Lucee is fine with it and IMHO we should support it.
		assertDoesNotThrow( () -> instance.executeStatement(
		    """
		           queryExecute( '
		               INSERT INTO developers (id) VALUES (111);
		               INSERT INTO developers (id) VALUES (222)
		               '
		           );
		    """ )
		);
		Query theResult = ( Query ) instance.executeStatement( "queryExecute( 'SELECT * FROM developers WHERE id IN (111,222)' );" );
		assertEquals( 2, theResult.size() );
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

	/**
	 * This feature is not supported in Hikari https://github.com/brettwooldridge/HikariCP/issues/231
	 */
	@DisplayName( "It can execute a query with a custom username and password" )
	@Test
	@Disabled( "Lacking support in HikariCP" )
	public void testCustomUsernameAndPassword() {
		// DataSource alternateDataSource = DataSource.fromStruct( Struct.of(
		// "connectionString", "jdbc:derby:memory:testQueryExecuteAlternateUserDB;user=foo;password=bar;create=true"
		// ) );
		// alternateDataSource.execute( "CREATE TABLE developers ( id INTEGER, name VARCHAR(155), role VARCHAR(155) )" );
		// datasourceService.register( Key.of( "alternate" ), alternateDataSource );
		instance.executeSource(
		    """
		    result = queryExecute( "SELECT * FROM developers ORDER BY id", [], { "username": "foo", "password": "bar", "datasource": "alternate" } );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 0, query.size() );
	}

}
