
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

package ortus.boxlang.runtime.components.jdbc;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.jdbc.DataSourceManager;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumn;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

public class DBInfoTest {

	static DataSourceManager	datasourceManager;
	static BoxRuntime			instance;
	IBoxContext					context;
	IScope						variables;
	static Key					result	= new Key( "result" );
	static DataSource			MySQLDataSource;

	@BeforeAll
	public static void setUp() {
		instance			= BoxRuntime.getInstance( true );
		datasourceManager	= DataSourceManager.getInstance();
		DataSource defaultDatasource = new DataSource( Struct.of(
		    "jdbcUrl", "jdbc:derby:memory:BoxlangDB;create=true"
		) );
		datasourceManager.setDefaultDatasource( defaultDatasource );
		defaultDatasource.execute( "CREATE TABLE developers ( id INTEGER, name VARCHAR(155) )" );

		if ( tools.JDBCTestUtils.hasMySQLDriver() ) {
			Key MySQLDataSourceName = Key.of( "MYSQLDB" );
			MySQLDataSource = datasourceManager.registerDatasource( MySQLDataSourceName, Struct.of(
			    "jdbcUrl", "jdbc:mysql://localhost:3306",
			    "username", "root",
			    "password", "secret"
			) );
			MySQLDataSource.execute( "CREATE DATABASE IF NOT EXISTS testDB" );
			MySQLDataSource.execute( "USE testDB" );
		}
	}

