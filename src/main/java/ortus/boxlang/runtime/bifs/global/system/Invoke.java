/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.system;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;

@BoxBIF
public class Invoke extends BIF {

	ClassLocator classLocator = BoxRuntime.getInstance().getClassLocator();

	/**
	 * Constructor
	 */
	public Invoke() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.object ),
		    new Argument( false, "string", Key.method ),
		    new Argument( false, "any", Key.arguments )
		};
	}

	/**
	 * Invokes an object method and returns the result of the invoked method.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.instance Instance of a Box Class or name of one to instantiate. If an empty string is provided, the method will be invoked within the
	 *                    same template or Box Class.
	 *
	 * @argument.method The name of the method to invoke
	 *
	 * @argument.arguments An array of positional arguments or a struct of named arguments to pass into the method.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	instance		= arguments.get( Key.object );
		Key		method			= Key.of( arguments.getAsString( Key.method ) );
		Object	args			= arguments.get( Key.arguments );
		IStruct	argCollection	= Struct.of();

		// If args were passed, they must be a struct or an array. Pass them as an argument collection which will handle all the possible cases
		if ( args != null ) {
			argCollection.put( Key.argumentCollection, args );
		}

		// If we had a non-empty string, create the Box Class instance
		IReferenceable actualInstance;
		if ( instance instanceof String str ) {

			// Empty string just calls local function in the existing context (box class or template)
			if ( str.isEmpty() ) {
				return context.invokeFunction( method, argCollection );
			}

			actualInstance = ( IClassRunnable ) classLocator.load( context, "bx:" + str, context.getCurrentImports() )
			    .invokeConstructor( context, Key.noInit )
			    .unWrapBoxLangClass();
		} else if ( instance instanceof IReferenceable cvs ) {
			actualInstance = cvs;
		} else {
			throw new BoxValidationException( "The instance parameter must be a Box Class, referencable struct or the name of a Box Class to instantiate." );
		}

		// ALERT!
		// Special Case: If the instance is a DynamicObject and the method is "init", we need to call the constructor
		if ( actualInstance instanceof DynamicObject castedDo && method.equals( Key.init ) ) {
			// The incoming args must be an array or throw an exception
			if ( ! ( args instanceof Array castedArray ) ) {
				throw new BoxValidationException( "The arguments must be an array in order to execute the Java constructor." );
			}
			return castedDo.invokeConstructor( context, castedArray.toArray() );
		}

		return actualInstance.dereferenceAndInvoke( context, method, argCollection, false );

	}
}
