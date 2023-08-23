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
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;

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
public class InterceptorService extends BaseService {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Logger
	 */
	private static final Logger					logger				= LoggerFactory.getLogger( InterceptorService.class );

	/**
	 * Singleton instance
	 */
	private static InterceptorService			instance;

	/**
	 * The list of interception points we can listen for
	 */
	private static Set<Key>						interceptionPoints	= ConcurrentHashMap.newKeySet( 32 );

	/**
	 * The collection of interception states registered with the service
	 */
	private static Map<Key, InterceptorState>	interceptionStates	= new ConcurrentHashMap<>();

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

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
	 * Get an instance of the service but init it with some interception points
	 *
	 * @param points The interception points to init the service with
	 *
	 * @return The singleton instance
	 */
	public static synchronized InterceptorService getInstance( Key... points ) {
		getInstance();
		registerInterceptionPoint( points );
		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Runtime Service Interface Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The startup event is fired when the runtime starts up
	 */
	public static void onStartup() {
		logger.info( "InterceptorService.onStartup()" );
	}

	/**
	 * The configuration load event is fired when the runtime loads its configuration
	 */
	public static void onConfigurationLoad() {
		logger.info( "InterceptorService.onConfigurationLoad()" );
	}

	/**
	 * The shutdown event is fired when the runtime shuts down
	 */
	public static void onShutdown() {
		logger.info( "InterceptorService.onShutdown()" );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Interception Point Methods
	 * --------------------------------------------------------------------------
	 * Interception points are just events that we must be able to listen to.
	 */

	/**
	 * Get the list of interception points that the service can announce
	 *
	 * @return The list of interception points
	 */
	public static Set<Key> getInterceptionPoints() {
		return interceptionPoints;
	}

	/**
	 * Get the list of interception points that the service can announce but as a Set of string names not
	 * case insensitive Key objects
	 *
	 * @return The list of interception points
	 */
	public static Set<String> getInterceptionPointsNames() {
		return interceptionPoints.stream().map( Key::getName ).collect( java.util.stream.Collectors.toSet() );
	}

	/**
	 * Check if the service has an interception point
	 *
	 * @param interceptionPoint The interception point to check
	 *
	 * @return True if the service has the interception point, false otherwise
	 */
	public static Boolean hasInterceptionPoint( Key interceptionPoint ) {
		return interceptionPoints.contains( interceptionPoint );
	}

	/**
	 * Register an interception point(s) with the service
	 *
	 * @param points The interception point(s) to register
	 *
	 * @return The same service
	 */
	public static InterceptorService registerInterceptionPoint( Key... points ) {
		logger.atDebug().log( "InterceptorService.registerInterceptionPoint() - registering {}", Arrays.toString( points ) );
		interceptionPoints.addAll( Arrays.asList( points ) );
		return instance;
	}

	/**
	 * Remove an interception point(s) from the service
	 *
	 * @param points The interception point(s) to remove
	 *
	 * @return The same service
	 */
	public static InterceptorService removeInterceptionPoint( Key... points ) {
		logger.atDebug().log( "InterceptorService.removeInterceptionPoint() - removing {}", Arrays.toString( points ) );
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

	/**
	 * Get the {@link InterceptorState} by name
	 *
	 * @param name The name of the state
	 *
	 * @return The state if it exists, null otherwise
	 */
	public static InterceptorState getState( Key name ) {
		return interceptionStates.get( name );
	}

	/**
	 * Check if the service has the {@link InterceptorState}
	 *
	 * @param name The name of the state
	 *
	 * @return True if the service has the state, false otherwise
	 */
	public static Boolean hasState( Key name ) {
		return interceptionStates.containsKey( name );
	}

	/**
	 * Register a new {@link InterceptorState} with the service and returns it.
	 * This verifies if there is already an interception point by that name.
	 * If there is not, it will add it.
	 *
	 * @param name The name of the state
	 *
	 * @return The registered {@link InterceptorState}
	 */
	public static synchronized InterceptorState registerState( Key name ) {
		logger.atDebug().log( "InterceptorService.registerState() - registering {}", name.getName() );

		// Verify point, else add it
		if ( !hasInterceptionPoint( name ) ) {
			logger.atDebug().log( "InterceptorService.registerState() - point not found, registering {}", name.getName() );
			registerInterceptionPoint( name );
		}

		// Register it
		interceptionStates.putIfAbsent( name, new InterceptorState( name.getName() ) );
		return getState( name );
	}

	/**
	 * Remove the {@link InterceptorState} from the service. This essentially
	 * destroys all the interceptor references in the state
	 *
	 * @param name The name of the state
	 *
	 * @return The same service
	 */
	public static synchronized InterceptorService removeState( Key name ) {
		if ( hasState( name ) ) {
			logger.atDebug().log( "InterceptorService.removeState() - removing {}", name.getName() );
			interceptionStates.remove( name );
		}
		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Interception Registration Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Register an interceptor with the service which must be an instance of
	 * {@link DynamicObject}. The interceptor must have a method(s) according to the passed
	 * states.
	 *
	 * @param interceptor The interceptor to register
	 * @param states      The states to register the interceptor with
	 *
	 * @return The same service
	 */
	public static InterceptorService register( DynamicObject interceptor, Key... states ) {
		Arrays.stream( states )
		    .forEach( state -> {
			    logger.atDebug().log(
			        "InterceptorService.register() - registering {} with {}",
			        interceptor.getTargetClass().getName(),
			        state.getName()
			    );
			    registerState( state ).register( interceptor );
		    } );
		return instance;
	}

	/**
	 * Unregister an interceptor from the provided states.
	 *
	 * @param interceptor The interceptor to unregister
	 * @param states      The states to unregister the interceptor from
	 *
	 * @return The same service
	 */
	public static InterceptorService unregister( DynamicObject interceptor, Key... states ) {
		Arrays.stream( states )
		    .forEach( state -> {
			    if ( hasState( state ) ) {
				    logger.atDebug().log(
				        "InterceptorService.unregister() - unregistering {} with {}",
				        interceptor.getTargetClass().getName(),
				        state.getName()
				    );
				    getState( state ).unregister( interceptor );
			    }
		    } );
		return instance;
	}

	/**
	 * Unregister an interceptor from all states
	 *
	 * @param interceptor The interceptor to unregister
	 *
	 * @return The same service
	 */
	public static InterceptorService unregister( DynamicObject interceptor ) {
		interceptionStates.values().stream()
		    .forEach( state -> {
			    logger.atDebug().log(
			        "InterceptorService.unregister() - unregistering {} with {}",
			        interceptor.getTargetClass().getName(),
			        state
			    );
			    state.unregister( interceptor );
		    } );
		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Announcements Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Announce an event with the provided {@link Struct} of data.
	 *
	 * @param state The state to announce
	 * @param data  The data to announce
	 */
	public static void announce( String state, Struct data ) {
		announce( Key.of( state ), data );
	}

	/**
	 * Announce an event with the provided {@link Struct} of data.
	 *
	 * @param state The state key to announce
	 * @param data  The data to announce
	 */
	public static void announce( Key state, Struct data ) {
		if ( hasState( state ) ) {
			logger.atDebug().log( "InterceptorService.announce() - announcing {}", state.getName() );

			try {
				getState( state ).announce( data );
			} catch ( Throwable e ) {
				String errorMessage = String.format( "Errors announcing [%s] interception", state.getName() );
				logger.error( errorMessage, e );
				throw new RuntimeException( errorMessage );
			}

			logger.atDebug().log( "Finished announcing {}", state.getName() );
		} else {
			logger.atDebug().log( "InterceptorService.announce() - No state found for: {}", state.getName() );
		}
	}

}
