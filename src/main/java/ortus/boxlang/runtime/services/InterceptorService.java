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
package ortus.boxlang.runtime.services;

import java.util.ServiceLoader;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.events.IInterceptor;
import ortus.boxlang.runtime.events.Interceptor;
import ortus.boxlang.runtime.events.InterceptorPool;
import ortus.boxlang.runtime.interceptors.ASTCapture;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.Key;

/**
 * The interceptor service is responsible for managing all events in BoxLang.
 * A developer will register an interceptor with the service, and the service will
 * invoke the interceptor when the event is fired.
 *
 * The interceptor service is a singleton.
 *
 * Each service manages interception points, which are the events that the service can announce
 * and their states, which are where interceptors can register to listen to.
 */
public class InterceptorService extends InterceptorPool implements IService {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The interceptor logger goes into the `runtime` category
	 */
	private BoxLangLogger logger;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get an instance of the service
	 *
	 * @param runtime The runtime singleton
	 */
	public InterceptorService( BoxRuntime runtime ) {
		super( Key.interceptorService, runtime );
		registerInterceptionPoint( BoxEvent.toArray() );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Runtime Service Interface Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The configuration load event is fired when the runtime loads the configuration
	 */
	@Override
	public void onConfigurationLoad() {
		// Startup the logger
		getLogger();

		// AST Capture experimental feature
		BooleanCaster.attempt(
		    this.runtime.getConfiguration().experimental.getOrDefault( "ASTCapture", false ) )
		    .ifSuccessful(
		        astCapture -> {
			        if ( astCapture ) {
				        register( DynamicObject.of( new ASTCapture( false, true ) ), Key.onParse );
			        }
		        } );

		// Auto-Load all interceptors found in the runtime classloader
		ServiceLoader.load( IInterceptor.class, this.runtime.getRuntimeLoader() )
		    .stream()
		    // Only load interceptors that are set to auto-load by default or by configuration
		    .filter( provider -> canLoadInterceptor( provider.type() ) )
		    // Register the interceptor
		    .map( ServiceLoader.Provider::get )
		    .forEach( this::register );
	}

	/**
	 * This method encapsulates the logic to determine if an interceptor can be loaded or not.
	 *
	 * @param targetClass The class of the interceptor to be loaded
	 *
	 * @return True if the interceptor can be loaded, false otherwise
	 */
	public boolean canLoadInterceptor( Class<? extends IInterceptor> targetClass ) {
		// Check the @Interceptor annotation config properties
		// AutoLoad defaults to true if the annotation is not found.
		if ( targetClass.isAnnotationPresent( Interceptor.class ) ) {
			Interceptor annotation = targetClass.getAnnotation( Interceptor.class );
			if ( !annotation.autoLoad() ) {
				getLogger().debug( "Interceptor [{}] is set to not auto-load, skipping.", targetClass.getName() );
				return false;
			}
		}
		return true;
	}

	/**
	 * The startup event is fired when the runtime starts up
	 */
	@Override
	public void onStartup() {
		getLogger().debug( "InterceptorService.onStartup()" );
	}

	/**
	 * The shutdown event is fired when the runtime shuts down
	 *
	 * @param force True if the shutdown is forced, false otherwise
	 */
	@Override
	public void onShutdown( Boolean force ) {
		getLogger().debug( "InterceptorService.onShutdown()" );
	}

}
