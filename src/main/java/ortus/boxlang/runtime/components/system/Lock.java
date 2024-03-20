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
package ortus.boxlang.runtime.components.system;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.LockException;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( requiresBody = true )
public class Lock extends Component {

	private ConcurrentHashMap<String, WeakReference<ReentrantReadWriteLock>>	lockMap	= new ConcurrentHashMap<>();
	private ReferenceQueue<ReentrantReadWriteLock>								queue	= new ReferenceQueue<>();

	public Lock() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key._NAME, "string", Set.of( Validator.NON_EMPTY ) ),
		    new Attribute( Key.scope, "string" ),
		    new Attribute( Key.type, "string", "exclusive", Set.of( Validator.valueOneOf( "readonly", "exclusive" ) ) ),
		    new Attribute( Key.timeout, "Integer", Set.of( Validator.REQUIRED, Validator.min( 1 ) ) ),
		    new Attribute( Key.throwOnTimeout, "boolean", true )
			// Lucee supports a "result" attribute, but it doens't seem very useful and its docs don't even seem to match its implementation!.
			// We can add it if it's really needed.
		};
	}

	/**
	 * Ensures the integrity of shared data. Instantiates the following kinds of locks:
	 * 
	 * - Exclusive allows single-thread access to the CFML constructs
	 * - Read-only allows multiple requests to access CFML constructs
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 * 
	 * @attribute.name Lock name. Mutually exclusive with the scope attribute. Only one request can execute the code within a lock component with a given
	 *                 name
	 *                 at a time. Cannot be an empty string.
	 * 
	 * @attribute.scope Lock scope. Mutually exclusive with the name attribut Lock name. Only one request in the specified scope can execute the code
	 *                  within this component (or within any other lock component with the same lock scope scope) at a time.
	 * 
	 * @attribute.type readOnly: lets more than one request read shared data. exclusive: lets one request read or write shared data.
	 * 
	 * @attribute.timeout Maximum length of time, in seconds, to wait to obtain a lock. If lock is obtained, tag execution continues. Otherwise, behavior
	 *                    depends on throwOnTimeout attribute value.
	 * 
	 * @attribute.throwOnTimeout True: if lock is not obtained within the timeout period, a runtime exception is thrown. False: if lock is not obtained,
	 *                           the body of the component is skipped and execution continues without running the statements in the component.
	 * 
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		String	type			= attributes.getAsString( Key.type ).toLowerCase();
		String	name			= attributes.getAsString( Key._NAME );
		String	scope			= attributes.getAsString( Key.scope );
		Integer	timeout			= attributes.getAsInteger( Key.timeout );
		Boolean	throwOnTimeout	= attributes.getAsBoolean( Key.throwOnTimeout );

		String	lockName;

		if ( name != null ) {
			lockName = name.toLowerCase();
		} else if ( scope != null ) {
			lockName = "scope_lock_" + context.getScopeNearby( Key.of( scope ) ).getLockName();
		} else {
			throw new BoxRuntimeException( "Lock requires either a 'name' or 'scope' attribute to be provided." );
		}
		ReentrantReadWriteLock				lock		= getLockByName( lockName );
		ReentrantReadWriteLock.ReadLock		readLock	= lock.readLock();
		ReentrantReadWriteLock.WriteLock	writeLock	= lock.writeLock();

		java.util.concurrent.locks.Lock		lockToUse	= null;
		try {
			// Will be set to false if we time out
			boolean acquired;
			if ( type.equals( "readonly" ) ) {
				acquired	= readLock.tryLock( timeout, TimeUnit.SECONDS );
				lockToUse	= readLock;
			} else if ( type.equals( "exclusive" ) ) {
				acquired	= writeLock.tryLock( timeout, TimeUnit.SECONDS );
				lockToUse	= writeLock;
			} else {
				// This will never happen based on the attribute validation, but the compiler doens't know that so it wants this
				throw new BoxRuntimeException( "Lock type [" + type + "] is not supported" );
			}

			if ( !acquired ) {
				if ( throwOnTimeout ) {
					throw new LockException( "Timeout of [" + timeout + "] seconds reached while waiting to acquire lock [" + lockName + "]", lockName,
					    "timeout" );
				} else {
					// No need to release anything, because we never aquired it!s
					return DEFAULT_RETURN;
				}
			}

			try {
				// process the body
				BodyResult bodyResult = processBody( context, body );
				// IF there was a return statement inside our body, we early exit now
				if ( bodyResult.isEarlyExit() ) {
					return bodyResult;
				}
				return DEFAULT_RETURN;
			} finally {
				// unlock the lock
				lockToUse.unlock();
			}

		} catch ( InterruptedException e ) {
			// This doesn't apply to the lock timing out. This just means our current thread was interuppted while waiting for the lock
			throw new LockException( "Interrupted while waiting for lock", "", lockName, "interrupted", e );
		}
	}

	/**
	 * Get the lock to use. A soft reference store will allow unused locks to be garbage collected
	 * 
	 * @param lockName The name of the lock to get
	 * 
	 * @return The lock to use
	 */
	private ReentrantReadWriteLock getLockByName( String lockName ) {
		cleanUp(); // Clean up garbage collected references before getting the lock

		// For high concurrency, this opts for possibly attempting to create the lock more than once for the
		// benifit of never locking the entire map.
		while ( true ) {
			WeakReference<ReentrantReadWriteLock>	lockRef	= lockMap.computeIfAbsent( lockName,
			    key -> new WeakReference<>( new ReentrantReadWriteLock(), queue ) );
			ReentrantReadWriteLock					lock	= lockRef.get();

			if ( lock != null ) {
				return lock;
			}

			// If we reach here, it means the WeakReference was garbage collected between the computeIfAbsent and get calls.
			// We remove the entry from the map and try again.
			lockMap.remove( lockName, lockRef );
		}
	}

	private void cleanUp() {
		Reference<? extends ReentrantReadWriteLock> ref;
		while ( ( ref = queue.poll() ) != null ) {
			lockMap.values().remove( ref );
		}
	}
}
