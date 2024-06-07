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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class QueryTest {

	private IBoxContext context = new ScriptingRequestBoxContext();

	@DisplayName( "Test Constructor" )
	@Test
	void testConstructors() {
		IBoxContext	ctx	= new ScriptingRequestBoxContext();
		Query		qry	= new Query();
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
		assertThat( ctx.unwrapQueryColumn( qry.dereference( context, Key.of( "foo" ), false ) ) ).isEqualTo( "bar" );
		assertThat( qry.assign( context, Key.of( "foo" ), "gavin" ) ).isEqualTo( "gavin" );
		assertThat( ctx.unwrapQueryColumn( qry.dereference( context, Key.of( "foo" ), false ) ) ).isEqualTo( "gavin" );
	}

	@DisplayName( "Test Query Iterator" )
	@Test
	void testQueryIterator() {
		IBoxContext	ctx	= new ScriptingRequestBoxContext();
		Query		qry	= new Query();
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

		Array stArray = qry.toStructArray();
		assertInstanceOf( Array.class, stArray );
		assertThat( stArray.size() ).isEqualTo( 2 );
	}

	@DisplayName( "Test Query Iterator Change access modifier" )
	@Test
	void testQueryIteratorThreadSafety() throws InterruptedException {
		IBoxContext	ctx	= new ScriptingRequestBoxContext();
		Query		qry	= new Query();
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

		// Thread 1 will add elements in query data (Query data as List of arrays)
		Thread t1 = new Thread( new Runnable() {

			public void run() {
				for ( int i = 0; i <= 3; i++ ) {
					try {
						Thread.sleep( 1000 );
					} catch ( InterruptedException e ) {
						// e.printStackTrace();
					}
					qry.addRow( new Object[] { "harris" + i } );
				}
			}
		}, "thread-1" );
		t1.start();

		// Thread 2 will iterate on data (Query data as List of arrays)
		Thread t2 = new Thread( new Runnable() {

			public void run() {
				// now get Iterator it should have only 2 records at the time of instantiation
				Iterator<IStruct>	it			= qry.iterator();
				int					totalMatch	= 0;
				while ( it.hasNext() ) {
					try {
						Thread.sleep( 100 );
					} catch ( InterruptedException e ) {
						// e.printStackTrace();
					}
					System.out.println( "thread2 " + it.next() );
					totalMatch++;
				}

				if ( totalMatch > 2 )
					throw new BoxRuntimeException( "Original Query changed while iterating the records" );
			}
		}, "thread-2" );
		t2.start();

		while ( t1.isAlive() ) {
			TimeUnit.SECONDS.sleep( 1L );
		}

		assertThat( t2.isInterrupted() ).isEqualTo( false );
		Array stArray = qry.toStructArray();
		// it should match with 6 finally
		assertThat( stArray.size() ).isEqualTo( 6 );
	}

}
