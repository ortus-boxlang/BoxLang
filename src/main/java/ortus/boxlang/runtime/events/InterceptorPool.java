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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ClassLocator.ClassLocation;
import ortus.boxlang.runtime.modules.ModuleRecord;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.AbortException;

/**
 * An InterceptorPool is a pool of interceptors that can be used to intercept events
 * within the BoxLang runtime.
 * <p>
 * This pool can be used at any level to provide encapsulation of events and
 * provide a way to intercept and modify the behavior of the runtime, service, etc.
 * <p>
 * This class should be instantiated and used as a singleton.
 */
public class InterceptorPool {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Logger
	 */
	private static final Logger					logger				= LoggerFactory.getLogger( InterceptorPool.class );

	/**
	 * The list of interception points we can listen for
	 */
	protected Set<Key>							interceptionPoints	= ConcurrentHashMap.newKeySet( 32 );

	/**
	 * The collection of interception states registered with the service
	 */
	protected Map<Key, InterceptorState>		interceptionStates	= new ConcurrentHashMap<>();

	/**
	 * Key registry of announced states, to avoid key creation
	 */
	protected ConcurrentHashMap<String, Key>	keyRegistry			= new ConcurrentHashMap<>();

	/**
	 * The name of the pool
	 */
	protected Key								name;

	/**
	 * The runtime singleton
	 */
	protected BoxRuntime						runtime;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Construct a pool with a unique name
	 *
	 * @param name    The name of the pool
	 * @param runtime The runtime singleton
	 */
	public InterceptorPool( Key name, BoxRuntime runtime ) {
		this.name		= name;
		this.runtime	= runtime;
	}

