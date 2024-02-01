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
import ortus.boxlang.runtime.components.validators.Validator;
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
			// call validators on attributes)
			for ( var attribute : component.getDeclaredAttributes() ) {
				// Automatically enforce type, if set
				Validator.TYPE.validate( context, component, attribute, attributes );
				// Automatically enforce default value, if set
				Validator.DEFAULT_VALUE.validate( context, component, attribute, attributes );
				// Now run the rest of the validators
				attribute.validate( context, component, attributes );
			}
			component.invoke( context, attributes, componentBody );
		} else {
			throw new BoxRuntimeException( "Component [" + name.getName() + "] not implemented yet" );
		}
	}

}