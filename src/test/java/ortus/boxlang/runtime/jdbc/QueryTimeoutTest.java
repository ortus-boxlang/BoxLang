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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.bifs.global.jdbc.BaseJDBCTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

public class QueryTimeoutTest extends BaseJDBCTest {

	@DisplayName( "It stores query timeout in QueryOptions from the public timeout key" )
	@Test
	public void testQueryTimeoutStorage() {
		IStruct			options			= Struct.of(
		    Key.timeout, 30,
		    Key.datasource, datasource
		);
		QueryOptions	queryOptions	= new QueryOptions( options );

		assertThat( queryOptions.queryTimeout ).isEqualTo( 30 );
	}

	@DisplayName( "It returns null for query timeout when not specified" )
	@Test
	public void testQueryTimeoutNull() {
		IStruct			options			= Struct.of(
		    Key.datasource, datasource
		);
		QueryOptions	queryOptions	= new QueryOptions( options );

		assertNull( queryOptions.queryTimeout );
	}

	@DisplayName( "It exports queryTimeout with the correct key in toStruct()" )
	@Test
	public void testQueryTimeoutExportKey() {
		IStruct			options			= Struct.of(
		    Key.timeout, 45,
		    Key.datasource, datasource
		);
		QueryOptions	queryOptions	= new QueryOptions( options );
		IStruct			exported		= queryOptions.toStruct();

		assertThat( exported.containsKey( Key.of( "queryTimeout" ) ) ).isTrue();
		assertThat( exported.getAsInteger( Key.of( "queryTimeout" ) ) ).isEqualTo( 45 );
	}

	@DisplayName( "It does not export setQueryTimeout in toStruct()" )
	@Test
	public void testNoSetQueryTimeoutKey() {
		IStruct			options			= Struct.of(
		    Key.timeout, 60,
		    Key.datasource, datasource
		);
		QueryOptions	queryOptions	= new QueryOptions( options );
		IStruct			exported		= queryOptions.toStruct();

		assertThat( exported.containsKey( Key.of( "setQueryTimeout" ) ) ).isFalse();
	}

	@DisplayName( "It exports maxRows with the correct key in toStruct()" )
	@Test
	public void testMaxRowsExportKey() {
		IStruct			options			= Struct.of(
		    Key.maxRows, 100,
		    Key.datasource, datasource
		);
		QueryOptions	queryOptions	= new QueryOptions( options );
		IStruct			exported		= queryOptions.toStruct();

		assertThat( exported.containsKey( Key.of( "maxRows" ) ) ).isTrue();
		assertThat( ( Long ) exported.get( Key.of( "maxRows" ) ) ).isEqualTo( 100L );
	}

	@DisplayName( "It does not export setMaxRows in toStruct()" )
	@Test
	public void testNoSetMaxRowsKey() {
		IStruct			options			= Struct.of(
		    Key.maxRows, 100,
		    Key.datasource, datasource
		);
		QueryOptions	queryOptions	= new QueryOptions( options );
		IStruct			exported		= queryOptions.toStruct();

		assertThat( exported.containsKey( Key.of( "setMaxRows" ) ) ).isFalse();
	}

	@DisplayName( "It handles zero timeout values" )
	@Test
	public void testZeroTimeout() {
		IStruct			options			= Struct.of(
		    Key.timeout, 0,
		    Key.datasource, datasource
		);
		QueryOptions	queryOptions	= new QueryOptions( options );

		assertThat( queryOptions.queryTimeout ).isEqualTo( 0 );
	}

	@DisplayName( "It handles negative timeout values" )
	@Test
	public void testNegativeTimeout() {
		IStruct			options			= Struct.of(
		    Key.timeout, -1,
		    Key.datasource, datasource
		);
		QueryOptions	queryOptions	= new QueryOptions( options );

		assertThat( queryOptions.queryTimeout ).isEqualTo( -1 );
	}

	@DisplayName( "It handles large timeout values" )
	@Test
	public void testLargeTimeout() {
		IStruct			options			= Struct.of(
		    Key.timeout, 3600,
		    Key.datasource, datasource
		);
		QueryOptions	queryOptions	= new QueryOptions( options );

		assertThat( queryOptions.queryTimeout ).isEqualTo( 3600 );
	}
}
