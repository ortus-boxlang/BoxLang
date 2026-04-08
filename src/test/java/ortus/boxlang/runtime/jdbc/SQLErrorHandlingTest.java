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
package ortus.boxlang.runtime.jdbc;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import tools.JDBCTestUtils;

/**
 * Test that SQL errors (like creating duplicate tables) are properly reported
 * instead of being masked by NullPointerException.
 */
@DisabledOnOs( OS.WINDOWS )
public class SQLErrorHandlingTest {

	static BoxRuntime	instance;
	static DataSource	datasource;
	static IBoxContext	context;

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		datasource	= JDBCTestUtils.buildDatasource( "sqlErrorTest" );

		// Register the datasource
		Key datasourceKey = Key.of( "sqlErrorTest" );
		instance.getDataSourceService().register( datasourceKey, datasource );
		instance.getConfiguration().datasources.put( datasourceKey, datasource.getConfiguration() );
	}

	@AfterAll
	public static void tearDown() {
		if ( datasource != null ) {
			// Clean up
			try {
				datasource.execute( "DROP TABLE testusers", context );
			} catch ( Exception e ) {
				// Ignore if table doesn't exist
			}
			datasource.shutdown();
		}
	}

	@DisplayName( "It should report the actual SQL error, not a NullPointerException" )
	@Test
	public void testDuplicateTableCreationError() {
		String createTableSQL = "CREATE TABLE testusers ( id INTEGER PRIMARY KEY, name VARCHAR(155) )";

		// First creation should succeed
		datasource.execute( createTableSQL, context );

		// Second creation should fail with proper SQL error message
		DatabaseException	exception	= assertThrows( DatabaseException.class, () -> {
											datasource.execute( createTableSQL, context );
										} );

		// Verify the exception message contains the actual SQL error, not NullPointerException
		String				message		= exception.getMessage();
		assertThat( message ).isNotNull();
		assertThat( message.toLowerCase() ).doesNotContain( "nullpointerexception" );
		assertThat( message.toLowerCase() ).doesNotContain( "this.pointer" );
		assertThat( message.toLowerCase() ).doesNotContain( "isclosed" );

		// Verify it contains information about the actual error (table exists)
		// Derby error message says "already exists"
		assertThat( message.toLowerCase() ).containsMatch( "(already exists|duplicate)" );
	}

	@DisplayName( "It should properly handle SQL syntax errors" )
	@Test
	public void testSQLSyntaxError() {
		String				invalidSQL	= "CREATE INVALID SYNTAX HERE";

		// Should fail with proper SQL error message
		DatabaseException	exception	= assertThrows( DatabaseException.class, () -> {
											datasource.execute( invalidSQL, context );
										} );

		// Verify the exception message contains the actual SQL error, not NullPointerException
		String				message		= exception.getMessage();
		assertThat( message ).isNotNull();
		assertThat( message.toLowerCase() ).doesNotContain( "nullpointerexception" );
		assertThat( message.toLowerCase() ).doesNotContain( "this.pointer" );

		// Verify it contains information about syntax error
		assertThat( message.toLowerCase() ).containsMatch( "(syntax|invalid)" );
	}
}
