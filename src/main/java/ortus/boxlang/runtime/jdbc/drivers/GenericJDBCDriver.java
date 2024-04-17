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
package ortus.boxlang.runtime.jdbc.drivers;

import ortus.boxlang.runtime.config.segments.DatasourceConfig;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

/**
 * This is the generic JDBC driver that can be used to register datasources in the system.
 * We use a generic JDBC Url connection schema to connect to the database.
 */
public class GenericJDBCDriver implements IJDBCDriver {

	/**
	 * The name of the driver
	 */
	private static final Key NAME = new Key( "Generic" );

	@Override
	public Key getName() {
		return NAME;
	}

	@Override
	public DatabaseDriverType getType() {
		return DatabaseDriverType.GENERIC;
	}

	/**
	 * We return an empty class, because we are using a generic JDBC driver
	 * that does not have a specific class name.
	 * <p>
	 * This will be based on the connection url built by the driver and it will be expecting
	 * the class to be in the class path.
	 */
	@Override
	public String getClassName() {
		return "";
	}

	@Override
	public String buildConnectionURL( DatasourceConfig config ) {

		// Validate the driver
		String jDriver = ( String ) config.properties.getOrDefault( "driver", "" );
		if ( jDriver.isEmpty() ) {
			throw new IllegalArgumentException( "The driver property is required for the Generic JDBC Driver" );
		}
		// Validate the port
		int port = ( int ) config.properties.getOrDefault( "port", 0 );
		if ( port == 0 ) {
			throw new IllegalArgumentException( "The port property is required for the Generic JDBC Driver" );
		}

		// Validate the database
		String	database		= ( String ) config.properties.getOrDefault( "database", "" );

		// Host we can use localhost
		String	host			= ( String ) config.properties.getOrDefault( "host", "localhost" );

		// Verify if custom is a struct or string
		String	targetCustom	= "";
		if ( config.properties.get( Key.custom ) instanceof String castedCustom ) {
			targetCustom = castedCustom;
		} else {
			targetCustom = customToString( ( IStruct ) config.properties.get( Key.custom ) );
		}

		// Build the Generic connection URL
		return String.format( "jdbc:%s://%s:%d/%s?%s", jDriver, host, port, database, targetCustom );
	}

	/**
	 * Convert the custom struct to a string
	 *
	 * @param target The struct to convert
	 *
	 * @return
	 */
	private String customToString( IStruct target ) {
		// convert the struct to a string: key=value&key=value
		StringBuilder sb = new StringBuilder();
		target.forEach( ( key, value ) -> {
			if ( sb.length() > 0 ) {
				sb.append( "&" );
			}
			sb.append( key ).append( "=" ).append( value );
		} );
		return sb.toString();
	}

}
