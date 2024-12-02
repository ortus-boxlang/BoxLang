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

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BIFDescriptor;
import ortus.boxlang.runtime.bifs.BIFNamespace;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.bifs.MemberDescriptor;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.ObjectRef;

/**
 * The {@code FunctionService} is in charge of managing the runtime's built-in functions.
 * It will also be used by the module services to register functions.
 */
public class FunctionService extends BaseService {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Logger
	 */
	private static final Logger								logger			= LoggerFactory.getLogger( FunctionService.class );

	/**
	 * The set of global functions registered with the service
	 */
	private Map<Key, BIFDescriptor>							globalFunctions	= new ConcurrentHashMap<>();

	/**
	 * The set of namespaced functions registered with the service
	 */
	private Map<Key, BIFNamespace>							namespaces		= new ConcurrentHashMap<>();

	/**
	 * Represents the set of registered member methods.
	 * The key is the name of the method, and the value is a map
	 * where each entry consists of a BoxLangType and its corresponding MemberDescriptor.
	 *
	 * (@code
	 * { "foo" : { BoxLangType.ARRAY : MemberDescriptor, BoxLangType.STRING : MemberDescriptor } }
	 * )
	 */
	private Map<Key, Map<BoxLangType, MemberDescriptor>>	memberMethods	= new ConcurrentHashMap<>();

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param runtime The runtime instance
	 */
	public FunctionService( BoxRuntime runtime ) {
		super( runtime, Key.functionService );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Runtime Service Event Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The startup event is fired when the runtime starts up
	 */
	@Override
	public void onStartup() {
		var timerLabel = "functionservice-loadglobalfunctions" + System.currentTimeMillis();
		BoxRuntime.timerUtil.start( timerLabel );

		try {
			loadGlobalFunctions();
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Cannot load global functions", e );
		}

		// Log it
		logger.info(
		    "+ Function Service: Registered [{}] global functions in [{}] ms",
		    getGlobalFunctionCount(),
		    BoxRuntime.timerUtil.stopAndGetMillis( timerLabel )
		);
	}

	/**
	 * The shutdown event is fired when the runtime shuts down
	 *
	 * @param force Whether the shutdown is forced
	 */
	@Override
	public void onShutdown( Boolean force ) {
		logger.info( "FunctionService.onShutdown()" );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Global Function Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Returns the number of global functions registered with the service
	 *
	 * @return The number of global functions registered with the service
	 */
	public long getGlobalFunctionCount() {
		return this.globalFunctions.size();
	}

	/**
	 * Returns the names of the global functions registered with the service
	 *
	 * @return A set of global function names
	 */
	public String[] getGlobalFunctionNames() {
		return this.globalFunctions.keySet()
		    .stream()
		    .sorted()
		    .map( Key::getName )
		    .toArray( String[]::new );
	}

	/**
	 * Returns whether or not the service has a global function with the given name
	 *
	 * @param name The name of the global function
	 *
	 * @return Whether or not the service has a global function with the given name
	 */
	public Boolean hasGlobalFunction( String name ) {
		return hasGlobalFunction( Key.of( name ) );
	}

	/**
	 * Returns whether or not the service has a global function with the given name
	 *
	 * @param name The key name of the global function
	 *
	 * @return Whether or not the service has a global function with the given name
	 */
	public Boolean hasGlobalFunction( Key name ) {
		return this.globalFunctions.containsKey( name );
	}

	/**
	 * Returns the global function with the given name
	 *
	 * @param name The name of the global function
	 *
	 * @return The global function with the given name or null if none exists
	 */
	public BIFDescriptor getGlobalFunction( String name ) {
		return getGlobalFunction( Key.of( name ) );
	}

	/**
	 * Returns the global function with the given name.
	 *
	 * @param name The name of the global function
	 *
	 * @return The global function with the given name or null if none exists
	 */
	public BIFDescriptor getGlobalFunction( Key name ) {
		return this.globalFunctions.get( name );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Member Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Returns the member method with the given name and type by verifying if the passed object can be cast to that type
	 *
	 * @param name   The name of the member method
	 * @param object An object to cast to the type of the member method
	 *
	 * @return The member method with the given name and type or null if none exists
	 */
	public MemberDescriptor getMemberMethod( IBoxContext context, Key name, Object object ) {
		return getMemberMethod( context, name, ObjectRef.of( object ) );
	}

	/**
	 * Returns the member method with the given name and type by verifying if the passed object can be cast to that type
	 *
	 * @param context The context to use for casting
	 * @param name    The name of the member method to get
	 * @param object  An object to cast to the type of the member method to
	 *
	 * @return The member method with the given name and type or null if none exists
	 */
	public MemberDescriptor getMemberMethod( IBoxContext context, Key name, ObjectRef object ) {
		// For obj.method() we first look for a registered member method of this name
		Map<BoxLangType, MemberDescriptor> targetMethodMap = this.memberMethods.get( name );
		if ( targetMethodMap != null ) {
			// Then we see if our object is castable to any of the possible types for that method registered
			// Breaks on first successful cast
			for ( Map.Entry<BoxLangType, MemberDescriptor> entry : targetMethodMap.entrySet() ) {
				MemberDescriptor descriptor = entry.getValue();
				// System.out.println( "descriptor.type: " + descriptor.type.toString() );

				// A workaround to let a member method can associate with up to 3 custom types
				if ( descriptor.type == BoxLangType.CUSTOM || descriptor.type == BoxLangType.CUSTOM2 || descriptor.type == BoxLangType.CUSTOM3 ) {
					if ( descriptor.customClass.isInstance( object.get() ) ) {
						return descriptor;
					}
				}

				CastAttempt<?> castAttempt = GenericCaster.attempt( context, object.get(), entry.getKey() );
				if ( castAttempt.wasSuccessful() ) {
					object.set( castAttempt.get() );
					return descriptor;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the member method with the given name and BoxLangType
	 *
	 * @param name The name of the member method
	 * @param type The BoxLangType of the member method requested
	 *
	 * @return The member method with the given name and BoxLangType or null if none exists
	 */
	public MemberDescriptor getMemberMethod( Key name, BoxLangType type ) {
		// For obj.method() we first look for a registered member method of this name
		Map<BoxLangType, MemberDescriptor> targetMethodMap = this.memberMethods.get( name );
		if ( targetMethodMap != null ) {
			// Then we see if this type is applicable, else returns null for the BoxLangType
			return targetMethodMap.get( type );
		}
		return null;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Registration Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Registers a global function with the service using a
	 * descriptor, a name, and if we want to override if it exists, else it will throw an exception
	 *
	 * @param descriptor The descriptor for the global function
	 * @param name       The name of the global function
	 * @param force      Whether or not to force the registration, usually it means an overwrite
	 *
	 * @throws BoxRuntimeException If the global function already exists
	 */
	public void registerGlobalFunction( BIFDescriptor descriptor, Key name, Boolean force ) {
		if ( hasGlobalFunction( descriptor.name ) && !force ) {
			throw new BoxRuntimeException( "Global function " + name.getName() + " already exists" );
		}
		this.globalFunctions.put( name, descriptor );
	}

	/**
	 * Registers a global function with the service only using a descriptor.
	 * We take the name from the descriptor itself {@code descriptor.name} and we do not force the registration.
	 *
	 * @param descriptor The descriptor for the global function
	 *
	 * @throws BoxRuntimeException If the global function already exists
	 */
	public void registerGlobalFunction( BIFDescriptor descriptor ) {
		registerGlobalFunction( descriptor, descriptor.name, false );
	}

	/**
	 * Unregisters a global function with the service
	 *
	 * @param name The name of the global function
	 */
	public void unregisterGlobalFunction( Key name ) {
		this.globalFunctions.remove( name );
	}

	/**
	 * Register a member method with the service using a member key and a {@link MemberDescriptor}
	 *
	 * @param memberKey  The key for the member method: Ex: "append", "insert", "remove"
	 * @param descriptor The descriptor for the member method: {@link MemberDescriptor}
	 */
	public void registerMemberMethod( Key memberKey, MemberDescriptor descriptor ) {

		// Make sure the container for the member key exists
		// Ex: memberMethods[ "foo" ] = { BoxLangType.ARRAY : MemberDescriptor, BoxLangType.STRING : MemberDescriptor }
		synchronized ( this.memberMethods ) {
			this.memberMethods.putIfAbsent( memberKey, Collections.synchronizedMap( new LinkedHashMap<>() ) );
		}

		// Now add them up
		this.memberMethods.get( memberKey ).put( descriptor.type, descriptor );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Global Loading
	 * --------------------------------------------------------------------------
	 */

	/**
	 * This method loads all of the global functions into the service by scanning the
	 * {@code ortus.boxlang.runtime.bifs.global} package.
	 *
	 * @throws IOException If there is an error loading the global functions
	 */
	public void loadGlobalFunctions() throws IOException {
		ServiceLoader
		    .load( BIF.class, BoxRuntime.class.getClassLoader() )
		    .stream()
		    .parallel()
		    .map( ServiceLoader.Provider::type )
		    // .peek( targetClass -> System.out.println( "targetClass: " + targetClass.getName() ) )
		    .forEach( targetClass -> processBIFRegistration( targetClass, null, null ) );
	}

	/**
	 * This method process a raw BIF registration usually from a {@code BIFClass} and a {@code function} instance.
	 * This creates an internal {@code BIFDescriptor} and registers it with the service.
	 *
	 * This is main way to register Java based BIFs with the runtime.
	 *
	 * @param BIFClass The BIF class to process for registration
	 * @param function The global function to process for registration
	 * @param module   The module the global function belongs to when registering
	 *
	 * @throws BoxRuntimeException If no BIF class or function was provided
	 */
	public void processBIFRegistration( Class<?> BIFClass, BIF function, String module ) {
		// If no BIFClass is provided, get it from the function instance
		if ( BIFClass == null && function != null ) {
			BIFClass = function.getClass();
			// if neither was provided, holler at the user
		} else if ( BIFClass == null ) {
			throw new BoxRuntimeException( "Cannot register global function because no BIF class or function was provided" );
		}

		// We'll re-use this same BIFDescriptor for each annotation to ensure there's only ever one actual BIF instance.
		String			className		= BIFClass.getSimpleName();
		Key				classNameKey	= Key.of( className );
		BIFDescriptor	descriptor		= new BIFDescriptor(
		    classNameKey,
		    BIFClass,
		    module,
		    null,
		    true,
		    function
		);

		// Register BIF with default name or alias
		BoxBIF[]		bifAnnotations	= BIFClass.getAnnotationsByType( BoxBIF.class );
		for ( BoxBIF bif : bifAnnotations ) {
			registerGlobalFunction( descriptor, bif.alias().equals( "" ) ? classNameKey : Key.of( bif.alias() ), true );
		}

		// Register member methods
		BoxMember[] boxMemberAnnotations = BIFClass.getAnnotationsByType( BoxMember.class );
		for ( BoxMember member : boxMemberAnnotations ) {

			// Discover the member method name
			Key memberKey;
			if ( member.name().equals( "" ) ) {
				// Default member name for class ArrayFoo with BoxType of Array is just foo()
				memberKey = Key.of(
				    StringUtils.replace( className.toLowerCase(), member.type().name().toLowerCase(), "" )
				);
			} else {
				memberKey = Key.of( member.name() );
			}

			// Register the member method using the data and BIF Descriptor
			registerMemberMethod(
			    memberKey,
			    new MemberDescriptor(
			        memberKey,
			        member.type(),
			        member.customType(),
			        // Pass null if objectArgument is empty
			        member.objectArgument().equals( "" ) ? null : Key.of( member.objectArgument() ),
			        descriptor
			    )
			);
		}

	}

	/**
	 * --------------------------------------------------------------------------
	 * Namespace Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The count of registered namespaces
	 *
	 * @return The count of registered namespaces
	 */
	public long getNamespaceCount() {
		return this.namespaces.size();
	}
}
