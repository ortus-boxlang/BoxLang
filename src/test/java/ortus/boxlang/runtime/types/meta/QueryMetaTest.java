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
package ortus.boxlang.runtime.types.meta;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.bifs.global.jdbc.BaseJDBCTest;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;

public class QueryMetaTest extends BaseJDBCTest {

	static Key result = new Key( "result" );

	@DisplayName( "Test basic query meta" )
	@Test
	void testQueryMeta() {

		Query		query	= new Query();
		QueryMeta	$bx		= ( QueryMeta ) Referencer.get( getContext(), query, BoxMeta.key, false );

		assertThat( $bx.$class ).isEqualTo( Query.class );
		assertTrue( $bx.meta instanceof IStruct );

	}

	@DisplayName( "Test JDBC query meta" )
	@Test
	void testJDBCQueryMeta() {

		getInstance().executeSource(
		    """
		    result = queryExecute( "SELECT * FROM developers ORDER BY id" );
		    """,
		    getContext() );
		assertThat( getVariables().get( result ) ).isInstanceOf( Query.class );
		Query		query	= getVariables().getAsQuery( result );
		QueryMeta	$bx		= ( QueryMeta ) Referencer.get( getContext(), query, BoxMeta.key, false );

		assertThat( $bx.$class ).isEqualTo( Query.class );
		assertThat( $bx.meta ).isInstanceOf( IStruct.class );

		assertThat( $bx.meta ).containsKey( Key.sql );
		assertThat( $bx.meta ).containsKey( Key.sqlParameters );
		assertThat( $bx.meta ).containsKey( Key.recordCount );
		assertThat( $bx.meta ).containsKey( Key.columnList );
		assertThat( $bx.meta ).containsKey( Key.executionTime );
		assertThat( $bx.meta ).containsKey( Key.cached );
		assertThat( $bx.meta ).containsKey( Key.cacheProvider );
		assertThat( $bx.meta ).containsKey( Key.cacheKey );
		assertThat( $bx.meta ).containsKey( Key.cacheTimeout );
		assertThat( $bx.meta ).containsKey( Key.cacheLastAccessTimeout );

	}

}
