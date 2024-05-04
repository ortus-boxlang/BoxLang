
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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.bifs.global.jdbc.BaseJDBCTest;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumn;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

public class DBInfoTest extends BaseJDBCTest {

	static Key			result	= new Key( "result" );
	static DataSource	MySQLDataSource;

	@BeforeAll
	public static void additionalSetup() {
		getDatasource().execute( "CREATE TABLE admins ( id INTEGER PRIMARY KEY, name VARCHAR(155) )" );
		getDatasource().execute( "CREATE TABLE projects ( id INTEGER, leadDev INTEGER, CONSTRAINT devID FOREIGN KEY (leadDev) REFERENCES admins(id) )" );
		getDatasource().execute(
		    "CREATE PROCEDURE FOO(IN S_MONTH INTEGER, IN S_YEAR INTEGER, OUT TOTAL DECIMAL(10,2)) PARAMETER STYLE JAVA READS SQL DATA LANGUAGE JAVA EXTERNAL NAME 'com.example.sales.calculateRevenueByMonth'" );
		if ( tools.JDBCTestUtils.hasMySQLDriver() ) {
			Key MySQLDataSourceName = Key.of( "MYSQLDB" );
			// MySQLDataSource = getDatasourceService().register( MySQLDataSourceName, Struct.of(
			// "connectionString", "jdbc:mysql://localhost:3306",
			// "username", "root",
			// "password", "secret"
			// ) );
			MySQLDataSource.execute( "CREATE DATABASE IF NOT EXISTS testDB" );
			MySQLDataSource.execute( "USE testDB" );
		}
	}

	@DisplayName( "It requires a non-null `type` argument matching a valid type" )
	@Test
	public void requiredTypeValidation() {
		assertThrows( BoxValidationException.class, () -> {
			getInstance().executeSource( "CFDBInfo();", getContext(), BoxSourceType.CFSCRIPT );
		} );
		assertThrows( BoxValidationException.class, () -> {
			getInstance().executeSource( "CFDBInfo( type='foo' );", getContext(), BoxSourceType.CFSCRIPT );
		} );
		assertDoesNotThrow( () -> {
			getInstance().executeSource( "CFDBInfo( type='version', name='result' );", getContext(), BoxSourceType.CFSCRIPT );
		} );
	}

	@DisplayName( "It requires the `table` argument on column, foreignkeys, and index types`" )
	@Test
	public void typeRequiresTableValidation() {
		assertThrows( BoxValidationException.class, () -> {
			getInstance().executeSource( "CFDBInfo( type='columns' );", getContext(), BoxSourceType.CFSCRIPT );
		} );
		assertThrows( BoxValidationException.class, () -> {
			getInstance().executeSource( "CFDBInfo( type='foreignkeys' );", getContext(), BoxSourceType.CFSCRIPT );
		} );
		assertThrows( BoxValidationException.class, () -> {
			getInstance().executeSource( "CFDBInfo( type='index' );", getContext(), BoxSourceType.CFSCRIPT );
		} );
	}

	@DisplayName( "Throws on non-existent datasource" )
	@Test
	public void testDataSourceAttribute() {
		assertThrows(
		    DatabaseException.class,
		    () -> getInstance().executeSource( "cfdbinfo( type='version', name='result', datasource='not_found' )", getContext(), BoxSourceType.CFSCRIPT )
		);
	}

