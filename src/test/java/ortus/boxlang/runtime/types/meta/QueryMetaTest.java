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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Disabled;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.bifs.global.jdbc.BaseJDBCTest;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.IStruct;

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

	// @Disabled( "Disabled until implementation is complete" )
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
		assertTrue( $bx.meta instanceof IStruct );

		assertTrue( $bx.meta.containsKey( Key.sql ) );
		assertTrue( $bx.meta.containsKey( Key.sqlParameters ) );
		assertTrue( $bx.meta.containsKey( Key.recordCount ) );
		assertTrue( $bx.meta.containsKey( Key.columnList ) );
		assertTrue( $bx.meta.containsKey( Key.executionTime ) );
		assertTrue( $bx.meta.containsKey( Key.cached ) );
		assertTrue( $bx.meta.containsKey( Key.cacheProvider ) );
		assertTrue( $bx.meta.containsKey( Key.cacheKey ) );
		assertTrue( $bx.meta.containsKey( Key.cacheTimeout ) );
		assertTrue( $bx.meta.containsKey( Key.cacheLastAccessTimeout ) );

	}

}
