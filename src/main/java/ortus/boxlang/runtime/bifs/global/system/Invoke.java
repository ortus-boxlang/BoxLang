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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;

@BoxBIF
public class Invoke extends BIF {

	ClassLocator classLocator = ClassLocator.getInstance();

	/**
	 * Constructor
	 */
	public Invoke() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.instance ),
		    new Argument( true, "string", Key.methodname ),
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
	 * @argument.methodname The name of the method to invoke
	 * 
	 * @argument.arguments An array of positional arguments or a struct of named arguments to pass into the method.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	instance		= arguments.get( Key.instance );
		Key		methodname		= Key.of( arguments.getAsString( Key.methodname ) );
		Object	args			= arguments.get( Key.arguments );
		IStruct	argsAsStruct	= null;
		Array	argsAsArray		= null;

		// If args were passed, they must be a struct or an array. Validate and cast accordingly.
		if ( args != null ) {
			CastAttempt<Array>		arrayCasterAttempt	= ArrayCaster.attempt( args );
			CastAttempt<IStruct>	structCasterAttempt	= StructCaster.attempt( args );
			if ( structCasterAttempt.wasSuccessful() ) {
				argsAsStruct = structCasterAttempt.get();
			} else if ( arrayCasterAttempt.wasSuccessful() ) {
				argsAsArray = arrayCasterAttempt.get();
			} else {
				throw new BoxValidationException( "The arguments parameter must be an array or a struct." );
			}
		}

		CastAttempt<String> stringCasterAttempt = StringCaster.attempt( instance );
		// Empty string just calls local function in the existing context (box class or template)
		if ( stringCasterAttempt.wasSuccessful() && stringCasterAttempt.get().isEmpty() ) {
			if ( argsAsArray != null ) {
				return context.invokeFunction( methodname, argsAsArray.toArray() );
			} else if ( argsAsStruct != null ) {
				return context.invokeFunction( methodname, argsAsStruct );
			} else {
				return context.invokeFunction( methodname );
			}
		}

		// If we had a non-empty string, create the Box Class instance
		IClassRunnable actualInstance;
		if ( stringCasterAttempt.wasSuccessful() ) {
			actualInstance = ( IClassRunnable ) classLocator.load( context, "bx:" + stringCasterAttempt.get(), context.getCurrentImports() )
			    .invokeConstructor( context, Key.noInit )
			    .unWrapBoxLangClass();
		} else if ( instance instanceof IClassRunnable ) {
			// If we got an already-instantiated Box Class, use it directly
			actualInstance = ( IClassRunnable ) instance;
		} else {
			throw new BoxValidationException( "The instance parameter must be a Box Class or the name of a Box Class to instantiate." );
		}

		// Invoke the method on the Box Class instance
		if ( argsAsArray != null ) {
			return actualInstance.dereferenceAndInvoke( context, methodname, argsAsArray.toArray(), false );
		} else if ( argsAsStruct != null ) {
			return actualInstance.dereferenceAndInvoke( context, methodname, argsAsStruct, false );
		} else {
			return actualInstance.dereferenceAndInvoke( context, methodname, new Object[] {}, false );
		}

	}
}
