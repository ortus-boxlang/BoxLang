
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.bifs.global.jdbc.BaseJDBCTest;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import tools.JDBCTestUtils;

public class QueryTest extends BaseJDBCTest {

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
		assertEquals( 4, query.size() );

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
		assertEquals( 4, query.size() );

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
		assertThat( getVariables().get( "bxquery" ) ).isInstanceOf( ortus.boxlang.runtime.types.Query.class );
		ortus.boxlang.runtime.types.Query query = getVariables().getAsQuery( Key.of( "bxquery" ) );
		assertEquals( 4, query.size() );

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

	@DisplayName( "It can execute a query with an array queryparam" )
	@Test
	public void testListArrayBinding() {
		getInstance().executeSource(
		    """
		        <cfset ids = [77, 1, 42] />
		        <cfquery name="result">
		        SELECT * FROM developers WHERE id IN (<cfqueryparam value="#ids#" list="true">)
		        </cfquery>
		    """,
		    context,
		    BoxSourceType.CFTEMPLATE );
		assertThat( getVariables().get( result ) ).isInstanceOf( ortus.boxlang.runtime.types.Query.class );
		ortus.boxlang.runtime.types.Query query = getVariables().getAsQuery( result );
		assertEquals( 3, query.size() );
	}

	@DisplayName( "It can execute a query with a list queryparam" )
	@Test
	public void testListStringBinding() {
		getInstance().executeSource(
		    """
		        <cfquery name="result" maxrows="3">
		        SELECT * FROM developers WHERE role IN (<cfqueryparam value="Developer,CEO,QA" list="true">)
		        </cfquery>
		    """,
		    context,
		    BoxSourceType.CFTEMPLATE );
		assertThat( getVariables().get( result ) ).isInstanceOf( ortus.boxlang.runtime.types.Query.class );
		ortus.boxlang.runtime.types.Query query = getVariables().getAsQuery( result );
		assertEquals( 3, query.size() );
	}

