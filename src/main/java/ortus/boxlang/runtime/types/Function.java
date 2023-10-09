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
package ortus.boxlang.runtime.types;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.runnables.IFunctionRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;

/**
 * A BoxLang Function base class
 */
public abstract class Function implements IType, IFunctionRunnable {

	/**
	 * --------------------------------------------------------------------------
	 * Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The supported access levels of the function
	 */
	public enum Access {
		PRIVATE,
		PUBLIC,
		PROTECTED,
		REMOTE
	}

	/**
	 * The argument collection key which defaults to : {@code argumentCollection}
	 */
	public static Key ARGUMENT_COLLECTION = Key.of( "argumentCollection" );

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 */
	protected Function() {
	}

	/**
	 * Return a string representation of the function
	 */
	public String asString() {
		return toString();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Invokers (These are the methods that are called by the runtime)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Call this method externally to invoke the function
	 *
	 * @param context
	 *
	 * @return
	 */
	public Object invoke( FunctionBoxContext context ) {

		InterceptorService	interceptorService	= InterceptorService.getInstance();

		// Announcements
		Struct				data				= Struct.of(
		    "context", context,
		    "function", this
		);
		interceptorService.announce( "preFunctionInvoke", data );
		Object result = ensureReturnType( _invoke( context ) );

		data.put( "result", result );
		interceptorService.announce( "postFunctionInvoke", data );

		return data.get( "result" );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Arguments Helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Create an arguments scope from the positional arguments
	 *
	 * @param positionalArguments The positional arguments
	 *
	 * @return The arguments scope
	 */
	public ArgumentsScope createArgumentsScope( Object[] positionalArguments ) {
		Argument[]		arguments	= getArguments();
		ArgumentsScope	scope		= new ArgumentsScope();
		// Add all incoming args to the scope, using the name if declared, otherwise using the position
		for ( int i = 0; i < positionalArguments.length; i++ ) {
			Key		name;
			Object	value	= positionalArguments[ i ];
			if ( arguments.length - 1 >= i ) {
				name	= arguments[ i ].name();
				value	= ensureArgumentType( name, value, arguments[ i ].type() );
			} else {
				name = Key.of( Integer.toString( i + 1 ) );
			}
			scope.put( name, value );
		}

		// Fill in any remaining declared arguments with default value
		if ( arguments.length > scope.size() ) {
			for ( int i = scope.size(); i < arguments.length; i++ ) {
				if ( arguments[ i ].required() && arguments[ i ].defaultValue() == null ) {
					throw new ApplicationException( "Required argument " + arguments[ i ].name() + " is missing" );
				}
				scope.put( arguments[ i ].name(),
				    ensureArgumentType( arguments[ i ].name(), arguments[ i ].defaultValue(), arguments[ i ].type() ) );
			}
		}
		return scope;
	}

	/**
	 * Create an arguments scope from the named arguments
	 *
	 * @param namedArguments The named arguments
	 *
	 * @return The arguments scope
	 */
	public ArgumentsScope createArgumentsScope( Map<Key, Object> namedArguments ) {
		Argument[]		arguments	= getArguments();
		ArgumentsScope	scope		= new ArgumentsScope();

		// If argumentCollection exists, add it
		if ( namedArguments.containsKey( ARGUMENT_COLLECTION ) ) {
			Object argCollection = namedArguments.get( ARGUMENT_COLLECTION );
			if ( argCollection instanceof Map<?, ?> ) {
				@SuppressWarnings( "unchecked" )
				Map<Key, Object> argumentCollection = ( Map<Key, Object> ) argCollection;
				scope.putAll( argumentCollection );
				namedArguments.remove( ARGUMENT_COLLECTION );
			}
			if ( argCollection instanceof List<?> ) {
				@SuppressWarnings( "unchecked" )
				List<Object> argumentCollection = ( List<Object> ) argCollection;

				for ( int i = 0; i < argumentCollection.size(); i++ ) {
					Key		name;
					Object	value	= argumentCollection.get( i );
					if ( arguments.length - 1 >= i ) {
						name = arguments[ i ].name();
					} else {
						name = Key.of( Integer.toString( i + 1 ) );
					}
					scope.put( name, value );
				}
				namedArguments.remove( ARGUMENT_COLLECTION );
			}
			// Lucee leaves non struct, non array argumentCollectionkeys as-is. Adobe removes them. We'll copy Lucee here, though it's an edge case.
		}

		// Put all remaining incoming args
		scope.putAll( namedArguments );

		// For all declared args
		for ( Argument argument : arguments ) {
			// If they aren't here, add their default value (if defined)
			if ( !scope.containsKey( argument.name() ) ) {
				if ( argument.required() && argument.defaultValue() == null ) {
					throw new ApplicationException( "Required argument " + argument.name() + " is missing" );
				}
				// Make sure the default value is valid
				scope.put( argument.name(), ensureArgumentType( argument.name(), argument.defaultValue(), argument.type() ) );
				// If they are here, confirm their types
			} else {
				scope.put( argument.name(),
				    ensureArgumentType( argument.name(), scope.get( argument.name() ), argument.type() ) );
			}
		}
		return scope;
	}

	/**
	 * Create an arguments scope from no arguments
	 *
	 * @return The arguments scope
	 */
	public ArgumentsScope createArgumentsScope() {
		return createArgumentsScope( new Object[] {} );
	}

	/**
	 * Ensure the argument is the correct type
	 *
	 * @param name  The name of the argument
	 * @param value The value of the argument
	 * @param type  The type of the argument
	 *
	 * @return The value of the argument
	 *
	 */
	protected Object ensureArgumentType( Key name, Object value, String type ) {
		CastAttempt<Object> typeCheck = GenericCaster.attempt( value, type, true );
		if ( !typeCheck.wasSuccessful() ) {
			throw new ApplicationException(
			    String.format( "Argument [%s] with a type of [%s] does not match the declared type of [%s]",
			        name.getName(), value.getClass().getName(), type )
			);
		}
		// Should we actually return the casted value??? Not CFML Compat! If so, return typeCheck.get() with check for NullValue instances.
		return value;
	}

	/**
	 * Ensure the return value of the function is the correct type
	 *
	 * @param value
	 *
	 * @return
	 */
	protected Object ensureReturnType( Object value ) {
		CastAttempt<Object> typeCheck = GenericCaster.attempt( value, getReturnType(), true );
		if ( !typeCheck.wasSuccessful() ) {
			throw new ApplicationException(
			    String.format( "The return value of the function [%s] does not match the declared type of [%s]",
			        value.getClass().getName(), getReturnType() )
			);
		}
		// Should we actually return the casted value??? Not CFML Compat! If so, return typeCheck.get() with check for NullValue instances.
		return value;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Abstract methods for concrete classes to implement.
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the name of the function.
	 *
	 * @return function name
	 */
	public abstract Key getName();

	/**
	 * Get the arguments of the function.
	 *
	 * @return array of arguments
	 */

	public abstract Argument[] getArguments();

	/**
	 * Get the return type of the function.
	 *
	 * @return return type
	 */
	public abstract String getReturnType();

	/**
	 * Get the hint of the function.
	 *
	 * @return function hint
	 */
	public abstract String getHint();

	/**
	 * Get the output of the function.
	 *
	 * @return function output flag
	 */
	public abstract boolean isOutput();

	/**
	 * Get any ad-hoc metadata keys that were declared for this function, not part of the standard function definition.
	 *
	 * @return function metadata
	 */
	public abstract Map<Key, Object> getAdditionalMetadata();

	/**
	 * Get access modifier of the function
	 *
	 * @return function access modifier
	 */
	public abstract Access getAccess();

	/**
	 * Implement this method to invoke the actual function logic
	 *
	 * @param context
	 *
	 * @return
	 */
	public abstract Object _invoke( FunctionBoxContext context );

	// ITemplateRunnable implementation methods

	/**
	 * Get the version of the BoxLang runtime
	 */
	public abstract long getRunnableCompileVersion();

	/**
	 * Get the date the template was compiled
	 */
	public abstract LocalDateTime getRunnableCompiledOn();

	/**
	 * The AST (abstract syntax tree) of the runnable
	 */
	public abstract Object getRunnableAST();

	/**
	 * Get the instance of the runnable class that declared this function
	 */
	public abstract IBoxRunnable getDeclaringRunnable();

	/**
	 * Get the combined metadata for this function and all it's parameters
	 *
	 * @return The metadata as a struct
	 */
	public Struct getMetaData() {
		Struct meta = new Struct();
		if ( getAdditionalMetadata() != null ) {
			meta.putAll( getAdditionalMetadata() );
		}
		meta.put( "name", getName() );
		meta.put( "returnType", getReturnType() );
		meta.put( "hint", getHint() );
		meta.put( "output", isOutput() );
		meta.put( "access", getAccess().toString().toLowerCase() );
		Array params = new Array();
		for ( Argument argument : getArguments() ) {
			Struct arg = new Struct();
			arg.put( "name", argument.name() );
			arg.put( "required", argument.required() );
			arg.put( "type", argument.type() );
			arg.put( "default", argument.defaultValue() );
			arg.put( "hint", argument.hint() );
			if ( argument.metadata() != null ) {
				arg.putAll( argument.metadata() );
			}
			params.add( arg );
		}
		meta.put( "parameters", params );

		// polymorphsism is a pain due to storing the metdata as static values on the class, so we'll just add the closure and lambda checks here
		// Adobe and Lucee only set the following flags when they are true, but that's inconsistent, so we will always set them.

		boolean isClosure = this instanceof Closure;
		// Adobe and Lucee have this
		meta.put( "closure", isClosure );
		// Adobe and Lucee have this
		meta.put( "ANONYMOUSCLOSURE", isClosure );

		// Adobe and Lucee don't have "lambdas" in the same way we have where the actual implementation is a pure function
		boolean isLambda = this instanceof Lambda;
		// Neither Adobe nor Lucee have this, but we're setting it for consistency
		meta.put( "lambda", isLambda );
		// Lucee has this, but it's true for fat arrow functions. We're setting it true only for skinny arrow (true lambda) functions
		meta.put( "ANONYMOUSLAMBDA", isLambda );

		return meta;
	}

	/**
	 * Represents an argument to a function
	 *
	 * @param required     Whether the argument is required
	 * @param type         The type of the argument
	 * @param name         The name of the argument
	 * @param defaultValue The default value of the argument
	 *
	 */
	public record Argument( boolean required, String type, Key name, Object defaultValue, String hint, Map<Key, Object> metadata ) {

		public Argument( Key name ) {
			this( false, "any", name, null, "", new HashMap<>() );
		}

		public Argument( boolean required, String type, Key name ) {
			this( required, type, name, null, "", new HashMap<>() );
		}

		public Argument( boolean required, String type, Key name, Object defaultValue ) {
			this( required, type, name, defaultValue, "", new HashMap<>() );
		}

		public Argument( boolean required, String type, Key name, Object defaultValue, String hint ) {
			this( required, type, name, defaultValue, hint, new HashMap<>() );
		}

		public Argument( boolean required, String type, Key name, Object defaultValue, String hint, Map<Key, Object> metadata ) {
			this.required		= required;
			this.type			= type;
			this.name			= name;
			this.defaultValue	= defaultValue;
			this.hint			= hint;
			this.metadata		= metadata;
		}

	}
}
