
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

import java.sql.SQLException;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.global.jdbc.BaseJDBCTest;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.jdbc.drivers.AbstractDriverTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumn;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import tools.JDBCTestUtils;

@EnabledIf( "tools.JDBCTestUtils#hasMySQLModule" )
public class DBInfoTest extends BaseJDBCTest {

	static Key					result				= new Key( "result" );
	public static DataSource	DBInfoDatasource;
	protected static Key		datasourceName		= Key.of( "DBInfoDatasource" );

	protected static IStruct	datasourceConfig	= Struct.of(
	    "username", "root",
	    "password", "123456Password",
	    "host", "localhost",
	    "port", "3309",
	    "driver", "mysql",
	    "database", "myDB",
	    "custom", "allowMultiQueries=true"
	);
	static BoxRuntime			instance;
	IBoxContext					context;

	@BeforeAll
	public static void additionalSetup() {
		instance = BoxRuntime.getInstance( true );
		IBoxContext setUpContext = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		DBInfoDatasource = AbstractDriverTest.setupTestDatasource( instance, setUpContext, datasourceName, datasourceConfig );

		try {
			DBInfoDatasource.execute( "CREATE TABLE admins ( id INTEGER PRIMARY KEY, name VARCHAR(155) )", setUpContext );
		} catch ( DatabaseException e ) {
			e.printStackTrace();
			// Ignore the exception if the table already exists
		}
		try {
			DBInfoDatasource.execute(
			    "CREATE TABLE projects ( id INTEGER PRIMARY KEY, name VARCHAR(155), leadDev INTEGER, CONSTRAINT devID FOREIGN KEY (leadDev) REFERENCES admins(id) )",
			    setUpContext );
		} catch ( DatabaseException e ) {
			e.printStackTrace();
			// Ignore the exception if the table already exists
		}
		DBInfoDatasource.execute( "DROP PROCEDURE IF EXISTS FOO;", setUpContext );
		DBInfoDatasource.execute(
		    """
		    CREATE PROCEDURE FOO(
		    	IN  S_MONTH INT,
		    	IN  S_YEAR  INT,
		    	OUT TOTAL   DECIMAL(10,2)
		    )
		    READS SQL DATA
		    BEGIN
		    	-- TODO: implement the body here, for example:
		    	-- SELECT SUM(amount)
		    	--   INTO TOTAL
		    	--   FROM sales
		    	--  WHERE MONTH(sale_date) = S_MONTH
		    	--    AND YEAR(sale_date)  = S_YEAR;
		    END
		    	""",
		    setUpContext );
	}

	@AfterAll
	public static void additionalTeardown() throws SQLException {
		IBoxContext tearDownContext = new ScriptingRequestBoxContext( instance.getRuntimeContext() );

		if ( DBInfoDatasource != null ) {
			JDBCTestUtils.dropTestTable( DBInfoDatasource, tearDownContext, "admins", true );
			JDBCTestUtils.dropTestTable( DBInfoDatasource, tearDownContext, "projects", true );
			DBInfoDatasource.execute( "DROP PROCEDURE IF EXISTS FOO;", tearDownContext );
			DBInfoDatasource.shutdown();
		}
	}

	@DisplayName( "It requires a non-null `type` argument matching a valid type" )
	@Test
	public void requiredTypeValidation() {
		assertThrows( BoxValidationException.class, () -> {
			getInstance().executeSource( "CFDBInfo( datasource='DBInfoDatasource' );", getContext(), BoxSourceType.CFSCRIPT );
		} );
		assertThrows( BoxValidationException.class, () -> {
			getInstance().executeSource( "CFDBInfo( datasource='DBInfoDatasource', type='foo' );", getContext(), BoxSourceType.CFSCRIPT );
		} );
		assertDoesNotThrow( () -> {
			getInstance().executeSource( "CFDBInfo( datasource='DBInfoDatasource', type='version', name='result' );", getContext(), BoxSourceType.CFSCRIPT );
		} );
	}