	@DisplayName( "It can execute a query on a named datasource" )
	@Test
	public void testNamedDataSource() {

		var dbName = Key.of( "derbyOnTheFly" );
		// Register the named datasource
		getInstance().getConfiguration().datasources.put(
		    Key.of( dbName ),
		    JDBCTestUtils.buildDatasourceConfig( dbName.getName() )
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
		        writeOutput( "SELECT * FROM developers ORDER BY id" );
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
		assertEquals( 4, results.size() );

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
		assertEquals( 3, results.size() );

		List<String> keys = results.getKeysAsStrings();
		assertEquals( keys, List.of( "QA", "Developer", "CEO" ) );

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

	@DisplayName( "It can insert a null" )
	@Test
	public void testNullQueryParam() {
		getInstance().executeSource(
		    """
		    <cfquery>
		        INSERT INTO developers (id, name, role )
		        VALUES (<cfqueryparam value="91" />, <cfqueryparam null="true" />, <cfqueryparam value="CEO" />)
		    </cfquery>
		    <cfquery name="result">
		        SELECT * FROM developers WHERE id = <cfqueryparam value="91" />
		    </cfquery>
		    """,
		    getContext(), BoxSourceType.CFTEMPLATE );
		assertThat( getVariables().get( result ) ).isInstanceOf( ortus.boxlang.runtime.types.Query.class );
		ortus.boxlang.runtime.types.Query query = getVariables().getAsQuery( result );
		assertEquals( 1, query.size() );

		IStruct ceo = query.getRowAsStruct( 0 );
		assertEquals( 91, ceo.get( "id" ) );
		assertNull( ceo.get( "name" ) );
		assertEquals( "CEO", ceo.get( "role" ) );
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

		assertThat( result ).containsKey( Key.sql );
		assertEquals( "SELECT * FROM developers WHERE role = 'Developer'", result.getAsString( Key.sql ) );

		assertThat( result ).containsKey( Key.cached );
		assertThat( result.getAsBoolean( Key.cached ) ).isEqualTo( false );

		assertThat( result ).containsKey( Key.sqlParameters );
		assertEquals( Array.of( "Developer" ), result.getAsArray( Key.sqlParameters ) );

		assertThat( result ).containsKey( Key.recordCount );
		assertEquals( 2, result.getAsInteger( Key.recordCount ) );

		assertThat( result ).containsKey( Key.columnList );
		assertEquals( "ID,NAME,ROLE,CREATEDAT", result.getAsString( Key.columnList ) );

		assertThat( result ).containsKey( Key.executionTime );
		assertThat( result.getAsLong( Key.executionTime ) ).isAtLeast( 0 );

		assertThat( result.containsKey( Key.generatedKey ) ).isEqualTo( false );
	}

	@DisplayName( "It closes connection on completion" )
	@Test
	public void testConnectionClose() {
		Integer initiallyActive = getDatasource().getPoolStats().getAsInteger( Key.of( "ActiveConnections" ) );
		getInstance().executeSource(
		    """
		      <bx:query name="result">
		      SELECT COUNT(*) FROM developers
		      </bx:query>
		    """,
		    getContext(), BoxSourceType.BOXTEMPLATE );
		Integer subsequentActive = getDatasource().getPoolStats().getAsInteger( Key.of( "ActiveConnections" ) );
		assertEquals( initiallyActive, subsequentActive );
	}

	@DisplayName( "It can return cached query results within the cache timeout" )
	@Test
	public void testQueryCaching() {
		getInstance().executeSource(
		    """
		       <bx:query name="result" cache="true" cacheTimeout="#createTimespan( 0, 0, 0, 2 )#" result="queryMeta" returnType="array">
		    SELECT id,name,role FROM developers WHERE role = <bx:queryparam value="Developer" />
		    </bx:query>
		       <bx:query name="result2" cache="true" cacheTimeout="#createTimespan( 0, 0, 0, 2 )#" result="queryMeta2" returnType="array">
		    SELECT id,name,role FROM developers WHERE role = <bx:queryparam value="Developer" />
		    </bx:query>
		       <bx:query name="result3" cache="true" cacheTimeout="#createTimespan( 0, 0, 0, 2 )#" result="queryMeta3" returnType="array">
		    SELECT id,name,role FROM developers WHERE role = <bx:queryparam value="Admin" />
		    </bx:query>, [ 'Admin
		       <bx:query name="result4" cache="false" cacheTimeout="#createTimespan( 0, 0, 0, 2 )#" result="queryMeta4" returnType="array">
		    SELECT id,name,role FROM developers WHERE role = <bx:queryparam value="Developer" />
		    </bx:query>
		       """,
		    getContext(), BoxSourceType.BOXTEMPLATE );
		Array	query1	= getVariables().getAsArray( result );
		Array	query2	= getVariables().getAsArray( Key.of( "result2" ) );
		Array	query3	= getVariables().getAsArray( Key.of( "result3" ) );
		Array	query4	= getVariables().getAsArray( Key.of( "result4" ) );

		// All 3 queries should have identical return values
		assertEquals( query1, query2 );
		assertEquals( query2, query4 );
		// query 3 should be a different, uncached result
		assertNotEquals( query1, query3 );

		// Query 1 should NOT be cached
		IStruct queryMeta = StructCaster.cast( getVariables().getAsStruct( Key.of( "queryMeta" ) ) );
		assertThat( queryMeta.getAsBoolean( Key.cached ) ).isEqualTo( false );

		// query 2 SHOULD be cached
		IStruct queryMeta2 = StructCaster.cast( getVariables().getAsStruct( Key.of( "queryMeta2" ) ) );
		assertThat( queryMeta2.getAsBoolean( Key.cached ) ).isEqualTo( true );

		// query 3 should NOT be cached because it has an additional param
		IStruct queryMeta3 = StructCaster.cast( getVariables().getAsStruct( Key.of( "queryMeta3" ) ) );
		assertThat( queryMeta3.getAsBoolean( Key.cached ) ).isEqualTo( false );

		// query 4 should NOT be cached because it strictly disallows it
		IStruct queryMeta4 = StructCaster.cast( getVariables().getAsStruct( Key.of( "queryMeta4" ) ) );
		assertThat( queryMeta4.getAsBoolean( Key.cached ) ).isEqualTo( false );
	}

	@DisplayName( "It can pass params as a struct in a query attribute" )
	@Test
	public void testParamAttribute() {
		getInstance().executeSource(
		    """
		        <bx:query name="result" params="#{ id : '1', again: { value : "77" } }#">
		        SELECT * FROM developers WHERE id = :id OR id = :again
		        </bx:query>
		    """,
		    getContext(), BoxSourceType.BOXTEMPLATE );
		assertThat( getVariables().get( result ) ).isInstanceOf( ortus.boxlang.runtime.types.Query.class );
		ortus.boxlang.runtime.types.Query query = getVariables().getAsQuery( result );
		assertEquals( 2, query.size() );
	}

	@DisplayName( "It throws if mutually exclusive param sources are used" )
	@Test
	public void testParamAttributeThrow() {
		IllegalArgumentException e = assertThrows( IllegalArgumentException.class, () -> getInstance().executeSource(
		    """
		        <bx:query name="result" params="#{ id : '1', id2: { value : "77" } }#">
		        SELECT * FROM developers WHERE id = <bx:queryparam value="1">
		        </bx:query>
		    """,
		    getContext(), BoxSourceType.BOXTEMPLATE ) );

		assertThat( e.getMessage() ).contains( "Cannot specify both query parameters in the body and as an attribute" );
		assertNull( getVariables().get( result ) );
	}
}