	/**
	 * Construct a pool with a unique name
	 *
	 * @param name The name of the pool
	 */
	public InterceptorPool( String name, BoxRuntime runtime ) {
		this( Key.of( name ), runtime );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters and Setters
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the name of the pool
	 *
	 * @return The name of the pool
	 */
	public Key getName() {
		return name;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Interception Point Methods
	 * --------------------------------------------------------------------------
	 * Interception points are keys we can register that the pool can listen/announce to.
	 * Think of them like channels of announcements, in which interceptors can listen to.
	 * <p>
	 * For example, if we have an interception point called "onRequestStart", then we can
	 * register an interceptor to listen to that point and execute when the pool announces
	 * that point: {@code pool.announce( "onRequestStart", Struct.of() )}
	 */

	/**
	 * Get the list of interception points that the pool can announce
	 *
	 * @return A set of unique interception points
	 */
	public Set<Key> getInterceptionPoints() {
		return this.interceptionPoints;
	}

	/**
	 * Get the registered interception states
	 *
	 * @return The registered interception states
	 */
	public Map<Key, InterceptorState> getInterceptionStates() {
		return this.interceptionStates;
	}

	/**
	 * Clear all the interception states
	 * Use with caution. Mostly left for testing purposes
	 */
	public void clearInterceptionStates() {
		this.interceptionStates.clear();
	}

	/**
	 * Get the list of interception points that the pool can announce but as a Set of string names not
	 * case insensitive Key objects
	 *
	 * @return The list of interception points
	 */
	public Set<String> getInterceptionPointsNames() {
		return this.interceptionPoints.stream().map( Key::getName ).collect( java.util.stream.Collectors.toSet() );
	}

	/**
	 * Check if the pool has an interception point
	 *
	 * @param interceptionPoint The interception point to check
	 *
	 * @return True if the pool has the interception point, false otherwise
	 */
	public Boolean hasInterceptionPoint( Key interceptionPoint ) {
		return this.interceptionPoints.contains( interceptionPoint );
	}

	/**
	 * Register an interception point(s) with the pool
	 *
	 * @param points The interception point(s) to register
	 *
	 * @return The same pool
	 */
	public synchronized InterceptorPool registerInterceptionPoint( Key... points ) {
		this.interceptionPoints.addAll( Arrays.asList( points ) );
		return this;
	}

	/**
	 * Remove an interception point(s) from the pool
	 *
	 * @param points The interception point(s) to remove
	 *
	 * @return The same pool
	 */
	public synchronized InterceptorPool removeInterceptionPoint( Key... points ) {
		this.interceptionPoints.removeAll( Arrays.asList( points ) );
		this.interceptionStates.keySet().removeAll( Arrays.asList( points ) );
		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Interceptor State Methods
	 * --------------------------------------------------------------------------
	 * An interceptor state follows the interceptor chain design pattern.
	 * An interceptor can be registered in many states and executed concurrently or asynchronously.
	 * <p>
	 * For example, if the interceptor is listening to the "preProcess" event, then the interceptor
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
		return this.interceptionStates.get( name );
	}

	/**
	 * Check if the service has the {@link InterceptorState}
	 *
	 * @param name The name of the state
	 *
	 * @return True if the service has the state, false otherwise
	 */
	public Boolean hasState( Key name ) {
		return this.interceptionStates.containsKey( name );
	}

	/**
	 * Register a new {@link InterceptorState} with the pool and returns it.
	 * This verifies if there is already an interception point by that name.
	 * If there is not, it will add it.
	 *
	 * @param name The name of the state
	 *
	 * @return The registered {@link InterceptorState}
	 */
	public synchronized InterceptorState registerState( Key name ) {
		// Ensure the interception point exists
		registerInterceptionPoint( name );

		// Register it or return it

		// Comput if absent
		return interceptionStates.computeIfAbsent(
		    name,
		    InterceptorState::new
		);
	}

	/**
	 * Remove the {@link InterceptorState} from the pool. This essentially
	 * destroys all the interceptor references in the state
	 *
	 * @param name The name of the state
	 *
	 * @return The same pool
	 */
	public synchronized InterceptorPool removeState( Key name ) {
		this.interceptionStates.remove( name );
		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Interceptor Registration Methods
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
		// Get the class location
		IBoxContext				context			= this.runtime.getRuntimeContext();
		Optional<ClassLocation>	classLocation	= this.runtime.getClassLocator().getBoxResolver().resolve(
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
		oInterceptor.getVariablesScope().put( Key.boxRuntime, this.runtime );

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
	 * This method registers a Java interceptor with the pool by metadata inspection
	 * of the {@link InterceptionPoint} annotation. It will inspect the interceptor for
	 * methods that match the states that the pool can announce. If the
	 *
	 * @param interceptor The interceptor to register that must implement {@link IInterceptor}
	 *
	 * @return The pool
	 */
	public InterceptorPool register( IInterceptor interceptor ) {
		// No properties
		return register( interceptor, new Struct() );
	}

	/**
	 * This method registers a Java interceptor with the pool by metadata inspection
	 * of the {@link InterceptionPoint} annotation. It will inspect the interceptor for
	 * methods that match the states that the pool can announce. If the
	 * interceptor has a method that matches the state, it will register it with the pool.
	 *
	 * @param interceptor The interceptor to register that must implement {@link IInterceptor}
	 * @param properties  A {@Link Struct} of properties to wire the interceptor with
	 *
	 * @return The same pool
	 */
	public InterceptorPool register( IInterceptor interceptor, IStruct properties ) {
		// Configure the interceptor
		interceptor.configure( properties );

		// Discover all @InterceptionPoint methods and build into an array of Keys to register
		DynamicObject	target	= DynamicObject.of( interceptor );
		Set<Key>		states	= target.getMethodsAsStream( true )
		    // filter only the methods that have the @InterceptionPoint annotation
		    .filter( method -> method.isAnnotationPresent( InterceptionPoint.class ) )
		    // map it to the method name
		    .map( method -> Key.of( method.getName() ) )
		    // Collect to the states set to register
		    .collect( Collectors.toSet() );

		return register( target, states.toArray( new Key[ 0 ] ) );
	}

	/**
	 * This method UNregisters a Java interceptor with the pool by metadata inspection
	 * of the {@link InterceptionPoint} annotation. It will inspect the interceptor for
	 * methods that match the states that the pool can announce. If the
	 * interceptor has a method that matches the state, it will UNregister it with the pool.
	 *
	 * @param interceptor The interceptor to UNregister that must implement {@link IInterceptor}
	 *
	 * @return The same pool
	 */
	public InterceptorPool unregister( IInterceptor interceptor ) {
		// Discover all @InterceptionPoint methods and build into an array of Keys to register
		DynamicObject	target	= DynamicObject.of( interceptor );
		Set<Key>		states	= target.getMethodsAsStream( true )
		    // filter only the methods that have the @InterceptionPoint annotation
		    .filter( method -> method.isAnnotationPresent( InterceptionPoint.class ) )
		    // map it to the method name
		    .map( method -> Key.of( method.getName() ) )
		    // Collect to the states set to register
		    .collect( Collectors.toSet() );

		return unregister( target, states.toArray( new Key[ 0 ] ) );
	}

	/**
	 * This method registers a BoxLang interceptor with the pool by metadata inspection.
	 * It will inspect the interceptor for methods that match the states that the
	 * pool can announce. If the interceptor has a method that matches
	 * the state, it will register it with the pool.
	 *
	 * If a method has a {@code interceptionPoint} BoxLang annotation, then the name
	 * of the method will be registered as a valid state and auto-registered.
	 *
	 * @param interceptor The interceptor to register that must implement {@link IClassRunnable}
	 *
	 * @return The same pool
	 */
	public InterceptorPool register( IClassRunnable interceptor ) {
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
	 * Register an interceptor with the pool which must be an instance of
	 * {@link DynamicObject}. The interceptor must have a method(s) according to the passed
	 * states.
	 *
	 * @param interceptor The interceptor to register
	 * @param states      The states to register the interceptor with
	 *
	 * @return The same pool
	 */
	public InterceptorPool register( DynamicObject interceptor, Key... states ) {
		Arrays.stream( states )
		    .forEach( state -> registerState( state ).register( interceptor ) );
		return this;
	}

	/**
	 * Register a Java Lambda Interceptor {@Link IInterceptorLambda} with the pool
	 * on the provided states.
	 *
	 * @param interceptor The interceptor lambda to register
	 * @param states      The states to register the interceptor with
	 *
	 * @return The same pool
	 */
	public InterceptorPool register( IInterceptorLambda interceptor, Key... states ) {
		Arrays.stream( states )
		    .forEach( state -> registerState( state ).register( DynamicObject.of( interceptor ) ) );
		return this;
	}

	/**
	 * Unregister an interceptor from the provided states.
	 *
	 * @param interceptor The interceptor to unregister
	 * @param states      The states to unregister the interceptor from
	 *
	 * @return The same pool
	 */
	public InterceptorPool unregister( DynamicObject interceptor, Key... states ) {
		Arrays.stream( states )
		    .forEach( state -> {
			    if ( hasState( state ) ) {
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
	 * @return The same pool
	 */
	public InterceptorPool unregister( DynamicObject interceptor ) {
		interceptionStates.values()
		    .stream()
		    .forEach( state -> state.unregister( interceptor ) );
		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Announcements Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Announce an event with no data using a Key object as the state
	 *
	 * @param state The state key to announce
	 */
	public void announce( Key state ) {
		announce( state, new Struct() );
	}

	/**
	 * Announce a BoxEvent with no data using a BoxEvent object as the state
	 *
	 * @param state The state key to announce
	 */
	public void announce( BoxEvent state ) {
		announce( state.key(), new Struct() );
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
	 * @param state The state to announce
	 * @param data  The data to announce
	 */
	public void announce( String state, IStruct data ) {
		announce( this.keyRegistry.computeIfAbsent( state, Key::of ), data );
	}

	/**
	 * Announce an event with the provided {@link IStruct} of data.
	 *
	 * @param state The state key to announce
	 * @param data  The data to announce
	 */
	public void announce( Key state, IStruct data ) {
		announce( state, data, this.runtime.getRuntimeContext() );
	}

	/**
	 * Announce an event with the provided {@link IStruct} of data and context
	 *
	 * @param state The state key to announce
	 * @param data  The data to announce
	 */
	public void announce( Key state, IStruct data, IBoxContext context ) {
		if ( hasState( state ) ) {
			// logger.trace( "InterceptorService.announce() - announcing {}", state.getName() );
			try {
				getState( state ).announce( data, context );
			} catch ( AbortException e ) {
				throw e;
			} catch ( Exception e ) {
				String errorMessage = String.format( "Errors announcing [%s] interception", state.getName() );
				logger.error( errorMessage, e );
				throw new BoxRuntimeException( errorMessage, e );
			}
			// logger.trace( "Finished announcing {}", state.getName() );
		} else {
			// logger.trace( "InterceptorService.announce() - No state found for: {}", state.getName() );
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Async Announcements Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Announce an event with no data using a Key object as the state asynchronously
	 *
	 * @param state The state key to announce
	 *
	 * @return A CompletableFuture of the data or null if the state does not exist
	 */
	public CompletableFuture<IStruct> announceAsync( Key state ) {
		return announceAsync( state, new Struct() );
	}

	/**
	 * Announce a BoxEvent with no data using a BoxEvent object as the state asynchronously
	 *
	 * @param state The state key to announce
	 *
	 * @return A CompletableFuture of the data or null if the state does not exist
	 */
	public CompletableFuture<IStruct> announceAsync( BoxEvent state ) {
		return announceAsync( state.key(), new Struct() );
	}

	/**
	 * Announce an event with the provided {@link IStruct} of data asynchronously.
	 *
	 * @param state The state to announce
	 * @param data  The data to announce
	 *
	 * @return A CompletableFuture of the data or null if the state does not exist
	 */
	public CompletableFuture<IStruct> announceAsync( BoxEvent state, IStruct data ) {
		return announceAsync( state.key(), data );
	}

	/**
	 * Announce an event with the provided {@link IStruct} of data asynchronously.
	 *
	 * @param state The state to announce
	 * @param data  The data to announce
	 *
	 * @return A CompletableFuture of the data or null if the state does not exist
	 */
	public CompletableFuture<IStruct> announceAsync( String state, IStruct data ) {
		return announceAsync( this.keyRegistry.computeIfAbsent( state, Key::of ), data );
	}

	/**
	 * Announce an event with the provided {@link IStruct} of data asynchronously.
	 *
	 * @param state The state key to announce
	 * @param data  The data to announce
	 *
	 * @return A CompletableFuture of the data or null if the state does not exist
	 */
	public CompletableFuture<IStruct> announceAsync( Key state, IStruct data ) {
		return announceAsync( state, data, this.runtime.getRuntimeContext() );
	}

	/**
	 * Announce an event with the provided {@link IStruct} of data and context asynchronously
	 *
	 * @param state The state key to announce
	 * @param data  The data to announce
	 *
	 * @return A CompletableFuture of the data or null if the state does not exist
	 */
	public CompletableFuture<IStruct> announceAsync( Key state, IStruct data, IBoxContext context ) {
		if ( hasState( state ) ) {
			return CompletableFuture.supplyAsync( () -> {
				try {
					getState( state ).announce( data, context );
					return data;
				} catch ( Exception e ) {
					String errorMessage = String.format( "Errors announcing [%s] interception", state.getName() );
					logger.error( errorMessage, e );
					throw new BoxRuntimeException( errorMessage, e );
				}
			} );
		}

		return null;
	}

}
