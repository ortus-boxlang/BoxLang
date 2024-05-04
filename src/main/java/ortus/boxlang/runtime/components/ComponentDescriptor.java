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
package ortus.boxlang.runtime.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.components.Component.BodyResult;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * This class is used to describe a component
 * as it can be a component or a member component or both or coming from a module
 * It also lazily creates the component instance and caches it upon first use
 */
public class ComponentDescriptor {

	/**
	 * component name
	 */
	public Key						name;

	/**
	 * component allows a body. Used to help validate parsing
	 */
	public Boolean					allowsBody;

	/**
	 * component requires a body. Used to help validate parsing
	 */
	public Boolean					requiresBody;

	/**
	 * component class
	 */
	public Class<?>					componentClass;

	/**
	 * Module name, or null if core
	 */
	public String					module;

	/**
	 * component instance, lazily created
	 */
	public volatile Component		componentInstance;

	/**
	 * The interceptor service helper
	 */
	protected InterceptorService	interceptorService	= BoxRuntime.getInstance().getInterceptorService();

	/**
	 * Constructor for a component
	 *
	 * @param name              The name of the component
	 * @param componentClass    The class of the component
	 * @param module            The module name, or null if core
	 * @param componentInstance The component instance or null by default
	 */
	public ComponentDescriptor(
	    Key name,
	    Class<?> componentClass,
	    String module,
	    String namespace,
	    Component componentInstance,
	    Boolean allowsBody,
	    Boolean requiresBody ) {
		this.name				= name;
		this.componentClass		= componentClass;
		this.module				= module;
		this.componentInstance	= componentInstance;
		this.allowsBody			= allowsBody;
		this.requiresBody		= requiresBody;
	}

	/**
	 * Descriptor belongs to a modules or not
	 *
	 * @return True if the descriptor belongs to a module, false otherwise
	 */
	public Boolean hasModule() {
		return module != null;
	}

	/**
	 * Descriptor allows a body or not
	 */
	public Boolean allowsBody() {
		return allowsBody || requiresBody;
	}

	/**
	 * Descriptor requires a body or not
	 */
	public Boolean requiresBody() {
		return requiresBody;
	}

	/**
	 * Get the component instance for this descriptor and lazily create it if needed
	 *
	 * @return The component instance
	 */
	public Component getComponent() {
		if ( this.componentInstance == null ) {
			synchronized ( this ) {
				// Double check inside lock
				if ( this.componentInstance == null ) {
					this.componentInstance = ( ( Component ) DynamicObject.of( this.componentClass )
					    .invokeConstructor( ( IBoxContext ) null, new Object[] {} )
					    .getTargetInstance() )
					    .setName( name );
					interceptorService.announce(
					    BoxEvent.ON_COMPONENT_INSTANCE,
					    new Struct(
					        Map.of(
					            Key.instance,
					            this.componentInstance,
					            Key._NAME,
					            this.name,
					            Key.descriptor,
					            this
					        )
					    )
					);
				}
			}
		}
		return this.componentInstance;
	}

	/**
	 * Invoke the component with no arguments
	 *
	 * @param context The context
	 *
	 * @return The result of the invocation
	 */
	public BodyResult invoke( IBoxContext context, Component.ComponentBody componentBody ) {
		return invoke( context, Struct.EMPTY, componentBody );
	}

	/**
	 * Invoke the component with attributes
	 *
	 * @param context    The context
	 * @param attributes The attributes
	 *
	 * @return The result of the invocation
	 */
	public BodyResult invoke( IBoxContext context, IStruct attributes, Component.ComponentBody componentBody ) {
		// the module component has special handling of attributes
		if ( name.equals( Key.module ) ) {
			return invokeModule( context, attributes, componentBody );
		}
		Component component = getComponent();
		// if attributeCollection key exists and is a struct, merge it into the main attributes and delete it
		// When merging, don't overwrite existing keys
		if ( attributes.containsKey( Key.attributeCollection ) && attributes.get( Key.attributeCollection ) instanceof IStruct attrCol ) {
			for ( var key : attrCol.keySet() ) {
				if ( !attributes.containsKey( key ) ) {
					attributes.put( key, attrCol.get( key ) );
				}
			}
			attributes.remove( Key.attributeCollection );
		}

		// call validators on attributes)
		for ( var attribute : component.getDeclaredAttributes() ) {
			attribute.validate( context, component.getName(), attributes );
		}
		// Invoke the component here. The component is responsible for calling its body, if one exists.
		return component.invoke( context, attributes, componentBody );
	}

	/**
	 * Invoke the component with attributes
	 *
	 * @param context    The context
	 * @param attributes The attributes
	 *
	 * @return The result of the invocation
	 */
	public BodyResult invokeModule( IBoxContext context, IStruct attributes, Component.ComponentBody componentBody ) {
		Component	component			= getComponent();
		Struct		moduleAttributes	= new Struct();
		// Add all attributes not named "name" or "template"
		List<Key>	keysToRemove		= new ArrayList<>();
		for ( var key : attributes.keySet() ) {
			if ( !key.equals( Key._NAME ) && !key.equals( Key.template ) ) {
				moduleAttributes.put( key, attributes.get( key ) );
				keysToRemove.add( key );
			}
		}

		// Remove the attributes outside the loop
		for ( var key : keysToRemove ) {
			attributes.remove( key );
		}
		// if attributeCollection key exists and is a struct, merge it into the main attributes and delete it
		// When merging, don't overwrite existing keys
		if ( moduleAttributes.containsKey( Key.attributeCollection ) && moduleAttributes.get( Key.attributeCollection ) instanceof IStruct attrCol ) {
			for ( var key : attrCol.keySet() ) {
				if ( !moduleAttributes.containsKey( key ) ) {
					moduleAttributes.put( key, attrCol.get( key ) );
				}
			}
			moduleAttributes.remove( Key.attributeCollection );
		}
		attributes.put( Key.attributes, moduleAttributes );
		// call validators on attributes)
		for ( var attribute : component.getDeclaredAttributes() ) {
			attribute.validate( context, component.getName(), attributes );
		}
		// Invoke the component here. The component is responsible for calling its body, if one exists.
		return component.invoke( context, attributes, componentBody );
	}

}
