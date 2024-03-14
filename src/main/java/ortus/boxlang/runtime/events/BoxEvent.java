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
	AFTER_DYNAMIC_OBJECT_CREATION( "afterDynamicObjectCreation" ),
	ON_RUNTIME_START( "onRuntimeStart" ),
	ON_RUNTIME_SHUTDOWN( "onRuntimeShutdown" ),
	ON_RUNTIME_CONFIGURATION_LOAD( "onRuntimeConfigurationLoad" ),
	ON_APPLICATION_START( "onApplicationStart" ),
	ON_APPLICATION_END( "onApplicationEnd" ),
	ON_APPLICATION_RESTART( "onApplicationRestart" ),
	PRE_TEMPLATE_INVOKE( "preTemplateInvoke" ),
	POST_TEMPLATE_INVOKE( "postTemplateInvoke" ),
	PRE_FUNCTION_INVOKE( "preFunctionInvoke" ),
	POST_FUNCTION_INVOKE( "postFunctionInvoke" ),
	ON_SCOPE_CREATION( "onScopeCreation" ),
	ON_CONFIGURATION_LOAD( "onConfigurationLoad" ),
	ON_CONFIGURATION_OVERRIDE_LOAD( "onConfigurationOverrideLoad" ),
	ON_PARSE( "onParse" ),
	PRE_QUERY_EXECUTE( "preQueryExecute" ),
	POST_QUERY_EXECUTE( "postQueryExecute" ),

	/**
	 * Module Service Events
	 */
	AFTER_MODULE_REGISTRATIONS( "afterModuleRegistrations" ),
	PRE_MODULE_REGISTRATION( "preModuleRegistration" ),
	POST_MODULE_REGISTRATION( "postModuleRegistration" ),
	AFTER_MODULE_ACTIVATIONS( "afterModuleActivations" ),
	PRE_MODULE_LOAD( "preModuleLoad" ),
	POST_MODULE_LOAD( "postModuleLoad" ),
	PRE_MODULE_UNLOAD( "preModuleUnload" ),
	POST_MODULE_UNLOAD( "postModuleUnload" ),
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
		    .toArray( Key[]::new );
	}

}
