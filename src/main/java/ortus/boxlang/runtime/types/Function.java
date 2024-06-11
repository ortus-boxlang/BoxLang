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

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.ClosureBoxContext;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.LambdaBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.runnables.BoxInterface;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.IFunctionRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.FunctionMeta;
import ortus.boxlang.runtime.util.ArgumentUtil;

/**
 * A BoxLang Function base class
 */
public abstract class Function implements IType, IFunctionRunnable, Serializable {

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
		REMOTE,
		PACKAGE
	}

	/**
	 * Metadata object
	 */
	public transient BoxMeta	$bx;

	/**
	 * The argument collection key which defaults to : {@code argumentCollection}
	 */
	public static final Key		ARGUMENT_COLLECTION	= Key.argumentCollection;

	/**
	 * Cached lookup of the output annotation
	 */
	private Boolean				canOutput			= null;

	/**
	 * Serialization version
	 */
	private static final long	serialVersionUID	= 1L;

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
		return ArgumentUtil.createArgumentsScope( context, positionalArguments, getArguments(), new ArgumentsScope(), getName() );
	}

	/**
	 * Create an arguments scope from the named arguments
	 *
	 * @param namedArguments The named arguments
	 *
	 * @return The arguments scope
	 */
	public ArgumentsScope createArgumentsScope( IBoxContext context, Map<Key, Object> namedArguments ) {
		return ArgumentUtil.createArgumentsScope( context, namedArguments, getArguments(), new ArgumentsScope(), getName() );
	}

	/**
	 * Create an arguments scope from no arguments
	 *
	 * @return The arguments scope
	 */
	public ArgumentsScope createArgumentsScope( IBoxContext context ) {
		return ArgumentUtil.createArgumentsScope( context, getArguments(), new ArgumentsScope(), getName() );
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
		    BoxEvent.PRE_FUNCTION_INVOKE,
		    data
		);

		Object result = null;
		context.pushTemplate( this );
		try {
			result = ensureReturnType( context, _invoke( context ) );
			data.put( Key.result, result );

			interceptorService.announce(
			    BoxEvent.POST_FUNCTION_INVOKE,
			    data
			);
		} catch ( AbortException e ) {
			if ( e.isLoop() ) {
				throw new BoxValidationException( "You cannot use the 'loop' method of the exit component outside of a custom tag." );
			} else if ( e.isTemplate() || e.isTag() ) {
				// These function basically as a return from the method
				return result;
			} else if ( e.isRequest() ) {
				context.flushBuffer( true );
			}
			throw e;
		} catch ( Throwable e ) {
			context.flushBuffer( true );
			throw e;
		} finally {
			context.popTemplate();
			context.flushBuffer( false );
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
		// CF doesn't enforce return types on null returns. I think that is a bug, but we'd need a compat layer to make existing CF code work.
		if ( value == null ) {
			return null;
		}
		CastAttempt<Object> typeCheck = GenericCaster.attempt( context, value, getReturnType(), true );
		if ( !typeCheck.wasSuccessful() ) {
			String actualType;
			if ( value == null ) {
				actualType = "null";
			} else {
				actualType = value.getClass().getName();
			}
			throw new BoxRuntimeException(
			    String.format( "The return value of the function [%s] is of type [%s] does not match the declared type of [%s]",
			        getName().getName(), actualType, getReturnType() )
			);
		}
		if ( typeCheck.get() instanceof NullValue ) {
			return null;
		}
		return typeCheck.get();
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
	 * Get modifier of the function
	 *
	 * @return function modifiers
	 */
	public List<BoxMethodDeclarationModifier> getModifiers() {
		return List.of();

	}

	/**
	 * Check if a specific modifier is present
	 * 
	 * @param modifier The modifier to check for
	 *
	 * @return true if the modifier is present
	 */
	public boolean hasModifier( BoxMethodDeclarationModifier modifier ) {
		return getModifiers().contains( modifier );
	}

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
		IStruct meta = new Struct( IStruct.TYPES.LINKED );
		if ( getDocumentation() != null ) {
			meta.putAll( getDocumentation() );
		}
		if ( getAnnotations() != null ) {
			meta.putAll( getAnnotations() );
		}
		meta.put( Key._NAME, getName().getName() );
		meta.put( Key.returnType, getReturnType() );
		meta.putIfAbsent( Key.hint, "" );
		// Passing null to canOutput will skip the class check
		meta.putIfAbsent( Key.output, canOutput( null ) );
		meta.put( Key.access, getAccess().toString().toLowerCase() );
		Array params = new Array();
		for ( Argument argument : getArguments() ) {
			IStruct arg = new Struct( IStruct.TYPES.LINKED );
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
	public static FunctionBoxContext generateFunctionContext( Function function, IBoxContext parentContext, Key calledName, Object[] positionalArguments,
	    IClassRunnable thisClass, BoxInterface thisInterface ) {
		if ( function instanceof Closure clos ) {
			return new ClosureBoxContext( parentContext, clos, calledName, positionalArguments );
		} else if ( function instanceof Lambda lam ) {
			return new LambdaBoxContext( parentContext, lam, calledName, positionalArguments );
		} else {
			return new FunctionBoxContext( parentContext, function, calledName, positionalArguments, thisClass ).setThisInterface( thisInterface );
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
	public static FunctionBoxContext generateFunctionContext( Function function, IBoxContext parentContext, Key calledName, Map<Key, Object> namedArguments,
	    IClassRunnable thisClass, BoxInterface thisInterface ) {
		if ( function instanceof Closure clos ) {
			return new ClosureBoxContext( parentContext, clos, calledName, namedArguments );
		} else if ( function instanceof Lambda lam ) {
			return new LambdaBoxContext( parentContext, lam, calledName, namedArguments );
		} else {
			return new FunctionBoxContext( parentContext, function, calledName, namedArguments, thisClass ).setThisInterface( thisInterface );
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
		// Check for function annotation
		if ( this.canOutput == null ) {
			Object anno = getAnnotations().get( Key.output );
			if ( anno != null ) {
				this.canOutput = BooleanCaster.cast( anno );
			}
		}

		// Check for class annotation
		if ( this.canOutput == null ) {
			// We don't cache this because a function can be moved between class instances, or have references in more than one
			// at a time. Each class has its own caching later for the output annotation.
			if ( context != null && context.isInClass() ) {
				// If we're in a class, we need to check the class output annotation
				this.canOutput = context.getThisClass().canOutput();
			}

		}

		// Default based on source type
		if ( this.canOutput == null ) {
			this.canOutput = getSourceType().equals( BoxSourceType.CFSCRIPT ) || getSourceType().equals( BoxSourceType.CFTEMPLATE ) ? true : false;
		}

		return this.canOutput;
	}

	public Boolean implementsSignature( Function func ) {
		if ( getArguments().length != func.getArguments().length ) {
			return false;
		}
		for ( int i = 0; i < getArguments().length; i++ ) {
			if ( !getArguments()[ i ].implementsSignature( func.getArguments()[ i ] ) ) {
				return false;
			}
		}
		if ( !func.getReturnType().equalsIgnoreCase( "any" ) && !getReturnType().equalsIgnoreCase( func.getReturnType() ) ) {
			return false;
		}

		return true;
	}

	public String signatureAsString() {
		StringBuilder sb = new StringBuilder();
		sb.append( getAccess().toString().toLowerCase() );
		sb.append( " " );
		sb.append( getReturnType() );
		sb.append( " function " );
		sb.append( getName().getName() );
		sb.append( "(" );
		for ( int i = 0; i < getArguments().length; i++ ) {
			if ( i > 0 ) {
				sb.append( ", " );
			}
			sb.append( getArguments()[ i ].signatureAsString() );
		}
		sb.append( ")" );
		return sb.toString();
	}
}
