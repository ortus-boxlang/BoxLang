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
package ortus.boxlang.runtime.util;

import java.time.Instant;
import java.util.Queue;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentLinkedQueue;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.ApplicationBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.logging.LogLevel;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;

/**
 * This class implements the tracing utilities in BoxLang.
 * <p>
 * Tracing allows you to add trace points across code that will be tracked during a specific request.
 * <p>
 * Each runtime (cli, lambda, server, web, android, etc) will determine how best to visualize the tracers.
 * <p>
 * Example: The cli and web server will output the tracers at the end of the request.
 * <p>
 * The components and bif entry points, will make sure this transient class get's placed into the running
 * request context as an attachment.
 */
public class Tracer {

	/**
	 * The queue of trace records.
	 */
	private final Queue<TracerRecord> traceRecords = new ConcurrentLinkedQueue<>();

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	public Tracer() {
		// Empty constructor
	}

	/**
	 * --------------------------------------------------------------------------
	 * Public Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Traces messages into the trace log and facilites so they can be display by the running runtime either in console, or debug
	 * output or whatever the runtime is configured to do.
	 * <p>
	 * It will track from where the trace call was made and the message will be logged with the category, type and text provided.
	 * <p>
	 * All tracers will be sent to the {@code trace.log} file in the logs directory of the runtime.
	 * <p>
	 * If the {@code abort} attribute is set to true, the current request will be aborted after the trace call.
	 * <p>
	 * Tracers will only be displayed if the runtime is in debug mode.
	 *
	 * @param category  The category of the trace
	 * @param type      The type of the trace
	 * @param text      The text of the trace
	 * @param extraInfo Any extra information to attach to the trace
	 * @param context   The context in which the trace is being invoked
	 */
	public void trace( String category, String type, String text, Object extraInfo, IBoxContext context ) {
		String					logType			= LogLevel.valueOf( type.trim(), false ).getName();
		String					extraAsString	= extraToString( extraInfo );
		String					positionInCode	= ExceptionUtil.getCurrentPositionInCode();
		String					applicationName	= "uknown-application";

		// Do we have an app name?
		ApplicationBoxContext	appContext		= context.getParentOfType( ApplicationBoxContext.class );
		if ( appContext != null ) {
			applicationName = appContext.getApplication().getName().getName();
		}

		// Create a new record
		this.traceRecords.add(
		    new TracerRecord(
		        applicationName,
		        Instant.now(),
		        positionInCode,
		        category,
		        logType,
		        text,
		        extraAsString
		    )
		);

		// TracerMessage
		// [applicationName] [category] [positionInCode] [text] [extraAsString]
		String traceMessage = new StringJoiner( " " )
		    .add( "[" ).add( applicationName ).add( "]" )
		    .add( "[" ).add( category ).add( "]" )
		    .add( "[" ).add( positionInCode ).add( "]" )
		    .add( "[" ).add( text ).add( "]" )
		    .add( "[" ).add( extraAsString ).add( "]" )
		    .toString();

		// Send to the logging facilities
		BoxRuntime.getInstance()
		    .getInterceptorService()
		    .announce(
		        BoxEvent.LOG_MESSAGE,
		        Struct.of(
		            // Log Message as text
		            Key.text, traceMessage,
		            // Log file
		            Key.log, "trace",
		            // Log type
		            Key.type, logType
		        )
		    );
	}

	/**
	 * Get the trace records.
	 *
	 * @return The trace records
	 */
	public Queue<ortus.boxlang.runtime.util.Tracer.TracerRecord> getTracers() {
		return this.traceRecords;
	}

	/**
	 * Count the number of trace records.
	 */
	public int count() {
		return this.traceRecords.size();
	}

	/**
	 * Reset the tracers.
	 */
	public Tracer reset() {
		this.traceRecords.clear();
		return this;
	}

	/**
	 * Helper to convert an object to a string.
	 *
	 * @param extraInfo The object to convert to a string
	 */
	private static String extraToString( Object extraInfo ) {
		if ( extraInfo == null ) {
			return "[null]";
		}

		// If it's a BoxLang type, let's use the string representation
		if ( extraInfo instanceof IType t ) {
			return t.asString();
		}

		// Default to the toString method
		return extraInfo.toString();
	}

	/**
	 * Create a tracer record that will be tracked.
	 */
	public record TracerRecord(
	    String applicationName,
	    Instant tracedAt,
	    String traceLocation,
	    String category,
	    String type,
	    String text,
	    String extraInfo ) {
	}
}
