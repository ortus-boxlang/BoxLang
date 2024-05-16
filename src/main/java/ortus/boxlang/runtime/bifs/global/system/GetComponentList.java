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
package ortus.boxlang.runtime.bifs.global.system;

import java.util.Arrays;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.components.ComponentDescriptor;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

@BoxBIF
public class GetComponentList extends BIF {

	/**
	 * Constructor
	 */
	public GetComponentList() {
		super();
	}

	/**
	 * Get a collection of all the registered global components in the runtime.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 */
	public IStruct _invoke( IBoxContext context, ArgumentsScope arguments ) {

		IStruct components = new Struct( Struct.TYPES.LINKED );

		// Build a struct of the global components
		Arrays.stream( componentService.getComponentNames() )
		    .forEach( componentName -> {
			    ComponentDescriptor descriptor = componentService.getComponent( componentName );
			    components.put(
			        componentName,
			        Struct.of(
			            "module", descriptor.hasModule() ? descriptor.module : "---",
			            "className", descriptor.componentClass.getCanonicalName(),
			            "allowsBody", descriptor.allowsBody(),
			            "requiresBody", descriptor.requiresBody()
			        )
			    );
		    } );

		return components;
	}

}
