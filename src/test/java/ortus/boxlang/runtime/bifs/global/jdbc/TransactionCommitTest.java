package ortus.boxlang.runtime.bifs.global.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class TransactionCommitTest extends BaseJDBCTest {

	static Key result = new Key( "result" );

	@DisplayName( "It throws if there's no surrounding transaction" )
	@Test
	public void testNoTransactionContext() {
		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> instance.executeStatement( "transactionCommit()" ) );
		assertEquals( "Transaction not started; Please place this method call inside a transaction{} block.", e.getMessage() );
	}

}
