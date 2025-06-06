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

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.interop.DynamicObject;
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

	ClassLocator			classLocator			= BoxRuntime.getInstance().getClassLocator();
	static final Set<Key>	reservedAttributeNames	= Set.of( Key._CLASS, Key.method, Key.returnVariable, Key.argumentCollection );

	/**
	 * Constructor
	 */
	public Invoke() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key._CLASS, "any", "" ),
		    new Attribute( Key.method, "string", Set.of( Validator.REQUIRED, Validator.NON_EMPTY ) ),
		    new Attribute( Key.returnVariable, "string", Set.of( Validator.NON_EMPTY ) ),
		    new Attribute( Key.argumentCollection, "any" )
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
		IStruct	argCollection	= Struct.of();
		Object	result			= null;

		// If args were passed, they must be a struct or an array. Validate and cast accordingly.
		if ( args != null ) {
			argCollection.put( Key.argumentCollection, args );
		}

		// loop over attributes and add all but class, method, returnvariable, and arguentcollection to the argCollection
		attributes.forEach( ( key, value ) -> {
			if ( !reservedAttributeNames.contains( key ) ) {
				argCollection.put( key, value );
			}
		} );

		executionState.put( Key.invokeArgs, argCollection );
		executionState.put( Key.of( "methodname" ), methodname.getName() );

		BodyResult bodyResult = processBody( context, body );

		// IF there was a return statement inside our body, we early exit now
		if ( bodyResult.isEarlyExit() ) {
			return bodyResult;
		}

		IReferenceable actualInstance;
		if ( instance instanceof String str ) {

			// Empty string just calls local function in the existing context (box class or template)
			if ( str.isEmpty() ) {
				result = context.invokeFunction( methodname, argCollection );
				if ( returnVariable != null ) {
					ExpressionInterpreter.setVariable( context, returnVariable, result );
				}

				return DEFAULT_RETURN;
			}

			// If we had a non-empty string, create the Box Class instance
			actualInstance = ( IClassRunnable ) classLocator.load( context, "bx:" + str, context.getCurrentImports() )
			    .invokeConstructor( context, Key.noInit )
			    .unWrapBoxLangClass();

		} else if ( instance instanceof IReferenceable cvs ) {
			actualInstance = cvs;
		} else {
			throw new BoxValidationException( "The instance parameter must be a Box Class or the name of a Box Class to instantiate." );
		}

		// ALERT!
		// Special Case: If the instance is a DynamicObject and the method is "init", we need to call the constructor
		if ( actualInstance instanceof DynamicObject castedDo && methodname.equals( Key.init ) ) {
			// The incoming args must be an array or throw an exception
			if ( ! ( args instanceof Array castedArray ) ) {
				throw new BoxValidationException( "The arguments must be an array in order to execute the Java constructor." );
			}
			castedDo.invokeConstructor( context, castedArray.toArray() );
			return DEFAULT_RETURN;
		}

		// Invoke the method on the Box Class instance
		result = actualInstance.dereferenceAndInvoke( context, methodname, argCollection, false );

		if ( returnVariable != null ) {
			ExpressionInterpreter.setVariable( context, returnVariable, result );
		}

		return DEFAULT_RETURN;
	}

}
