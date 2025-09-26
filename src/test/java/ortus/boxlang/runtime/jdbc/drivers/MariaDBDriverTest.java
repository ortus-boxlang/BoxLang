package ortus.boxlang.runtime.jdbc.drivers;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.compiler.parser.BoxSourceType;
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
		IStruct dsConfig = Struct.of(
		    "username", "root",
		    "password", "123456Password",
		    "connectionString", "jdbc:mariadb://localhost:3360",
		    "database", "myDB",
		    "custom", "allowMultiQueries=true&returnMultiValuesGeneratedIds=true"
		);
		mariaDBDatasource = AbstractDriverTest.setupTestDatasource( instance, setUpContext, datasourceName, dsConfig );
		MariaDBDriverTest.createGeneratedKeyTable( mariaDBDatasource, setUpContext );
		return mariaDBDatasource;
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
	 * 
	 * @param ds      Datasource object
	 * @param context Box context
	 */
	public static void createGeneratedKeyTable( DataSource ds, IBoxContext context ) {
		try {
			mariaDBDatasource.execute( "CREATE TABLE generatedKeyTest( id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(155))", context );
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

	@DisplayName( "It supports timestamp param types" )
	@Test
	public void testTimestampDateParam() {
		instance.executeSource(
		    """
		       queryExecute(
		    	"
		    	INSERT INTO developers ( id, name, role, createdAt )
		    	VALUES ( 100, 'Tony Skipponi', 'Engineer', :timestamp )",
		    	{
		    		timestamp : { sqltype : "cf_sql_timestamp", value : now() }
		    	},
		    	{ "datasource" : "MariaDBdatasource" }
		    );
		    result = queryExecute( "SELECT * FROM developers WHERE id = 100", [], { "datasource" : "MariaDBdatasource" } );
		       """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );
	}

	@DisplayName( "It can use string values as timestamp params." )
	@Test
	public void testTimestampParamCompare() {
		instance.executeSource(
		    """
		      queryExecute( "INSERT INTO developers ( id, name, role, createdAt )
		      	VALUES ( 101, 'Tony Skipponi', 'Engineer', NOW() )", {}, { "datasource" : "MariaDBdatasource" } );
		      result = queryExecute(
		      	"
		      	SELECT * FROM developers
		    WHERE createdAt IS NOT NULL AND createdAt < :timestamp",
		      	{
		      		timestamp : { sqltype : "cf_sql_timestamp", value : "09/24/2099" }
		      	},
		      	{ "datasource" : "MariaDBdatasource" }
		      );
		      """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 5, query.size() );
	}

	@DisplayName( "It can execute multiple statements in a single queryExecute() call" )
	@Test
	public void testMultipleStatements() {
		assertDoesNotThrow( () -> instance.executeStatement(
		    """
		           result = queryExecute( '
		     	   TRUNCATE TABLE developers;
		               INSERT INTO developers (id) VALUES (111);
		               INSERT INTO developers (id) VALUES (222);
		               SELECT * FROM developers;
		               INSERT INTO developers (id) VALUES (333);
		               INSERT INTO developers (id) VALUES (444);
		               ',
		      [],
		      { "datasource" : "MariaDBdatasource" }
		           );
		    """, context )
		);
		Object multiStatementQueryReturn = variables.get( Key.of( "result" ) );
		assertThat( multiStatementQueryReturn ).isInstanceOf( Query.class );
		assertEquals( 2, ( ( Query ) multiStatementQueryReturn ).size(), "For compatibility, the last result should be returned" );

		Query newTableRows = ( Query ) instance
		    .executeStatement( "queryExecute( 'SELECT * FROM developers WHERE id IN (111,222)', [],{ 'datasource' : 'MariaDBdatasource' } );", context );
		assertEquals( 2, newTableRows.size() );

	}

	@DisplayName( "Gets empty tables query when unmatched database name is provided" )
	@Test
	public void testTablesTypeBadDBName() {
		getInstance().executeSource(
		    """
		        cfdbinfo( type='tables', name='result', datasource="MariaDBdatasource", dbname="foo" )
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );
		Query resultQuery = ( Query ) getVariables().get( result );
		assertThat( resultQuery.size() ).isEqualTo( 0 );
	}

	@DisplayName( "Can get catalog and schema names" )
	@Test
	public void testDBNamesType() {
		getInstance().executeSource(
		    """
		        cfdbinfo( type='dbnames', name='result', datasource='MariaDBdatasource' )
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );
		Object theResult = getVariables().get( result );
		assertThat( theResult ).isInstanceOf( Query.class );

		Query dbNamesQuery = ( Query ) theResult;
		assertThat( dbNamesQuery.size() ).isGreaterThan( 0 );
		assertEquals( 2, dbNamesQuery.getColumns().size() );

		IStruct ourDBRow = dbNamesQuery.stream()
		    .filter( row -> row.getAsString( Key.of( "DBNAME" ) ).equals( "myDB" ) )
		    .findFirst()
		    .orElse( null );

		assertNotNull( ourDBRow );
		assertEquals( "CATALOG", ourDBRow.getAsString( Key.type ) );
		assertEquals( "myDB", ourDBRow.getAsString( Key.of( "DBNAME" ) ) );
	}

	@DisplayName( "It can raise SQL error" )
	@Test
	public void testSQLError() {
		Throwable t = assertThrows( Throwable.class, () -> instance.executeStatement(
		    """
		        queryExecute( "
		    		SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Boom!';
		        ", {}, { "datasource" : "MariaDBdatasource" } );
		    """, context ) );
		assertThat( t.getMessage() ).contains( "Boom!" );
		assertThat( t.getCause() ).isNotNull();
		assertThat( t.getCause().getMessage() ).contains( "Boom!" );
	}

	@DisplayName( "It can raise SQL error after success" )
	@Test
	public void testSQLErrorAfterSuccess() {
		Throwable t = assertThrows( Throwable.class, () -> instance.executeStatement(
		    """
		          queryExecute( "
		    SELECT 'brad' as dev;
		      		SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Boom!';
		          ", {}, { "datasource" : "MariaDBdatasource" } );
		      """, context ) );
		assertThat( t.getMessage() ).contains( "Boom!" );
		assertThat( t.getCause() ).isNotNull();
		assertThat( t.getCause().getMessage() ).contains( "Boom!" );
	}

	@DisplayName( "It can read from a table with a vector column" )
	@Test
	public void testVectorColumnRead() {
		// Create vector table if it doesn't exist and insert test data
		instance.executeSource(
		    """
		    queryExecute( "DROP TABLE IF EXISTS vectorTable", {}, { "datasource" : "MariaDBdatasource" } );

		    queryExecute(
		        "CREATE TABLE vectorTable (
		            id INT AUTO_INCREMENT PRIMARY KEY,
		            name VARCHAR(100),
		            embedding VECTOR(3)
		        )",
		        {},
		        { "datasource" : "MariaDBdatasource" }
		    );

		    queryExecute(
		        "INSERT INTO vectorTable (name, embedding) VALUES
		            ('sample1', VEC_FromText('[1.0, 2.0, 3.0]')),
		            ('sample2', VEC_FromText('[4.0, 5.0, 6.0]'))",
		        {},
		        { "datasource" : "MariaDBdatasource" }
		    );

		    result = queryExecute( "SELECT name, embedding, VEC_ToText( embedding ) as embedding_JSON FROM vectorTable", {}, { "datasource" : "MariaDBdatasource" } );
		    """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 2, query.size() );

		// Verify the data contains our test records
		assertThat( query.getCell( Key.of( "name" ), 0 ) ).isEqualTo( "sample1" );
		assertThat( query.getCell( Key.of( "name" ), 1 ) ).isEqualTo( "sample2" );

		// Verify vector column exists
		assertThat( query.getColumnNames() ).contains( "embedding" );

		// Check vector data is returned as byte arrays
		Object	vectorData1	= query.getCell( Key.of( "embedding" ), 0 );
		Object	vectorData2	= query.getCell( Key.of( "embedding" ), 1 );

		assertThat( vectorData1 ).isInstanceOf( byte[].class );
		assertThat( vectorData2 ).isInstanceOf( byte[].class );

		// Convert byte arrays to readable format (you'll need to implement this based on MariaDB's binary format)
		byte[]	vector1Bytes	= ( byte[] ) vectorData1;
		byte[]	vector2Bytes	= ( byte[] ) vectorData2;

		// For now, just verify we got some data
		assertThat( vector1Bytes.length ).isGreaterThan( 0 );
		assertThat( vector2Bytes.length ).isGreaterThan( 0 );

		// Decode the byte arrays into float arrays
		float[]	vector1	= decodeMariaDBVector( vector1Bytes );
		float[]	vector2	= decodeMariaDBVector( vector2Bytes );

		assertThat( vector1 ).isEqualTo( new float[] { 1.0f, 2.0f, 3.0f } );
		assertThat( vector2 ).isEqualTo( new float[] { 4.0f, 5.0f, 6.0f } );

		// Verify JSON representation matches expected
		Object	jsonData1	= query.getCell( Key.of( "embedding_JSON" ), 0 );
		Object	jsonData2	= query.getCell( Key.of( "embedding_JSON" ), 1 );

		assertThat( jsonData1 ).isInstanceOf( String.class );
		assertThat( jsonData2 ).isInstanceOf( String.class );
		assertThat( jsonData1 ).isEqualTo( "[1,2,3]" );
		assertThat( jsonData2 ).isEqualTo( "[4,5,6]" );
	}

	private static float[] decodeMariaDBVector( byte[] vectorBytes ) {
		if ( vectorBytes == null ) {
			return new float[ 0 ];
		}

		if ( vectorBytes.length % 4 != 0 ) {
			throw new IllegalArgumentException(
			    String.format( "Vector bytes length (%d) must be divisible by 4", vectorBytes.length )
			);
		}

		int			numFloats	= vectorBytes.length / 4;
		float[]		values		= new float[ numFloats ];

		ByteBuffer	buffer		= ByteBuffer.wrap( vectorBytes );
		buffer.order( ByteOrder.LITTLE_ENDIAN );

		for ( int i = 0; i < numFloats; i++ ) {
			values[ i ] = buffer.getFloat();
		}

		return values;
	}
}
