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
	ON_RUNTIME_BOX_CONTEXT_STARTUP( "onRuntimeBoxContextStartup" ),
	ON_SERVER_SCOPE_CREATION( "onServerScopeCreation" ),
	ON_CONFIGURATION_LOAD( "onConfigurationLoad" ),
	ON_CONFIGURATION_OVERRIDE_LOAD( "onConfigurationOverrideLoad" ),
	ON_PARSE( "onParse" ),
	ON_MISSING_MAPPING( "onMissingMapping" ),

	/**
	 * Lifecycle Methods for Components and BIFS
	 */
	ON_BIF_INSTANCE( "onBIFInstance" ),
	ON_BIF_INVOCATION( "onBIFInvocation" ),
	ON_COMPONENT_INSTANCE( "onComponentInstance" ),
	ON_COMPONENT_INVOCATION( "onComponentInvocation" ),
	ON_FILECOMPONENT_ACTION( "onFileComponentAction" ),
	ON_CREATEOBJECT_REQUEST( "onCreateObjectRequest" ),

	/**
	 * Dynamic Object Events
	 */
	AFTER_DYNAMIC_OBJECT_CREATION( "afterDynamicObjectCreation" ),

	/**
	 * Application Related Events
	 */
	ON_APPLICATION_START( "onApplicationStart" ),
	ON_APPLICATION_END( "onApplicationEnd" ),
	ON_APPLICATION_RESTART( "onApplicationRestart" ),
	BEFORE_APPLICATION_LISTENER_LOAD( "beforeApplicationListenerLoad" ),
	AFTER_APPLICATION_LISTENER_LOAD( "afterApplicationListenerLoad" ),
	ON_REQUEST_FLUSH_BUFFER( "onRequestFlushBuffer" ),
	ON_SESSION_CREATED( "onSessionCreated" ),
	ON_SESSION_DESTROYED( "onSessionDestroyed" ),

	/**
	 * Request Events
	 */
	ON_REQUEST_CONTEXT_CONFIG( "onRequestContextConfig" ),

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
	ON_QUERY_BUILD( "onQueryBuild" ),
	PRE_QUERY_EXECUTE( "preQueryExecute" ),
	POST_QUERY_EXECUTE( "postQueryExecute" ),

	/**
	 * Cache Store Events
	 */
	AFTER_CACHE_ELEMENT_INSERT( "afterCacheElementInsert" ),
	BEFORE_CACHE_ELEMENT_REMOVED( "beforeCacheElementRemoved" ),
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
	 * Datasource Service Events
	 */
	ON_DATASOURCE_SERVCE_STARTUP( "onDatasourceServiceStartup" ),
	ON_DATASOURCE_SERVICE_SHUTDOWN( "onDatasourceServiceShutdown" ),
	ON_DATASOURCE_STARTUP( "onDatasourceStartup" ),

	/**
	 * Scheduler Events
	 */
	ON_SCHEDULER_STARTUP( "onSchedulerStartup" ),
	ON_SCHEDULER_SHUTDOWN( "onSchedulerShutdown" ),
	ON_SCHEDULER_RESTART( "onSchedulerRestart" ),
	SCHEDULER_BEFORE_ANY_TASK( "schedulerBeforeAnyTask" ),
	SCHEDULER_AFTER_ANY_TASK( "schedulerAfterAnyTask" ),
	SCHEDULER_ON_ANY_TASK_SUCCESS( "schedulerOnAnyTaskSuccess" ),
	SCHEDULER_ON_ANY_TASK_ERROR( "schedulerOnAnyTaskError" ),

	/**
	 * Scheduler Service Events
	 */
	ON_SCHEDULER_SERVICE_STARTUP( "onSchedulerServiceStartup" ),
	ON_SCHEDULER_SERVICE_SHUTDOWN( "onSchedulerServiceShutdown" ),
	ON_ALL_SCHEDULERS_STARTED( "onAllSchedulersStarted" ),
	ON_SCHEDULER_REMOVAL( "onSchedulerRemoval" ),
	ON_SCHEDULER_REGISTRATION( "onSchedulerRegistration" ),

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
