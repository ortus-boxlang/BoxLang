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

package ortus.boxlang.runtime.bifs.global.jdbc;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.services.DatasourceService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

public class RegisterDatasourceTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result				= new Key( "result" );
	DatasourceService	datasourceService	= null;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		context				= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables			= context.getScopeNearby( VariablesScope.name );
		datasourceService	= instance.getDataSourceService();
	}

	@DisplayName( "It can register a datasource" )
	@Test
	public void testSimpleRegistration() {
		// check datasource list
		IStruct configDatasources = ( IStruct ) this.context.getConfigItems( Key.datasources );
		assertThat( configDatasources.containsKey( Key.of( "myDynamicDS" ) ) ).isFalse();

		instance.executeSource(
		    """
		       RegisterDatasource( "myDynamicDS", {
		    	"driver": "mysql",
		    	"host": "localhost",
		    	"port": "3306",
		    	"database": "myDB",
		    	"username": "user",
		    	"password": "pass"
		    } );
		       """,
		    context );

		// refetch datasource list
		configDatasources = ( IStruct ) this.context.getConfigItems( Key.datasources );
		assertThat( configDatasources.containsKey( Key.of( "myDynamicDS" ) ) ).isTrue();
	}

	@DisplayName( "It refuses to overwrite existing datasource names" )
	@Test
	public void testExistingDSOverwrite() {
		// check datasource list
		IStruct configDatasources = ( IStruct ) this.context.getConfigItems( Key.datasources );
		assertThat( configDatasources.containsKey( Key.of( "test1" ) ) ).isFalse();

		instance.executeSource(
		    """
		    RegisterDatasource( "test1", { "driver": "derby" } );
		    """,
		    context );
		// check datasource list
		configDatasources = ( IStruct ) this.context.getConfigItems( Key.datasources );
		assertThat( configDatasources.containsKey( Key.of( "test1" ) ) ).isTrue();

		DatabaseException e = assertThrows( DatabaseException.class, () -> instance.executeSource(
		    """
		    RegisterDatasource( "test1", { "driver": "derby" } );
		    """,
		    context ) );

		assertThat( e.getMessage() )
		    .contains( "Datasource name 'test1' already exists" );
	}

	@DisplayName( "It don't like empty datasource config" )
	@Test
	public void testEmptyConfig() {
		// check datasource list
		IStruct configDatasources = ( IStruct ) this.context.getConfigItems( Key.datasources );
		assertThat( configDatasources.containsKey( Key.of( "myFunkyDS" ) ) ).isFalse();

		assertThrows( BoxValidationException.class, () -> instance.executeSource(
		    """
		    RegisterDatasource( "myFunkyDS", {} );
		    """,
		    context )
		);

		// refetch datasource list
		configDatasources = ( IStruct ) this.context.getConfigItems( Key.datasources );
		assertThat( configDatasources.containsKey( Key.of( "myFunkyDS" ) ) ).isFalse();
	}
}
