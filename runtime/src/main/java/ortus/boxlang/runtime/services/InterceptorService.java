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

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.events.InterceptorState;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.Struct;

/**
 * The interceptor service is responsible for managing all events in BoxLang.
 * A developer will register an interceptor with the service, and the service will
 * invoke the interceptor when the event is fired.
 */
public class InterceptorService {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Logger
	 */
	private static final Logger						logger				= LoggerFactory.getLogger( InterceptorService.class );

	/**
	 * Singleton instance
	 */
	private static InterceptorService				instance;

	/**
	 * The list of interception points we can listen for
	 */
	private static Set<String>						interceptionPoints	= ConcurrentHashMap.newKeySet( 32 );

	/**
	 * The collection of interception states registered with the service
	 */
	private static Map<String, InterceptorState>	interceptionStates	= new ConcurrentHashMap<>();

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 */
	private InterceptorService() {
	}

	/**
	 * Get an instance of the service
	 *
	 * @return The singleton instance
	 */
	public static synchronized InterceptorService getInstance() {
		if ( instance == null ) {
			instance = new InterceptorService();
		}
		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Runtime Service Interface Methods
	 * --------------------------------------------------------------------------
	 */

	public static void onStartup() {
		logger.info( "InterceptorService.onStartup()" );
	}

	public static void onConfigurationLoad() {
		logger.info( "InterceptorService.onConfigurationLoad()" );
	}

	public static void onShutdown() {
		logger.info( "InterceptorService.onShutdown()" );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Interception Point Methods
	 * --------------------------------------------------------------------------
	 * Interception points are just events that we must be able to listen to.
	 */

	public static Set<String> getInterceptionPoints() {
		return interceptionPoints;
	}

	public static Boolean hasInterceptionPoint( String interceptionPoint ) {
		return interceptionPoints.contains( interceptionPoint );
	}

	public static InterceptorService registerInterceptionPoint( String... points ) {
		interceptionPoints.addAll( Arrays.asList( points ) );
		return instance;
	}

	public static InterceptorService removeInterceptionPoint( String... points ) {
		interceptionPoints.removeAll( Arrays.asList( points ) );
		interceptionStates.keySet().removeAll( Arrays.asList( points ) );
		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Interceptor State Methods
	 * --------------------------------------------------------------------------
	 * The interceptor state is the state of the interceptor. For example, if the
	 * interceptor is listening to the "preProcess" event, then the interceptor
	 * state is "preProcess". All states are lazy loaded upon first interceptor
	 * registration.
	 */

	public static InterceptorState getState( String name ) {
		return interceptionStates.get( name );
	}

	public static Boolean hasState( String name ) {
		return interceptionStates.containsKey( name );
	}

	public static synchronized InterceptorState registerState( String name ) {
		interceptionStates.putIfAbsent( name, new InterceptorState( name ) );
		return getState( name );
	}

	public static synchronized InterceptorService removeState( String name ) {
		if ( hasState( name ) ) {
			interceptionStates.remove( name );
		}
		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Interception Registration Methods
	 * --------------------------------------------------------------------------
	 */

	public static InterceptorService register( DynamicObject interceptor, String... states ) {
		Arrays.stream( states )
		        .forEach( state -> {
			        registerState( state ).register( interceptor );
		        } );
		return instance;
	}

	public static InterceptorService unregister( DynamicObject interceptor, String... states ) {
		Arrays.stream( states )
		        .forEach( state -> {
			        if ( hasState( state ) ) {
				        getState( state ).unregister( interceptor );
			        }
		        } );
		return instance;
	}

	public static InterceptorService unregister( DynamicObject interceptor ) {
		interceptionStates.values().stream()
		        .forEach( state -> {
			        state.unregister( interceptor );
		        } );
		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Announcements Methods
	 * --------------------------------------------------------------------------
	 * 
	 * @throws Throwable
	 */

	public static void announce( String state, Struct data ) throws Throwable {
		if ( hasState( state ) ) {
			getState( state ).announce( data );
		}
	}

}
