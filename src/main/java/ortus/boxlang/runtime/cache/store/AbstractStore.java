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
package ortus.boxlang.runtime.cache.store;

import java.lang.reflect.InvocationTargetException;

import ortus.boxlang.runtime.cache.policies.ICachePolicy;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * The base implementation of a cache store in BoxLang.
 */
public abstract class AbstractStore {

	private static final String	POLICIES_PACKAGE	= "ortus.boxlang.runtime.cache.policies";
	private static final String	VALID_POLICIES		= "LRU|MRU|LFU|MFU|FIFO|LIFO|Random";

	/**
	 * The cache provider associated with this store
	 */
	protected ICacheProvider	provider;

	/**
	 * The configuration for the store
	 */
	protected IStruct			config;

	/**
	 * The Eviction policy we lazy load in.
	 */
	private ICachePolicy		policy;

	/**
	 * Get the configuration for the store
	 */
	public IStruct getConfig() {
		return this.config;
	}

	/**
	 * Get the associated cache provider
	 */
	public ICacheProvider getProvider() {
		return this.provider;
	}

	/**
	 * Get a policy for usage by the store.
	 *
	 * The policies are stored in the ortus.boxlang.runtime.cache.policies package.
	 *
	 * - LRU: Least Recently Used
	 * - MRU: Most Recently Used
	 * - LFU: Least Frequently Used
	 * - MFU: Most Frequently Used
	 * - FIFO: First In First Out
	 * - LIFO: Last In First Out
	 * - Random: Randomly evict objects
	 *
	 * You can also register your own policy by implementing the ICachePolicy interface.
	 */
	public ICachePolicy getPolicy() {

		if ( this.policy == null ) {
			this.policy = buildPolicy();
		}

		return this.policy;
	}

	/**
	 * Build the cache policy from the configuration
	 *
	 * @return The built policy
	 */
	private synchronized ICachePolicy buildPolicy() {
		Object thisPolicy = this.config.get( Key.evictionPolicy );

		// Is it a policy object?
		if ( thisPolicy instanceof ICachePolicy castedPolicy ) {
			return castedPolicy;
		}
		// else if it's a string
		else if ( thisPolicy instanceof String castedPolicy ) {
			// If the policy is not one of: LRU, MRU, LFU, MFU, FIFO, LIFO, Random then throw an exception
			if ( !castedPolicy.matches( VALID_POLICIES ) ) {
				throw new BoxRuntimeException( "The eviction policy is not a valid policy." + policy.toString() );
			}
			return buildPolicyByName( castedPolicy );
		}
		// Else throw an exception
		else {
			throw new BoxRuntimeException( "The eviction policy is not a valid policy." + policy.toString() );
		}
	}

	/**
	 * Get a policy by name from BoxLang's policies package.
	 *
	 * @param policyName The name of the policy
	 *
	 * @return The policy
	 */
	protected ICachePolicy buildPolicyByName( String policyName ) {
		// Access the class by name
		String targetPolicy = POLICIES_PACKAGE + "." + policyName;

		try {
			// Load the class
			Class<?> clazz = Class.forName( targetPolicy );
			if ( ICachePolicy.class.isAssignableFrom( clazz ) ) {
				// Create an instance of the class
				try {
					return ( ICachePolicy ) clazz.getDeclaredConstructor().newInstance();
				} catch ( IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e ) {
					throw new BoxRuntimeException( "Cannot call the constructor on the policy: " + policy, e );
				}
			} else {
				throw new BoxRuntimeException( "The policy does not implement ICachePolicy: " + policy );
			}
		} catch ( ClassNotFoundException | InstantiationException | IllegalAccessException e ) {
			// Log the error
			throw new BoxRuntimeException( "Unable to load the policy: " + policy, e );
		}
	}

}