	@DisplayName( "Can get JDBC driver version info" )
	@Test
	public void testVersionType() {
		getInstance().executeSource(
		    """
		        cfdbinfo( type='version', name='result' )
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );
		Object theResult = getVariables().get( result );
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
		getInstance().executeSource(
		    """
		        cfdbinfo( type='dbnames', name='result', datasource='MYSQLDB' )
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );
		Object theResult = getVariables().get( result );
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
		getInstance().executeSource(
		    """
		        cfdbinfo( type='columns', name='result', table='ADMINS' )
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );
		Object theResult = getVariables().get( result );
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
		assertThrows( DatabaseException.class,
		    () -> getInstance().executeSource( "cfdbinfo( type='columns', name='result', table='404NotFound' )", getContext(), BoxSourceType.CFSCRIPT ) );
	}

	@DisplayName( "Can get db tables when type=tables" )
	@Test
	public void testTablesType() {
		getInstance().executeSource(
		    """
		        cfdbinfo( type='tables', name='result' )
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );
		Object theResult = getVariables().get( result );
		assertTrue( theResult instanceof Query );

		Query resultQuery = ( Query ) theResult;
		assertTrue( resultQuery.size() > 0 );
		Map<Key, QueryColumn> columns = resultQuery.getColumns();

		assertEquals( 10, columns.size() );
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
		    .filter( row -> row.getAsString( Key.of( "TABLE_NAME" ) ).equals( "ADMINS" ) )
		    .findFirst()
		    .orElse( null );
		assertNotNull( testTableRow );
	}

	@DisplayName( "Can get db tables when type=tables" )
	@Test
	public void testTablesTypeWithDBName() {
		getInstance().executeSource(
		    """
		        cfdbinfo( type='tables', name='result', dbname="BoxlangDB" )
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );
		Query resultQuery = ( Query ) getVariables().get( result );
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
		getInstance().executeSource(
		    """
		        cfdbinfo( type='tables', name='result', datasource="MYSQLDB", dbname="foo" )
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );
		Query resultQuery = ( Query ) getVariables().get( result );
		assertTrue( resultQuery.size() == 0 );
	}

	@DisplayName( "Can get filter table results by pattern name" )
	@Test
	public void testTablesTypeWithTablePattern() {
		getInstance().executeSource(
		    """
		        cfdbinfo( type='tables', name='result', pattern="DEV%" )
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );
		Query resultQuery = ( Query ) getVariables().get( result );
		assertTrue( resultQuery.size() == 1 );
		Boolean isDeveloperTable = resultQuery.stream()
		    .allMatch( row -> row.getAsString( Key.of( "TABLE_NAME" ) ).equals( "ADMINS" ) );
		assertNotNull( isDeveloperTable );
	}

	@DisplayName( "Can get foreign keys on a table via type=foreignkeys" )
	@Test
	public void testForeignKeysType() {
		getInstance().executeSource(
		    """
		        cfdbinfo( type='foreignkeys', table="ADMINS", name='result' )
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );
		Object theResult = getVariables().get( result );
		assertTrue( theResult instanceof Query );

		Query resultQuery = ( Query ) theResult;
		assertTrue( resultQuery.size() > 0 );
		Map<Key, QueryColumn> columns = resultQuery.getColumns();

		assertEquals( 14, columns.size() );
		assertTrue( columns.containsKey( Key.of( "PKTABLE_CAT" ) ) );
		assertTrue( columns.containsKey( Key.of( "PKTABLE_SCHEM" ) ) );
		assertTrue( columns.containsKey( Key.of( "PKTABLE_NAME" ) ) );
		assertTrue( columns.containsKey( Key.of( "PKCOLUMN_NAME" ) ) );
		assertTrue( columns.containsKey( Key.of( "FKTABLE_CAT" ) ) );
		assertTrue( columns.containsKey( Key.of( "FKTABLE_SCHEM" ) ) );
		assertTrue( columns.containsKey( Key.of( "FKTABLE_NAME" ) ) );
		assertTrue( columns.containsKey( Key.of( "FKCOLUMN_NAME" ) ) );
		assertTrue( columns.containsKey( Key.of( "KEY_SEQ" ) ) );
		assertTrue( columns.containsKey( Key.of( "UPDATE_RULE" ) ) );
		assertTrue( columns.containsKey( Key.of( "DELETE_RULE" ) ) );
		assertTrue( columns.containsKey( Key.of( "FK_NAME" ) ) );
		assertTrue( columns.containsKey( Key.of( "PK_NAME" ) ) );
		assertTrue( columns.containsKey( Key.of( "DEFERRABILITY" ) ) );

		IStruct testTableRow = resultQuery.stream()
		    .filter( row -> row.getAsString( Key.of( "FK_NAME" ) ).equals( "DEVID" ) )
		    .findFirst()
		    .orElse( null );
		assertNotNull( testTableRow );
	}

