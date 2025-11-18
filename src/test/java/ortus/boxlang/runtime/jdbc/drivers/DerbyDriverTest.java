package ortus.boxlang.runtime.jdbc.drivers;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import tools.JDBCTestUtils;

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

	@DisplayName( "It can handle large blob columns" )
	@Test
	public void testLargeBlobColumns() {
		instance.executeStatement(
		    """
		        result = queryExecute( "CREATE TABLE large_blob ( id INTEGER PRIMARY KEY, data BLOB )" );
		    """, context );

		// Build a large string (~300KB)
		instance.executeStatement(
		    """
		    	newBlob = "";
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
		    		newBlob = newBlob & quote;
		    	}
		        insert = queryExecute( "INSERT INTO large_blob ( id, data ) VALUES ( 1, :newBlob )", { newBlob: { value: newBlob, sqltype: "blob" } } );
		    """, context );

		instance.executeStatement(
		    """
		        result = queryExecute( "SELECT * FROM large_blob WHERE id = 1" );
		    """, context );

		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );
		IStruct	firstRow	= query.getRowAsStruct( 0 );
		Object	data		= firstRow.get( Key.of( "data" ) );
		assertNotNull( data );
		if ( data instanceof byte[] ) {
			byte[]	bytes		= ( byte[] ) data;
			String	retrieved	= new String( bytes, java.nio.charset.StandardCharsets.UTF_8 );
			assertThat( retrieved.length() ).isGreaterThan( 300000 );
		}
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
