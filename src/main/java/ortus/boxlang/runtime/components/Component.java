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

import java.util.Optional;

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

	public static final Optional<Object> DEFAULT_RETURN = Optional.empty();

	@FunctionalInterface
	public static interface ComponentBody {

		Optional<Object> process( IBoxContext context );
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
	 * Flag to capture buffered output from the body
	 */
	protected boolean				captureBodyOutput	= false;

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
	 * A base component
	 *
	 * @param name The name of the component
	 */
	protected Component( Key name ) {
		this.name = name;
	}

	/**
	 * Invoke the BIF with the given arguments. This is a wrapper method that sets up the execution.
	 *
	 * @param context   The context in which the BIF is being invoked
	 * @param arguments The arguments to the BIF
	 *
	 * @return The result of the invocation
	 */
	public Optional<Object> invoke( IBoxContext context, IStruct attributes, ComponentBody body ) {
		validateAttributes( attributes );
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
	 * Invoke the BIF with the given arguments. This is the actual method that must be implemented by the component.
	 *
	 * @param context   The context in which the BIF is being invoked
	 * @param arguments The arguments to the BIF
	 *
	 * @return The result of the invocation
	 */
	public abstract Optional<Object> _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState );

	/**
	 * Announce an event with the provided {@link IStruct} of data.
	 *
	 * @param state The state key to announce
	 * @param data  The data to announce
	 */
	public void announce( Key state, IStruct data ) {
		interceptorService.announce( state, data );
	}

	public void validateAttributes( IStruct attributes ) {
		// Do nothing by default. Override this method to provide validation
	}

	/**
	 * Process the body of the component.
	 *
	 * @param context The context in which the body is being processed
	 *
	 * @return If captureBodyOutput is set to true, the captured output of the body will be returned as a string. Otherwise,
	 *         null will be returned.
	 */
	public BodyResult processBody( IBoxContext context, ComponentBody body ) {
		String				bufferResult	= null;
		Optional<Object>	returnValue		= DEFAULT_RETURN;
		if ( body != null ) {
			// If we want to capture generated output, then we need to buffer it
			if ( captureBodyOutput ) {
				// Register a new buffer with the context
				StringBuffer buffer = new StringBuffer();
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
				// Get the generated content from the buffer and return it
				bufferResult = buffer.toString();
			} else {
				returnValue = body.process( context );
			}

		}
		return new BodyResult( bufferResult, returnValue );
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

	/**
	 * Get whether this component captures the output of the body
	 *
	 * @return Whether this component captures the output of the body
	 */
	public boolean capturesBodyOutput() {
		return captureBodyOutput;
	}

	public record BodyResult( String buffer, Optional<Object> returnValue ) {
	}

}
