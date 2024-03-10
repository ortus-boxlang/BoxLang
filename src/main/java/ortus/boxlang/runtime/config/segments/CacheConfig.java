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
package ortus.boxlang.runtime.config.segments;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * A BoxLang cache configuration
 * This is a configuration segment for a cache engine
 *
 * The default name is 'default'
 * The default provider is 'BoxCacheProvider'
 * The default properties are based on the DEFAULTS struct
 */
public class CacheConfig {

	/**
	 * The name of the cache engine, default is ='default'
	 */
	public Key					name		= Key._DEFAULT;

	/**
	 * The default cache engine provider is BoxCacheProvider
	 */
	public Key					provider	= Key.boxCacheProvider;

	/**
	 * The properties for the cache engine, based on {@code DEFAULTS}
	 */
	public IStruct				properties	= new Struct( DEFAULTS );

	/**
	 * BoxLang Cache Provider Defaults
	 */
	public static final IStruct	DEFAULTS	= Struct.of(
	    // How many to evict at a time once a policy is triggered
	    "evictCount", 1,
	    // The eviction policy to use: Least Recently Used
	    // Other policies are: LRU, LFU, FIFO, LIFO, RANDOM
	    "evictionPolicy", "LRU",
	    // The free memory percentage threshold to trigger eviction
	    // 0 = disabled, 1-100 = percentage of available free memory in heap
	    // If the threadhold is reached, the eviction policy is triggered
	    "freeMemoryPercentageThreshold", 0,
	    // The maximum number of objects to store in the cache
	    "maxObjects", 1000,
	    // The maximum in seconds to keep an object in the cache since it's last access
	    // So if an object is not accessed in this time or greater, it will be removed from the cache
	    "defaultLastAccessTimeout", 30 * 60,
	    // The maximum time in seconds to keep an object in the cache regardless if it's used or not
	    // A default timeout of 0 = never expire, careful with this setting
	    "defaultTimeout", 60 * 60,
	    // The object store to use to store the objects.
	    // The default is a ConcurrentStore which is a memory sensitive store
	    "objectStore", "ConcurrentStore",
	    // The frequency in seconds to check for expired objects and expire them using the policy
	    // This creates a BoxLang task that runs every X seconds to check for expired objects
	    "reapFrequency", 10,
	    // If enabled, the last access timeout will be reset on every access
	    // This means that the last access timeout will be reset to the defaultLastAccessTimeout on every access
	    // Usually for session caches or to simulate a session
	    "resetTimeoutOnAccess", false,
	    // If enabled, the last access timeout will be used to evict objects from the cache
	    "useLastAccessTimeouts", true
	);

	/**
	 * Logger
	 */
	private static final Logger	logger		= LoggerFactory.getLogger( CacheConfig.class );

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Default Empty Constructor
	 */
	public CacheConfig() {
		// Default all things
	}

	/**
	 * Constructor by name, provider and properties
	 *
	 * @param name       The key name of the cache engine
	 * @param provider   The name of the cache provider
	 * @param properties The properties of the cache engine
	 */
	public CacheConfig( Key name, Key provider, IStruct properties ) {
		this.name		= name;
		this.provider	= provider;
		processProperties( properties );
	}

	/**
	 * Constructor by name and properties
	 *
	 * @param name       The key name of the cache engine
	 * @param properties The properties of the cache engine
	 */
	public CacheConfig( Key name, IStruct properties ) {
		this( name, Key.boxCacheProvider, properties );
	}

	/**
	 * Constructor by name of a boxCacheProvider
	 *
	 * @param name The key name of the cache engine
	 */
	public CacheConfig( Key name ) {
		this( name, new Struct() );
	}

	/**
	 * Constructor by string name
	 *
	 * @param name The string name of the cache engine
	 */
	public CacheConfig( String name ) {
		this( Key.of( name ) );
	}

	/**
	 * Processes the configuration struct. Each segment is processed individually from the initial configuration struct.
	 *
	 * @param config the configuration struct
	 *
	 * @return the configuration
	 */
	public CacheConfig process( IStruct config ) {
		// Name
		if ( config.containsKey( "name" ) ) {
			this.name = Key.of( ( String ) config.get( "name" ) );
		}

		// Provider
		if ( config.containsKey( "provider" ) ) {
			this.provider = Key.of( ( String ) config.get( "provider" ) );
		}

		// Properties
		if ( config.containsKey( "properties" ) ) {
			if ( config.get( "properties" ) instanceof Map<?, ?> castedProps ) {
				processProperties( new Struct( castedProps ) );
			} else {
				logger.warn( "The [runtime.caches.{}.properties] configuration is not a JSON Object, ignoring it.", this.name );
			}
		}

		return this;
	}

	/**
	 * This processes a struct of properties for a BoxLang cache engine
	 *
	 * @param properties The properties to process
	 *
	 * @return the configuration
	 */
	public CacheConfig processProperties( IStruct properties ) {
		// Store
		this.properties = properties;

		// Merge defaults if it's a BoxLang cache
		if ( this.provider.equals( Key.boxCacheProvider ) ) {
			DEFAULTS
			    .entrySet()
			    .stream()
			    .forEach( entry -> this.properties.putIfAbsent( entry.getKey(), entry.getValue() ) );
		}

		return this;
	}

	/**
	 * Returns the cache configuration as a struct
	 */
	public IStruct toStruct() {
		return Struct.of(
		    "name", this.name.getName(),
		    "provider", this.provider.getName(),
		    "properties", this.properties
		);
	}

}
