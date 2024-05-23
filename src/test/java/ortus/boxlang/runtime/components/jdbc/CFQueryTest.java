
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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.bifs.global.jdbc.BaseJDBCTest;
import ortus.boxlang.runtime.config.segments.DatasourceConfig;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import tools.JDBCTestUtils;

public class CFQueryTest extends BaseJDBCTest {

	static Key			result	= new Key( "result" );
	static DataSource	MySQLDataSource;

	@DisplayName( "It can execute a query with no bindings on the default datasource" )
	@Test
	public void testSimpleExecute() {
		getInstance().executeSource(
		    """
		        <cfquery name="result">
		        SELECT * FROM developers ORDER BY id
		        </cfquery>
		    """,
		    getContext(), BoxSourceType.CFTEMPLATE );
		assertThat( getVariables().get( result ) ).isInstanceOf( ortus.boxlang.runtime.types.Query.class );
		ortus.boxlang.runtime.types.Query query = getVariables().getAsQuery( result );
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

	@DisplayName( "It can execute a query with no bindings on the default datasource in BL Tags" )
	@Test
	public void testSimpleExecuteBLTag() {
		getInstance().executeSource(
		    """
		        <bx:query name="result">
		        SELECT * FROM developers ORDER BY id
		        </bx:query>
		    """,
		    getContext(), BoxSourceType.BOXTEMPLATE );
		assertThat( getVariables().get( result ) ).isInstanceOf( ortus.boxlang.runtime.types.Query.class );
		ortus.boxlang.runtime.types.Query query = getVariables().getAsQuery( result );
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
		getInstance().executeSource(
		    """
		        <cfquery>
		        SELECT * FROM developers ORDER BY id
		        </cfquery>
		    """,
		    getContext(), BoxSourceType.CFTEMPLATE );
		assertThat( getVariables().get( "cfquery" ) ).isInstanceOf( ortus.boxlang.runtime.types.Query.class );
		ortus.boxlang.runtime.types.Query query = getVariables().getAsQuery( Key.of( "cfquery" ) );
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
		getInstance().executeSource(
		    """
		        <cfquery name="result">
		        SELECT * FROM developers WHERE id = <cfqueryparam value="77" />
		        </cfquery>
		    """,
		    getContext(), BoxSourceType.CFTEMPLATE );
		assertThat( getVariables().get( result ) ).isInstanceOf( ortus.boxlang.runtime.types.Query.class );
		ortus.boxlang.runtime.types.Query query = getVariables().getAsQuery( result );
		assertEquals( 1, query.size() );

		IStruct michael = query.getRowAsStruct( 0 );
		assertEquals( 77, michael.get( "id" ) );
		assertEquals( "Michael Born", michael.get( "name" ) );
		assertEquals( "Developer", michael.get( "role" ) );
	}

	@DisplayName( "It can execute a query on a named datasource" )
	@Test
	public void testNamedDataSource() {

		var dbName = Key.of( "derbyOnTheFly" );
		// Register the named datasource
		getInstance().getConfiguration().runtime.datasources.put(
		    Key.of( dbName ),
		    DatasourceConfig.fromStruct( JDBCTestUtils.getDatasourceConfig( dbName.getName() ) )
		);

		// @formatter:off
		getInstance().executeSource(
		    """
			<bx:try>
				<bx:query name="buildDatabase" datasource="derbyOnTheFly">
					CREATE TABLE developers ( id INTEGER, name VARCHAR(155), role VARCHAR(155) )
				</bx:query>
				<bx:catch>
				</bx:catch>
			</bx:try>
		    <bx:query name="result" datasource="derbyOnTheFly">
		 	   SELECT * FROM developers ORDER BY id
		    </bx:query>
		    """,
		    getContext(), BoxSourceType.BOXTEMPLATE );

		// @formatter:on

		assertThat( getVariables().get( result ) ).isInstanceOf( ortus.boxlang.runtime.types.Query.class );
		ortus.boxlang.runtime.types.Query query = getVariables().getAsQuery( result );
		assertEquals( 0, query.size() );
	}

	@DisplayName( "It throws an exception if the specified datasource is not registered" )
	@Test
	public void testMissingNamedDataSource() {
		DatabaseException e = assertThrows( DatabaseException.class, () -> getInstance().executeSource(
		    """
		    			cfquery( name="result", datasource="not_found" ) {
		    	SELECT * FROM developers ORDER BY id
		    }
		    """,
		    getContext(), BoxSourceType.CFSCRIPT ) );

		assertThat( e.getMessage() ).contains( "Datasource with name [not_found] not found" );
		assertNull( getVariables().get( result ) );
	}

	@DisplayName( "It can return query results as an array" )
	@Test
	public void testReturnTypeArray() {
		getInstance().executeSource(
		    """
		    			cfquery( name="result", returntype = "array" ) {
		    	writeOutput( "SELECT * FROM developers ORDER BY id" );
		    };
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );
		assertThat( getVariables().get( result ) ).isInstanceOf( Array.class );
		Array results = getVariables().getAsArray( result );
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
		getInstance().executeSource(
		    """
		    query name="result" returntype="struct" columnKey="role" {
		    	echo( "SELECT * FROM developers ORDER BY id" );
		    };
		    """,
		    getContext() );
		assertThat( getVariables().get( result ) ).isInstanceOf( IStruct.class );
		IStruct results = getVariables().getAsStruct( result );
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
		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> getInstance().executeSource(
		    """
		    query name="result" returntype="struct" {
		    	echo( "SELECT * FROM developers ORDER BY id" );
		    };
		    """,
		    getContext() ) );

		assertThat( e.getMessage() ).isEqualTo( "Input [returnType] for component [Query] requires the following records to be present: columnKey" );
		assertNull( getVariables().get( result ) );
	}

	@DisplayName( "It throws an exception if the returnType is invalid" )
	@Test
	public void testInvalidReturnType() {
		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> getInstance().executeSource(
		    """
		    <cfquery name="result" returntype="foobar">
		    	SELECT * FROM developers WHERE id = <cfqueryparam value="77" />
		    </cfquery>
		    """,
		    getContext(), BoxSourceType.CFTEMPLATE ) );

		assertThat( e.getMessage() ).isEqualTo( "Unknown return type: foobar" );
		assertNull( getVariables().get( result ) );
	}

	@DisplayName( "It can access the results of a queryExecute call" )
	@Test
	public void testResultVariable() {
		getInstance().executeSource(
		    """
		    <cfquery name="result" result="queryResults">
		    SELECT * FROM developers WHERE role = <cfqueryparam value="Developer" />
		    </cfquery>
		    """,
		    getContext(), BoxSourceType.CFTEMPLATE );
		Object resultObject = getVariables().get( Key.of( "queryResults" ) );
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
