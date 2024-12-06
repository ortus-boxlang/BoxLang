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

import ortus.boxlang.runtime.BoxRuntime;
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
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
public class CreateObject extends BIF {

	/**
	 * How we find classes
	 */
	private static final ClassLocator	CLASS_LOCATOR	= BoxRuntime.getInstance().getClassLocator();
	private static final String			CLASS_TYPE		= "class";
	private static final String			COMPONENT_TYPE	= "component";

	/**
	 * Constructor
	 */
	public CreateObject() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.STRING, Key.type, CLASS_TYPE ),
		    new Argument( false, Argument.STRING, Key.className ),
		    new Argument( false, Argument.ANY, Key.properties )
		};
	}

	/**
	 * Creates a new object representation according to the {@code type} and {@code className} arguments.
	 * <p>
	 * Available <strong>types</strong> are:
	 * <ul>
	 * <li><strong>class/component</strong> - Creates a new instance of a BoxLang class (Default if not used)</li>
	 * <li><strong>java</strong> - Creates a new instance of a Java class</li>
	 * <li><strong>{anything}</strong> - Passes the request to the {@code BoxEvent.ON_CREATEOBJECT_REQUEST} event for further processing</li>
	 * </ul>
	 * <p>
	 * If the type requested is not supported, then it passes to an interception call to the {@code BoxEvent.ON_CREATEOBJECT_REQUEST} event,
	 * so any listeners can contribute to the object creation request (if any). If there are no listeners, an exception is thrown.
	 * <p>
	 * You can also target an explicit class from a loaded BoxLang module by using the {@code @moduleName} suffix.
	 * Example: {@code createObject( 'class', 'class.name.path@module' )}
	 * <p>
	 * The <strong>properties</strong> is an optional argument that can be used to pass to the object creation process according to the type.
	 * <ul>
	 * <li><strong>class/component</strong> - The properties are not used</li>
	 * <li><strong>java</strong> - The properties can be a single or an array of absolute path(s) to a directory containing Jars/Classes, or absolute path(s) to specific Jars/Classes to classload</li>
	 * <li><strong>{anything}</strong> - The properties can be any object that the listener can use to create the object</li>
	 * </ul>
	 * <p>
	 * <strong>IMPORTANT:</strong> This does NOT create an instance of the class, for that you will need to call the {@code init()} method on the returned object.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.type The type of object to create: java, class (component), or any other type
	 *
	 * @argument.className A fully qualified class name to create an instance of
	 *
	 * @argument.properties Depending on the type, this can be used to pass additional properties to the object creation process
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	type		= arguments.getAsString( Key.type );
		String	className	= arguments.getAsString( Key.className );
		Object	properties	= arguments.get( Key.properties );

		// If no type is provided, default to class
		if ( className == null ) {
			className	= type;
			type		= CLASS_TYPE;
		}

		// Java Classes
		if ( type.equalsIgnoreCase( ClassLocator.JAVA_PREFIX ) ) {
			return createJavaClass( context, className, properties );
		}

		// Class and Component left for backward compatibility
		if ( type.equalsIgnoreCase( COMPONENT_TYPE ) ||
		    type.equalsIgnoreCase( CLASS_TYPE ) ) {
			return createBoxClass( context, className );
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

	/**
	 * Creates a new Java class instance.
	 *
	 * @param context    The context in which the BIF is being invoked.
	 * @param className  The fully qualified class name to create an instance of.
	 * @param properties The class paths to load the class from.
	 */
	private Object createJavaClass( IBoxContext context, String className, Object properties ) {
		// If we have properties, we need to load the class with the properties
		if ( properties != null ) {
			Array classPaths;
			// Normalize to an array
			if ( properties instanceof String ) {
				classPaths = Array.of( properties );
			} else if ( properties instanceof Array ) {
				classPaths = ( Array ) properties;
			} else {
				throw new BoxRuntimeException( "Invalid properties type: " + properties.getClass().getName() );
			}

			return CLASS_LOCATOR.loadFromClassPaths(
			    context,
			    className,
			    classPaths,
			    true,
			    context.getCurrentImports()
			);
		}

		// Otherwise, traditional class loading
		return CLASS_LOCATOR.load(
		    context,
		    className,
		    ClassLocator.JAVA_PREFIX,
		    true,
		    context.getCurrentImports()
		);
	}

	/**
	 * Creates a new BoxLang class or component instance.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param className The fully qualified class name to create an instance of.
	 *
	 * @return The created object.
	 */
	private Object createBoxClass( IBoxContext context, String className ) {
		// Load up the class
		DynamicObject result = CLASS_LOCATOR.load(
		    context,
		    className,
		    ClassLocator.BX_PREFIX,
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
}
