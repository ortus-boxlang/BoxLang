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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.events.IInterceptor;
import ortus.boxlang.runtime.events.InterceptionPoint;
import ortus.boxlang.runtime.events.InterceptorState;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ClassLocator.ClassLocation;
import ortus.boxlang.runtime.loader.resolvers.BoxResolver;
import ortus.boxlang.runtime.modules.ModuleRecord;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

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
	private static final Logger				logger				= LoggerFactory.getLogger( InterceptorService.class );

	/**
	 * The list of interception points we can listen for
	 */
	private Set<Key>						interceptionPoints	= ConcurrentHashMap.newKeySet( 32 );

	/**
	 * The collection of interception states registered with the service
	 */
	private Map<Key, InterceptorState>		interceptionStates	= new ConcurrentHashMap<>();

	/**
	 * Key registry of announced states, to avoid key creation
	 */
	private ConcurrentHashMap<String, Key>	keyRegistry			= new ConcurrentHashMap<>();

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
		super( runtime );
		registerInterceptionPoint( BoxEvent.toArray() );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Runtime Service Interface Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The startup event is fired when the runtime starts up
	 */
	@Override
	public void onStartup() {
		logger.info( "InterceptorService.onStartup()" );
	}

	/**
	 * The shutdown event is fired when the runtime shuts down
	 *
	 * @param force True if the shutdown is forced, false otherwise
	 */
	@Override
	public void onShutdown( Boolean force ) {
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
	public Set<Key> getInterceptionPoints() {
		return interceptionPoints;
	}

	/**
	 * Get the list of interception points that the service can announce but as a Set of string names not
	 * case insensitive Key objects
	 *
	 * @return The list of interception points
	 */
	public Set<String> getInterceptionPointsNames() {
		return interceptionPoints.stream().map( Key::getName ).collect( java.util.stream.Collectors.toSet() );
	}

	/**
	 * Check if the service has an interception point
	 *
	 * @param interceptionPoint The interception point to check
	 *
	 * @return True if the service has the interception point, false otherwise
	 */
	public Boolean hasInterceptionPoint( Key interceptionPoint ) {
		return interceptionPoints.contains( interceptionPoint );
	}

	/**
	 * Register an interception point(s) with the service
	 *
	 * @param points The interception point(s) to register
	 *
	 * @return The same service
	 */
	public synchronized InterceptorService registerInterceptionPoint( Key... points ) {
		logger.atDebug().log( "InterceptorService.registerInterceptionPoint() - registering {}", Arrays.toString( points ) );
		interceptionPoints.addAll( Arrays.asList( points ) );
		return this;
	}

	/**
	 * Remove an interception point(s) from the service
	 *
	 * @param points The interception point(s) to remove
	 *
	 * @return The same service
	 */
	public synchronized InterceptorService removeInterceptionPoint( Key... points ) {
		logger.atDebug().log( "InterceptorService.removeInterceptionPoint() - removing {}", Arrays.toString( points ) );
		interceptionPoints.removeAll( Arrays.asList( points ) );
		interceptionStates.keySet().removeAll( Arrays.asList( points ) );
		return this;
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
	public InterceptorState getState( Key name ) {
		return interceptionStates.get( name );
	}

	/**
	 * Check if the service has the {@link InterceptorState}
	 *
	 * @param name The name of the state
	 *
	 * @return True if the service has the state, false otherwise
	 */
	public Boolean hasState( Key name ) {
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
	public synchronized InterceptorState registerState( Key name ) {
		logger.atDebug().log( "InterceptorService.registerState() - registering {}", name.getName() );

		// Verify point, else add it
		if ( !hasInterceptionPoint( name ) ) {
			logger.atDebug().log( "InterceptorService.registerState() - point not found, registering {}", name.getName() );
			registerInterceptionPoint( name );
		}

		// Register it
		interceptionStates.putIfAbsent( name, new InterceptorState( name ) );
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
	public synchronized InterceptorService removeState( Key name ) {
		if ( hasState( name ) ) {
			logger.atDebug().log( "InterceptorService.removeState() - removing {}", name.getName() );
			interceptionStates.remove( name );
		}
		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Interception Registration Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * This method resolves, creates, wires, and registers a BoxLang interceptor Class with the service.
	 *
	 * @param clazz        The BoxLang class invocation path of the interceptor to resolve. Example: "ortus.interceptors.MyInterceptor"
	 * @param properties   A {@Link Struct} of properties to wire the interceptor with
	 * @param name         The unique name of the interceptor
	 * @param moduleRecord If passed (it can be null), it means that a module is registering the interceptor
	 *
	 * @throws BoxRuntimeException If the interceptor class cannot be found
	 *
	 * @return The newly built interceptor
	 */
	public IClassRunnable newAndRegister(
	    String clazz,
	    IStruct properties,
	    String name,
	    ModuleRecord moduleRecord ) {

		IBoxContext				context			= runtime.getRuntimeContext();
		Optional<ClassLocation>	classLocation	= BoxResolver.getInstance().resolve(
		    context,
		    clazz
		);
		// Throw an exception if we can't find the class
		if ( !classLocation.isPresent() ) {
			throw new BoxRuntimeException( "Interceptor class [" + clazz + "] not found locally or with any mappings" );
		}

		// Load the Interceptor by it's class, Construct it and assign it to the record
		IClassRunnable oInterceptor = ( IClassRunnable ) DynamicObject.of( classLocation.get().clazz() )
		    .invokeConstructor( context )
		    .getTargetInstance();

		// Interceptor DI Injections into the variables scope
		// - Name : The name of the interceptor
		// - Properties : The properties of the interceptor
		// - Log : A logger for the interceptor itself
		// - InterceptorService : The InterceptorService instance
		// - BoxRuntime : The BoxRuntime instance
		// - ModuleRecord : The ModuleRecord instance, if passed
		oInterceptor.getVariablesScope().put( Key._NAME, name );
		oInterceptor.getVariablesScope().put( Key.properties, properties );
		oInterceptor.getVariablesScope().put( Key.log, LoggerFactory.getLogger( oInterceptor.getClass() ) );
		oInterceptor.getVariablesScope().put( Key.interceptorService, this );
		oInterceptor.getVariablesScope().put( Key.boxRuntime, runtime );

		// If we are in module mode, then add it in.
		if ( moduleRecord != null ) {
			oInterceptor.getVariablesScope().put( Key.moduleRecord, moduleRecord );
		}

		// Call the configure method if it exists in the interceptor
		if ( oInterceptor.getThisScope().containsKey( Key.configure ) ) {
			oInterceptor.dereferenceAndInvoke(
			    context,
			    Key.configure,
			    new Object[] {},
			    false
			);
		}

		// Now we can register it
		register( oInterceptor );

		return oInterceptor;
	}

	/**
	 * This method registers a Java interceptor with the service by metadata inspection
	 * of the {@link InterceptionPoint} annotation. It will inspect the interceptor for
	 * methods that match the states that the InterceptorService can announce. If the
	 * interceptor has a method that matches the state, it will register it with the service.
	 *
	 * @param interceptor The interceptor to register that must implement {@link IInterceptor}
	 *
	 * @return The same service
	 */
	public InterceptorService register( IInterceptor interceptor ) {
		// No properties
		return register( interceptor, new Struct() );
	}

	/**
	 * This method registers a Java interceptor with the service by metadata inspection
	 * of the {@link InterceptionPoint} annotation. It will inspect the interceptor for
	 * methods that match the states that the InterceptorService can announce. If the
	 * interceptor has a method that matches the state, it will register it with the service.
	 *
	 * @param interceptor The interceptor to register that must implement {@link IInterceptor}
	 * @param properties  A {@Link Struct} of properties to wire the interceptor with
	 *
	 * @return The same service
	 */
	public InterceptorService register( IInterceptor interceptor, IStruct properties ) {
		// Configure the interceptor
		interceptor.configure( properties );

		// Discover all @InterceptionPoint methods and build into an array of Keys to register
		DynamicObject	target	= DynamicObject.of( interceptor );
		Set<Key>		states	= target.getMethodsAsStream()
		    // filter only the methods that have the @InterceptionPoint annotation
		    .filter( method -> method.isAnnotationPresent( InterceptionPoint.class ) )
		    // map it to the method name
		    .map( method -> Key.of( method.getName() ) )
		    // Collect to the states set to register
		    .collect( Collectors.toSet() );

		return register( target, states.toArray( new Key[ 0 ] ) );
	}

	/**
	 * This method registers a BoxLang interceptor with the service by metadata inspection.
	 * It will inspect the interceptor for methods that match the states that the
	 * InterceptorService can announce. If the interceptor has a method that matches
	 * the state, it will register it with the service.
	 *
	 * If a method has a {@code interceptionPoint} BoxLang annotation, then the name
	 * of the method will be registered as a valid state and auto-registered.
	 *
	 * @param interceptor The interceptor to register that must implement {@link IClassRunnable}
	 *
	 * @return The same service
	 */
	public InterceptorService register( IClassRunnable interceptor ) {
		// Get the metadata of the class
		IStruct	metadata	= interceptor.getBoxMeta().getMeta();

		// System.out.println( metadata.getAsArray( Key.functions ).toString() );

		// Discover the states to register the interceptor with
		Key[]	states		= metadata
		    .getAsArray( Key.functions )
		    .stream()
		    // Casting so compiler is happy
		    .map( IStruct.class::cast )
		    // Iterate and find all the functions with interception points or that they are interception points
		    .filter( function -> {
			    // Check if the function has the @interceptionPoint annotation
			    // Check if the name of the Function is a valid interception point
			    return function.getAsStruct( Key.annotations ).containsKey( Key.interceptionPoint ) ||
			        interceptionPoints.contains( function.getAsKey( Key.nameAsKey ) );
		    } )
		    // Map it to the function name only now, this is what we need
		    .map( function -> function.getAsKey( Key.nameAsKey ) )
		    // Collect to the states array to register
		    .toArray( Key[]::new );

		// If we have any states, register them
		if ( states.length > 0 ) {
			register( DynamicObject.of( interceptor ), states );
		}

		return this;
	}

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
	public InterceptorService register( DynamicObject interceptor, Key... states ) {
		Arrays.stream( states )
		    .forEach( state -> {
			    logger.atDebug().log(
			        "InterceptorService.register() - registering {} with {}",
			        interceptor.getTargetClass().getName(),
			        state.getName()
			    );
			    registerState( state ).register( interceptor );
		    } );
		return this;
	}

	/**
	 * Unregister an interceptor from the provided states.
	 *
	 * @param interceptor The interceptor to unregister
	 * @param states      The states to unregister the interceptor from
	 *
	 * @return The same service
	 */
	public InterceptorService unregister( DynamicObject interceptor, Key... states ) {
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
		return this;
	}

	/**
	 * Unregister an interceptor from all states
	 *
	 * @param interceptor The interceptor to unregister
	 *
	 * @return The same service
	 */
	public InterceptorService unregister( DynamicObject interceptor ) {
		interceptionStates.values().stream()
		    .forEach( state -> {
			    logger.atDebug().log(
			        "InterceptorService.unregister() - unregistering {} with {}",
			        interceptor.getTargetClass().getName(),
			        state
			    );
			    state.unregister( interceptor );
		    } );
		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Announcements Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Announce an event with no data.
	 *
	 * @param state The state key to announce
	 */
	@Override
	public void announce( Key state ) {
		announce( state, new Struct() );
	}

	/**
	 * Announce an event with no data.
	 *
	 * @param state The state key to announce
	 */
	@Override
	public void announce( BoxEvent state ) {
		announce( state.key(), new Struct() );
	}

	/**
	 * Announce an event with the provided {@link IStruct} of data.
	 *
	 * @param state The state to announce
	 * @param data  The data to announce
	 */
	public void announce( String state, IStruct data ) {
		announce( keyRegistry.computeIfAbsent( state, Key::of ), data );
	}

	/**
	 * Announce an event with the provided {@link IStruct} of data.
	 *
	 * @param state The state to announce
	 * @param data  The data to announce
	 */
	public void announce( BoxEvent state, IStruct data ) {
		announce( state.key(), data );
	}

	/**
	 * Announce an event with the provided {@link IStruct} of data.
	 *
	 * @param state The state key to announce
	 * @param data  The data to announce
	 */
	@Override
	public void announce( Key state, IStruct data ) {
		if ( hasState( state ) ) {
			// logger.atDebug().log( "InterceptorService.announce() - announcing {}", state.getName() );

			try {
				getState( state ).announce( data, runtime.getRuntimeContext() );
			} catch ( Exception e ) {
				String errorMessage = String.format( "Errors announcing [%s] interception", state.getName() );
				logger.error( errorMessage, e );
				throw new BoxRuntimeException( errorMessage, e );
			}

			// logger.atDebug().log( "Finished announcing {}", state.getName() );
		} else {
			// logger.atDebug().log( "InterceptorService.announce() - No state found for: {}", state.getName() );
		}
	}

}
