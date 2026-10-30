package ortus.boxlang.runtime.bifs.global.jdbc;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext; // runtime access
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.services.DatasourceService;
import ortus.boxlang.runtime.config.segments.DatasourceConfig;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.jdbc.ConnectionManager;
import ortus.boxlang.runtime.context.IJDBCCapableContext;

@BoxBIF( description = "Returns datasource configuration and pool metadata" )
public class GetDataSourceMetaData extends BIF {

	/**
	 * Constructor
	 */
	public GetDataSourceMetaData() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.STRING, Key.datasource )
		};
	}

	/**
	 * Returns datasource configuration and pool metadata.
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments If specified, returns the metadata for a specific named datasource. If not, returns all datasource metadata.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		DatasourceService	datasourceService	= context.getRuntime().getDataSourceService();
		String				datasourceName		= arguments.getAsString( Key.datasource );
		IJDBCCapableContext	jdbcContext			= context.getParentOfType( IJDBCCapableContext.class );
		ConnectionManager	connectionManager	= jdbcContext.getConnectionManager();
		// Get all datasource names from datasource service, then iterate through them to retrieve metadata
		Array				metadataArray		= new Array();
		String[]			datasourceNames		= datasourceService.getNames();

		// Check if one specific datasource is requested and return accordingly
		if ( datasourceName != null && !datasourceName.isEmpty() ) {
			DataSource			datasource		= connectionManager.getDatasourceOrThrow( Key.of( datasourceName ) );
			DatasourceConfig	configuration	= datasource.getConfiguration();

			IStruct				metadata		= Struct.of(
			    "datasourceName", datasourceName,
			    "configuration", configuration.toConfigStruct(),
			    "applicationName", configuration.getApplicationName(),
			    "poolStats", datasource.getPoolStats()
			);
			return metadata;
		} else {
			// Iterate through all datasource names and append metadata to a struct to the metadata array
			for ( String dsName : datasourceNames ) {
				DataSource			datasource		= datasourceService.get( Key.of( dsName ) );
				DatasourceConfig	configuration	= datasource.getConfiguration();

				IStruct				metadata		= Struct.of(
				    "datasourceName", dsName,
				    "configuration", configuration.toConfigStruct(),
				    "applicationName", configuration.getApplicationName(),
				    "poolStats", datasource.getPoolStats()
				);
				metadataArray.append( metadata );
			}
		}
		return metadataArray;
	}
}
