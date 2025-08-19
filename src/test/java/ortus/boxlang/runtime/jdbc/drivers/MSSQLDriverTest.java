package ortus.boxlang.runtime.jdbc.drivers;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;

@EnabledIf( "tools.JDBCTestUtils#hasMSSQLModule" )
public class MSSQLDriverTest extends AbstractDriverTest {

	/**
	 * Override to provide driver-specific datasource name
	 */
	@Override
	String getDatasourceName() {
		return "MSSQLdatasource";
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
	@Disabled( "Disabled until BL-1186 is resolved" )
	@Test
	public void testRowCount() {
		// @formatter:off
		instance.executeStatement(
		    String.format( """
				result = queryExecute( "
					update developers set name = 'Michael Borne' where name = 'Michael Born';
					select @@rowcount c;
				", {}, { "datasource" : "%s"} );
			""", getDatasourceName() ),
		    context );
		// @formatter:on
		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@Disabled( "Couldn't get a working datetime column in the CREATE TABLE statement in JDBCTestUtils." )
	@DisplayName( "It can pass date object params without specifying a sql type" )
	@Test
	public void testDateParamNoSqlType() {
		instance.executeSource(
		    """
		    result = queryExecute(
		    	"SELECT id from developers WHERE myDate <= :created",
		    	{ "created" : now() },
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
}
