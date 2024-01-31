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

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * I am a proof of concept for running templating components generically
 */
public class TemplateUtil {

	@FunctionalInterface
	public interface ContextConsumer {

		void accept( IBoxContext context );
	}

	public static void doComponent( IBoxContext context, Key name, IStruct attributes, ContextConsumer componentBody ) {
		if ( name.equals( Key.of( "Brad" ) ) ) {
			System.out.println( "Brad component attributes: " + attributes.asString() );
			if ( componentBody != null ) {
				componentBody.accept( context );
			}
			System.out.println( "end of brad component" );
		} else if ( name.equals( Key.of( "sdf" ) ) ) {
			System.out.println( "sdf component attributes: " + attributes.asString() );
		} else if ( name.equals( Key.of( "http" ) ) ) {
			System.out.println( "http component attributes: " + attributes.asString() );
		} else {
			throw new BoxRuntimeException( "Component [" + name.getName() + "] not implemented yet" );
		}
	}

}