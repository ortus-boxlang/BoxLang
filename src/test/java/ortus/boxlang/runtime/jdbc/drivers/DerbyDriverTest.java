package ortus.boxlang.runtime.jdbc.drivers;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import tools.JDBCTestUtils;

@EnabledIf( "tools.JDBCTestUtils#hasDerbyModule" )
public class DerbyDriverTest extends AbstractDriverTest {

	/**
	 * Override to provide driver-specific datasource name
	 */
	@Override
	String getDatasourceName() {
		return datasource.getOriginalName();
	}

	@Override
	@DisplayName( "It sets generatedKey in query meta" )
	@Test
	public void testGeneratedKey() {
		// broken in Apache Derby due to `getUpdateCount()` returning -1.
		// https://issues.apache.org/jira/browse/DERBY-211
	}

	@AfterEach
	public void cleanupBlobTests() {
		// Clean up any BLOB test tables
		JDBCTestUtils.dropTestTable( datasource, context, "blob_test", true );
		JDBCTestUtils.dropTestTable( datasource, context, "large_blob", true );
	}

	@DisplayName( "It can insert and retrieve BLOB data from string" )
	@Test
	public void testBlobInsertAndRetrieveFromString() {
		// Create table with BLOB column
		instance.executeStatement(
		    """
		        result = queryExecute( "CREATE TABLE blob_test ( id INTEGER PRIMARY KEY, data BLOB )" );
		    """, context );

		// Insert string data as BLOB
		instance.executeStatement(
		    """
		        insert = queryExecute(
		        	"INSERT INTO blob_test ( id, data ) VALUES ( 1, :blobData )",
		        	{ blobData: { value: "Hello, BLOB World!", sqltype: "blob" } }
		        );
		    """, context );

		// Retrieve the data
		instance.executeStatement(
		    """
		        result = queryExecute( "SELECT * FROM blob_test WHERE id = 1" );
		    """, context );

		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );
		IStruct	firstRow	= query.getRowAsStruct( 0 );
		Object	data		= firstRow.get( Key.of( "data" ) );
		assertNotNull( data );
		// BLOB data may be returned as byte[] or other types depending on driver
		if ( data instanceof byte[] ) {
			String retrieved = new String( ( byte[] ) data, java.nio.charset.StandardCharsets.UTF_8 );
			assertEquals( "Hello, BLOB World!", retrieved );
		}
	}

	@DisplayName( "It can handle large blob and clob columns" )
	@Test
	public void testLargeBlobAndClobColumns() {
		instance.executeStatement(
		    """
		    queryExecute( "
		    	CREATE TABLE large_lob (
		    		id INT PRIMARY KEY,
		    		blob_data BLOB,
		    		clob_data CLOB
		    	)
		    ",{}
		    );

		    queryExecute( "DELETE FROM large_lob", {} );
		    """, context );
		// @formatter:off
		instance.executeStatement(
			"""
				newLobData = "";
				// ~300 chars
				quote = "
		               	Farewell, Aragorn! Go to Minas Tirith and save my people! I
		               	have failed.
		               	No! said Aragorn, taking his hand and kissing his brow. You
		               	have conquered. Few have gained such a victory. Be at peace! Minas
		               	Tirith shall not fall!
		        ";
				// times 1000 = ~300,000 chars
				for( i=1; i <= 1000; i = i + 1 ) {
					newLobData = newLobData & quote;
				}
				
				insert = queryExecute( "INSERT INTO large_lob ( id, blob_data, clob_data ) VALUES ( 1, ?, ? )", [ 
					{ value: newLobData, sqltype: "blob" },
					{ value: newLobData, sqltype: "clob" }
				] );
			""", context );
		instance.executeStatement(
			"""
				result = queryExecute( "SELECT * FROM large_lob WHERE id = 1" );
			""", context );
		// @formatter:on
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );
		IStruct	firstRow	= query.getRowAsStruct( 0 );

		// Test BLOB data
		Object	blobData	= firstRow.get( Key.of( "BLOB_DATA" ) );
		assertThat( blobData ).isNotNull();
		// BLOB data may be returned as byte[] or other types depending on driver
		if ( blobData instanceof byte[] ) {
			String retrieved = new String( ( byte[] ) blobData, java.nio.charset.StandardCharsets.UTF_8 );
			assertThat( retrieved.length() ).isAtLeast( 300000 );
		}

		// Test CLOB data
		Object clobData = firstRow.get( Key.of( "CLOB_DATA" ) );
		assertThat( clobData ).isNotNull();
		assertThat( clobData ).isInstanceOf( String.class );
		String clobString = ( String ) clobData;
		assertThat( clobString.length() ).isAtLeast( 300000 );
		// Use containsMatch to handle whitespace variations
		assertThat( clobString ).containsMatch( "Farewell,\\s+Aragorn!" );
		assertThat( clobString ).containsMatch( "Minas\\s+Tirith\\s+shall\\s+not\\s+fall!" );
	}

	@DisplayName( "It can insert null BLOB values" )
	@Test
	public void testNullBlobValue() {
		instance.executeStatement(
		    """
		        result = queryExecute( "CREATE TABLE blob_test ( id INTEGER PRIMARY KEY, data BLOB )" );
		    """, context );

		// Insert null BLOB
		instance.executeStatement(
		    """
		        insert = queryExecute(
		        	"INSERT INTO blob_test ( id, data ) VALUES ( 1, :blobData )",
		        	{ blobData: { value: null, sqltype: "blob", nulls: true } }
		        );
		    """, context );

		// Retrieve the data
		instance.executeStatement(
		    """
		        result = queryExecute( "SELECT * FROM blob_test WHERE id = 1" );
		    """, context );

		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );
		IStruct	firstRow	= query.getRowAsStruct( 0 );
		Object	data		= firstRow.get( Key.of( "data" ) );
		// Null BLOB should be null
		assertThat( data ).isNull();
	}

	@DisplayName( "It can insert byte array as BLOB" )
	@Test
	public void testBlobInsertFromByteArray() {
		instance.executeStatement(
		    """
		        result = queryExecute( "CREATE TABLE blob_test ( id INTEGER PRIMARY KEY, data BLOB )" );
		    """, context );

		// Insert byte array directly
		instance.executeStatement(
		    """
		        byteData = "Test Data".getBytes();
		        insert = queryExecute(
		        	"INSERT INTO blob_test ( id, data ) VALUES ( 1, :blobData )",
		        	{ blobData: { value: byteData, sqltype: "blob" } }
		        );
		    """, context );

		// Retrieve the data
		instance.executeStatement(
		    """
		        result = queryExecute( "SELECT * FROM blob_test WHERE id = 1" );
		    """, context );

		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );
		IStruct	firstRow	= query.getRowAsStruct( 0 );
		Object	data		= firstRow.get( Key.of( "data" ) );
		assertNotNull( data );
		if ( data instanceof byte[] ) {
			String retrieved = new String( ( byte[] ) data, java.nio.charset.StandardCharsets.UTF_8 );
			assertEquals( "Test Data", retrieved );
		}
	}
}
