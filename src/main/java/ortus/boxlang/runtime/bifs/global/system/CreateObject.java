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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
public class CreateObject extends BIF {

	/**
	 * How we find classes
	 */
	private static final ClassLocator	CLASS_LOCATOR	= ClassLocator.getInstance();
	// Resolver Prefixes
	private static final String			BX_PREFIX		= "bx";
	private static final String			JAVA_PREFIX		= "java";

	/**
	 * Constructor
	 */
	public CreateObject() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.STRING, Key.type, "class" ),
		    new Argument( false, Argument.STRING, Key.className )
		};
	}

	/**
	 * Creates a new object representation
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.type The type of object to create: java, bx, or component
	 *
	 * @argument.className A classname for a component/class request or the java class to create
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	type		= arguments.getAsString( Key.type );
		String	className	= arguments.getAsString( Key.className );

		if ( className == null ) {
			className	= type;
			type		= "class";
		}

		// Java Classes
		if ( type.equalsIgnoreCase( JAVA_PREFIX ) ) {
			return CLASS_LOCATOR.load(
			    context,
			    className,
			    JAVA_PREFIX,
			    true,
			    context.getCurrentImports()
			);
		}

		// Class and Component left for backward compatibility
		if ( type.equalsIgnoreCase( "component" ) ||
		    type.equalsIgnoreCase( "class" ) ) {

			// Load up the class
			DynamicObject result = CLASS_LOCATOR.load(
			    context,
			    className,
			    BX_PREFIX,
			    true,
			    context.getCurrentImports()
			);

			// If it's a class, bootstrap the constructor
			if ( IClassRunnable.class.isAssignableFrom( result.getTargetClass() ) ) {
				return result.invokeConstructor( context, Key.noInit ).unWrapBoxLangClass();
			} else {
				// Otherwise, an interface-- just return it. These are singletons
				return result.unWrapBoxLangClass();
			}
		}

		// Uknown, let's see if we can intercept it
		// Announce an interception so that modules can contribute to object creation requests
		// If the response is set, we'll use that as the object to return
		IStruct interceptorArgs = Struct.of(
		    Key.response, null,
		    Key.context, context,
		    Key.arguments, arguments
		);
		interceptorService.announce( BoxEvent.ON_CREATEOBJECT_REQUEST, interceptorArgs );
		if ( interceptorArgs.get( Key.response ) != null ) {
			return interceptorArgs.get( Key.response );
		}

		throw new BoxRuntimeException( "Unsupported type: " + arguments.getAsString( Key.type ) );
	}
}
