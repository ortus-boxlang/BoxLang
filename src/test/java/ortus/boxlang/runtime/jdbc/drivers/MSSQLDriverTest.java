package ortus.boxlang.runtime.jdbc.drivers;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

@EnabledIf( "tools.JDBCTestUtils#hasMSSQLModule" )
public class MSSQLDriverTest extends AbstractDriverTest {

	public static DataSource	mssqlDatasource;

	protected static Key		datasourceName		= Key.of( "MSSQLdatasource" );

	protected static IStruct	datasourceConfig	= Struct.of(
	    "username", "sa",
	    "password", "123456Password",
	    "host", "localhost",
	    "port", "1433",
	    "driver", "mssql",
	    "database", "master"
	);

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
		IBoxContext setUpContext = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		mssqlDatasource = AbstractDriverTest.setupTestDatasource( instance, setUpContext, datasourceName, datasourceConfig );
		MSSQLDriverTest.createGeneratedKeyTable( mssqlDatasource, setUpContext );
		MSSQLDriverTest.createStoredProcedure( mssqlDatasource, setUpContext );
	}

	@AfterAll
	public static void teardown() throws SQLException {
		IBoxContext tearDownContext = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		AbstractDriverTest.teardownTestDatasource( tearDownContext, mssqlDatasource );
	}

	public static void createGeneratedKeyTable( DataSource dataSource, IBoxContext context ) {
		try {
			dataSource.execute( "CREATE TABLE generatedKeyTest( id INT IDENTITY(1,1) PRIMARY KEY, name VARCHAR(155))", context );
		} catch ( DatabaseException ignored ) {
		}
	}

	public static void createStoredProcedure( DataSource dataSource, IBoxContext context ) {
		dataSource.execute(
		    """
		     CREATE OR ALTER PROCEDURE testProcedure
		    @in1 INT = 45,
		    @in2 NVARCHAR(50),
		    @inout1 INT OUTPUT,
		    @out1 NVARCHAR(50) OUTPUT
		     AS
		     BEGIN
		    -- Sleep for 10 ms to ensure measurable execution time
		    WAITFOR DELAY '00:00:00.010';
		    SET @out1 = CONCAT('foo-', @in1, '-', @in2);
		    SET @inout1 = @in1 + 100;
		    SELECT 'foo' as col UNION SELECT 'bar';
		    SELECT 'second' as myColumn;
		    RETURN 42;
		     END
		     """,
		    context
		);
	}

	/**
	 * Override to provide driver-specific datasource name
	 */
	@Override
	String getDatasourceName() {
		return "MSSQLdatasource";
	}

	@DisplayName( "It sets generatedKey in query meta" )
	@Test
	public void testGeneratedKey() {
		instance.executeStatement(
		    String.format( """
		                                          queryExecute( "
		                                          	INSERT INTO generatedKeyTest (name) VALUES ( 'Michael' ), ( 'Michael2');
		                                          	INSERT INTO generatedKeyTest (name) VALUES ( 'Brad' ), ( 'Brad2' );
		                                          	INSERT INTO generatedKeyTest (name) VALUES ( 'Luis' );
		                                          	INSERT INTO generatedKeyTest (name) VALUES ( 'Jon' ), ( 'Jon2' ), ( 'Jon3' );
		                   ",
		                                          	{},
		                                          	{ "result": "variables.result", "datasource" : "%s" }
		                                          );
		                                                         """, getDatasourceName() ),
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		IStruct meta = variables.getAsStruct( result );

		// MSSQL JDBC driver only hands back the last generated key
		assertThat( DoubleCaster.cast( meta.get( Key.generatedKey ), false ) ).isEqualTo( 8 );

		Array generatedKeys = meta.getAsArray( Key.generatedKeys );

		assertThat( generatedKeys ).hasSize( 1 );

		// MSSQL JDBC driver only hands back the last generated key
		Integer[] firstKeys = ( ( Array ) generatedKeys.get( 0 ) ).stream().map( IntegerCaster::cast ).toArray( Integer[]::new );
		assertThat( firstKeys ).isEqualTo( new Integer[] { 8 } );

		assertThat( meta.get( "updateCount" ) ).isEqualTo( 8 );
		Array updateCounts = meta.getAsArray( Key.of( "updateCounts" ) );
		assertThat( updateCounts.toArray() ).isEqualTo( new Integer[] { 2, 2, 1, 3 } );
	}

	@DisplayName( "It can get a MSSQL JDBC connection" )
	@Test
	void testMSSQLConnection() throws SQLException {
		DataSource myDataSource = DataSource.fromStruct(
		    Key.of( "mssql" ),
		    Struct.of(
		        "host", "localhost",
		        "port", "1433",
		        "dbdriver", "MSSQL",
		        "database", "master",
		        "dsn", "jdbc:sqlserver://{host}:{port}",
		        "custom", "DATABASENAME=master&sendStringParametersAsUnicode=false&SelectMethod=direct&applicationName=fooey",
		        "class", "com.microsoft.sqlserver.jdbc.SQLServerDriver",
		        "username", "${DB_USER}",
		        "password", "${DB_PASSWORD}",
		        "connectionLimit", "100",
		        "connectionTimeout", "20",
		        "username", "sa",
		        "password", "123456Password"
		    ) );
		try ( Connection conn = myDataSource.getConnection() ) {
			assertThat( conn ).isInstanceOf( Connection.class );
		}
		myDataSource.shutdown();
	}

	@DisplayName( "It can return a rowcount in the second SQL statement" )
	@Test
	public void testRowCount() {
		// @formatter:off
		instance.executeStatement(
		    String.format( """
				result = queryExecute( "
					update developers set name = 'Michael Borne'  where name = 'Michael Born';
					select @@rowcount c;
				", {}, { "datasource" : "%s"} ).c;
			""", getDatasourceName(), getDatasourceName() ),
		    context );
		// @formatter:on
		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@DisplayName( "It can pass date object params without specifying a sql type" )
	@Test
	public void testDateParamNoSqlType() {
		instance.executeSource(
		    """
		         result = queryExecute(
		         	"SELECT id from developers WHERE createdAt <= :created",
		    // Database is in UTC and this fails locally unless I artifically push this to tomorrow
		         	{ "created" : dateAdd( 'd', 1, now() ) },
		         	{ "datasource" : "MSSQLdatasource" }
		         );
		         """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertThat( query.size() ).isGreaterThan( 0 );
	}

	@DisplayName( "It won't throw on DROP statements like MSSQL does" )
	@Test
	public void testTableDrop() {
		// asking for a result set from a statement that doesn't return one should return an empty query
		instance.executeSource(
		    """
		    result = queryExecute( "DROP TABLE IF EXISTS foo", {}, { "datasource" : "MSSQLdatasource" } );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 0, query.size() );
	}

	@DisplayName( "It can return inserted values" )
	@Test
	public void testSQLOutput() {
		instance.executeStatement(
		    """
		        result = queryExecute( "
		            insert into developers (id, name) OUTPUT INSERTED.*
		            VALUES (1, 'Luis'), (2, 'Brad'), (3, 'Jon')
		        ", {}, { "datasource" : "MSSQLdatasource" } );
		    """, context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 3, query.size() );
		assertEquals( "Luis", query.getRowAsStruct( 0 ).get( Key._NAME ) );
		assertEquals( "Brad", query.getRowAsStruct( 1 ).get( Key._NAME ) );
		assertEquals( "Jon", query.getRowAsStruct( 2 ).get( Key._NAME ) );
	}

	@DisplayName( "It can raise SQL error" )
	@Test
	public void testSQLError() {
		Throwable t = assertThrows( Throwable.class, () -> instance.executeStatement(
		    """
		        queryExecute( "
		        	RAISERROR('Boom!',11,1);
		        ", {}, { "datasource" : "MSSQLdatasource" } );
		    """, context ) );
		assertThat( t.getMessage() ).contains( "Boom!" );
		assertThat( t.getCause() ).isNotNull();
		assertThat( t.getCause().getMessage() ).contains( "Boom!" );
	}

	@DisplayName( "It can raise multiple SQL errors" )
	@Test
	public void testSQLErrors() {
		Throwable t = assertThrows( Throwable.class, () -> instance.executeStatement(
		    """
		        result = queryExecute( "
		        	RAISERROR('Boom 1!',11,1);
		        	RAISERROR('Boom 2!',11,1);
		        	RAISERROR('Boom 3!',11,1);
		        ", {}, { "datasource" : "MSSQLdatasource" } );
		    """, context ) );
		assertThat( t.getMessage() ).contains( "Boom 3!" );
		assertThat( t.getCause() ).isNotNull();
		assertThat( t.getCause().getMessage() ).contains( "Boom 3!" );
		assertThat( t.getCause().getCause() ).isNotNull();
		assertThat( t.getCause().getCause().getMessage() ).contains( "Boom 2!" );
		assertThat( t.getCause().getCause().getCause() ).isNotNull();
		assertThat( t.getCause().getCause().getCause().getMessage() ).contains( "Boom 1!" );

	}

	@DisplayName( "It can raise multiple SQL errors" )
	@Test
	public void testSQLErrorsAfterSuccess() {
		Throwable t = assertThrows( Throwable.class, () -> instance.executeStatement(
		    """
		        result = queryExecute( "
		    	SELECT 'brad' as dev;
		        	RAISERROR('Boom 1!',11,1);
		        	RAISERROR('Boom 2!',11,1);
		        	RAISERROR('Boom 3!',11,1);
		        ", {}, { "datasource" : "MSSQLdatasource" } );
		    """, context ) );
		assertThat( t.getMessage() ).contains( "Boom 3!" );
		assertThat( t.getCause() ).isNotNull();
		assertThat( t.getCause().getMessage() ).contains( "Boom 3!" );
		assertThat( t.getCause().getCause() ).isNotNull();
		assertThat( t.getCause().getCause().getMessage() ).contains( "Boom 2!" );
		assertThat( t.getCause().getCause().getCause() ).isNotNull();
		assertThat( t.getCause().getCause().getCause().getMessage() ).contains( "Boom 1!" );
	}

	@DisplayName( "It can call stored proc" )
	@Test
	public void testCallStoredProc() {
		instance.executeSource(
		    """
		    <bx:storedproc procedure="testProcedure" datasource="MSSQLdatasource" result="variables.result" returncode="true">
		        <bx:procparam name="in1" value="123" type="in" sqltype="integer" />
		        <bx:procparam name="in2" value="hello" type="in" sqltype="nvarchar" />
		        <bx:procparam name="inout1" value="10" type="inout" sqltype="integer" variable="inout1" />
		        <bx:procparam name="out1" type="out" sqltype="nvarchar" variable="out1" />
		        <bx:procresult name="resultSet1" resultSet=1 />
		        <bx:procresult name="resultSet2" resultSet=2 />
		    </bx:storedproc>
		    """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.get( "inout1" ) ).isEqualTo( 223 );
		assertThat( variables.get( "out1" ) ).isEqualTo( "foo-123-hello" );

		assertThat( variables.get( "resultSet1" ) ).isInstanceOf( Query.class );
		Query rs1 = variables.getAsQuery( Key.of( "resultSet1" ) );
		assertThat( rs1.size() ).isEqualTo( 2 );
		assertThat( rs1.getRowAsStruct( 0 ).getAsString( Key.of( "col" ) ) ).isEqualTo( "foo" );
		assertThat( rs1.getRowAsStruct( 1 ).getAsString( Key.of( "col" ) ) ).isEqualTo( "bar" );

		assertThat( variables.get( "resultSet2" ) ).isInstanceOf( Query.class );
		Query rs2 = variables.getAsQuery( Key.of( "resultSet2" ) );
		assertThat( rs2.size() ).isEqualTo( 1 );
		assertThat( rs2.getRowAsStruct( 0 ).getAsString( Key.of( "myColumn" ) ) ).isEqualTo( "second" );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		IStruct resultStruct = variables.getAsStruct( result );

		assertThat( resultStruct.get( "returnCode" ) ).isEqualTo( 42 );
		assertThat( resultStruct.getAsNumber( Key.of( "executionTime" ) ).doubleValue() ).isGreaterThan( 0.0 );
	}

	@DisplayName( "It can call stored proc with cf_sql_ prefix" )
	@Test
	public void testCallStoredProcWithCfSqlPrefix() {
		instance.executeSource(
		    """
		    <cfstoredproc procedure="testProcedure" datasource="MSSQLdatasource" result="variables.result" returncode="true">
		    	<cfprocparam name="in1" value="123" type="in" cfsqltype="cf_sql_integer" />
		    	<cfprocparam name="in2" value="hello" type="in" cfsqltype="cf_sql_nvarchar" />
		    	<cfset theType = "cf_sql_integer" />
		    	<cfprocparam name="inout1" value="10" type="inout" cfsqltype="#theType#" variable="inout1" />
		    	<cfprocparam name="out1" type="out" cfsqltype="cf_sql_nvarchar" variable="out1" />
		    	<cfprocresult name="resultSet1" resultSet=1 />
		    	<cfprocresult name="resultSet2" resultSet=2 />
		    </cfstoredproc>
		         """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.get( "inout1" ) ).isEqualTo( 223 );
		assertThat( variables.get( "out1" ) ).isEqualTo( "foo-123-hello" );

		assertThat( variables.get( "resultSet1" ) ).isInstanceOf( Query.class );
		Query rs1 = variables.getAsQuery( Key.of( "resultSet1" ) );
		assertThat( rs1.size() ).isEqualTo( 2 );
		assertThat( rs1.getRowAsStruct( 0 ).getAsString( Key.of( "col" ) ) ).isEqualTo( "foo" );
		assertThat( rs1.getRowAsStruct( 1 ).getAsString( Key.of( "col" ) ) ).isEqualTo( "bar" );

		assertThat( variables.get( "resultSet2" ) ).isInstanceOf( Query.class );
		Query rs2 = variables.getAsQuery( Key.of( "resultSet2" ) );
		assertThat( rs2.size() ).isEqualTo( 1 );
		assertThat( rs2.getRowAsStruct( 0 ).getAsString( Key.of( "myColumn" ) ) ).isEqualTo( "second" );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		IStruct resultStruct = variables.getAsStruct( result );

		assertThat( resultStruct.get( "returnCode" ) ).isEqualTo( 42 );
		assertThat( resultStruct.getAsNumber( Key.of( "executionTime" ) ).doubleValue() ).isGreaterThan( 0.0 );
	}

	@DisplayName( "It can call stored proc with max rows" )
	@Test
	public void testCallStoredProcWithMaxRows() {
		instance.executeSource(
		    """
		    <bx:storedproc procedure="testProcedure" datasource="MSSQLdatasource" result="variables.result" returncode="true">
		        <bx:procparam name="in1" value="123" type="in" sqltype="integer" />
		        <bx:procparam name="in2" value="hello" type="in" sqltype="nvarchar" />
		        <bx:procparam name="inout1" value="10" type="inout" sqltype="integer" variable="inout1" />
		        <bx:procparam name="out1" type="out" sqltype="nvarchar" variable="out1" />
		        <bx:procresult name="resultSet1" resultSet=1 maxRows=1 />
		        <bx:procresult name="resultSet2" resultSet=2 maxRows=0 />
		    </bx:storedproc>
		    """,
		    context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.get( "resultSet1" ) ).isInstanceOf( Query.class );
		Query rs1 = variables.getAsQuery( Key.of( "resultSet1" ) );
		assertThat( rs1.size() ).isEqualTo( 1 );
		assertThat( rs1.getRowAsStruct( 0 ).getAsString( Key.of( "col" ) ) ).isEqualTo( "foo" );

		assertThat( variables.get( "resultSet2" ) ).isInstanceOf( Query.class );
		Query rs2 = variables.getAsQuery( Key.of( "resultSet2" ) );
		assertThat( rs2.size() ).isEqualTo( 0 );
	}

	@DisplayName( "It can call stored proc with default prefix name" )
	@Test
	public void testCallStoredProcWithDefaultPrefix() {
		instance.executeSource(
		    """
		    <bx:storedproc procedure="testProcedure" datasource="MSSQLdatasource" returncode="true">
		        <bx:procparam name="in1" value="123" type="in" sqltype="integer" />
		        <bx:procparam name="in2" value="hello" type="in" sqltype="nvarchar" />
		        <bx:procparam name="inout1" value="10" type="inout" sqltype="integer" variable="inout1" />
		        <bx:procparam name="out1" type="out" sqltype="nvarchar" variable="out1" />
		        <bx:procresult name="resultSet1" resultSet=1 />
		        <bx:procresult name="resultSet2" resultSet=2 />
		    </bx:storedproc>
		    """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.get( "inout1" ) ).isEqualTo( 223 );
		assertThat( variables.get( "out1" ) ).isEqualTo( "foo-123-hello" );

		assertThat( variables.get( "resultSet1" ) ).isInstanceOf( Query.class );
		Query rs1 = variables.getAsQuery( Key.of( "resultSet1" ) );
		assertThat( rs1.size() ).isEqualTo( 2 );
		assertThat( rs1.getRowAsStruct( 0 ).getAsString( Key.of( "col" ) ) ).isEqualTo( "foo" );
		assertThat( rs1.getRowAsStruct( 1 ).getAsString( Key.of( "col" ) ) ).isEqualTo( "bar" );

		assertThat( variables.get( "resultSet2" ) ).isInstanceOf( Query.class );
		Query rs2 = variables.getAsQuery( Key.of( "resultSet2" ) );
		assertThat( rs2.size() ).isEqualTo( 1 );
		assertThat( rs2.getRowAsStruct( 0 ).getAsString( Key.of( "myColumn" ) ) ).isEqualTo( "second" );

		assertThat( variables.get( Key.of( "bxstoredproc" ) ) ).isInstanceOf( IStruct.class );
		IStruct resultStruct = variables.getAsStruct( Key.of( "bxstoredproc" ) );

		assertThat( resultStruct.get( "returnCode" ) ).isEqualTo( 42 );
		assertThat( resultStruct.getAsNumber( Key.of( "executionTime" ) ).doubleValue() ).isGreaterThan( 0.0 );
	}

	@DisplayName( "It can call stored proc without types" )
	@Test
	public void testCallStoredProcNoTypes() {
		instance.executeSource(
		    """
		    <bx:storedproc procedure="testProcedure" datasource="MSSQLdatasource" result="variables.result" returncode="true">
		        <bx:procparam name="in1" value="123" type="in" />
		        <bx:procparam name="in2" value="hello" type="in" />
		        <bx:procparam name="inout1" value="10" type="inout" variable="inout1" />
		        <bx:procparam name="out1" type="out" variable="out1" />
		        <bx:procresult name="resultSet1" resultSet=1 />
		        <bx:procresult name="resultSet2" resultSet=2 />
		    </bx:storedproc>
		    """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.get( "inout1" ) ).isEqualTo( "223" );
		assertThat( variables.get( "out1" ) ).isEqualTo( "foo-123-hello" );

		assertThat( variables.get( "resultSet1" ) ).isInstanceOf( Query.class );
		Query rs1 = variables.getAsQuery( Key.of( "resultSet1" ) );
		assertThat( rs1.size() ).isEqualTo( 2 );
		assertThat( rs1.getRowAsStruct( 0 ).getAsString( Key.of( "col" ) ) ).isEqualTo( "foo" );
		assertThat( rs1.getRowAsStruct( 1 ).getAsString( Key.of( "col" ) ) ).isEqualTo( "bar" );

		assertThat( variables.get( "resultSet2" ) ).isInstanceOf( Query.class );
		Query rs2 = variables.getAsQuery( Key.of( "resultSet2" ) );
		assertThat( rs2.size() ).isEqualTo( 1 );
		assertThat( rs2.getRowAsStruct( 0 ).getAsString( Key.of( "myColumn" ) ) ).isEqualTo( "second" );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		IStruct resultStruct = variables.getAsStruct( result );

		assertThat( resultStruct.get( "returnCode" ) ).isEqualTo( 42 );
		assertThat( resultStruct.getAsNumber( Key.of( "executionTime" ) ).doubleValue() ).isGreaterThan( 0.0 );
	}

	@DisplayName( "It can call stored proc with dbvarname" )
	@Test
	public void testCallStoredProcWithDBVarName() {
		instance.executeSource(
		    """
		    <bx:storedproc procedure="testProcedure" datasource="MSSQLdatasource" result="variables.result" returncode="true">
		        <bx:procparam name="in1" value="123" type="in" sqltype="integer" dbvarname="@in1" />
		        <bx:procparam name="in2" value="hello" type="in" sqltype="nvarchar" dbvarname="@in2" />
		        <bx:procparam name="inout1" value="10" type="inout" sqltype="integer" variable="inout1" dbvarname="@inout1" />
		        <bx:procparam name="out1" type="out" sqltype="nvarchar" variable="out1" dbvarname="@out1" />
		        <bx:procresult name="resultSet1" resultSet=1 />
		        <bx:procresult name="resultSet2" resultSet=2 />
		    </bx:storedproc>
		    """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.get( "inout1" ) ).isEqualTo( 223 );
		assertThat( variables.get( "out1" ) ).isEqualTo( "foo-123-hello" );

		assertThat( variables.get( "resultSet1" ) ).isInstanceOf( Query.class );
		Query rs1 = variables.getAsQuery( Key.of( "resultSet1" ) );
		assertThat( rs1.size() ).isEqualTo( 2 );
		assertThat( rs1.getRowAsStruct( 0 ).getAsString( Key.of( "col" ) ) ).isEqualTo( "foo" );
		assertThat( rs1.getRowAsStruct( 1 ).getAsString( Key.of( "col" ) ) ).isEqualTo( "bar" );

		assertThat( variables.get( "resultSet2" ) ).isInstanceOf( Query.class );
		Query rs2 = variables.getAsQuery( Key.of( "resultSet2" ) );
		assertThat( rs2.size() ).isEqualTo( 1 );
		assertThat( rs2.getRowAsStruct( 0 ).getAsString( Key.of( "myColumn" ) ) ).isEqualTo( "second" );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		IStruct resultStruct = variables.getAsStruct( result );

		assertThat( resultStruct.get( "returnCode" ) ).isEqualTo( 42 );
		assertThat( resultStruct.getAsNumber( Key.of( "executionTime" ) ).doubleValue() ).isGreaterThan( 0.0 );
	}

	@DisplayName( "It can call stored proc with dbvarname skipping defaulted inputs" )
	@Test
	public void testCallStoredProcWithDBVarNameSkippingDefaults() {
		instance.executeSource(
		    """
		    <bx:storedproc procedure="testProcedure" datasource="MSSQLdatasource" result="variables.result" returncode="true">
		        <!---<bx:procparam name="in1" value="123" type="in" sqltype="integer" dbvarname="@in1" />--->
		        <bx:procparam name="in2" value="hello" type="in" sqltype="nvarchar" dbvarname="@in2" />
		        <bx:procparam name="inout1" value="10" type="inout" sqltype="integer" variable="inout1" dbvarname="@inout1" />
		        <bx:procparam name="out1" type="out" sqltype="nvarchar" variable="out1" dbvarname="@out1" />
		        <bx:procresult name="resultSet1" resultSet=1 />
		        <bx:procresult name="resultSet2" resultSet=2 />
		    </bx:storedproc>
		    """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.get( "inout1" ) ).isEqualTo( 145 );
		assertThat( variables.get( "out1" ) ).isEqualTo( "foo-45-hello" );

		assertThat( variables.get( "resultSet1" ) ).isInstanceOf( Query.class );
		Query rs1 = variables.getAsQuery( Key.of( "resultSet1" ) );
		assertThat( rs1.size() ).isEqualTo( 2 );
		assertThat( rs1.getRowAsStruct( 0 ).getAsString( Key.of( "col" ) ) ).isEqualTo( "foo" );
		assertThat( rs1.getRowAsStruct( 1 ).getAsString( Key.of( "col" ) ) ).isEqualTo( "bar" );

		assertThat( variables.get( "resultSet2" ) ).isInstanceOf( Query.class );
		Query rs2 = variables.getAsQuery( Key.of( "resultSet2" ) );
		assertThat( rs2.size() ).isEqualTo( 1 );
		assertThat( rs2.getRowAsStruct( 0 ).getAsString( Key.of( "myColumn" ) ) ).isEqualTo( "second" );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		IStruct resultStruct = variables.getAsStruct( result );

		assertThat( resultStruct.get( "returnCode" ) ).isEqualTo( 42 );
		assertThat( resultStruct.getAsNumber( Key.of( "executionTime" ) ).doubleValue() ).isGreaterThan( 0.0 );
	}

	@DisplayName( "It can call stored proc no return code" )
	@Test
	public void testCallStoredProcNoReturnCode() {
		instance.executeSource(
		    """
		    <bx:storedproc procedure="testProcedure" datasource="MSSQLdatasource" result="variables.result" returncode="false">
		        <bx:procparam name="in1" value="123" type="in" sqltype="integer" />
		        <bx:procparam name="in2" value="hello" type="in" sqltype="nvarchar" />
		        <bx:procparam name="inout1" value="10" type="inout" sqltype="integer" variable="inout1" />
		        <bx:procparam name="out1" type="out" sqltype="nvarchar" variable="out1" />
		        <bx:procresult name="resultSet1" resultSet=1 />
		        <bx:procresult name="resultSet2" resultSet=2 />
		    </bx:storedproc>
		    """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.get( "inout1" ) ).isEqualTo( 223 );
		assertThat( variables.get( "out1" ) ).isEqualTo( "foo-123-hello" );

		assertThat( variables.get( "resultSet1" ) ).isInstanceOf( Query.class );
		Query rs1 = variables.getAsQuery( Key.of( "resultSet1" ) );
		assertThat( rs1.size() ).isEqualTo( 2 );
		assertThat( rs1.getRowAsStruct( 0 ).getAsString( Key.of( "col" ) ) ).isEqualTo( "foo" );
		assertThat( rs1.getRowAsStruct( 1 ).getAsString( Key.of( "col" ) ) ).isEqualTo( "bar" );

		assertThat( variables.get( "resultSet2" ) ).isInstanceOf( Query.class );
		Query rs2 = variables.getAsQuery( Key.of( "resultSet2" ) );
		assertThat( rs2.size() ).isEqualTo( 1 );
		assertThat( rs2.getRowAsStruct( 0 ).getAsString( Key.of( "myColumn" ) ) ).isEqualTo( "second" );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		IStruct resultStruct = variables.getAsStruct( result );

		assertThat( resultStruct.get( "returnCode" ) ).isEqualTo( 0 );
		assertThat( resultStruct.getAsNumber( Key.of( "executionTime" ) ).doubleValue() ).isGreaterThan( 0.0 );
	}

	@DisplayName( "It can call stored proc no result sets" )
	@Test
	public void testCallStoredProcNoResultSets() {
		instance.executeSource(
		    """
		    <bx:storedproc procedure="testProcedure" datasource="MSSQLdatasource" result="variables.result" returncode="false">
		        <bx:procparam name="in1" value="123" type="in" sqltype="integer" />
		        <bx:procparam name="in2" value="hello" type="in" sqltype="nvarchar" />
		        <bx:procparam name="inout1" value="10" type="inout" sqltype="integer" variable="inout1" />
		        <bx:procparam name="out1" type="out" sqltype="nvarchar" variable="out1" />
		    </bx:storedproc>
		    """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.get( "inout1" ) ).isEqualTo( 223 );
		assertThat( variables.get( "out1" ) ).isEqualTo( "foo-123-hello" );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		IStruct resultStruct = variables.getAsStruct( result );

		assertThat( resultStruct.get( "returnCode" ) ).isEqualTo( 0 );
		assertThat( resultStruct.getAsNumber( Key.of( "executionTime" ) ).doubleValue() ).isGreaterThan( 0.0 );
	}

	@DisplayName( "It can call stored proc skipping result set" )
	@Test
	public void testCallStoredProcSkippingResult() {
		instance.executeSource(
		    """
		    <bx:storedproc procedure="testProcedure" datasource="MSSQLdatasource" result="variables.result" returncode="false">
		        <bx:procparam name="in1" value="123" type="in" sqltype="integer" />
		        <bx:procparam name="in2" value="hello" type="in" sqltype="nvarchar" />
		        <bx:procparam name="inout1" value="10" type="inout" sqltype="integer" variable="inout1" />
		        <bx:procparam name="out1" type="out" sqltype="nvarchar" variable="out1" />
		        <bx:procresult name="resultSet2" resultSet=2 />
		    </bx:storedproc>
		    """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.get( "inout1" ) ).isEqualTo( 223 );
		assertThat( variables.get( "out1" ) ).isEqualTo( "foo-123-hello" );

		assertThat( variables.get( "resultSet1" ) ).isNull();

		assertThat( variables.get( "resultSet2" ) ).isInstanceOf( Query.class );
		Query rs2 = variables.getAsQuery( Key.of( "resultSet2" ) );
		assertThat( rs2.size() ).isEqualTo( 1 );
		assertThat( rs2.getRowAsStruct( 0 ).getAsString( Key.of( "myColumn" ) ) ).isEqualTo( "second" );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		IStruct resultStruct = variables.getAsStruct( result );

		assertThat( resultStruct.get( "returnCode" ) ).isEqualTo( 0 );
		assertThat( resultStruct.getAsNumber( Key.of( "executionTime" ) ).doubleValue() ).isGreaterThan( 0.0 );
	}

	@DisplayName( "It can call stored proc with positional results" )
	@Test
	public void testCallStoredProcPositionalResults() {
		instance.executeSource(
		    """
		    <bx:storedproc procedure="testProcedure" datasource="MSSQLdatasource" result="variables.result" returncode="true">
		        <bx:procparam name="in1" value="123" type="in" sqltype="integer" />
		        <bx:procparam name="in2" value="hello" type="in" sqltype="nvarchar" />
		        <bx:procparam name="inout1" value="10" type="inout" sqltype="integer" variable="inout1" />
		        <bx:procparam name="out1" type="out" sqltype="nvarchar" variable="out1" />
		        <bx:procresult name="resultSet1" />
		        <bx:procresult name="resultSet2" />
		    </bx:storedproc>
		    """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.get( "inout1" ) ).isEqualTo( 223 );
		assertThat( variables.get( "out1" ) ).isEqualTo( "foo-123-hello" );

		assertThat( variables.get( "resultSet1" ) ).isInstanceOf( Query.class );
		Query rs1 = variables.getAsQuery( Key.of( "resultSet1" ) );
		assertThat( rs1.size() ).isEqualTo( 2 );
		assertThat( rs1.getRowAsStruct( 0 ).getAsString( Key.of( "col" ) ) ).isEqualTo( "foo" );
		assertThat( rs1.getRowAsStruct( 1 ).getAsString( Key.of( "col" ) ) ).isEqualTo( "bar" );

		assertThat( variables.get( "resultSet2" ) ).isInstanceOf( Query.class );
		Query rs2 = variables.getAsQuery( Key.of( "resultSet2" ) );
		assertThat( rs2.size() ).isEqualTo( 1 );
		assertThat( rs2.getRowAsStruct( 0 ).getAsString( Key.of( "myColumn" ) ) ).isEqualTo( "second" );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		IStruct resultStruct = variables.getAsStruct( result );

		assertThat( resultStruct.get( "returnCode" ) ).isEqualTo( 42 );
		assertThat( resultStruct.getAsNumber( Key.of( "executionTime" ) ).doubleValue() ).isGreaterThan( 0.0 );
	}

	@DisplayName( "It can't call stored proc mixing positional and indexed proc results" )
	@Test
	public void testCallStoredProcErrorOnMixedResults() {
		Throwable t = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    <bx:storedproc procedure="testProcedure" datasource="MSSQLdatasource">
		        <bx:procparam name="in1" value="123" type="in" sqltype="integer" />
		        <bx:procparam name="in2" value="hello" type="in" sqltype="nvarchar" />
		        <bx:procparam name="inout1" value="10" type="inout" sqltype="integer" variable="inout1" />
		        <bx:procparam name="out1" type="out" sqltype="nvarchar" variable="out1" />
		        <bx:procresult name="resultSet1" resultSet=1 />
		        <bx:procresult name="resultSet2" />
		    </bx:storedproc>
		    """,
		    context, BoxSourceType.BOXTEMPLATE ) );
		assertThat( t.getMessage() ).contains( "Cannot mix" );
	}

	@DisplayName( "It can call stored proc with selects after insert" )
	@Test
	public void testStoredProcSelectAfterInsert() {
		instance.executeSource(
		    """
		       <bx:query datasource="MSSQLdatasource">
		    	create or alter procedure [dbo].[_testQ] as
		    	begin
		    		-- Statement 1 (Declare, no results)
		    		DECLARE @TestTable TABLE (
		    			id INT idENTITY(1,1) PRIMARY KEY,
		    			TestName VARCHAR(100),
		    			dateAdded DATETIME DEFAULT GETDATE()
		    		);
		    		-- Statement 2 (insert, no results, generatedKey when inserting into actual table and not variable)
		    		INSERT INTO @TestTable (TestName)
		    		VALUES
		    			('Test ' + CAST(FORMAT(GETDATE(), 'yyyyMMddHHmmss') AS VARCHAR)),
		    			('Test ' + CAST(FORMAT(DATEADD(SECOND, 1, GETDATE()), 'yyyyMMddHHmmss') AS VARCHAR)),
		    			('Test ' + CAST(FORMAT(DATEADD(SECOND, 2, GETDATE()), 'yyyyMMddHHmmss') AS VARCHAR));
		    		-- Statement 3 (select, no results)
		    		SELECT id, TestName, dateAdded FROM @TestTable WHERE id = 0;
		    		-- Statement 4 (select, 1 result)
		    		SELECT id, TestName, dateAdded FROM @TestTable WHERE id = 1;
		    		-- Statement 5 (select, all results)
		    		SELECT id, TestName, dateAdded FROM @TestTable;
		    	end
		    </bx:query>
		       """,
		    context, BoxSourceType.BOXTEMPLATE );

		instance.executeSource(
		    """
		    <bx:storedproc procedure="_testQ" datasource="MSSQLdatasource">
		       	<bx:procresult name="rs1" resultset="1" />
		       	<bx:procresult name="rs2" resultset="2" />
		       	<bx:procresult name="rs3" resultset="3" />
		       	<bx:procresult name="rs4" resultset="4" />
		       	<bx:procresult name="rs5" resultset="5" />
		    </bx:storedproc>
		         """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.get( "rs1" ) ).isInstanceOf( Query.class );
		Query rs1 = variables.getAsQuery( Key.of( "rs1" ) );
		assertThat( rs1.size() ).isEqualTo( 0 );

		assertThat( variables.get( "rs2" ) ).isInstanceOf( Query.class );
		Query rs2 = variables.getAsQuery( Key.of( "rs2" ) );
		assertThat( rs2.size() ).isEqualTo( 1 );
		assertThat( rs2.getRowAsStruct( 0 ).getAsInteger( Key.of( "id" ) ) ).isEqualTo( 1 );

		assertThat( variables.get( "rs3" ) ).isInstanceOf( Query.class );
		Query rs3 = variables.getAsQuery( Key.of( "rs3" ) );
		assertThat( rs3.size() ).isEqualTo( 3 );
		assertThat( rs3.getRowAsStruct( 0 ).getAsInteger( Key.of( "id" ) ) ).isEqualTo( 1 );
		assertThat( rs3.getRowAsStruct( 1 ).getAsInteger( Key.of( "id" ) ) ).isEqualTo( 2 );
		assertThat( rs3.getRowAsStruct( 2 ).getAsInteger( Key.of( "id" ) ) ).isEqualTo( 3 );

		assertThat( variables.get( "rs4" ) ).isNull();
		assertThat( variables.get( "rs5" ) ).isNull();

	}

	@Test
	public void testStoredProcOutParamMixup() {
		instance.executeSource(
		    """
		        <bx:query datasource="MSSQLdatasource">
		     	create or ALTER PROCEDURE [dbo].[_testQtestStoredProcOutParamMixup]
		    (
		    	@prm_drug_exclusion_quote_id smallint = NULL,
		    	@prm_return_success_flag bit OUTPUT,
		    	@prm_return_status_code int OUTPUT,
		    	@prm_return_status_message varchar(max) OUTPUT,
		    	@prm_last_updated_user_id varchar(50) = NULL,
		    	@prm_ip_address varchar(45) = NULL
		    ) AS
		     	begin
		     		SET NOCOUNT ON;
		     		SET @prm_return_success_flag = 1;
		     		SET @prm_return_status_code = 200;
		     		SET @prm_return_status_message = 'OK';

		    SELECT 1 as test;
		     	end
		     </bx:query>
		        """,
		    context, BoxSourceType.BOXTEMPLATE );

		instance.executeSource(
		    """
		       <bx:storedproc procedure="_testQtestStoredProcOutParamMixup" datasource="MSSQLdatasource" debug=false >
		       	<!---pagination/sort params--->
		       	<bx:procparam sqltype="integer" dbVarName="@prm_drug_exclusion_quote_id"        value="123" null="false">,
		       	<!--- audit --->
		       	<bx:procparam sqltype="varchar" dbVarName="@prm_ip_address"            value="127.0.0.1"           maxlength="45" type="in">
		       	<bx:procparam sqltype="varchar" dbVarName="@prm_last_updated_user_id"  value="abcd" maxlength="50" type="in">
		       	<!--- output --->
		       	<bx:procparam sqltype="bit"    dbVarName="@prm_return_success_flag"    variable="bSqlFlag"     type="out">
		       	<bx:procparam sqltype="integer" dbVarName="@prm_return_status_code"        variable="iSqlCode"     type="out">
		       	<bx:procparam sqltype="varchar" dbVarName="@prm_return_status_message"     variable="sSqlMessage"  type="out">
		       	<!--- output --->
		       	<bx:procresult name="qryReturn">
		       </bx:storedproc>
		    <bx:script>
		    	/* println( bSqlFlag );
		    	println( iSqlCode );
		    	println( sSqlMessage );
		    	println( qryReturn ); */
		    </bx:script>
		               """,
		    context, BoxSourceType.BOXTEMPLATE );
	}

	@Test
	public void testStoredProcTimestampIn() {
		instance.executeSource(
		    """
		        <bx:query datasource="MSSQLdatasource">
		     	create or ALTER PROCEDURE [dbo].[_testTestStoredProcTimestampIn]
		    (
		    	@today datetime
		    ) AS
		     	begin
		    		SELECT @today as today;
		     	end
		     </bx:query>
		        """,
		    context, BoxSourceType.BOXTEMPLATE );

		instance.executeSource(
		    """
		    <bx:set theDate = now() />
		          <bx:storedproc procedure="_testTestStoredProcTimestampIn" datasource="MSSQLdatasource" debug=false >
		          	<bx:procparam sqltype="timestamp" value="#theDate#" type="in">
		          	<bx:procresult name="qryReturn">
		          </bx:storedproc>
		       <bx:script>
		    		result = (datetimeformat( theDate, "yyyy-MM-dd HH:mm:ss" ) == datetimeformat( qryReturn.today, "yyyy-MM-dd HH:mm:ss" ));
		       </bx:script>
		                  """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.get( "qryReturn" ) ).isInstanceOf( Query.class );
		Query rs = variables.getAsQuery( Key.of( "qryReturn" ) );
		assertThat( rs.size() ).isEqualTo( 1 );
		assertThat( rs.getRowAsStruct( 0 ).get( Key.of( "today" ) ) ).isNotNull();
		assertThat( variables.getAsBoolean( Key.of( "result" ) ) ).isTrue();
	}

	@DisplayName( "It ignores non-integer values when null=true and passes NULL to stored proc" )
	@Test
	public void testStoredProcNullTrueIgnoresInvalidValue() {
		// Create a stored procedure that expects an integer parameter
		instance.executeSource(
		    """
		        <bx:query datasource="MSSQLdatasource">
		     	CREATE OR ALTER PROCEDURE [dbo].[_testNullTrueParam]
		    (
		    	@intParam INT = NULL
		    ) AS
		     	BEGIN
		    		SELECT @intParam as receivedValue,
		    		       CASE WHEN @intParam IS NULL THEN 1 ELSE 0 END as isNull;
		     	END
		     </bx:query>
		        """,
		    context, BoxSourceType.BOXTEMPLATE );

		// Call the stored procedure with null=true and pass a non-integer value
		// The value should be ignored and NULL should be passed to the stored procedure
		instance.executeSource(
		    """
		    <bx:storedproc procedure="_testNullTrueParam" datasource="MSSQLdatasource" debug=false >
		    	<bx:procparam sqltype="integer" value="not_an_integer" type="in" null="true">
		    	<bx:procresult name="qryReturn">
		    </bx:storedproc>
		            """,
		    context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.get( "qryReturn" ) ).isInstanceOf( Query.class );
		Query rs = variables.getAsQuery( Key.of( "qryReturn" ) );
		assertThat( rs.size() ).isEqualTo( 1 );

		IStruct row = rs.getRowAsStruct( 0 );

		// Verify that the received value is NULL (not the invalid string we passed)
		assertThat( row.get( Key.of( "receivedValue" ) ) ).isNull();

		// Verify that the isNull flag is 1 (true)
		assertThat( row.getAsInteger( Key.of( "isNull" ) ) ).isEqualTo( 1 );
	}

}
