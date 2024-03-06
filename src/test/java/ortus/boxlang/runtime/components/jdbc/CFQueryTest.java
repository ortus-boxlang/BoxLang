
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

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import ortus.boxlang.parser.BoxScriptType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.jdbc.DataSourceManager;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.*;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;

public class CFQueryTest {

	static BoxRuntime			instance;
	IBoxContext					context;
	IScope						variables;
	static Key					result	= new Key( "result" );
	static DataSource			MySQLDataSource;
	static DataSourceManager	datasourceManager;
	static DataSource			datasource;

	@BeforeAll
	public static void setUp() {
		instance			= BoxRuntime.getInstance( true );
		datasourceManager	= DataSourceManager.getInstance();
		datasource			= new DataSource( Struct.of(
		    "jdbcUrl", "jdbc:derby:memory:testQueryComponentDB;create=true"
		) );
		datasource.execute( "CREATE TABLE developers ( id INTEGER, name VARCHAR(155), role VARCHAR(155) )" );
	}

	@AfterAll
	public static void teardown() throws SQLException {
		datasource.shutdown();
		datasourceManager.clear( true );
	}

	@BeforeEach
	public void resetTable() {
		assertDoesNotThrow( () -> {
			datasource.execute( "TRUNCATE TABLE developers" );
			datasource.execute( "INSERT INTO developers ( id, name, role ) VALUES ( 77, 'Michael Born', 'Developer' )" );
			datasource.execute( "INSERT INTO developers ( id, name, role ) VALUES ( 1, 'Luis Majano', 'CEO' )" );
			datasource.execute( "INSERT INTO developers ( id, name, role ) VALUES ( 42, 'Eric Peterson', 'Developer' )" );
		} );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
		datasourceManager.clear( false );
	}

	@DisplayName( "It can execute a query with no bindings on the default datasource" )
	@Test
	public void testSimpleExecute() {
		datasourceManager.setDefaultDataSource( datasource );
		instance.executeSource(
		    """
		    			<cfquery name="result">
		    SELECT * FROM developers ORDER BY id
		    </cfquery>
		    """,
		    context, BoxScriptType.CFMARKUP );
		assertThat( variables.get( result ) ).isInstanceOf( ortus.boxlang.runtime.types.Query.class );
		ortus.boxlang.runtime.types.Query query = variables.getAsQuery( result );
		assertEquals( 3, query.size() );

		IStruct luis = query.getRowAsStruct( 0 );
		assertEquals( 1, luis.get( "id" ) );
		assertEquals( "Luis Majano", luis.get( "name" ) );
		assertEquals( "CEO", luis.get( "role" ) );

		IStruct eric = query.getRowAsStruct( 1 );
		assertEquals( 42, eric.get( "id" ) );
		assertEquals( "Eric Peterson", eric.get( "name" ) );
		assertEquals( "Developer", eric.get( "role" ) );

		IStruct michael = query.getRowAsStruct( 2 );
		assertEquals( 77, michael.get( "id" ) );
		assertEquals( "Michael Born", michael.get( "name" ) );
		assertEquals( "Developer", michael.get( "role" ) );
	}

	@DisplayName( "It uses the default name of cfquery for the query results" )
	@Test
	public void testDefaultName() {
		datasourceManager.setDefaultDataSource( datasource );
		instance.executeSource(
		    """
		    			<cfquery>
		    SELECT * FROM developers ORDER BY id
		    </cfquery>
		    """,
		    context, BoxScriptType.CFMARKUP );
		assertThat( variables.get( "cfquery" ) ).isInstanceOf( ortus.boxlang.runtime.types.Query.class );
		ortus.boxlang.runtime.types.Query query = variables.getAsQuery( Key.of( "cfquery" ) );
		assertEquals( 3, query.size() );

		IStruct luis = query.getRowAsStruct( 0 );
		assertEquals( 1, luis.get( "id" ) );
		assertEquals( "Luis Majano", luis.get( "name" ) );
		assertEquals( "CEO", luis.get( "role" ) );

		IStruct eric = query.getRowAsStruct( 1 );
		assertEquals( 42, eric.get( "id" ) );
		assertEquals( "Eric Peterson", eric.get( "name" ) );
		assertEquals( "Developer", eric.get( "role" ) );

		IStruct michael = query.getRowAsStruct( 2 );
		assertEquals( 77, michael.get( "id" ) );
		assertEquals( "Michael Born", michael.get( "name" ) );
		assertEquals( "Developer", michael.get( "role" ) );
	}

