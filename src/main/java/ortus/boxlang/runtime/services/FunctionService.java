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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BIFDescriptor;
import ortus.boxlang.runtime.bifs.BIFNamespace;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxBIFs;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.bifs.BoxMembers;
import ortus.boxlang.runtime.bifs.MemberDescriptor;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.loader.util.ClassDiscovery;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

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
	 * The location of the core bifs
	 */
	private static final String								FUNCTIONS_PACKAGE	= "ortus.boxlang.runtime.bifs";

	/**
	 * Logger
	 */
	private static final Logger								logger				= LoggerFactory.getLogger( FunctionService.class );

	/**
	 * The set of global functions registered with the service
	 */
	private Map<Key, BIFDescriptor>							globalFunctions		= new ConcurrentHashMap<>();

	/**
	 * The set of namespaced functions registered with the service
	 */
	private Map<Key, BIFNamespace>							namespaces			= new ConcurrentHashMap<>();

	/**
	 * Represents the set of registered member methods.
	 * The key is the name of the method, and the value is a map
	 * where each entry consists of a BoxLangType and its corresponding MemberDescriptor.
	 *
	 * (@code
	 * { "foo" : { BoxLangType.ARRAY : MemberDescriptor, BoxLangType.STRING : MemberDescriptor } }
	 * )
	 */
	private Map<Key, Map<BoxLangType, MemberDescriptor>>	memberMethods		= new ConcurrentHashMap<>();

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
		super( runtime );

		// Load global functions immediately
		try {
			loadGlobalFunctions();
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Cannot load global functions", e );
		}
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
		logger.info( "FunctionService.onStartup()" );
	}

	/**
	 * The configuration load event is fired when the runtime loads its configuration
	 */
	@Override
	public void onConfigurationLoad() {
		logger.info( "FunctionService.onConfigurationLoad()" );
	}

	/**
	 * The shutdown event is fired when the runtime shuts down
	 */
	@Override
	public void onShutdown() {
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
	 * Returns the global function with the given name
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
	public MemberDescriptor getMemberMethod( Key name, Object object ) {
		// For obj.method() we first look for a registered member method of this name
		Map<BoxLangType, MemberDescriptor> targetMethodMap = this.memberMethods.get( name );
		if ( targetMethodMap != null ) {
			// Then we see if our object is castable to any of the possible types for that method registered
			// Breaks on first successful cast
			for ( Map.Entry<BoxLangType, MemberDescriptor> entry : targetMethodMap.entrySet() ) {
				CastAttempt<?> castAttempt = GenericCaster.attempt( object, entry.getKey() );
				if ( castAttempt.wasSuccessful() ) {
					return entry.getValue();
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
	 * Registers a global function with the service
	 *
	 * @param descriptor The descriptor for the global function
	 *
	 * @throws IllegalArgumentException If the global function already exists
	 */
	public void registerGlobalFunction( BIFDescriptor descriptor ) throws IllegalArgumentException {
		if ( hasGlobalFunction( descriptor.name ) ) {
			throw new BoxRuntimeException( "Global function " + descriptor.name + " already exists" );
		}
		this.globalFunctions.put( Key.of( descriptor.name ), descriptor );
	}

	/**
	 * Registers a global function with the service. The BIF class needs to be annotated with {@link BoxBIF} or {@link BoxMember}
	 *
	 * @param BIFClass The BIF class
	 * @param module   The module the global function belongs to
	 *
	 * @throws IllegalArgumentException If the global function already exists
	 */
	public void registerGlobalFunction( Class<?> BIFClass, String module ) throws IllegalArgumentException {
		registerGlobalFunction( BIFClass, null, module );
	}

	/**
	 * Registers a global function with the service. The BIF class needs to be annotated with {@link BoxBIF} or {@link BoxMember}
	 *
	 * @param BIFClass The BIF class
	 * @param function The global function
	 *
	 * @throws IllegalArgumentException If the global function already exists
	 */
	public void registerGlobalFunction( BIF function, String module ) throws IllegalArgumentException {
		registerGlobalFunction( null, function, module );
	}

	/**
	 * Registers a global function with the service. The BIF class needs to be annotated with {@link BoxBIF} or {@link BoxMember}
	 *
	 * @param BIFClass The BIF class
	 * @param function The global function
	 * @param module   The module the global function belongs to
	 *
	 * @throws IllegalArgumentException If the global function already exists
	 */
	private void registerGlobalFunction( Class<?> BIFClass, BIF function, String module ) throws IllegalArgumentException {
		// If no BIFClass is provided, get it from the function instance
		if ( BIFClass == null && function != null ) {
			BIFClass = function.getClass();
			// if neither was provided, holler at the user
		} else if ( BIFClass == null ) {
			throw new BoxRuntimeException( "Cannot register global function because no BIF class or function was provided" );
		}

		// We'll re-use this same BIFDescriptor for each annotation to ensure there's onlky ever one actual BIF instance.
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
			globalFunctions.put(
			    // Use the annotation's alias, if present, if not, the name of the class.
			    bif.alias().equals( "" ) ? classNameKey : Key.of( bif.alias() ),
			    descriptor
			);
		}

		// Register member methods
		BoxMember[] boxMemberAnnotations = BIFClass.getAnnotationsByType( BoxMember.class );
		for ( BoxMember member : boxMemberAnnotations ) {
			Key memberKey;
			if ( member.name().equals( "" ) ) {
				// Default member name for class ArrayFoo with BoxType of Array is just foo()
				memberKey = Key.of( className.toLowerCase().replaceAll( member.type().name().toLowerCase(), "" ) );
			} else {
				memberKey = Key.of( member.name() );
			}
			// Since we're processing in parallel, syncronize the addition of new member method keys to our map
			synchronized ( memberMethods ) {
				if ( !memberMethods.containsKey( memberKey ) ) {
					memberMethods.put( memberKey, new ConcurrentHashMap<BoxLangType, MemberDescriptor>() );
				}
			}
			Map<BoxLangType, MemberDescriptor> memberMethods = this.memberMethods.get( memberKey );
			// member method map is in format memberMethods[ "nameOfMethod" ][ BoxLangType.ARRAY ] = MemberDesccriptor
			// Whih optimizes lookup for matching member methods at runtime.
			memberMethods.put(
			    member.type(),
			    new MemberDescriptor(
			        memberKey,
			        member.type(),
			        // Pass null if objectArgument is empty
			        member.objectArgument().equals( "" ) ? null : Key.of( member.objectArgument() ),
			        descriptor
			    )
			);
		}

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
		ClassDiscovery
		    .findAnnotatedClasses(
		        ( FUNCTIONS_PACKAGE + ".global" ).replace( '.', '/' ),
		        BoxBIF.class, BoxBIFs.class, BoxMember.class, BoxMembers.class
		    )
		    .parallel()
		    // Filter to subclasses of BIF
		    .filter( BIF.class::isAssignableFrom )
		    // Process each class
		    .forEach( targetClass -> registerGlobalFunction( targetClass, null, null ) );
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
