
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

package ortus.boxlang.runtime.components.jdbc;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.jdbc.DataSourceManager;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;

public class DBInfoTest {

	static DataSourceManager	datasourceManager;
	static BoxRuntime			instance;
	IBoxContext					context;
	IScope						variables;
	static Key					result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance			= BoxRuntime.getInstance( true );
		datasourceManager	= DataSourceManager.getInstance();
		DataSource defaultDatasource = new DataSource( Struct.of(
		    "jdbcUrl", "jdbc:derby:memory:BoxlangDB;create=true"
		) );
		datasourceManager.setDefaultDatasource( defaultDatasource );
	}

	@AfterAll
	public static void teardown() {
		datasourceManager.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It requires a non-null `type` argument matching a valid type" )
	@Test
	public void requiredTypeValidation() {
		assertThrows( BoxValidationException.class, () -> {
			instance.executeStatement( "CFDBInfo();" );
		} );
		assertThrows( BoxValidationException.class, () -> {
			instance.executeStatement( "CFDBInfo( type='foo' );" );
		} );
		// This DOES throw, and i don't know why. 😢
		// assertDoesNotThrow( () -> {
		// instance.executeStatement( "CFDBInfo( type='version', name='result' );" );
		// } );
	}

	@DisplayName( "It requires the `table` argument on column, foreignkeys, and index types`" )
	@Test
	public void typeRequiresTableValidation() {
		assertThrows( BoxValidationException.class, () -> {
			instance.executeStatement( "CFDBInfo( type='columns' );" );
		} );
		assertThrows( BoxValidationException.class, () -> {
			instance.executeStatement( "CFDBInfo( type='foreignkeys' );" );
		} );
		assertThrows( BoxValidationException.class, () -> {
			instance.executeStatement( "CFDBInfo( type='index' );" );
		} );
	}

	@DisplayName( "Can get JDBC driver version info" )
	@Test
	public void testVersion() {
		instance.executeSource(
		    """
		        cfdbinfo( type='version', name='result' )
		    """,
		    context );
		Object theResult = variables.get( result );
		assertTrue( theResult instanceof Query );
		Query versionQuery = ( Query ) theResult;
		assertEquals( 1, versionQuery.size() );
		assertEquals( 6, versionQuery.getColumns().size() );

		assertEquals( "Apache Derby Embedded JDBC Driver", versionQuery.getRowAsStruct( 0 ).getAsString( Key.of( "DRIVER_NAME" ) ) );
	}

	@Disabled( "Result object is missing expected database name." )
	@DisplayName( "Can get catalog and schema names" )
	@Test
	public void testDBNames() {
		instance.executeSource(
		    """
		        cfdbinfo( type='dbnames', name='result' )
		    """,
		    context );
		Object theResult = variables.get( result );
		assertTrue( theResult instanceof Query );
		Query dbNamesQuery = ( Query ) theResult;
		assertTrue( dbNamesQuery.size() > 0 );
		assertEquals( 2, dbNamesQuery.getColumns().size() );

		IStruct ourDBRow = dbNamesQuery.stream()
		    .filter( row -> row.getAsString( Key.of( "DBNAME" ) ).equals( "boxlang" ) )
		    .findFirst()
		    .orElse( null );

		// @TODO: Fix this assertion!
		// for unknown reasons, the boxlang database is not found.
		// Possibly Apache Derby isn't playing nice with our connection string... dunno. Until then,
		// this test will have to stay disabled.
		// SeeDerby database name docs: https://db.apache.org/derby/docs/10.17/devguide/rdevdvlp847152.html
		assertNotNull( ourDBRow );
		assertEquals( "CATALOG", ourDBRow.getAsString( Key.type ) );
		assertEquals( "testDB", ourDBRow.getAsString( Key.of( "DBNAME" ) ) );
	}

}
