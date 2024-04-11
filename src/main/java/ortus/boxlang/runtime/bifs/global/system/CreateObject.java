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

import java.util.HashMap;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
public class CreateObject extends BIF {

	ClassLocator classLocator = ClassLocator.getInstance();

	/**
	 * Constructor
	 */
	public CreateObject() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.type ),
		    new Argument( true, "string", Key.className )
		};
	}

	/**
	 * Creates a new object representation
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.type The type of object to create
	 *
	 * @argument.className A classname for a component/class request or the java class to create
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		if ( arguments.getAsString( Key.type ).equalsIgnoreCase( "java" ) ) {
			return classLocator.load( context, "java:" + arguments.getAsString( Key.className ), context.getCurrentImports() );
		} else if ( arguments.getAsString( Key.type ).equalsIgnoreCase( "component" ) ) {
			return classLocator.load( context, "bx:" + arguments.getAsString( Key.className ), context.getCurrentImports() )
			    .invokeConstructor( context, Key.noInit )
			    .unWrapBoxLangClass();
		} else {
			// Announce an interception so that modules can contribute to object creation requests
			HashMap<Key, Object> interceptorArgs = new HashMap<Key, Object>() {

				{
					put( Key.response, null );
					put( Key.context, context );
					put( Key.arguments, arguments );
				}
			};
			interceptorService.announce( BoxEvent.ON_CREATEOBJECT_REQUEST, new Struct( interceptorArgs ) );
			if ( interceptorArgs.get( Key.response ) != null ) {
				return interceptorArgs.get( Key.response );
			} else {
				throw new BoxRuntimeException( "Unsupported type: " + arguments.getAsString( Key.type ) );
			}
		}
	}
}
