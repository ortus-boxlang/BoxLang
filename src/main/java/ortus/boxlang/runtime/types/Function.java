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
import java.util.Map;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.ClosureBoxContext;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.LambdaBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.runnables.IFunctionRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.FunctionMeta;

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
	 * Metadata object
	 */
	public BoxMeta			$bx;

	/**
	 * Cached lookup of the output annotation
	 */
	private Boolean			canOutput			= null;

	/**
	 * The argument collection key which defaults to : {@code argumentCollection}
	 */
	public static final Key	ARGUMENT_COLLECTION	= Key.argumentCollection;

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

	public BoxMeta getBoxMeta() {
		if ( this.$bx == null ) {
			this.$bx = new FunctionMeta( this );
		}
		return this.$bx;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Invokers (These are the methods that are called by the runtime)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Create an arguments scope from the positional arguments
	 *
	 * @param positionalArguments The positional arguments
	 *
	 * @return The arguments scope
	 */
	public ArgumentsScope createArgumentsScope( IBoxContext context, Object[] positionalArguments ) {
		return ArgumentUtil.createArgumentsScope( context, positionalArguments, getArguments() );
	}

	/**
	 * Create an arguments scope from the named arguments
	 *
	 * @param namedArguments The named arguments
	 *
	 * @return The arguments scope
	 */
	public ArgumentsScope createArgumentsScope( IBoxContext context, Map<Key, Object> namedArguments ) {
		return ArgumentUtil.createArgumentsScope( context, namedArguments, getArguments() );
	}

	/**
	 * Create an arguments scope from no arguments
	 *
	 * @return The arguments scope
	 */
	public ArgumentsScope createArgumentsScope( IBoxContext context ) {
		return ArgumentUtil.createArgumentsScope( context, getArguments() );
	}

	/**
	 * Call this method externally to invoke the function
	 *
	 * @param context The context to invoke the function in
	 *
	 * @return The result of the function, which may be null
	 */
	public Object invoke( FunctionBoxContext context ) {
		InterceptorService	interceptorService	= BoxRuntime.getInstance().getInterceptorService();

		// Announcements
		IStruct				data				= Struct.of(
		    Key.context, context,
		    Key.function, this
		);
		interceptorService.announce(
		    BoxRuntime.RUNTIME_EVENTS.get( "preFunctionInvoke" ),
		    data
		);
		Object result = null;
		context.pushTemplate( this );
		try {
			result = ensureReturnType( context, _invoke( context ) );
			data.put( Key.result, result );

			interceptorService.announce(
			    BoxRuntime.RUNTIME_EVENTS.get( "postFunctionInvoke" ),
			    data
			);
		} catch ( Throwable e ) {
			context.flushBuffer( true );
			throw e;
		} finally {
			context.popTemplate();
			// If output=true, then flush any content in buffer
			if ( canOutput( context ) ) {
				context.flushBuffer( false );
			}
		}
		return result;
	}

	/**
	 * Ensure the return value of the function is the correct type
	 *
	 * @param value
	 *
	 * @return
	 */
	protected Object ensureReturnType( IBoxContext context, Object value ) {
		CastAttempt<Object> typeCheck = GenericCaster.attempt( context, value, getReturnType(), true );
		if ( !typeCheck.wasSuccessful() ) {
			throw new BoxRuntimeException(
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
	 * Get any annotations declared for this function, both the @annotation syntax and inline.
	 *
	 * @return function metadata
	 */
	public abstract IStruct getAnnotations();

	/**
	 * Get the contents of the documentation comment for this function.
	 *
	 * @return function metadata
	 */
	public abstract IStruct getDocumentation();

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
	 * Get the combined metadata for this function and all it's parameters
	 * This follows the format of Lucee and Adobe's "combined" metadata
	 * TODO: Move this to compat module
	 *
	 * @return The metadata as a struct
	 */
	public IStruct getMetaData() {
		IStruct meta = new Struct();
		if ( getDocumentation() != null ) {
			meta.putAll( getDocumentation() );
		}
		if ( getAnnotations() != null ) {
			meta.putAll( getAnnotations() );
		}
		meta.put( Key._NAME, getName().getName() );
		meta.put( Key.returnType, getReturnType() );
		meta.putIfAbsent( Key.hint, "" );
		meta.putIfAbsent( Key.output, false );
		meta.put( Key.access, getAccess().toString().toLowerCase() );
		Array params = new Array();
		for ( Argument argument : getArguments() ) {
			IStruct arg = new Struct();
			arg.put( Key._NAME, argument.name().getName() );
			arg.put( Key.required, argument.required() );
			arg.put( Key.type, argument.type() );
			arg.put( Key._DEFAULT, argument.defaultValue() );
			if ( argument.documentation() != null ) {
				arg.putAll( argument.documentation() );
			}
			if ( argument.annotations() != null ) {
				arg.putAll( argument.annotations() );
			}
			// Always have this key present?
			arg.putIfAbsent( Key.hint, "" );
			params.add( arg );
		}
		meta.put( Key.parameters, params );

		// polymorphsism is a pain due to storing the metdata as static values on the class, so we'll just add the closure and lambda checks here
		// Adobe and Lucee only set the following flags when they are true, but that's inconsistent, so we will always set them.

		boolean isClosure = this instanceof Closure;
		// Adobe and Lucee have this
		meta.put( Key.closure, isClosure );
		// Adobe and Lucee have this
		meta.put( Key.ANONYMOUSCLOSURE, isClosure );

		// Adobe and Lucee don't have "lambdas" in the same way we have where the actual implementation is a pure function
		boolean isLambda = this instanceof Lambda;
		// Neither Adobe nor Lucee have this, but we're setting it for consistency
		meta.put( Key.lambda, isLambda );
		// Lucee has this, but it's true for fat arrow functions. We're setting it true only for skinny arrow (true lambda) functions
		meta.put( Key.ANONYMOUSLAMBDA, isLambda );

		return meta;
	}

	/**
	 * This is a helper method to generate the correct context for a function based on type
	 *
	 * @param function            The function to generate the context for
	 * @param parentContext       The parent context
	 * @param calledName          The name the function was called with
	 * @param positionalArguments The arguments for the function
	 */
	public static FunctionBoxContext generateFunctionContext( Function function, IBoxContext parentContext, Key calledName, Object[] positionalArguments ) {
		if ( function instanceof Closure clos ) {
			return new ClosureBoxContext( parentContext, clos, calledName, positionalArguments );
		} else if ( function instanceof Lambda lam ) {
			return new LambdaBoxContext( parentContext, lam, calledName, positionalArguments );
		} else {
			return new FunctionBoxContext( parentContext, function, calledName, positionalArguments );
		}
	}

	/**
	 * This is a helper method to generate the correct context for a function based on type
	 *
	 * @param function       The function to generate the context for
	 * @param parentContext  The parent context
	 * @param calledName     The name the function was called with
	 * @param namedArguments The arguments for the function
	 */
	public static FunctionBoxContext generateFunctionContext( Function function, IBoxContext parentContext, Key calledName, Map<Key, Object> namedArguments ) {
		if ( function instanceof Closure clos ) {
			return new ClosureBoxContext( parentContext, clos, calledName, namedArguments );
		} else if ( function instanceof Lambda lam ) {
			return new LambdaBoxContext( parentContext, lam, calledName, namedArguments );
		} else {
			return new FunctionBoxContext( parentContext, function, calledName, namedArguments );
		}
	}

	/**
	 * A helper to look at the "output" annotation, caching the result
	 *
	 * @param context If not null, will be checked for an output annotation
	 *
	 * @return Whether the function can output
	 */
	public boolean canOutput( FunctionBoxContext context ) {
		// Initialize if neccessary
		if ( this.canOutput == null ) {
			this.canOutput = BooleanCaster.cast( getAnnotations().getOrDefault( Key.output, false ) );
		}

		if ( this.canOutput ) {
			// We don't cache this because a function can be moved between CFC instances, or have refernces in more than one
			// at a time. Each class has its own caching later for the output annotation.
			if ( context != null && context.isInClass() ) {
				// If we're in a class, we need to check the class output annotation
				return context.getThisClass().canOutput();
			}
			// If not in a class, we're good
			return true;
		} else {
			// We're not outputting, so we didn't even check for a class.
			return false;
		}
	}
}
