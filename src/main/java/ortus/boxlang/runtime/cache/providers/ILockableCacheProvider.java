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
package ortus.boxlang.runtime.cache.providers;

/**
 * Interface for cache providers which offer distributed locking functionality.
 */
public interface ILockableCacheProvider {

	/**
	 * Acquire a distributed lock for the given key.
	 * 
	 * @param key                  The key to lock.
	 * @param acquireTimeoutMillis The maximum time to wait to acquire the lock in milliseconds.
	 * @param expiryTimeMillis     The time after which the lock will expire in milliseconds.
	 * 
	 * @return True if the lock was successfully acquired, false otherwise.
	 */
	ILock acquireLock( String lockKey, int acquireTimeoutMillis,
	    int expiryTimeMillis );

	/**
	 * Renew a distributed lock for the given key.
	 * 
	 * @param key              The key to renew the lock for.
	 * @param expiryTimeMillis The new expiry time for the lock in milliseconds.
	 * 
	 * @return True if the lock was successfully renewed, false otherwise.
	 */
	boolean renewLock( ILock lock, int expiryTimeMillis );

	/**
	 * Release a distributed lock for the given key.
	 * 
	 * @param lock The lock to release.
	 */
	void releaseLock( ILock lock );

}
