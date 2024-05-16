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
import ortus.boxlang.runtime.jdbc.Transaction;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
public class TransactionSetSavepoint extends TransactionBIF {

	/**
	 * Constructor
	 */
	public TransactionSetSavepoint() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.savepoint )
		};
	}

	/**
	 * Sets a savepoint in the current transaction. This savepoint can then be a rollback point when executing a rollback via `transactionRollback(
	 * "mySavepointName" )`.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.savepoint Specify a savepoint name.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Transaction transaction = getTransactionForContext( context );

		transaction.setSavepoint( arguments.getAsString( Key.savepoint ) );
		return null;
	}

}
