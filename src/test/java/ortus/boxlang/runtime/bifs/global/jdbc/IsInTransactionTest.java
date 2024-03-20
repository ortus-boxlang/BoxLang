package ortus.boxlang.runtime.bifs.global.jdbc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;

public class IsInTransactionTest extends BaseJDBCTest {

	static Key result = new Key( "result" );

	@DisplayName( "It detects a surrounding transaction" )
	@Test
	public void testWithinTransaction() {
		instance.executeSource(
		    """
		    transaction{
		        variables.result = isWithinTransaction();
		    }
		    """,
		    context );
		assertTrue( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It detects no surrounding transaction" )
	@Test
	public void testNotWithinTransaction() {
		instance.executeSource(
		    """
		    transaction{
		    	queryExecute( "SELECT 1 FROM developers" )
		    }
		    variables.result = isWithinTransaction();
		    """,
		    context );
		assertFalse( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It detects a surrounding transaction" )
	@Test
	public void testInTransaction() {
		instance.executeSource(
		    """
		    transaction{
		        variables.result = isInTransaction();
		    }
		    """,
		    context );
		assertTrue( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It detects no surrounding transaction" )
	@Test
	public void testNotInTransaction() {
		instance.executeSource(
		    """
		    transaction{
		    	queryExecute( "SELECT 1 FROM developers" )
		    }
		    variables.result = isInTransaction();
		    """,
		    context );
		assertFalse( variables.getAsBoolean( result ) );
	}

}
