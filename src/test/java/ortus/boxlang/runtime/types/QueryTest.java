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
package ortus.boxlang.runtime.types;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Iterator;

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

public class QueryTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "Test Methods" )
	@Test
	void testMethods() {
		Query qry = new Query();
		assertThat( qry.size() ).isEqualTo( 0 );
		assertThat( qry.hasColumns() ).isEqualTo( false );

		qry.addColumn( Key.of( "foo" ), QueryColumnType.VARCHAR );
		assertThat( qry.hasColumns() ).isEqualTo( true );

		qry.addRow( new Object[] { "bar" } );
		assertThat( qry.size() ).isEqualTo( 1 );

		qry.addRow( Struct.of( Key.of( "foo" ), "brad" ) );
		assertThat( qry.size() ).isEqualTo( 2 );

		qry.addColumn( Key.of( "col2" ), QueryColumnType.INTEGER );
		assertThat( qry.getColumns().size() ).isEqualTo( 2 );

		qry.addRow( Struct.of( Key.of( "foo" ), "luis", "col2", 42 ) );
		assertThat( qry.size() ).isEqualTo( 3 );

		qry.setCell( Key.of( "col2" ), 0, 100 );
		qry.setCell( Key.of( "col2" ), 1, 101 );
		for ( IStruct row : qry ) {
			assertThat( row.get( Key.of( "foo" ) ) ).isNotNull();
			assertThat( row.get( Key.of( "col2" ) ) ).isNotNull();
		}

		Object[] rowData = qry.getColumnData( Key.of( "foo" ) );
		assertThat( rowData.length ).isEqualTo( 3 );
		assertThat( rowData[ 0 ] ).isEqualTo( "bar" );
		assertThat( rowData[ 1 ] ).isEqualTo( "brad" );
		assertThat( rowData[ 2 ] ).isEqualTo( "luis" );

		rowData = qry.getColumnData( Key.of( "col2" ) );
		assertThat( rowData.length ).isEqualTo( 3 );
		assertThat( rowData[ 0 ] ).isEqualTo( 100 );
		assertThat( rowData[ 1 ] ).isEqualTo( 101 );
		assertThat( rowData[ 2 ] ).isEqualTo( 42 );

		Array colData = qry.getColumnDataAsArray( Key.of( "col2" ) );
		assertThat( colData.size() ).isEqualTo( 3 );
		assertThat( colData.get( 0 ) ).isEqualTo( 100 );
		assertThat( colData.get( 1 ) ).isEqualTo( 101 );
		assertThat( colData.get( 2 ) ).isEqualTo( 42 );

		assertThat( qry.dereference( context, Key.recordCount, false ) ).isEqualTo( 3 );
		assertThat( qry.dereference( context, Key.columnList, false ) ).isEqualTo( "foo,col2" );
		assertThat( qry.dereference( context, Key.currentRow, false ) ).isEqualTo( 1 );

		assertThat( qry.dereference( context, Key.of( "foo" ), false ) ).isInstanceOf( QueryColumn.class );
		assertThat( context.unwrapQueryColumn( qry.dereference( context, Key.of( "foo" ), false ) ) ).isEqualTo( "bar" );
		assertThat( qry.assign( context, Key.of( "foo" ), "gavin" ) ).isEqualTo( "gavin" );
		assertThat( context.unwrapQueryColumn( qry.dereference( context, Key.of( "foo" ), false ) ) ).isEqualTo( "gavin" );
	}

	@DisplayName( "Test Query Iterator" )
	@Test
	void testQueryIterator() {
		Query qry = new Query();
		assertThat( qry.size() ).isEqualTo( 0 );
		assertThat( qry.hasColumns() ).isEqualTo( false );
		assertThat( qry.iterator().hasNext() ).isEqualTo( false );

		qry.addColumn( Key.of( "name" ), QueryColumnType.VARCHAR );
		assertThat( qry.hasColumns() ).isEqualTo( true );

		qry.addRow( new Object[] { "sana" } );
		assertThat( qry.size() ).isEqualTo( 1 );

		assertThat( qry.iterator().hasNext() ).isEqualTo( true );

		qry.addRow( new Object[] { "harris" } );
		assertThat( qry.size() ).isEqualTo( 2 );

		assertInstanceOf( Iterator.class, qry.iterator() );

		Array stArray = qry.toArrayOfStructs();
		assertInstanceOf( Array.class, stArray );
		assertThat( stArray.size() ).isEqualTo( 2 );
	}

	@DisplayName( "Test get metadata" )
	@Test
	void testGetMetadata() {
		// Build a query with metadata
		Query qry = new Query();
		qry.addColumn( Key.of( "name" ), QueryColumnType.VARCHAR );
		qry.addColumn( Key.of( "age" ), QueryColumnType.INTEGER );
		qry.addRow( Struct.of( Key.of( "name" ), "sana", Key.of( "age" ), 30 ) );
		qry.addRow( Struct.of( Key.of( "name" ), "harris", Key.of( "age" ), 25 ) );
		assertThat( qry.size() ).isEqualTo( 2 );
		IStruct metadata = qry.getMetaData();
		assertThat( metadata ).isNotNull();
		assertThat( metadata.get( Key.executionTime ) ).isEqualTo( 0 );
		assertThat( metadata.get( Key.cached ) ).isEqualTo( false );
		assertThat( metadata.get( Key.cacheKey ) ).isNull();
		assertThat( metadata.get( Key.cacheProvider ) ).isNull();
		assertThat( metadata.get( Key.cacheTimeout ) ).isEqualTo( java.time.Duration.ZERO );
		assertThat( metadata.get( Key.cacheLastAccessTimeout ) ).isEqualTo( java.time.Duration.ZERO );
		assertThat( metadata.get( Key.recordCount ) ).isEqualTo( 2 );
		assertThat( metadata.get( Key.columnList ) ).isEqualTo( qry.getColumnList() );
		assertThat( metadata.get( Key._HASHCODE ) ).isEqualTo( qry.hashCode() );
		assertThat( metadata.getAsStruct( Key.columnMetadata ).size() ).isEqualTo( 2 );
	}

	@DisplayName( "Can create fromArray method" )
	@Test
	void testFromArray() {
		Array	columnNames	= Array.of( "name", "age" );
		Array	columnTypes	= Array.of( "varchar", "integer" );
		Array	rowData		= Array.of(
		    Array.of( "sana", 30 ),
		    Array.of( "harris", 25 )
		);
		Query	qry			= Query.fromArray( columnNames, columnTypes, rowData );
		assertThat( qry.size() ).isEqualTo( 2 );
		assertThat( qry.getColumnList() ).isEqualTo( "name,age" );
		assertThat( qry.getRowAsStruct( 0 ).getAsString( Key.of( "name" ) ) ).isEqualTo( "sana" );
		assertThat( qry.getRowAsStruct( 1 ).getAsString( Key.of( "name" ) ) ).isEqualTo( "harris" );

		// Assert using the getColumns()
		assertThat( qry.getColumns().get( Key.of( "name" ) ).getType() ).isEqualTo( QueryColumnType.VARCHAR );
		assertThat( qry.getColumns().get( Key.of( "age" ) ).getType() ).isEqualTo( QueryColumnType.INTEGER );
		// hasColumn()
		assertThat( qry.hasColumn( Key.of( "name" ) ) ).isTrue();
		assertThat( qry.hasColumn( Key.of( "age" ) ) ).isTrue();
		assertThat( qry.hasColumn( Key.of( "nonexistent" ) ) ).isFalse();
		// getColumnData()
		assertThat( qry.getColumnData( Key.of( "name" ) ).length ).isEqualTo( 2 );
		assertThat( qry.getColumnData( Key.of( "name" ) )[ 0 ] ).isEqualTo( "sana" );
		assertThat( qry.getColumnData( Key.of( "name" ) )[ 1 ] ).isEqualTo( "harris" );
		assertThat( qry.getColumnData( Key.of( "age" ) ).length ).isEqualTo( 2 );
		assertThat( qry.getColumnData( Key.of( "age" ) )[ 0 ] ).isEqualTo( 30 );
		assertThat( qry.getColumnData( Key.of( "age" ) )[ 1 ] ).isEqualTo( 25 );
		// getColumnDataAsArray()
		assertThat( qry.getColumnDataAsArray( Key.of( "name" ) ).size() ).isEqualTo( 2 );
		assertThat( qry.getColumnDataAsArray( Key.of( "name" ) ).get( 0 ) ).isEqualTo( "sana" );
		assertThat( qry.getColumnDataAsArray( Key.of( "name" ) ).get( 1 ) ).isEqualTo( "harris" );
		assertThat( qry.getColumnDataAsArray( Key.of( "age" ) ).size() ).isEqualTo( 2 );
		assertThat( qry.getColumnDataAsArray( Key.of( "age" ) ).get( 0 ) ).isEqualTo( 30 );
		assertThat( qry.getColumnDataAsArray( Key.of( "age" ) ).get( 1 ) ).isEqualTo( 25 );
	}

	@DisplayName( "Add 100 rows to a query synchronously " )
	@Test
	void testAdd100Rows() {
		// Create an Array with 100 rows, each with a Struct of data
		Query qry = new Query();
		qry.addColumn( Key.of( "id" ), QueryColumnType.INTEGER );
		qry.addColumn( Key.of( "name" ), QueryColumnType.VARCHAR );

		for ( int i = 1; i <= 100; i++ ) {
			qry.addRow( Struct.of( Key.of( "id" ), i, Key.of( "name" ), "Name " + i ) );
		}

		assertThat( qry.size() ).isEqualTo( 100 );
		assertThat( qry.getColumnList() ).isEqualTo( "id,name" );
	}

	@DisplayName( "Add 100 rows to a query in parallel" )
	@Test
	void testAdd100RowsInParallel() {
		// Create an Array with 100 rows, each with a Struct of data
		Query qry = new Query();
		qry.addColumn( Key.of( "id" ), QueryColumnType.INTEGER );
		qry.addColumn( Key.of( "name" ), QueryColumnType.VARCHAR );

		// Create an array of 100 rows
		Array rows = Array.of();
		for ( int i = 1; i <= 100; i++ ) {
			rows.add( Struct.of( Key.of( "id" ), i, Key.of( "name" ), "Name " + i ) );
		}
		// Add rows to the query in parallel
		rows.parallelStream().forEach( row -> qry.addRow( ( IStruct ) row ) );

		assertThat( qry.size() ).isEqualTo( 100 );
		assertThat( qry.getColumnList() ).isEqualTo( "id,name" );
	}

	@DisplayName( "Test concurrency modifications with a dataset of 1000 rows" )
	@Test
	void testConcurrentModifications() {
		// Create a query with 1000 rows
		Query qry = new Query();
		qry.addColumn( Key.of( "id" ), QueryColumnType.INTEGER );
		qry.addColumn( Key.of( "name" ), QueryColumnType.VARCHAR );

		// Create an array of 1000 rows
		Array rows = Array.of();
		for ( int i = 1; i <= 1000; i++ ) {
			rows.add( Struct.of( Key.of( "id" ), i, Key.of( "name" ), "Name " + i ) );
		}

		// Now do concurrent modifications, add rows, update rows, and remove rows
		rows.parallelStream().forEach( row -> {
			qry.addRow( ( IStruct ) row );
			synchronized ( qry ) {
				if ( qry.size() % 100 == 0 ) {
					qry.setCell( Key.of( "name" ), qry.size() - 1, "Updated Name " + qry.size() );
				}
			}
		} );
		assertThat( qry.size() ).isEqualTo( 1000 );
		assertThat( qry.getColumnList() ).isEqualTo( "id,name" );
	}

}
