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
 * distributed under the License is distribu ted on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.jdbc;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.segments.DatasourceConfig;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Struct;
import tools.JDBCTestUtils;

public class ConnectionManagerTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It can create a connection manager" )
	@Test
	public void testCreateConnectionManager() {
		ConnectionManager manager = new ConnectionManager( context );
		assertThat( manager ).isNotNull();
	}

	@DisplayName( "It can get the default datasource" )
	@Test
	public void testGetDefaultDatasource() {
		ConnectionManager	manager		= new ConnectionManager( context );
		DataSource			datasource	= manager.getDefaultDatasource();
		assertThat( datasource ).isNull();

		// Set the default datasource
		manager.setDefaultDatasource(
		    JDBCTestUtils.buildDatasource( "test" )
		);
		// Now get it
		assertThat( manager.hasDefaultDatasource() ).isTrue();
		assertThat( manager.getDefaultDatasource() ).isNotNull();
		assertThat( manager.getDefaultDatasource().getUniqueName().getName() ).contains( "test" );
	}

	@DisplayName( "It can get the default datasource with a context override" )
	@Test
	public void testGetDefaultDatasourceWithContextOverride() {
		ConnectionManager manager = new ConnectionManager( context );

		// Mock a context override for default datasource
		instance.getConfiguration().runtime.defaultDatasource = "override";
		instance.getConfiguration().runtime.datasources.put(
		    Key.of( "override" ),
		    DatasourceConfig.fromStruct( JDBCTestUtils.getDatasourceConfig( "override" ) )
		);

		// Get the default datasource
		DataSource datasource = manager.getDefaultDatasource();

		assertThat( datasource ).isNotNull();
		assertThat( datasource.getUniqueName().getName() ).contains( "override" );
	}

	@DisplayName( "It can get a datasource by name" )
	@Test
	public void testGetDatasourceByName() {
		ConnectionManager manager = new ConnectionManager( context );

		// Set up a datasource
		instance.getConfiguration().runtime.datasources.put(
		    Key.of( "bdd" ),
		    DatasourceConfig.fromStruct( JDBCTestUtils.getDatasourceConfig( "bdd" ) )
		);

		// Get the datasource
		DataSource datasource = manager.getDatasource( Key.of( "bdd" ) );
		assertThat( datasource ).isNotNull();
		assertThat( datasource.getUniqueName().getName() ).contains( "bdd" );
		assertThat( manager.hasCachedDatasource( Key.of( "bdd" ) ) ).isTrue();
	}

	@DisplayName( "It will return null for a non-existent datasource" )
	@Test
	public void testGetNonExistentDatasource() {
		ConnectionManager	manager		= new ConnectionManager( context );

		// Get the datasource
		DataSource			datasource	= manager.getDatasource( Key.of( "nonexistent" ) );
		assertThat( datasource ).isNull();
	}

	@DisplayName( "It can get a datasource on the fly" )
	@Test
	public void testGetDatasourceOnTheFly() {
		ConnectionManager	manager		= new ConnectionManager( context );

		// Get the datasource
		DataSource			datasource	= manager.getOnTheFlyDataSource( Struct.of(
		    "driver", "derby",
		    "database", "myDB",
		    "connectionString", "jdbc:derby:memory:myDB;create=true"
		) );
		assertThat( datasource ).isNotNull();
		assertThat( datasource.getConfiguration().isOnTheFly() ).isTrue();
	}

	@DisplayName( "It will throw an exception for an on the fly missing a driver" )
	@Test
	public void testGetDatasourceOnTheFlyMissingDriver() {
		ConnectionManager manager = new ConnectionManager( context );
		// Get the datasource
		try {
			manager.getOnTheFlyDataSource( Struct.of(
			    "host", "127.0.0.1"
			) );
		} catch ( Exception e ) {
			assertThat( e ).isInstanceOf( IllegalArgumentException.class );
			assertThat( e.getMessage() )
			    .contains( "Datasource configuration must contain a 'driver' or 'type key', or a valid JDBC connection string in 'url'." );
		}
	}

	@DisplayName( "It will get a datasource on the fly using 'type' instead of driver" )
	@Test
	public void testGetDatasourceOnTheFlyUsingType() {
		ConnectionManager	manager		= new ConnectionManager( context );
		// Get the datasource
		DataSource			datasource	= manager.getOnTheFlyDataSource( Struct.of(
		    "type", "derby",
		    "database", "myDB",
		    "connectionString", "jdbc:derby:memory:myDB;create=true"
		) );
		assertThat( datasource ).isNotNull();
		assertThat( datasource.getConfiguration().isOnTheFly() ).isTrue();
	}

}