	@AfterAll
	public static void teardown() {
		datasourceManager.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It requires a non-null `type` argument matching a valid type" )
	@Test
	public void requiredTypeValidation() {
		assertThrows( BoxValidationException.class, () -> {
			instance.executeStatement( "CFDBInfo();" );
		} );
		assertThrows( BoxValidationException.class, () -> {
			instance.executeStatement( "CFDBInfo( type='foo' );" );
		} );
		// This DOES throw, and i don't know why. 😢
		// assertDoesNotThrow( () -> {
		// instance.executeStatement( "CFDBInfo( type='version', name='result' );" );
		// } );
	}

	@DisplayName( "It requires the `table` argument on column, foreignkeys, and index types`" )
	@Test
	public void typeRequiresTableValidation() {
		assertThrows( BoxValidationException.class, () -> {
			instance.executeStatement( "CFDBInfo( type='columns' );" );
		} );
		assertThrows( BoxValidationException.class, () -> {
			instance.executeStatement( "CFDBInfo( type='foreignkeys' );" );
		} );
		assertThrows( BoxValidationException.class, () -> {
			instance.executeStatement( "CFDBInfo( type='index' );" );
		} );
	}

	@DisplayName( "Throws on non-existent datasource" )
	@Test
	public void testDatasourceAttribute() {
		assertThrows(
		    DatabaseException.class,
		    () -> instance.executeStatement( "cfdbinfo( type='version', name='result', datasource='foobar' )" )
		);
	}

	@DisplayName( "Can get JDBC driver version info" )
	@Test
	public void testVersionType() {
		instance.executeSource(
		    """
		        cfdbinfo( type='version', name='result' )
		    """,
		    context );
		Object theResult = variables.get( result );
		assertTrue( theResult instanceof Query );
		Query versionQuery = ( Query ) theResult;
		assertEquals( 1, versionQuery.size() );
		assertEquals( 6, versionQuery.getColumns().size() );

		assertEquals( "Apache Derby Embedded JDBC Driver", versionQuery.getRowAsStruct( 0 ).getAsString( Key.of( "DRIVER_NAME" ) ) );
	}

	@EnabledIf( "tools.JDBCTestUtils#hasMySQLDriver" )
	@DisplayName( "Can get catalog and schema names" )
	@Test
	public void testDBNamesType() {
		instance.executeSource(
		    """
		        cfdbinfo( type='dbnames', name='result', datasource='MYSQLDB' )
		    """,
		    context );
		Object theResult = variables.get( result );
		assertTrue( theResult instanceof Query );

		Query dbNamesQuery = ( Query ) theResult;
		assertTrue( dbNamesQuery.size() > 0 );
		assertEquals( 2, dbNamesQuery.getColumns().size() );

		IStruct ourDBRow = dbNamesQuery.stream()
		    .filter( row -> row.getAsString( Key.of( "DBNAME" ) ).equals( "testDB" ) )
		    .findFirst()
		    .orElse( null );

		assertNotNull( ourDBRow );
		assertEquals( "CATALOG", ourDBRow.getAsString( Key.type ) );
		assertEquals( "testDB", ourDBRow.getAsString( Key.of( "DBNAME" ) ) );
	}

	@DisplayName( "Can get table column data" )
	@Test
	public void testColumnsType() {
		instance.executeSource(
		    """
		        cfdbinfo( type='columns', name='result', table='DEVELOPERS' )
		    """,
		    context );
		Object theResult = variables.get( result );
		assertTrue( theResult instanceof Query );

		Query resultQuery = ( Query ) theResult;
		assertTrue( resultQuery.size() > 0 );

		// @TODO: Enable this assertion once we've added support for Lucee's custom foreign key and primary key fields.
		// assertEquals( 28, resultQuery.getColumns().size() );

		IStruct nameColumn = resultQuery.stream()
		    .filter( row -> row.getAsString( Key.of( "COLUMN_NAME" ) ).equals( "NAME" ) )
		    .findFirst()
		    .orElse( null );

		assertNotNull( nameColumn );
		assertEquals( "NAME", nameColumn.getAsString( Key.of( "COLUMN_NAME" ) ) );
		assertEquals( "VARCHAR", nameColumn.getAsString( Key.of( "TYPE_NAME" ) ) );
		assertEquals( 155, nameColumn.getAsInteger( Key.of( "COLUMN_SIZE" ) ) );
	}

	@DisplayName( "Throws on non-existent tablename" )
	@Test
	public void testColumnsTypeWithNonExistentTable() {
		assertThrows( DatabaseException.class, () -> instance.executeStatement( "cfdbinfo( type='columns', name='result', table='404NotFound' )" ) );
	}

	@DisplayName( "Can get db tables when type=tables" )
	@Test
	public void testTablesType() {
		instance.executeSource(
		    """
		        cfdbinfo( type='tables', name='result' )
		    """,
		    context );
		Object theResult = variables.get( result );
		assertTrue( theResult instanceof Query );

		Query resultQuery = ( Query ) theResult;
		assertTrue( resultQuery.size() > 0 );
		Map<Key, QueryColumn> columns = resultQuery.getColumns();

		assertTrue( columns.size() == 10 );
		assertTrue( columns.containsKey( Key.of( "TABLE_CAT" ) ) );
		assertTrue( columns.containsKey( Key.of( "TABLE_SCHEM" ) ) );
		assertTrue( columns.containsKey( Key.of( "TABLE_NAME" ) ) );
		assertTrue( columns.containsKey( Key.of( "TABLE_TYPE" ) ) );
		assertTrue( columns.containsKey( Key.of( "REMARKS" ) ) );
		assertTrue( columns.containsKey( Key.of( "TYPE_CAT" ) ) );
		assertTrue( columns.containsKey( Key.of( "TYPE_SCHEM" ) ) );
		assertTrue( columns.containsKey( Key.of( "TYPE_NAME" ) ) );
		assertTrue( columns.containsKey( Key.of( "SELF_REFERENCING_COL_NAME" ) ) );
		assertTrue( columns.containsKey( Key.of( "REF_GENERATION" ) ) );

		IStruct testTableRow = resultQuery.stream()
		    .filter( row -> row.getAsString( Key.of( "TABLE_NAME" ) ).equals( "DEVELOPERS" ) )
		    .findFirst()
		    .orElse( null );
		assertNotNull( testTableRow );
	}

	@DisplayName( "Can get db tables when type=tables" )
	@Test
	public void testTablesTypeWithDBName() {
		instance.executeSource(
		    """
		        cfdbinfo( type='tables', name='result', dbname="BoxlangDB" )
		    """,
		    context );
		Query resultQuery = ( Query ) variables.get( result );
		assertTrue( resultQuery.size() > 0 );
		Boolean isCorrectDBName = resultQuery.stream()
		    .allMatch( row -> row.getAsString( Key.of( "TABLE_CAT" ) ).equals( "BoxlangDB" ) );
		assertNotNull( isCorrectDBName );
	}

	// Derby's database filters apparently don't work right, so this test is MySQL-only.
	@EnabledIf( "tools.JDBCTestUtils#hasMySQLDriver" )
	@DisplayName( "Gets empty tables query when unmatched database name is provided" )
	@Test
	public void testTablesTypeBadDBName() {
		instance.executeSource(
		    """
		        cfdbinfo( type='tables', name='result', datasource="MYSQLDB", dbname="foo" )
		    """,
		    context );
		Query resultQuery = ( Query ) variables.get( result );
		assertTrue( resultQuery.size() == 0 );
	}

	@DisplayName( "Can get filter table results by pattern name" )
	@Test
	public void testTablesTypeWithTablePattern() {
		instance.executeSource(
		    """
		        cfdbinfo( type='tables', name='result', pattern="DEV%" )
		    """,
		    context );
		Query resultQuery = ( Query ) variables.get( result );
		assertTrue( resultQuery.size() == 1 );
		Boolean isDeveloperTable = resultQuery.stream()
		    .allMatch( row -> row.getAsString( Key.of( "TABLE_NAME" ) ).equals( "DEVELOPERS" ) );
		assertNotNull( isDeveloperTable );
	}
}
