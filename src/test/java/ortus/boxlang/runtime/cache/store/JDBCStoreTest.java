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
package ortus.boxlang.runtime.cache.store;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.scopes.Key;
import tools.JDBCTestUtils;

class JDBCStoreTest extends BaseStoreTest {

	static String		datasourceName	= "jdbcStoreTest";
	static DataSource	datasource;
	static IBoxContext	context;
	static BoxRuntime	runtime;

	@AfterAll
	public static void teardown() {
		// Shutdown the store first to close any connections
		if ( store != null ) {
			store.shutdown();
		}

		// Shutdown Derby database
		if ( datasource != null ) {
			try {
				datasource.shutdown();
			} catch ( Exception e ) {
				// Ignore shutdown errors
			}
		}
	}

	@BeforeAll
	static void setUp() {
		// Initialize BoxLang runtime
		runtime		= BoxRuntime.getInstance( true );

		// Build Derby in-memory database datasource using JDBCTestUtils
		datasource	= JDBCTestUtils.buildDatasource( datasourceName );

		// Register the datasource with BoxLang runtime
		runtime
		    .getDataSourceService()
		    .register(
		        Key.of( datasourceName ),
		        datasource
		    );

		// Create a scripting context for testing
		context			= new ScriptingRequestBoxContext( runtime.getRuntimeContext() );

		// Prep the fields to use in the base test
		mockProvider	= getMockProvider( "jdbcStoreTest" );
		mockConfig.properties.put( Key.datasource, datasourceName );
		mockConfig.properties.put( Key.of( "tableName" ), "cacheStore" );
		mockConfig.properties.put( Key.of( "autoCreate" ), true );

		// Initialize the JDBC store
		store = new JDBCStore().init( mockProvider, mockConfig.properties );
	}

}
