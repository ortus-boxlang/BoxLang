package ortus.boxlang.runtime.jdbc;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;

/**
 * Unit tests for the RETURNING clause detection used to surface RETURNING rows that a driver
 * hands back as generated keys ( see ExecutedQuery ). No database needed.
 */
public class PendingQueryReturningTest {

	static BoxRuntime	instance;
	static IBoxContext	context;

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
	}

	private static boolean hasReturning( String sql ) {
		return new PendingQuery( context, sql, new ArrayList<>() ).hasReturningClause();
	}

	@DisplayName( "It detects a RETURNING clause regardless of case and placement" )
	@Test
	public void testDetectsReturning() {
		assertThat( hasReturning( "INSERT INTO t (name) VALUES ('a') RETURNING id" ) ).isTrue();
		assertThat( hasReturning( "update t set name = 'b' where id = 1 returning id, name" ) ).isTrue();
		assertThat( hasReturning( "DELETE FROM t WHERE id = 1\n\tReturning *" ) ).isTrue();
		assertThat( hasReturning( "INSERT INTO t (name) VALUES ('a') RETURNING id;\n-- INSERT INTO t (name) VALUES ('b')" ) ).isTrue();
	}

	@DisplayName( "It ignores the word inside string literals, quoted identifiers and comments" )
	@Test
	public void testIgnoresLiteralsAndComments() {
		assertThat( hasReturning( "INSERT INTO t (name) VALUES ('returning is a word')" ) ).isFalse();
		assertThat( hasReturning( "INSERT INTO t (name) VALUES ('it''s returning') " ) ).isFalse();
		assertThat( hasReturning( "INSERT INTO t (\"returning\") VALUES ('x')" ) ).isFalse();
		assertThat( hasReturning( "INSERT INTO t (name) VALUES ('a') -- returning id" ) ).isFalse();
		assertThat( hasReturning( "INSERT INTO t (name) VALUES ('a') /* RETURNING\n id */" ) ).isFalse();
		assertThat( hasReturning( "SELECT returningdate FROM t" ) ).isFalse();
		assertThat( hasReturning( "SELECT * FROM t" ) ).isFalse();
	}

	@DisplayName( "It strips literals and comments but keeps the SQL tokens around them" )
	@Test
	public void testStripLiteralsAndComments() {
		assertThat( PendingQuery.stripLiteralsAndComments( "a 'x''y' b -- c\nd /* e */ f \"g\" h" ) ).isEqualTo( "a   b  \nd   f   h" );
		// unterminated literal or comment swallows the rest instead of throwing
		assertThat( PendingQuery.stripLiteralsAndComments( "a 'open" ) ).isEqualTo( "a  " );
		assertThat( PendingQuery.stripLiteralsAndComments( "a /* open" ) ).isEqualTo( "a  " );
	}
}
