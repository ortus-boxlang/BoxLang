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
package ortus.boxlang.runtime.bifs.global.jdbc;

import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.jdbc.ITransaction;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF

public class TransactionRollback extends TransactionBIF {

	/**
	 * Constructor
	 */
	public TransactionRollback() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, "string", Key.savepoint )
		};
	}

	/**
	 * Rollback the current transaction and discard all unpersisted queries.
	 * 
	 * <p>
	 * Only the changes made since the last <code>transactionSetSavepoint()</code> or <code>transactionCommit()</code> call will be discarded.
	 * <p>
	 * If no transaction is found in the current context, this method will throw an exception.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.savepoint String name of the savepoint to rollback to. If not provided, the entire transaction will be rolled back.
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		ITransaction	transaction	= getTransactionForContext( context );
		String			savepoint	= arguments.getAsString( Key.savepoint );
		if ( savepoint != null ) {
			transaction.rollback( Key.of( savepoint ) );
		} else {
			transaction.rollback();
		}
		return null;
	}

}
