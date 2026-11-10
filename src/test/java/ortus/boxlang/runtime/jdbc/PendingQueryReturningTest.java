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

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Struct;

public class PendingQueryReturningTest {

	private static IBoxContext context;

	@BeforeAll
	public static void setUp() {
		BoxRuntime runtime = BoxRuntime.getInstance( true );
		context = new ScriptingRequestBoxContext( runtime.getRuntimeContext() );
	}

	@DisplayName( "It detects an INSERT RETURNING clause" )
	@Test
	public void testDetectsInsertReturningClause() {
		assertThat( hasReturningClause( "INSERT INTO users (name) VALUES ('Michael') RETURNING id" ) ).isTrue();
	}

	@DisplayName( "It detects supported DML RETURNING statements" )
	@ParameterizedTest( name = "{index}: {0}" )
	@ValueSource( strings = {
	    "  insert into users (name) values ('Michael') returning id, name  ",
	    "UPDATE users SET name = 'Michael' WHERE id = 1 RETURNING id",
	    "DELETE FROM users WHERE id = 1 RETURNING *;",
	    "WITH selected AS (SELECT 1 AS id) INSERT INTO users (id) SELECT id FROM selected RETURNING id",
	    "WITH updated AS (UPDATE users SET name = 'Michael' RETURNING id) DELETE FROM users WHERE id IN (SELECT id FROM updated) RETURNING id",
	    "WITH RECURSIVE ids AS (SELECT 1 UNION ALL SELECT id + 1 FROM ids WHERE id < 2) INSERT INTO users (id) SELECT id FROM ids RETURNING id",
	    "INSERT INTO users (note) VALUES ($$a semicolon; and RETURNING text$$) RETURNING id; -- trailing comment",
	    "UPDATE users SET name = 'Michael' WHERE id = 1 RETURNING \"returning\""
	} )
	public void testDetectsSupportedDmlReturningStatements( String sql ) {
		assertThat( hasReturningClause( sql ) ).isTrue();
	}

	@DisplayName( "It detects RETURNING when the statement has bound parameters" )
	@Test
	public void testDetectsReturningWithBoundParameters() {
		PendingQuery query = new PendingQuery(
		    context,
		    "UPDATE users SET name = ? WHERE id = ? RETURNING id, name",
		    Array.of( "Michael", 42 ),
		    new QueryOptions( new Struct() )
		);

		assertThat( query.hasReturningClause() ).isTrue();
	}

	@DisplayName( "It ignores text that is not a supported top-level DML RETURNING clause" )
	@ParameterizedTest( name = "{index}: {0}" )
	@ValueSource( strings = {
	    "SELECT returning FROM users",
	    "SELECT 1 RETURNING id",
	    "INSERT INTO users (note) VALUES ('RETURNING id')",
	    "UPDATE users SET note = 'RETURNING id' WHERE id = 1",
	    "UPDATE users SET note = \"RETURNING id\" WHERE id = 1",
	    "UPDATE users SET note = $body$RETURNING id; DELETE FROM users$body$ WHERE id = 1",
	    "INSERT INTO users (\"returning\") VALUES (1)",
	    "UPDATE users SET returning = true WHERE id = 1",
	    "UPDATE users SET name = CASE WHEN active THEN returning ELSE name END WHERE id = 1",
	    "UPDATE users SET name = name WHERE active AND returning IS TRUE",
	    "UPDATE users SET name = coalesce(returning, name) WHERE id = 1",
	    "DELETE FROM users WHERE returning IS TRUE",
	    "INSERT INTO returning (id) VALUES (1)",
	    "INSERT /* RETURNING id */ INTO users (name) VALUES ('Michael')",
	    "INSERT INTO users (name) VALUES ('Michael') -- RETURNING id",
	    "INSERT INTO users (id) SELECT returning FROM source_rows",
	    "WITH inserted AS (INSERT INTO users (name) VALUES ('Michael') RETURNING id) SELECT * FROM inserted",
	    "INSERT INTO users (name) VALUES ('Michael') RETURNING id; SELECT 1",
	    "UPDATE users SET name = 'Michael'; DELETE FROM users RETURNING id"
	} )
	public void testIgnoresUnsupportedReturningText( String sql ) {
		assertThat( hasReturningClause( sql ) ).isFalse();
	}

	private static boolean hasReturningClause( String sql ) {
		return new PendingQuery( context, sql, new ArrayList<>() ).hasReturningClause();
	}
}