	@DisplayName( "It can execute a query with query param bindings on the default datasource" )
	@Test
	public void testArrayBindings() {
		datasourceManager.setDefaultDataSource( datasource );
		instance.executeSource(
		    """
		    			<cfquery name="result">
		    SELECT * FROM developers WHERE id = <cfqueryparam value="77" />
		    </cfquery>
		    """,
		    context, BoxScriptType.CFMARKUP );
		assertThat( variables.get( result ) ).isInstanceOf( ortus.boxlang.runtime.types.Query.class );
		ortus.boxlang.runtime.types.Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );

		IStruct michael = query.getRowAsStruct( 0 );
		assertEquals( 77, michael.get( "id" ) );
		assertEquals( "Michael Born", michael.get( "name" ) );
		assertEquals( "Developer", michael.get( "role" ) );
	}

	@DisplayName( "It throws an exception if no default datasource is defined and no datasource is specified" )
	@Test
	public void testMissingDefaultDataSource() {
		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    			<cfquery name="result">
		    SELECT * FROM developers WHERE id = <cfqueryparam value="77" />
		    </cfquery>
		    """,
		    context, BoxScriptType.CFMARKUP ) );

		assertThat( e.getMessage() )
		    .isEqualTo( "No default datasource has been defined. Either register a default datasource or provide a datasource name in the query options." );
		assertNull( variables.get( result ) );
	}

	@DisplayName( "It can execute a query on a named datasource" )
	@Test
	public void testNamedDataSource() {
		datasourceManager.registerDataSource( Key.of( "derby" ), datasource );
		instance.executeSource(
		    """
		    			<cfquery name="result" datasource="derby">
		    SELECT * FROM developers ORDER BY id
		    </cfquery>
		    """,
		    context, BoxScriptType.CFMARKUP );
		assertThat( variables.get( result ) ).isInstanceOf( ortus.boxlang.runtime.types.Query.class );
		ortus.boxlang.runtime.types.Query query = variables.getAsQuery( result );
		assertEquals( 3, query.size() );

		IStruct luis = query.getRowAsStruct( 0 );
		assertEquals( 1, luis.get( "id" ) );
		assertEquals( "Luis Majano", luis.get( "name" ) );
		assertEquals( "CEO", luis.get( "role" ) );

		IStruct eric = query.getRowAsStruct( 1 );
		assertEquals( 42, eric.get( "id" ) );
		assertEquals( "Eric Peterson", eric.get( "name" ) );
		assertEquals( "Developer", eric.get( "role" ) );

		IStruct michael = query.getRowAsStruct( 2 );
		assertEquals( 77, michael.get( "id" ) );
		assertEquals( "Michael Born", michael.get( "name" ) );
		assertEquals( "Developer", michael.get( "role" ) );
	}

	@DisplayName( "It throws an exception if the specified datasource is not registered" )
	@Test
	public void testMissingNamedDataSource() {
		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    			cfquery( name="result", datasource="derby" ) {
		    	SELECT * FROM developers ORDER BY id
		    }
		    """,
		    context ) );

		assertThat( e.getMessage() ).isEqualTo( "No [derby] datasource defined." );
		assertNull( variables.get( result ) );
	}

	@DisplayName( "It can return query results as an array" )
	@Test
	public void testReturnTypeArray() {
		datasourceManager.setDefaultDataSource( datasource );
		instance.executeSource(
		    """
		    			cfquery( name="result", returntype = "array" ) {
		    	writeOutput( "SELECT * FROM developers ORDER BY id" );
		    };
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Array.class );
		Array results = variables.getAsArray( result );
		assertEquals( 3, results.size() );

		Object luisObject = results.get( 0 );
		assertThat( luisObject ).isInstanceOf( IStruct.class );
		IStruct luis = ( IStruct ) luisObject;
		assertEquals( 1, luis.get( "id" ) );
		assertEquals( "Luis Majano", luis.get( "name" ) );
		assertEquals( "CEO", luis.get( "role" ) );

		Object ericObject = results.get( 1 );
		assertThat( ericObject ).isInstanceOf( IStruct.class );
		IStruct eric = ( IStruct ) ericObject;
		assertEquals( 42, eric.get( "id" ) );
		assertEquals( "Eric Peterson", eric.get( "name" ) );
		assertEquals( "Developer", eric.get( "role" ) );

		Object michaelObject = results.get( 2 );
		assertThat( michaelObject ).isInstanceOf( IStruct.class );
		IStruct michael = ( IStruct ) michaelObject;
		assertEquals( 77, michael.get( "id" ) );
		assertEquals( "Michael Born", michael.get( "name" ) );
		assertEquals( "Developer", michael.get( "role" ) );
	}

	@DisplayName( "It can return query results as a struct" )
	@Test
	public void testReturnTypeStruct() {
		datasourceManager.setDefaultDataSource( datasource );
		instance.executeSource(
		    """
		    			query name="result" returntype="struct" columnKey="role" {
		    	echo( "SELECT * FROM developers ORDER BY id" );
		    };
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		IStruct results = variables.getAsStruct( result );
		assertEquals( 2, results.size() );

		List<String> keys = results.getKeysAsStrings();
		assertEquals( keys, List.of( "Developer", "CEO" ) );

		assertInstanceOf( Array.class, results.get( "CEO" ) );
		Array ceoRecords = results.getAsArray( Key.of( "CEO" ) );
		assertEquals( 1, ceoRecords.size() );

		Object luisObject = ceoRecords.get( 0 );
		assertThat( luisObject ).isInstanceOf( IStruct.class );
		IStruct luis = ( IStruct ) luisObject;
		assertEquals( 1, luis.get( "id" ) );
		assertEquals( "Luis Majano", luis.get( "name" ) );
		assertEquals( "CEO", luis.get( "role" ) );

		assertInstanceOf( Array.class, results.get( "Developer" ) );
		Array developerRecords = results.getAsArray( Key.of( "Developer" ) );
		assertEquals( 2, developerRecords.size() );

		Object ericObject = developerRecords.get( 0 );
		assertThat( ericObject ).isInstanceOf( IStruct.class );
		IStruct eric = ( IStruct ) ericObject;
		assertEquals( 42, eric.get( "id" ) );
		assertEquals( "Eric Peterson", eric.get( "name" ) );
		assertEquals( "Developer", eric.get( "role" ) );

		Object michaelObject = developerRecords.get( 1 );
		assertThat( michaelObject ).isInstanceOf( IStruct.class );
		IStruct michael = ( IStruct ) michaelObject;
		assertEquals( 77, michael.get( "id" ) );
		assertEquals( "Michael Born", michael.get( "name" ) );
		assertEquals( "Developer", michael.get( "role" ) );
	}

	@DisplayName( "It throws an exception if the returnType is struct but no columnKey is provided" )
	@Test
	public void testMissingColumnKey() {
		datasourceManager.setDefaultDataSource( datasource );
		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    			query name="result" returntype="struct" {
		    	echo( "SELECT * FROM developers ORDER BY id" );
		    };
		    """,
		    context ) );

		assertThat( e.getMessage() ).isEqualTo( "Record [returnType] for component [Query] requires the following records to be present: columnKey" );
		assertNull( variables.get( result ) );
	}

	@DisplayName( "It throws an exception if the returnType is invalid" )
	@Test
	public void testInvalidReturnType() {
		datasourceManager.setDefaultDataSource( datasource );
		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    			<cfquery name="result" returntype="foobar">
		    SELECT * FROM developers WHERE id = <cfqueryparam value="77" />
		    </cfquery>
		    """,
		    context, BoxScriptType.CFMARKUP ) );

		assertThat( e.getMessage() ).isEqualTo( "Unknown return type: foobar" );
		assertNull( variables.get( result ) );
	}

	@DisplayName( "It can access the results of a queryExecute call" )
	@Test
	public void testResultVariable() {
		datasourceManager.setDefaultDataSource( datasource );
		instance.executeSource(
		    """
		    			<cfquery name="result" result="queryResults">
		    SELECT * FROM developers WHERE role = <cfqueryparam value="Developer" />
		    </cfquery>
		    """,
		    context, BoxScriptType.CFMARKUP );
		Object resultObject = variables.get( Key.of( "queryResults" ) );
		assertInstanceOf( IStruct.class, resultObject );
		IStruct result = StructCaster.cast( resultObject );

		assertTrue( result.containsKey( "sql" ) );
		assertEquals( "SELECT * FROM developers WHERE role = ?", result.getAsString( Key.sql ) );

		assertTrue( result.containsKey( "cached" ) );
		assertFalse( result.getAsBoolean( Key.cached ) );

		assertTrue( result.containsKey( "sqlParameters" ) );
		assertEquals( Array.of( "Developer" ), result.getAsArray( Key.sqlParameters ) );

		assertTrue( result.containsKey( "recordCount" ) );
		assertEquals( 2, result.getAsInteger( Key.recordCount ) );

		assertTrue( result.containsKey( "columnList" ) );
		assertEquals( "ID,NAME,ROLE", result.getAsString( Key.columnList ) );

		assertTrue( result.containsKey( "executionTime" ) );
		assertThat( result.getAsLong( Key.executionTime ) ).isAtLeast( 0 );

		assertFalse( result.containsKey( "generatedKey" ) );
	}

}
