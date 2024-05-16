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
package TestCases.phase2;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;

public class QueryTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "Query" )
	@Disabled( "Issue with member function vs java method" )
	@Test
	public void testQuery() {

		// @formatter:off
		instance.executeSource(
		    """
				import java:ortus.boxlang.runtime.scopes.Key;
				import java:ortus.boxlang.runtime.types.QueryColumnType;
				import java:ortus.boxlang.runtime.types.Query;

				qry = new java:Query();
				qry.addColumn( Key.of( "col1" ), QueryColumnType.VARCHAR )
				qry.addColumn( Key.of( "col2" ), QueryColumnType.INTEGER )
				qry.addRow( [ "brad", 1000 ] )
				qry.addRow( [ "luis", 2000 ] )
				recordCount = qry.recordCount
		        i=0
		        println( qry.recordCount )
		        firstCol = qry.col1;
				colavg = arrayAvg( qry.col2 )
				collen = len( qry.col2 )
				colList = qry.columnList;
				for( row in qry ) {
				variables[ "row#++i#" ]=row
					println( row )
					println( qry.col1 )
					println( qry.col2 )
					println( qry.currentRow )
				}
				result = qry
			""",
		   	context
		);
		// @formatter:on

		assertThat( variables.getAsQuery( result ) instanceof Query ).isEqualTo( true );
		assertThat( variables.getAsInteger( Key.of( "recordcount" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsStruct( Key.of( "row1" ) ) instanceof IStruct ).isEqualTo( true );
		assertThat( variables.getAsStruct( Key.of( "row2" ) ) instanceof IStruct ).isEqualTo( true );
		assertThat( variables.getAsString( Key.of( "firstCol" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( Key.of( "colavg" ) ) ).isEqualTo( 1500 );
		assertThat( variables.get( Key.of( "collen" ) ) ).isEqualTo( 4 );
		assertThat( variables.get( Key.of( "colList" ) ) ).isEqualTo( "col1,col2" );
	}

	@DisplayName( "Query Column assignment" )
	@Disabled( "Issue with member function vs java method" )
	@Test
	public void testColumnAssignemtnQuery() {

		// @formatter:off
		instance.executeSource(
		    """
				import java:ortus.boxlang.runtime.scopes.Key;
				import java:ortus.boxlang.runtime.types.QueryColumnType;
				import java:ortus.boxlang.runtime.types.Query;

				qry = new java:Query();
				qry.addColumn( Key.of( "col1" ), QueryColumnType.VARCHAR )
				qry.addColumn( Key.of( "col2" ), QueryColumnType.INTEGER )
				qry.addRow( [ "brad", 1000 ] )
				qry.addRow( [ "luis", 2000 ] )
				recordCount = qry.recordCount
				i=0
				println( qry.recordCount )

				colavg = arrayAvg( qry.col2 )
				collen = len( qry.col2 )
				colList = qry.columnList;
				for( row in qry ) {
					variables[ "row#++i#" ]=row
					println( row )
					println( qry.col1 )
					println( qry.col2 )
					println( qry.currentRow )
				}
				qry.col1[1] = "test"
				firstCol = qry.col1;
				result = qry
			""",
		    context );
		// @formatter:on

		assertThat( variables.getAsQuery( result ) instanceof Query ).isEqualTo( true );
		assertThat( variables.getAsInteger( Key.of( "recordcount" ) ) ).isEqualTo( 2 );
		assertThat( variables.getAsStruct( Key.of( "row1" ) ) instanceof IStruct ).isEqualTo( true );
		assertThat( variables.getAsStruct( Key.of( "row2" ) ) instanceof IStruct ).isEqualTo( true );
		assertThat( variables.getAsString( Key.of( "firstCol" ) ) ).isEqualTo( "test" );
		assertThat( variables.get( Key.of( "colavg" ) ) ).isEqualTo( 1500 );
		assertThat( variables.get( Key.of( "collen" ) ) ).isEqualTo( 4 );
		assertThat( variables.get( Key.of( "colList" ) ) ).isEqualTo( "col1,col2" );
	}

}
