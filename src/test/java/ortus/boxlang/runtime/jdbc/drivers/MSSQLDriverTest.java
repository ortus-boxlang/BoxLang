package ortus.boxlang.runtime.jdbc.drivers;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

@EnabledIf( "tools.JDBCTestUtils#hasMSSQLModule" )
public class MSSQLDriverTest extends AbstractDriverTest {

	protected static Key datasourceName = Key.of( "MSSQLdatasource" );

	public static DataSource setupTestDatasource( BoxRuntime instance, IBoxContext setUpContext ) {
		IStruct		dsConfig		= Struct.of(
		    "username", "sa",
		    "password", "123456Password",
		    "host", "localhost",
		    "port", "1433",
		    "driver", "mssql",
		    "database", "master"
		);
		DataSource	theDatasource	= AbstractDriverTest.setupTestDatasource( instance, setUpContext, datasourceName, dsConfig );
		MSSQLDriverTest.createGeneratedKeyTable( theDatasource, setUpContext );
		return theDatasource;
	}

	public static void createGeneratedKeyTable( DataSource dataSource, IBoxContext context ) {
		try {
			dataSource.execute( "CREATE TABLE generatedKeyTest( id INT IDENTITY(1,1) PRIMARY KEY, name VARCHAR(155))", context );
		} catch ( DatabaseException ignored ) {
			// foo
		}
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

}
