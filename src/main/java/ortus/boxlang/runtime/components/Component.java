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
package ortus.boxlang.runtime.components;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * Base class for all Components. Components are invoked by the runtime from a template or script.
 */
public abstract class Component {

	public static final BodyResult DEFAULT_RETURN = BodyResult.ofDefault();

	@FunctionalInterface
	public static interface ComponentBody {

		BodyResult process( IBoxContext context );
	}

	/**
	 * The name of the component
	 */
	protected Key					name;

	/**
	 * Component Attributes
	 */
	protected Attribute[]			declaredAttributes	= new Attribute[] {};

	/**
	 * The runtime instance
	 */
	protected BoxRuntime			runtime				= BoxRuntime.getInstance();

	/**
	 * The component service helper
	 */
	// protected ComponentService componentService = BoxRuntime.getInstance().getComponentService();

	/**
	 * The interceptor service helper
	 */
	protected InterceptorService	interceptorService	= BoxRuntime.getInstance().getInterceptorService();

	/**
	 * A base component
	 */
	protected Component() {

	}

	/**
	 * Set name
	 * 
	 * @param name The name of the component
	 * 
	 * @return this
	 */
	public Component setName( Key name ) {
		this.name = name;
		return this;
	}

	/**
	 * Invoke the Component with the given arguments. This is a wrapper method that sets up the execution.
	 *
	 * @param context   The context in which the Component is being invoked
	 * @param arguments The arguments to the Component
	 *
	 * @return The result of the invocation
	 */
	public BodyResult invoke( IBoxContext context, IStruct attributes, ComponentBody body ) {
		validateAttributes( context, attributes );
		IStruct executionState = new Struct();
		executionState.put( Key._NAME, name );
		executionState.put( Key._CLASS, this.getClass() );
		executionState.put( Key.attributes, attributes );
		context.pushComponent( executionState );
		try {
			return _invoke( context, attributes, body, executionState );
		} finally {
			context.popComponent();
		}

	}

	/**
	 * Invoke the Component with the given arguments. This is the actual method that must be implemented by the component.
	 *
	 * @param context   The context in which the Component is being invoked
	 * @param arguments The arguments to the Component
	 *
	 * @return The result of the invocation
	 */
	public abstract BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState );

	/**
	 * Announce an event with the provided {@link IStruct} of data.
	 *
	 * @param state The state key to announce
	 * @param data  The data to announce
	 */
	public void announce( Key state, IStruct data ) {
		interceptorService.announce( state, data );
	}

	public void validateAttributes( IBoxContext context, IStruct attributes ) {
		// Do nothing by default. Override this method to provide validation
	}

	/**
	 * Process the body of the component and not do capture output.
	 *
	 * @param context The context in which the body is being processed
	 * @param body    The body to process
	 *
	 * @return A BodyResult object which describes the result of the body processing
	 */
	public BodyResult processBody( IBoxContext context, ComponentBody body ) {
		return processBody( context, body, null );
	}

	/**
	 * Process the body of the component while capturing the output into a provided buffer.
	 *
	 * @param context The context in which the body is being processed
	 * @param body    The body to process
	 * @param buffer  The buffer to capture the output into
	 *
	 * @return A BodyResult object which describes the result of the body processing
	 */
	public BodyResult processBody( IBoxContext context, ComponentBody body, StringBuffer buffer ) {
		String		bufferResult	= null;
		BodyResult	returnValue		= DEFAULT_RETURN;
		if ( body != null ) {
			// If we want to capture generated output, then we need to buffer it
			if ( buffer != null ) {
				// Register the buffer with the context
				context.pushBuffer( buffer );
				try {
					returnValue = body.process( context );
				} catch ( Throwable e ) {
					// If there is an error, we flush out all content regardless and rethrow
					bufferResult = buffer.toString();
					context.writeToBuffer( bufferResult );
					throw e;
				} finally {
					context.popBuffer();
				}
			} else {
				returnValue = body.process( context );
			}

		}
		return returnValue;
	}

	/**
	 * Get the name of the component
	 *
	 * @return The name of the component
	 */
	public Key getName() {
		return name;
	}

	/**
	 * Get the attributes for this component
	 *
	 * @return The attributes for this component
	 */
	public Attribute[] getDeclaredAttributes() {
		return declaredAttributes;
	}

	public record BodyResult( int resultType, Object returnValue ) {

		public static final int DEFAULT = 0;
		public static final int RETURN = 1;
		public static final int BREAK = 2;
		public static final int CONTINUE = 3;

		public static BodyResult ofBreak() {
			return new BodyResult( BREAK, null );
		}

		public static BodyResult ofContinue() {
			return new BodyResult( CONTINUE, null );
		}

		public static BodyResult ofReturn( Object returnValue ) {
			return new BodyResult( RETURN, returnValue );
		}

		public static BodyResult ofDefault() {
			return new BodyResult( DEFAULT, null );
		}

		public boolean isBreak() {
			return resultType == BREAK;
		}

		public boolean isContinue() {
			return resultType == CONTINUE;
		}

		public boolean isReturn() {
			return resultType == RETURN;
		}

		public boolean isEarlyExit() {
			return isBreak() || isContinue() || isReturn();
		}
	}

}
