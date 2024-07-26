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
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.components.ComponentDescriptor;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * The {@code ComponentService} is in charge of managing the runtime's built-in components.
 * It will also be used by the module services to register components.
 */
public class ComponentService extends BaseService {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Logger
	 */
	private static final Logger				logger				= LoggerFactory.getLogger( ComponentService.class );

	/**
	 * The set of components registered with the service
	 */
	private Map<Key, ComponentDescriptor>	componentRegistry	= new ConcurrentHashMap<>();

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
	public ComponentService( BoxRuntime runtime ) {
		super( runtime, Key.componentService );
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
		var timerLabel = "componentservice-loadcomponentregistry";
		BoxRuntime.timerUtil.start( timerLabel );

		try {
			loadComponentRegistry();
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Cannot load components", e );
		}

		// Log it
		logger.info(
		    "+ Component Service: Registered [{}] components in [{}] ms",
		    getComponentCount(),
		    BoxRuntime.timerUtil.stopAndGetMillis( timerLabel )
		);
	}

	/**
	 * The shutdown event is fired when the runtime shuts down
	 *
	 * @param force Whether or not to force the shutdown
	 */
	@Override
	public void onShutdown( Boolean force ) {
		logger.debug( "+ Component Service: Shutting down" );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Component Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Returns the number of components registered with the service
	 *
	 * @return The number of components registered with the service
	 */
	public long getComponentCount() {
		return this.componentRegistry.size();
	}

	/**
	 * Returns the names of the components registered with the service
	 *
	 * @return A set of component names
	 */
	public String[] getComponentNames() {
		return this.componentRegistry.keySet()
		    .stream()
		    .sorted()
		    .map( Key::getName )
		    .toArray( String[]::new );
	}

	/**
	 * Returns whether or not the service has a component with the given name
	 *
	 * @param name The name of the component
	 *
	 * @return Whether or not the service has a component with the given name
	 */
	public Boolean hasComponent( String name ) {
		return hasComponent( Key.of( name ) );
	}

	/**
	 * Returns whether or not the service has a component with the given name
	 *
	 * @param name The key name of the component
	 *
	 * @return Whether or not the service has a component with the given name
	 */
	public Boolean hasComponent( Key name ) {
		return this.componentRegistry.containsKey( name );
	}

	/**
	 * Returns the component with the given name
	 *
	 * @param name The name of the component
	 *
	 * @return The component with the given name or null if none exists
	 */
	public ComponentDescriptor getComponent( String name ) {
		return getComponent( Key.of( name ) );
	}

	/**
	 * Returns the component with the given name
	 *
	 * @param name The name of the component
	 *
	 * @return The component with the given name or null if none exists
	 */
	public ComponentDescriptor getComponent( Key name ) {
		return this.componentRegistry.get( name );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Registration Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Registers a component with the service using a
	 * descriptor, a name, and if we want to override if it exists, else it will throw an exception
	 *
	 * @param descriptor The descriptor for the component
	 * @param name       The name of the component
	 * @param force      Whether or not to force the registration, usually it means an overwrite
	 *
	 * @throws BoxRuntimeException If the component already exists
	 */
	public void registerComponent( ComponentDescriptor descriptor, Key name, Boolean force ) {
		if ( hasComponent( descriptor.name ) && !force ) {
			throw new BoxRuntimeException( " component " + name.getName() + " already exists" );
		}
		this.componentRegistry.put( name, descriptor );
	}

	/**
	 * Registers a component with the service only using a descriptor.
	 * We take the name from the descriptor itself {@code descriptor.name} and we do not force the registration.
	 *
	 * @param descriptor The descriptor for the component
	 * @param force      Whether or not to force the registration, usually it means an overwrite
	 *
	 * @throws BoxRuntimeException If the component already exists
	 */
	public void registerComponent( ComponentDescriptor descriptor, Boolean force ) {
		registerComponent( descriptor, descriptor.name, force );
	}

	/**
	 * Unregisters a component with the service
	 *
	 * @param name The name of the component
	 */
	public void unregisterComponent( Key name ) {
		this.componentRegistry.remove( name );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Loading
	 * --------------------------------------------------------------------------
	 */

	/**
	 * This method loads all of the components into the service by scanning the
	 * {@code ortus.boxlang.runtime.components.} package.
	 *
	 * @throws IOException If there is an error loading the components
	 */
	public void loadComponentRegistry() throws IOException {
		ServiceLoader
		    .load( Component.class, BoxRuntime.class.getClassLoader() )
		    .stream()
		    .parallel()
		    .map( ServiceLoader.Provider::type )
		    // .peek( targetClass -> System.out.println( "targetClass: " + targetClass.getName() ) )
		    .forEach( targetClass -> registerComponent( targetClass, null, null ) );
	}

	/**
	 * Registers a component with the service.
	 * This is mostly called by the component loader.
	 *
	 * @param componentClass The component class
	 * @param component      The component
	 * @param module         The module the component belongs to
	 *
	 * @throws BoxRuntimeException If no component class or component was provided
	 */
	public void registerComponent( Class<?> componentClass, Component component, String module ) {
		// If no componentClass is provided, get it from the component instance
		if ( componentClass == null && component != null ) {
			componentClass = component.getClass();
			// if neither was provided, holler at the user
		} else if ( componentClass == null ) {
			throw new BoxRuntimeException( "Cannot register component because no component class or component was provided" );
		}

		// Component Properties
		Key				customName				= Key.of( "" );
		String			alias					= "";
		boolean			allowsBody				= false;
		boolean			requiresBody			= false;
		Key				className				= Key.of( componentClass.getSimpleName() );

		// Parse the annotations
		BoxComponent[]	commponentAnnotations	= componentClass.getAnnotationsByType( BoxComponent.class );

		for ( BoxComponent annotation : commponentAnnotations ) {
			customName		= annotation.name().length() > 0 ? Key.of( annotation.name() ) : className;
			allowsBody		= annotation.allowsBody();
			requiresBody	= annotation.requiresBody();
			alias			= annotation.alias();

			ComponentDescriptor descriptor = new ComponentDescriptor(
			    // Use the annotation name or the class name
			    customName,
			    componentClass,
			    module,
			    null,
			    component,
			    allowsBody,
			    requiresBody
			);

			// Register normal first
			registerComponent( descriptor, true );

			// Do we have an alias?
			if ( alias.length() > 0 ) {
				registerComponent( descriptor, Key.of( alias ), true );
			}
		}

	}

}
