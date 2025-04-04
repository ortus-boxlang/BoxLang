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
package ortus.boxlang.runtime.types.exceptions;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

/**
 * This exception is thrown when a locking operation fails - either with a failure to obtain the lock or due to a timeout
 */
public class LockException extends BoxLangException {

	/**
	 * Name of affected lock (if the lock is unnamed, the value is "anonymous").
	 */
	protected String	lockName		= "";
	/**
	 * Operation that failed (Timeout, Create Mutex, or Unknown).
	 */
	protected String	lockOperation	= "";

	/**
	 * Constructor
	 *
	 * @param message  The message
	 * @param lockName The lock name
	 */
	public LockException( String message, String lockName ) {
		this( message, null, lockName, null, null );
	}

	/**
	 * Constructor
	 *
	 * @param message       The message
	 * @param lockName      The lock name
	 * @param lockOperation The lock operation
	 */
	public LockException( String message, String lockName, String lockOperation ) {
		this( message, null, lockName, lockOperation, null );
	}

	/**
	 * Constructor
	 *
	 * @param message       The message
	 * @param detail        The detail
	 * @param lockName      The lock name
	 * @param lockOperation The lock operation
	 * @param cause         The cause
	 */
	public LockException( String message, String detail, String lockName, String lockOperation, Throwable cause ) {
		super( message, detail, "lock", cause );
		this.lockName		= lockName;
		this.lockOperation	= lockOperation;
	}

	// getters

	public String getLockName() {
		return lockName;
	}

	public String getLockOperation() {
		return lockOperation;
	}

	public IStruct dataAsStruct() {
		IStruct result = super.dataAsStruct();
		result.put( Key.lockName, lockName );
		result.put( Key.lockOperation, lockOperation );
		return result;
	}

}
