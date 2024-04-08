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
package ortus.boxlang.runtime.components.system;

import java.util.Set;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( allowsBody = true )
public class Invoke extends Component {

	ClassLocator classLocator = ClassLocator.getInstance();

	/**
	 * Constructor
	 */
	public Invoke() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key._CLASS, "any", "" ),
		    new Attribute( Key.method, "string", Set.of( Validator.REQUIRED, Validator.NON_EMPTY ) ),
		    new Attribute( Key.returnVariable, "string", Set.of( Validator.NON_EMPTY ) ),
		    new Attribute( Key.argumentCollection, "any", Set.of( Validator.NON_EMPTY ) )
		};
	}

	/**
	 * Invokes a method from within a template or class.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 * 
	 * @attribute.class The Box Class instance or the name of the Box Class to instantiate.
	 * 
	 * @attribute.method The name of the method to invoke.
	 * 
	 * @attribute.returnVariable The variable to store the result of the method invocation.
	 * 
	 * @attribute.argumentCollection An array or struct of arguments to pass to the method.
	 * 
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		String	returnVariable	= attributes.getAsString( Key.returnVariable );
		Key		methodname		= Key.of( attributes.getAsString( Key.method ) );
		Object	instance		= attributes.get( Key._CLASS );
		Object	args			= attributes.get( Key.argumentCollection );
		IStruct	argsAsStruct	= null;
		Array	argsAsArray		= null;
		Object	result			= null;

		// If args were passed, they must be a struct or an array. Validate and cast accordingly.
		if ( args != null ) {
			CastAttempt<Array>		arrayCasterAttempt	= ArrayCaster.attempt( args );
			CastAttempt<IStruct>	structCasterAttempt	= StructCaster.attempt( args );
			if ( structCasterAttempt.wasSuccessful() ) {
				argsAsStruct	= structCasterAttempt.get();
				argsAsArray		= new Array();
			} else if ( arrayCasterAttempt.wasSuccessful() ) {
				argsAsArray		= arrayCasterAttempt.get();
				argsAsStruct	= new Struct();
			} else {
				throw new BoxValidationException( "The argumentCollection attribute must be an array or a struct." );
			}
		} else {
			argsAsStruct	= new Struct();
			argsAsArray		= new Array();
		}
		executionState.put( Key.invokeArgs, argsAsStruct );

		BodyResult bodyResult = processBody( context, body );
		// IF there was a return statement inside our body, we early exit now
		if ( bodyResult.isEarlyExit() ) {
			return bodyResult;
		}

		if ( !argsAsArray.isEmpty() && !argsAsStruct.isEmpty() ) {
			throw new BoxValidationException( "The argumentCollection attribute must be a struct if used in combination with the invokeAttribute component." );
		}

		CastAttempt<String> stringCasterAttempt = StringCaster.attempt( instance );
		// Empty string just calls local function in the existing context (box class or template)
		if ( stringCasterAttempt.wasSuccessful() && stringCasterAttempt.get().isEmpty() ) {
			if ( !argsAsArray.isEmpty() ) {
				result = context.invokeFunction( methodname, argsAsArray.toArray() );
			} else if ( !argsAsStruct.isEmpty() ) {
				result = context.invokeFunction( methodname, argsAsStruct );
			} else {
				result = context.invokeFunction( methodname );
			}
		} else {

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
			if ( !argsAsArray.isEmpty() ) {
				result = actualInstance.dereferenceAndInvoke( context, methodname, argsAsArray.toArray(), false );
			} else if ( !argsAsStruct.isEmpty() ) {
				result = actualInstance.dereferenceAndInvoke( context, methodname, argsAsStruct, false );
			} else {
				result = actualInstance.dereferenceAndInvoke( context, methodname, new Object[] {}, false );
			}
		}
		if ( returnVariable != null ) {
			ExpressionInterpreter.setVariable( context, returnVariable, result );
		}
		return DEFAULT_RETURN;
	}

}
