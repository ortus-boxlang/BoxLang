package ortus.boxlang.runtime.jdbc.drivers;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

@EnabledIf( "tools.JDBCTestUtils#hasMariaDBModule" )
public class MariaDBDriverTest extends AbstractDriverTest {

	protected static Key datasourceName = Key.of( "MariaDBdatasource" );

	public static DataSource setupTestDatasource( BoxRuntime instance, IBoxContext setUpContext ) {
		IStruct		dsConfig		= Struct.of(
		    "username", "root",
		    "password", "123456Password",
		    "connectionString", "jdbc:mariadb://localhost:3360"
		);
		DataSource	theDatasource	= AbstractDriverTest.setupTestDatasource( instance, setUpContext, datasourceName, dsConfig );
		MariaDBDriverTest.createGeneratedKeyTable( theDatasource, setUpContext );
		return theDatasource;
	}

	/**
	 * Override to provide driver-specific datasource name
	 */
	@Override
	String getDatasourceName() {
		return "MariaDBdatasource";
	}

	/**
	 * Create a table that uses generated keys so we can test our generated key retrieval in BL.
	 * Create a table that uses generated keys so we can test our generated key retrieval in BL.
	 * 
	 * @param ds      Datasource object
	 * @param context Box context
	 */
	public static void createGeneratedKeyTable( DataSource ds, IBoxContext context ) {
		try {
			mysqlDatasource.execute( "CREATE TABLE generatedKeyTest( id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(155))", context );
		} catch ( DatabaseException ignored ) {
		}
	}

	@DisplayName( "It can get a MariaDB JDBC connection" )
	@Test
	void testMariaDBConnection() throws SQLException {
		DataSource myDataSource = DataSource.fromStruct(
		    Key.of( "mariadb" ),
		    Struct.of(
		        "username", "root",
		        "password", "123456Password",
		        "connectionString", "jdbc:mariadb://localhost:3360"
		    ) );
		try ( Connection conn = myDataSource.getConnection() ) {
			assertThat( conn ).isInstanceOf( Connection.class );
		}
		myDataSource.shutdown();
	}

	@DisplayName( "It can read from a table with a vector column" )
	@Test
	public void testVectorColumnRead() {
		instance.executeSource(
		    """
		    result = queryExecute( "SELECT * FROM vectorTable", {}, { "datasource" : "MariaDBdatasource" } );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );
	}

}
