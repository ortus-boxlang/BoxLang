package ortus.boxlang.runtime.bifs.global.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

public class GetDataSourceMetaDataTest extends BaseJDBCTest {

	static Key result = new Key( "result" );

	@DisplayName( "It returns metadata for a named datasource." )
	@Test
	public void testNamedDataSourceMetadata() {
		instance.executeSource(
		    """
		    result = getDataSourceMetaData( datasource = "%s" )
		    """.formatted( datasource.getOriginalName() ),
		    context
		);
		IStruct metadata = variables.getAsStruct( result );

		assertEquals( datasource.getOriginalName(), metadata.get( "datasourceName" ) );
		assertNotNull( metadata.get( "configuration" ) );
		assertNotNull( metadata.get( "applicationName" ) );
		assertNotNull( metadata.get( "poolStats" ) );
	}

	@DisplayName( "It returns metadata when no named datasource provided." )
	@Test
	public void testAllDatasourceMetadata() {
		instance.executeSource(
		    """
		    	result = getDataSourceMetaData();
		    """, context );
		Array	metadata	= variables.getAsArray( result );
		IStruct	first		= ( IStruct ) metadata.get( 0 );
		assertFalse( metadata.isEmpty() );

		assertNotNull( first.get( "datasourceName" ) );
		assertNotNull( first.get( "configuration" ) );
		assertNotNull( first.get( "applicationName" ) );
		assertNotNull( first.get( "poolStats" ) );
	}

	@DisplayName( "It rejects an unknown datasource" )
	@Test
	public void testUnknownDatasource() {
		assertThrows( DatabaseException.class, () -> {
			instance.executeSource(
			    """
			    	getDataSourceMetaData( datasource = "does-not-exist" );
			    """, context );
		} );
	}
}