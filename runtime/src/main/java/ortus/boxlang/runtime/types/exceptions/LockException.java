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

/**
 * Base exception for all database-related errors
 */
public class LockException extends BoxLangException {

	public static final Key	LockNameKey			= Key.of( "LockName" );
	public static final Key	LockOperationKey	= Key.of( "LockOperation" );

	/**
	 * Name of affected lock (if the lock is unnamed, the value is "anonymous").
	 */
	public String			lockName			= "";
	/**
	 * Operation that failed (Timeout, Create Mutex, or Unknown).
	 */
	public String			lockOperation		= "";

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
		super( message, "lock", cause );
		this.detail			= detail;
		this.lockName		= lockName;
		this.lockOperation	= lockOperation;
	}

}
