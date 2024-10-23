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

import java.util.Set;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.util.Tracer;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF
public class Trace extends BIF {

	/**
	 * Constructor
	 */
	public Trace() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.text, Set.of( Validator.REQUIRED, Validator.NON_EMPTY ) ),
		    new Argument( false, "String", Key.category, "" ),
		    new Argument( false, "string", Key.type, "Information", Set.of( Validator.REQUIRED ) ),
		    new Argument( false, "any", Key.extrainfo, "" ),
		    new Argument( false, "boolean", Key.abort, false ),
		};
	}

	/**
	 * Traces messages into the tracing facilites so they can be display by the running runtime either in console, or debug
	 * output or whatever the runtime is configured to do.
	 * <p>
	 * It will track from where the trace call was made and the message will be logged with the category, type and text provided.
	 * <p>
	 * All tracers will be sent to the {@code trace.log} file in the logs directory of the runtime as well.
	 * <p>
	 * If the {@code abort} attribute is set to true, the current request will be aborted after the trace call.
	 * <p>
	 * Tracers will <strong>only</strong> be displayed if the runtime is in <strong>debug mode.</strong>. However, all
	 * traces will be logged regardless of the debug mode flag.
	 * <p>
	 * The {@code extraInfo} attribute is optional and can be any simple or complex object. We will convert it to string for logging
	 * using the {@code toString()} method on the object. You can also use the alias of {@code var} for this attribute as well.
	 *
	 * @param context   The context in which the Component is being invoked
	 * @param arguments The BIF Arguments
	 *
	 * @arguments.abort If true, the current request will be aborted after the trace call. Default is false.
	 *
	 * @arguments.category The category of the trace message. Default is an empty string.
	 *
	 * @arguments.text The text of the trace message. Required.
	 *
	 * @arguments.type The type of the trace message. Default is "Information".
	 *
	 * @arguments.extrainfo Any extra information to be logged with the trace message. This can be simple or a complex object. We will convert it to string for logging.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Verify if the Tracer exists in the request context, else initialize it
		context
		    .getParentOfType( RequestBoxContext.class )
		    .computeAttachmentIfAbsent( Key.bxTracers, key -> new Tracer() )
		    // Add the trace record
		    .trace(
		        arguments.getAsString( Key.category ),
		        arguments.getAsString( Key.type ),
		        arguments.getAsString( Key.text ),
		        arguments.get( Key.extrainfo ),
		        context
		    );

		// If we are aborting, then throw an abort exception.
		if ( arguments.getAsBoolean( Key.abort ) ) {
			throw new AbortException( "request", null );
		}

		return null;
	}
}
