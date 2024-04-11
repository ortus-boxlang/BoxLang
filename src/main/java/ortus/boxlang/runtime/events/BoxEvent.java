/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http: //www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.events;

import java.util.Arrays;

import ortus.boxlang.runtime.scopes.Key;

/**
 * These are all the core BoxLang interception events that can be intercepted by the BoxLang runtime.
 */
public enum BoxEvent {

	/**
	 * Runtime Events
	 */
	ON_RUNTIME_START( "onRuntimeStart" ),
	ON_RUNTIME_SHUTDOWN( "onRuntimeShutdown" ),
	ON_RUNTIME_CONFIGURATION_LOAD( "onRuntimeConfigurationLoad" ),
	ON_SCOPE_CREATION( "onScopeCreation" ),
	ON_CONFIGURATION_LOAD( "onConfigurationLoad" ),
	ON_CONFIGURATION_OVERRIDE_LOAD( "onConfigurationOverrideLoad" ),
	ON_PARSE( "onParse" ),

	/**
	 * Lifecycle Methods for Components ad BIFS
	 */
	ON_BIF_INSTANCE( "onBIFInstance" ),
	ON_COMPONENT_INSTANCE( "onComponentInstance" ),

	/**
	 * Dynamic Object Events
	 */
	ON_CREATEOBJECT_REQUEST( "onCreateObjectRequest" ),
	AFTER_DYNAMIC_OBJECT_CREATION( "afterDynamicObjectCreation" ),

	/**
	 * Application Related Events
	 */
	ON_APPLICATION_START( "onApplicationStart" ),
	ON_APPLICATION_END( "onApplicationEnd" ),
	ON_APPLICATION_RESTART( "onApplicationRestart" ),

	/**
	 * Template
	 */
	PRE_TEMPLATE_INVOKE( "preTemplateInvoke" ),
	POST_TEMPLATE_INVOKE( "postTemplateInvoke" ),

	/**
	 * Function Execution Events
	 */
	PRE_FUNCTION_INVOKE( "preFunctionInvoke" ),
	POST_FUNCTION_INVOKE( "postFunctionInvoke" ),

	/**
	 * Query Events
	 */
	PRE_QUERY_EXECUTE( "preQueryExecute" ),
	POST_QUERY_EXECUTE( "postQueryExecute" ),

	/**
	 * Cache Store Events
	 */
	AFTER_CACHE_ELEMENT_INSERT( "afterCacheElementInsert" ),
	AFTER_CACHE_ELEMENT_REMOVED( "afterCacheElementRemoved" ),
	AFTER_CACHE_ELEMENT_UPDATED( "afterCacheElementUpdated" ),

	/**
	 * Cache Provider Events
	 */
	AFTER_CACHE_CLEAR_ALL( "afterCacheClearAll" ),
	AFTER_CACHE_REGISTRATION( "afterCacheRegistration" ),
	AFTER_CACHE_REMOVAL( "afterCacheRemoval" ),
	BEFORE_CACHE_REMOVAL( "beforeCacheRemoval" ),
	BEFORE_CACHE_REPLACEMENT( "beforeCacheReplacement" ),
	BEFORE_CACHE_SHUTDOWN( "beforeCacheShutdown" ),
	AFTER_CACHE_SHUTDOWN( "afterCacheShutdown" ),

	/**
	 * Cache Service Events
	 */
	AFTER_CACHE_SERVICE_STARTUP( "afterCacheServiceStartup" ),
	BEFORE_CACHE_SERVICE_SHUTDOWN( "beforeCacheServiceShutdown" ),
	AFTER_CACHE_SERVICE_SHUTDOWN( "afterCacheServiceShutdown" ),

	/**
	 * Log Events
	 */
	LOG_MESSAGE( "logMessage" ),

	/**
	 * Module Events
	 */
	AFTER_MODULE_REGISTRATIONS( "afterModuleRegistrations" ),
	PRE_MODULE_REGISTRATION( "preModuleRegistration" ),
	POST_MODULE_REGISTRATION( "postModuleRegistration" ),
	AFTER_MODULE_ACTIVATIONS( "afterModuleActivations" ),
	PRE_MODULE_LOAD( "preModuleLoad" ),
	POST_MODULE_LOAD( "postModuleLoad" ),
	PRE_MODULE_UNLOAD( "preModuleUnload" ),
	POST_MODULE_UNLOAD( "postModuleUnload" ),

	/**
	 * Module Service Events
	 */
	ON_MODULE_SERVICE_STARTUP( "onModuleServiceStartup" ),
	ON_MODULE_SERVICE_SHUTDOWN( "onModuleServiceShutdown" );

	/**
	 * The key representing the event name.
	 *
	 */
	private final Key key;

	/**
	 * Constructor
	 *
	 * @param eventName The name of the event.
	 */
	BoxEvent( String eventName ) {
		this.key = Key.of( eventName );
	}

	/**
	 * Returns the key representing the event name.
	 *
	 * @return The key representing the event name.
	 */
	public Key key() {
		return key;
	}

	/**
	 * Returns an array of all the event keys.
	 *
	 * @return An array of all the event keys.
	 */
	public static Key[] toArray() {
		return Arrays.stream( values() )
		    .map( val -> val.key )
		    .sorted()
		    .toArray( Key[]::new );
	}

}
