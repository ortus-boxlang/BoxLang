
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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
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
		getDatasource().execute(
		    "CREATE TABLE projects ( id INTEGER PRIMARY KEY, name VARCHAR(155), leadDev INTEGER, CONSTRAINT devID FOREIGN KEY (leadDev) REFERENCES admins(id) )" );
		getDatasource().execute(
		    "CREATE PROCEDURE FOO(IN S_MONTH INTEGER, IN S_YEAR INTEGER, OUT TOTAL DECIMAL(10,2)) PARAMETER STYLE JAVA READS SQL DATA LANGUAGE JAVA EXTERNAL NAME 'com.example.sales.calculateRevenueByMonth'" );
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
		assertThat( theResult ).isInstanceOf( Query.class );
		Query versionQuery = ( Query ) theResult;
		assertEquals( 1, versionQuery.size() );
		assertEquals( 6, versionQuery.getColumns().size() );

		assertEquals( "Apache Derby Embedded JDBC Driver", versionQuery.getRowAsStruct( 0 ).getAsString( Key.of( "DRIVER_NAME" ) ) );
	}

	@EnabledIf( "tools.JDBCTestUtils#hasMySQLModule" )
	@DisplayName( "Can get catalog and schema names" )
	@Test
	public void testDBNamesType() {
		getInstance().executeSource(
		    """
		        cfdbinfo( type='dbnames', name='result', datasource='mysqldatasource' )
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );
		Object theResult = getVariables().get( result );
		assertThat( theResult ).isInstanceOf( Query.class );

		Query dbNamesQuery = ( Query ) theResult;
		assertThat( dbNamesQuery.size() ).isGreaterThan( 0 );
		assertEquals( 2, dbNamesQuery.getColumns().size() );

		IStruct ourDBRow = dbNamesQuery.stream()
		    .filter( row -> row.getAsString( Key.of( "DBNAME" ) ).equals( "mysqlDB" ) )
		    .findFirst()
		    .orElse( null );

		assertNotNull( ourDBRow );
		assertEquals( "CATALOG", ourDBRow.getAsString( Key.type ) );
		assertEquals( "mysqlDB", ourDBRow.getAsString( Key.of( "DBNAME" ) ) );
	}

	@DisplayName( "Can get table column data" )
	@Test
	public void testColumnsType() {
		getInstance().executeSource(
		    """
		        cfdbinfo( type='columns', name='result', table='admins' )
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );
		Object theResult = getVariables().get( result );
		assertThat( theResult ).isInstanceOf( Query.class );

		Query resultQuery = ( Query ) theResult;
		assertThat( resultQuery.size() ).isGreaterThan( 0 );
	}

	@Disabled( "Test incomplete." )
	@DisplayName( "Can specify database.dbo.table in table attribute" )
	@Test
	public void testDatabaseSchemaNavigation() {
		String databaseName = getDatasource().getConfiguration().properties.getAsString( Key.database );
		getInstance().executeSource( "cfdbinfo( type='columns', name='result', table='%s.dbo.admins' )".formatted( databaseName ),
		    getContext(), BoxSourceType.CFSCRIPT );
		Object theResult = getVariables().get( result );
		assertThat( theResult ).isInstanceOf( Query.class );

		Query resultQuery = ( Query ) theResult;
		assertThat( resultQuery.size() ).isGreaterThan( 0 );
	}

	@DisplayName( "Can get primary and foreign key info in type=columns" )
	@Test
	public void testKeyInfoInColumnsResult() {
		getInstance().executeSource(
		    """
		        cfdbinfo( type='columns', name='result', table='PROJECTS' )
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );
		Query resultQuery = getVariables().getAsQuery( result );
		assertThat( resultQuery.size() ).isGreaterThan( 0 );
		assertEquals( 29, resultQuery.getColumns().size() );

		IStruct nameRow = resultQuery.stream()
		    .filter( row -> row.getAsString( Key.of( "COLUMN_NAME" ) ).equals( "NAME" ) )
		    .findFirst()
		    .orElse( null );

		assertNotNull( nameRow );
		assertEquals( "NAME", nameRow.getAsString( Key.of( "COLUMN_NAME" ) ) );
		assertEquals( "VARCHAR", nameRow.getAsString( Key.of( "TYPE_NAME" ) ) );
		assertEquals( 155, nameRow.getAsInteger( Key.of( "COLUMN_SIZE" ) ) );

		IStruct idRow = resultQuery.stream()
		    .filter( row -> row.getAsString( Key.of( "COLUMN_NAME" ) ).equals( "ID" ) )
		    .findFirst()
		    .orElse( null );

		assertNotNull( idRow );
		assertEquals( true, idRow.getAsBoolean( Key.of( "IS_PRIMARYKEY" ) ) );
		assertEquals( false, idRow.getAsBoolean( Key.of( "IS_FOREIGNKEY" ) ) );

		IStruct leadDevRow = resultQuery.stream()
		    .filter( row -> row.getAsString( Key.of( "COLUMN_NAME" ) ).equals( "LEADDEV" ) )
		    .findFirst()
		    .orElse( null );

		assertNotNull( leadDevRow );
		assertEquals( false, leadDevRow.getAsBoolean( Key.of( "IS_PRIMARYKEY" ) ) );
		assertEquals( true, leadDevRow.getAsBoolean( Key.of( "IS_FOREIGNKEY" ) ) );
		assertEquals( "ID", leadDevRow.getAsString( Key.of( "REFERENCED_PRIMARYKEY" ) ) );
		assertEquals( "ADMINS", leadDevRow.getAsString( Key.of( "REFERENCED_PRIMARYKEY_TABLE" ) ) );
	}

	@DisplayName( "Throws on non-existent tablename" )
	@Test
	public void testColumnsTypeWithNonExistentTable() {
		assertThrows( DatabaseException.class,
		    () -> getInstance().executeSource( "cfdbinfo( type='columns', name='result', table='404NotFound' )", getContext(), BoxSourceType.CFSCRIPT ) );
	}

	@DisplayName( "Can get db tables with a filter, but the type is NOT tables, so it must throw an exception" )
	@Test
	public void testTablesTypeWithFilterAndBadType() {
		assertThrows( BoxValidationException.class,
		    () -> getInstance().executeSource( "cfdbinfo( type='columns', name='result', filter='TABLE' )", getContext(), BoxSourceType.CFSCRIPT ) );
	}

	@DisplayName( "Can get db tables with a basic filter of 'TABLE'" )
	@Test
	public void testTablesTypeWithFilter() {
		getInstance().executeSource(
		    """
		        cfdbinfo( type='tables', name='result', filter='TABLE' )
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );
		Query theResult = getVariables().getAsQuery( result );
		assertThat( theResult.size() ).isGreaterThan( 0 );
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
		assertThat( theResult ).isInstanceOf( Query.class );

		Query resultQuery = ( Query ) theResult;
		assertThat( resultQuery.size() ).isGreaterThan( 0 );
		Map<Key, QueryColumn> columns = resultQuery.getColumns();

		assertEquals( 10, columns.size() );
		assertThat( columns ).containsKey( Key.of( "TABLE_CAT" ) );
		assertThat( columns ).containsKey( Key.of( "TABLE_SCHEM" ) );
		assertThat( columns ).containsKey( Key.of( "TABLE_NAME" ) );
		assertThat( columns ).containsKey( Key.of( "TABLE_TYPE" ) );
		assertThat( columns ).containsKey( Key.of( "REMARKS" ) );
		assertThat( columns ).containsKey( Key.of( "TYPE_CAT" ) );
		assertThat( columns ).containsKey( Key.of( "TYPE_SCHEM" ) );
		assertThat( columns ).containsKey( Key.of( "TYPE_NAME" ) );
		assertThat( columns ).containsKey( Key.of( "SELF_REFERENCING_COL_NAME" ) );
		assertThat( columns ).containsKey( Key.of( "REF_GENERATION" ) );

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
		assertThat( resultQuery.size() ).isGreaterThan( 0 );
		Boolean isCorrectDBName = resultQuery.stream()
		    .allMatch( row -> row.getAsString( Key.of( "TABLE_CAT" ) ).equals( "BoxlangDB" ) );
		assertNotNull( isCorrectDBName );
	}

	// Derby's database filters apparently don't work right, so this test is MySQL-only.
	@EnabledIf( "tools.JDBCTestUtils#hasMySQLModule" )
	@DisplayName( "Gets empty tables query when unmatched database name is provided" )
	@Test
	public void testTablesTypeBadDBName() {
		getInstance().executeSource(
		    """
		        cfdbinfo( type='tables', name='result', datasource="mysqldatasource", dbname="foo" )
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );
		Query resultQuery = ( Query ) getVariables().get( result );
		assertThat( resultQuery.size() ).isEqualTo( 0 );
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
		assertThat( resultQuery.size() ).isEqualTo( 1 );
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
		assertThat( theResult ).isInstanceOf( Query.class );

		Query resultQuery = ( Query ) theResult;
		assertThat( resultQuery.size() ).isGreaterThan( 0 );
		Map<Key, QueryColumn> columns = resultQuery.getColumns();

		assertEquals( 14, columns.size() );
		assertThat( columns ).containsKey( Key.of( "PKTABLE_CAT" ) );
		assertThat( columns ).containsKey( Key.of( "PKTABLE_SCHEM" ) );
		assertThat( columns ).containsKey( Key.of( "PKTABLE_NAME" ) );
		assertThat( columns ).containsKey( Key.of( "PKCOLUMN_NAME" ) );
		assertThat( columns ).containsKey( Key.of( "FKTABLE_CAT" ) );
		assertThat( columns ).containsKey( Key.of( "FKTABLE_SCHEM" ) );
		assertThat( columns ).containsKey( Key.of( "FKTABLE_NAME" ) );
		assertThat( columns ).containsKey( Key.of( "FKCOLUMN_NAME" ) );
		assertThat( columns ).containsKey( Key.of( "KEY_SEQ" ) );
		assertThat( columns ).containsKey( Key.of( "UPDATE_RULE" ) );
		assertThat( columns ).containsKey( Key.of( "DELETE_RULE" ) );
		assertThat( columns ).containsKey( Key.of( "FK_NAME" ) );
		assertThat( columns ).containsKey( Key.of( "PK_NAME" ) );
		assertThat( columns ).containsKey( Key.of( "DEFERRABILITY" ) );

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
		assertThat( theResult ).isInstanceOf( Query.class );

		Query resultQuery = ( Query ) theResult;
		assertThat( resultQuery.size() ).isGreaterThan( 0 );
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
		assertThat( theResult ).isInstanceOf( Query.class );

		Query resultQuery = ( Query ) theResult;
		assertThat( resultQuery.size() ).isGreaterThan( 0 );
		Map<Key, QueryColumn> columns = resultQuery.getColumns();

		assertEquals( 13, columns.size() );
		assertThat( columns ).containsKey( Key.of( "TABLE_CAT" ) );
		assertThat( columns ).containsKey( Key.of( "TABLE_SCHEM" ) );
		assertThat( columns ).containsKey( Key.of( "TABLE_NAME" ) );
		assertThat( columns ).containsKey( Key.of( "NON_UNIQUE" ) );
		assertThat( columns ).containsKey( Key.of( "INDEX_QUALIFIER" ) );
		assertThat( columns ).containsKey( Key.of( "INDEX_NAME" ) );
		assertThat( columns ).containsKey( Key.of( "TYPE" ) );
		assertThat( columns ).containsKey( Key.of( "ORDINAL_POSITION" ) );
		assertThat( columns ).containsKey( Key.of( "COLUMN_NAME" ) );
		assertThat( columns ).containsKey( Key.of( "ASC_OR_DESC" ) );
		assertThat( columns ).containsKey( Key.of( "CARDINALITY" ) );
		assertThat( columns ).containsKey( Key.of( "PAGES" ) );
		assertThat( columns ).containsKey( Key.of( "FILTER_CONDITION" ) );

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
		assertThat( theResult ).isInstanceOf( Query.class );

		Query resultQuery = ( Query ) theResult;
		assertThat( resultQuery.size() ).isGreaterThan( 0 );
		Map<Key, QueryColumn> columns = resultQuery.getColumns();

		assertEquals( 9, columns.size() );
		assertThat( columns ).containsKey( Key.of( "PROCEDURE_CAT" ) );
		assertThat( columns ).containsKey( Key.of( "PROCEDURE_SCHEM" ) );
		assertThat( columns ).containsKey( Key.of( "PROCEDURE_NAME" ) );
		assertThat( columns ).containsKey( Key.of( "RESERVED1" ) );
		assertThat( columns ).containsKey( Key.of( "RESERVED2" ) );
		assertThat( columns ).containsKey( Key.of( "RESERVED3" ) );
		assertThat( columns ).containsKey( Key.of( "REMARKS" ) );
		assertThat( columns ).containsKey( Key.of( "PROCEDURE_TYPE" ) );
		assertThat( columns ).containsKey( Key.of( "SPECIFIC_NAME" ) );

		IStruct testTableRow = resultQuery.stream()
		    .filter( row -> row.getAsString( Key.of( "PROCEDURE_NAME" ) ).equals( "FOO" ) )
		    .findFirst()
		    .orElse( null );
		assertNotNull( testTableRow );
	}
}
