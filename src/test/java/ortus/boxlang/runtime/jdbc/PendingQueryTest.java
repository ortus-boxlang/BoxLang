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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

/**
 * Tests for the SQL parameterization performed by {@link PendingQuery} when it
 * is constructed. These tests do not require a live database connection; the
 * named-parameter scanning happens eagerly in the constructor.
 */
public class PendingQueryTest {

	static BoxRuntime	instance;
	static IBoxContext	context;

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
	}

	/**
	 * Build a {@link PendingQuery} with the given SQL and named-parameter struct.
	 */
	private PendingQuery build( String sql, IStruct bindings ) {
		return new PendingQuery( context, sql, bindings, new QueryOptions( new Struct() ) );
	}

	/**
	 * Build a {@link PendingQuery} with the given SQL and positional-parameter array.
	 */
	private PendingQuery buildPositional( String sql, Array bindings ) {
		return new PendingQuery( context, sql, bindings, new QueryOptions( new Struct() ) );
	}

	@DisplayName( "It leaves PostgreSQL :: cast shorthand literal while still binding real named params" )
	@Test
	public void testPostgresCastShorthand() {
		PendingQuery query = build( "SELECT * FROM developers WHERE name::varchar = :name", Struct.of( "name", "Michael Born" ) );

		assertEquals( 1, query.getParameterValues().size() );
		assertThat( query.getParameterValues().get( 0 ) ).isEqualTo( "Michael Born" );
	}

	@DisplayName( "It leaves a trailing :: cast shorthand literal after a bound named param" )
	@Test
	public void testPostgresCastOfBoundParam() {
		PendingQuery query = build( "SELECT * FROM developers WHERE :id::varchar IS NOT NULL", Struct.of( "id", 42 ) );

		assertEquals( 1, query.getParameterValues().size() );
		assertThat( query.getParameterValues().get( 0 ) ).isEqualTo( 42 );
	}

	@DisplayName( "It still throws for a genuinely missing named param alongside :: cast shorthand" )
	@Test
	public void testMissingNamedParamWithCastShorthand() {
		DatabaseException e = assertThrows( DatabaseException.class,
		    () -> build( "SELECT * FROM developers WHERE name::varchar = :id", Struct.of( "name", "Michael Born" ) ) );

		assertThat( e.getMessage() ).isEqualTo( "Named parameter [:id] not provided to query." );
	}

	@DisplayName( "It leaves the PostgreSQL ?| JSON operator literal while binding real positional params" )
	@Test
	public void testPostgresJSONExistsAnyOperator() {
		PendingQuery query = buildPositional( "SELECT * FROM developers WHERE tags ?| ?", Array.of( "developer" ) );

		assertEquals( 1, query.getParameterValues().size() );
		assertThat( query.getParameterValues().get( 0 ) ).isEqualTo( "developer" );
	}

	@DisplayName( "It leaves the PostgreSQL ?& JSON operator literal while binding real positional params" )
	@Test
	public void testPostgresJSONExistsAllOperator() {
		PendingQuery query = buildPositional( "SELECT * FROM developers WHERE tags ?& ?", Array.of( "developer" ) );

		assertEquals( 1, query.getParameterValues().size() );
		assertThat( query.getParameterValues().get( 0 ) ).isEqualTo( "developer" );
	}

	@DisplayName( "It still binds a bare ? as a positional placeholder" )
	@Test
	public void testBareQuestionMarkPositional() {
		PendingQuery query = buildPositional( "SELECT * FROM developers WHERE id = ?", Array.of( 42 ) );

		assertEquals( 1, query.getParameterValues().size() );
		assertThat( query.getParameterValues().get( 0 ) ).isEqualTo( 42 );
	}
}
