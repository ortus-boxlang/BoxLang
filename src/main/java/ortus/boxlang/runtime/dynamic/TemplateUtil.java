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
package ortus.boxlang.runtime.dynamic;

import java.util.Map;

import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.components.net.HTTP;
import ortus.boxlang.runtime.components.net.HTTPParam;
import ortus.boxlang.runtime.components.system.Dump;
import ortus.boxlang.runtime.components.system.Include;
import ortus.boxlang.runtime.components.system.SaveContent;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * I am a proof of concept for running templating components generically
 */
public class TemplateUtil {

	// This needs to change to actually looking up the component in a registry and running it dynamically
	// Components are designed to be singletons.
	// Use a ComponentDescriptor like we do with BIFs and member methods.
	private static Map<Key, Component> registry = Map.of(
	    Key.dump, new Dump(),
	    Key.HTTP, new HTTP(),
	    Key.HTTPParam, new HTTPParam(),
	    Key.of( "include" ), new Include(),
	    Key.of( "SaveContent" ), new SaveContent()
	);

	// This method needs to become the invoke method in the ComponentDescriptor to run a given component
	public static void doComponent( IBoxContext context, Key name, IStruct attributes, Component.ComponentBody componentBody ) {
		if ( registry.containsKey( name ) ) {
			Component component = registry.get( name );
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
				attribute.validate( context, component, attributes );
			}
			// Invoke the component here. The component is responsible for calling its body, if one exists.
			component.invoke( context, attributes, componentBody );

		} else {
			throw new BoxRuntimeException( "Component [" + name.getName() + "] not implemented yet" );
		}
	}

}