	@DisplayName( "It requires the `table` argument on column, foreignkeys, and index types`" )
	@Test
	public void typeRequiresTableValidation() {
		assertThrows( BoxValidationException.class, () -> {
			getInstance().executeSource( "CFDBInfo( datasource='DBInfoDatasource', type='columns' );", getContext(), BoxSourceType.CFSCRIPT );
		} );
		assertThrows( BoxValidationException.class, () -> {
			getInstance().executeSource( "CFDBInfo( datasource='DBInfoDatasource', type='foreignkeys' );", getContext(), BoxSourceType.CFSCRIPT );
		} );
		assertThrows( BoxValidationException.class, () -> {
			getInstance().executeSource( "CFDBInfo( datasource='DBInfoDatasource', type='index' );", getContext(), BoxSourceType.CFSCRIPT );
		} );
	}

	@DisplayName( "Throws on non-existent datasource" )
	@Test
	public void testDataSourceAttribute() {
		assertThrows(
		    DatabaseException.class,
		    () -> getInstance().executeSource( "cfdbinfo( datasource='DBInfoDatasource', type='version', name='result', datasource='not_found' )", getContext(),
		        BoxSourceType.CFSCRIPT )
		);
	}

	@DisplayName( "Can get JDBC driver version info" )
	@Test
	public void testVersionType() {
		getInstance().executeSource(
		    """
		        cfdbinfo( datasource='DBInfoDatasource', type='version', name='result' )
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );
		Object theResult = getVariables().get( result );
		assertThat( theResult ).isInstanceOf( Query.class );
		Query versionQuery = ( Query ) theResult;
		assertThat( versionQuery.size() ).isEqualTo( 1 );
		assertThat( versionQuery.getColumns().size() ).isEqualTo( 6 );
		assertThat( versionQuery.getRowAsStruct( 0 ).getAsString( Key.of( "DRIVER_NAME" ) ) ).isEqualTo( "MySQL Connector/J" );
	}

	@DisplayName( "Can get table column data" )
	@Test
	public void testColumnsType() {
		getInstance().executeSource(
		    """
		        cfdbinfo( datasource='DBInfoDatasource', type='columns', name='result', table='admins' )
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
		String databaseName = DBInfoDatasource.getConfiguration().properties.getAsString( Key.database );
		getInstance().executeSource(
		    "cfdbinfo( datasource='DBInfoDatasource', type='columns', name='result', table='%s.dbo.admins' )".formatted( databaseName ),
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
		        cfdbinfo( datasource='DBInfoDatasource', type='columns', name='result', table='projects' )
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );
		Query resultQuery = getVariables().getAsQuery( result );
		assertThat( resultQuery.size() ).isGreaterThan( 0 );
		assertThat( resultQuery.getColumns().size() ).isEqualTo( 28 );

		IStruct nameRow = resultQuery.stream()
		    .filter( row -> row.getAsString( Key.of( "COLUMN_NAME" ) ).equalsIgnoreCase( "name" ) )
		    .findFirst()
		    .orElse( null );

		assertNotNull( nameRow );
		assertThat( nameRow.getAsString( Key.of( "COLUMN_NAME" ) ) ).isEqualTo( "name" );
		assertThat( nameRow.getAsString( Key.of( "TYPE_NAME" ) ) ).isEqualTo( "VARCHAR" );
		assertThat( nameRow.getAsInteger( Key.of( "COLUMN_SIZE" ) ) ).isEqualTo( 155 );

		IStruct idRow = resultQuery.stream()
		    .filter( row -> row.getAsString( Key.of( "COLUMN_NAME" ) ).equalsIgnoreCase( "id" ) )
		    .findFirst()
		    .orElse( null );

		assertNotNull( idRow );
		assertThat( idRow.getAsBoolean( Key.of( "IS_PRIMARYKEY" ) ) ).isEqualTo( true );
		assertThat( idRow.getAsBoolean( Key.of( "IS_FOREIGNKEY" ) ) ).isEqualTo( false );

		IStruct leadDevRow = resultQuery.stream()
		    .filter( row -> row.getAsString( Key.of( "COLUMN_NAME" ) ).equalsIgnoreCase( "leaddev" ) )
		    .findFirst()
		    .orElse( null );

		assertNotNull( leadDevRow );
		assertThat( leadDevRow.getAsBoolean( Key.of( "IS_PRIMARYKEY" ) ) ).isEqualTo( false );
		assertThat( leadDevRow.getAsBoolean( Key.of( "IS_FOREIGNKEY" ) ) ).isEqualTo( true );

		String	referencedKey	= leadDevRow.getAsString( Key.of( "REFERENCED_PRIMARYKEY" ) );
		String	referencedTable	= leadDevRow.getAsString( Key.of( "REFERENCED_PRIMARYKEY_TABLE" ) );
		assertThat( referencedKey.equalsIgnoreCase( "id" ) ).isTrue();
		assertThat( referencedTable.equalsIgnoreCase( "admins" ) ).isTrue();
	}

	@DisplayName( "Throws on non-existent tablename" )
	@Test
	public void testColumnsTypeWithNonExistentTable() {
		assertThrows( DatabaseException.class,
		    () -> getInstance().executeSource( "cfdbinfo( datasource='DBInfoDatasource', type='columns', name='result', table='404NotFound' )", getContext(),
		        BoxSourceType.CFSCRIPT ) );
	}

	@DisplayName( "Can get db tables with a filter, but the type is NOT tables, so it must throw an exception" )
	@Test
	public void testTablesTypeWithFilterAndBadType() {
		assertThrows( BoxValidationException.class,
		    () -> getInstance().executeSource( "cfdbinfo( datasource='DBInfoDatasource', type='columns', name='result', filter='TABLE' )", getContext(),
		        BoxSourceType.CFSCRIPT ) );
	}

	@DisplayName( "Can get db tables with a basic filter of 'TABLE'" )
	@Test
	public void testTablesTypeWithFilter() {
		getInstance().executeSource(
		    """
		        cfdbinfo( datasource='DBInfoDatasource', type='tables', name='result', filter='TABLE' )
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
		        cfdbinfo( datasource='DBInfoDatasource', type='tables', name='result' )
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
		    .filter( row -> row.getAsString( Key.of( "TABLE_NAME" ) ).equals( "admins" ) )
		    .findFirst()
		    .orElse( null );
		assertNotNull( testTableRow );
	}

	@DisplayName( "Can get db tables with DBName filter" )
	@Test
	public void testTablesTypeWithDBName() {
		getInstance().executeSource(
		    """
		        cfdbinfo( datasource='DBInfoDatasource', type='tables', name='result' )
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );
		Query resultQuery = ( Query ) getVariables().get( result );
		assertThat( resultQuery.size() ).isGreaterThan( 0 );
		Boolean isCorrectDBName = resultQuery.stream()
		    .allMatch( row -> row.getAsString( Key.of( "TABLE_CAT" ) ).equals( "DBInfoDatasource" ) );
		assertNotNull( isCorrectDBName );
	}

	@DisplayName( "Can get filter table results by pattern name" )
	@Test
	public void testTablesTypeWithTablePattern() {
		getInstance().executeSource(
		    """
		        cfdbinfo( datasource='DBInfoDatasource', type='tables', name='result', pattern="dev%" )
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
		        cfdbinfo( datasource='DBInfoDatasource', type='foreignkeys', table="admins", name='result' )
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
		    .filter( row -> row.getAsString( Key.of( "FK_NAME" ) ).equalsIgnoreCase( "devID" ) )
		    .findFirst()
		    .orElse( null );
		assertNotNull( testTableRow );
	}

	@DisplayName( "Can get table indices via type=index" )
	@Test
	public void testIndexType() {
		getInstance().executeSource(
		    """
		        cfdbinfo( datasource='DBInfoDatasource', type='index', table="admins", name='result' )
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
		    .filter( row -> row.getAsString( Key.of( "COLUMN_NAME" ) ).equalsIgnoreCase( "id" ) )
		    .findFirst()
		    .orElse( null );
		assertNotNull( testTableRow );
	}

	@DisplayName( "Can get procedures via type=index" )
	@Test
	public void testProceduresType() {
		getInstance().executeSource(
		    """
		        cfdbinfo( datasource='DBInfoDatasource', type='procedures', name='result' )
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
