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
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;
import tools.JDBCTestUtils;

class JDBCStoreTest extends BaseStoreTest {

	static DataSource					datasource;
	static ScriptingRequestBoxContext	context;
	static BoxRuntime					runtime;

	@AfterAll
	public static void teardown() {
		if ( datasource != null ) {
			try {
				// Try to drop the table
				datasource.execute( "DROP TABLE boxlang_cache", context );
			} catch ( Exception e ) {
				// Ignore errors
			}
		}
		// Clean up the Derby database directory
		try {
			ortus.boxlang.runtime.util.FileSystemUtil.deleteDirectory( "src/test/resources/tmp/JDBCStoreTest", true );
		} catch ( Exception e ) {
			// Ignore errors
		}
	}

	@BeforeAll
	static void setUp() {
		runtime	= BoxRuntime.getInstance( true );
		context	= new ScriptingRequestBoxContext( runtime.getRuntimeContext() );

		// Clean up any existing database directory
		try {
			ortus.boxlang.runtime.util.FileSystemUtil.deleteDirectory( "src/test/resources/tmp/JDBCStoreTest", true );
		} catch ( Exception e ) {
			// Ignore errors
		}

		// Ensure the test directory exists
		ortus.boxlang.runtime.util.FileSystemUtil.createDirectory( "src/test/resources/tmp/JDBCStoreTest" );

		// Register the datasource in the runtime and get the instance - using file-based Derby
		datasource		= runtime.getDataSourceService().register(
		    Key.of( "JDBCStoreTest" ),
		    Struct.of(
		        "driver", "derby",
		        "connectionString", "jdbc:derby:src/test/resources/tmp/JDBCStoreTest/JDBCStoreTestDB;create=true"
		    )
		);

		// Prep the fields to use in the base test
		mockProvider	= getMockProvider( "test" );
		mockConfig.properties.put( Key.datasource, "JDBCStoreTest" );
		mockConfig.properties.put( Key.table, "boxlang_cache" );
		mockConfig.properties.put( Key.of( "autoCreate" ), true );

		// Initialize the store
		store = new JDBCStore().init( mockProvider, mockConfig.properties );
	}

}