	@DisplayName( "Can get table indices on any table case" )
	@Test
	public void testTableNameCasing() {
		getInstance().executeSource(
		    """
		    	cfdbinfo( type='index', table="adMIns", name='result' )
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );
		Object theResult = getVariables().get( result );
		assertTrue( theResult instanceof Query );

		Query resultQuery = ( Query ) theResult;
		assertTrue( resultQuery.size() > 0 );
	}

	@DisplayName( "Can get table indices via type=index" )
	@Test
	public void testIndexType() {
		getInstance().executeSource(
		    """
		        cfdbinfo( type='index', table="ADMINS", name='result' )
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );
		Object theResult = getVariables().get( result );
		assertTrue( theResult instanceof Query );

		Query resultQuery = ( Query ) theResult;
		assertTrue( resultQuery.size() > 0 );
		Map<Key, QueryColumn> columns = resultQuery.getColumns();

		assertEquals( 13, columns.size() );
		assertTrue( columns.containsKey( Key.of( "TABLE_CAT" ) ) );
		assertTrue( columns.containsKey( Key.of( "TABLE_SCHEM" ) ) );
		assertTrue( columns.containsKey( Key.of( "TABLE_NAME" ) ) );
		assertTrue( columns.containsKey( Key.of( "NON_UNIQUE" ) ) );
		assertTrue( columns.containsKey( Key.of( "INDEX_QUALIFIER" ) ) );
		assertTrue( columns.containsKey( Key.of( "INDEX_NAME" ) ) );
		assertTrue( columns.containsKey( Key.of( "TYPE" ) ) );
		assertTrue( columns.containsKey( Key.of( "ORDINAL_POSITION" ) ) );
		assertTrue( columns.containsKey( Key.of( "COLUMN_NAME" ) ) );
		assertTrue( columns.containsKey( Key.of( "ASC_OR_DESC" ) ) );
		assertTrue( columns.containsKey( Key.of( "CARDINALITY" ) ) );
		assertTrue( columns.containsKey( Key.of( "PAGES" ) ) );
		assertTrue( columns.containsKey( Key.of( "FILTER_CONDITION" ) ) );

		IStruct testTableRow = resultQuery.stream()
		    .filter( row -> row.getAsString( Key.of( "COLUMN_NAME" ) ).equals( "ID" ) )
		    .findFirst()
		    .orElse( null );
		assertNotNull( testTableRow );
	}

	@DisplayName( "Can get procedures via type=index" )
	@Test
	public void testProceduresType() {
		getInstance().executeSource(
		    """
		        cfdbinfo( type='procedures', name='result' )
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );
		Object theResult = getVariables().get( result );
		assertTrue( theResult instanceof Query );

		Query resultQuery = ( Query ) theResult;
		assertTrue( resultQuery.size() > 0 );
		Map<Key, QueryColumn> columns = resultQuery.getColumns();

		assertEquals( 9, columns.size() );
		assertTrue( columns.containsKey( Key.of( "PROCEDURE_CAT" ) ) );
		assertTrue( columns.containsKey( Key.of( "PROCEDURE_SCHEM" ) ) );
		assertTrue( columns.containsKey( Key.of( "PROCEDURE_NAME" ) ) );
		assertTrue( columns.containsKey( Key.of( "RESERVED1" ) ) );
		assertTrue( columns.containsKey( Key.of( "RESERVED2" ) ) );
		assertTrue( columns.containsKey( Key.of( "RESERVED3" ) ) );
		assertTrue( columns.containsKey( Key.of( "REMARKS" ) ) );
		assertTrue( columns.containsKey( Key.of( "PROCEDURE_TYPE" ) ) );
		assertTrue( columns.containsKey( Key.of( "SPECIFIC_NAME" ) ) );

		IStruct testTableRow = resultQuery.stream()
		    .filter( row -> row.getAsString( Key.of( "PROCEDURE_NAME" ) ).equals( "FOO" ) )
		    .findFirst()
		    .orElse( null );
		assertNotNull( testTableRow );
	}
}
