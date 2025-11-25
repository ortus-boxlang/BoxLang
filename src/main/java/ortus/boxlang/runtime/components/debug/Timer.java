
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

package ortus.boxlang.runtime.components.debug;

import java.util.Set;
import java.util.UUID;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( description = "Measure execution time of code blocks", name = "Timer", allowsBody = true, requiresBody = true )
@BoxComponent( description = "Measure execution time of code blocks", name = "Stopwatch", allowsBody = true, requiresBody = true )
public class Timer extends Component {

	private static ortus.boxlang.runtime.util.Timer	bxTimer		= new ortus.boxlang.runtime.util.Timer();

	private static final IStruct					TIME_UNITS	= Struct.of(
	    Key.of( "nano" ), ortus.boxlang.runtime.util.Timer.TimeUnit.NANOSECONDS,
	    Key.of( "micro" ), ortus.boxlang.runtime.util.Timer.TimeUnit.MICROSECONDS,
	    Key.of( "milli" ), ortus.boxlang.runtime.util.Timer.TimeUnit.MILLISECONDS,
	    Key.of( "second" ), ortus.boxlang.runtime.util.Timer.TimeUnit.SECONDS
	);

	/**
	 * Enumeration of all possible `type` attribute values.
	 */
	private enum TimerType {

		DEBUG,
		DUMP,
		COMMENT,
		INLINE,
		OUTLINE;

		public static TimerType fromString( String type ) {
			return TimerType.valueOf( type.trim().toUpperCase() );
		}
	}

	/**
	 * Constructor
	 */
	public Timer() {
		super();
		// Uncomment and define declare argument to this Component
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.type, "string", Set.of( Validator.valueOneOf( "debug", "comment", "inline", "outline", "dump" ) ) ),
		    new Attribute( Key.label, "string" ),
		    new Attribute( Key.unit, "string", "milli", Set.of( Validator.valueOneOf( "nano", "micro", "milli", "second" ) ) ),
		    new Attribute( Key.variable, "string" )
		};
	}

	/**
	 * Times a block of code and outputs the result in a specified format.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @component.Stopwatch Used to time a block of code and assigns the result to a variable
	 *
	 * @component.Stopwatch.attributes.exclude type
	 *
	 * @attribute.type The type of output to generate. One of `debug`, `comment`, `inline`, or `outline` or `dump`.
	 *
	 * @attribute.label The label to use for the output.
	 *
	 * @attribute.unit The unit of time to use for the output. One of `nano`, `micro`, `milli`, or `second`.
	 *
	 * @attribute.variable The name of the variable to store the result in.
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		String										variable			= attributes.getAsString( Key.variable );
		String										timerType			= attributes.getAsString( Key.type );
		String										label				= attributes.getAsString( Key.label );
		String										precision			= attributes.getAsString( Key.unit );
		Key											precisionKey		= Key.of( precision );
		ortus.boxlang.runtime.util.Timer.TimeUnit	unit				= ( ortus.boxlang.runtime.util.Timer.TimeUnit ) TIME_UNITS.get( precisionKey );
		StringBuffer								bodyOutputBuffer	= new StringBuffer();

		// If we have a label, but no timer type, default to dump
		if ( label != null && timerType == null ) {
			timerType = "dump";
		}

		/**
		 * --------------------------------------------------------
		 * Time into a variable
		 * --------------------------------------------------------
		 */
		if ( variable != null ) {
			long timerResult = bxTimer.timeItRaw( () -> processBody( context, body, bodyOutputBuffer ), unit );
			ExpressionInterpreter.setVariable(
			    context,
			    variable,
			    timerResult
			);
			return DEFAULT_RETURN;
		}

		/**
		 * --------------------------------------------------------
		 * Time into different output types
		 * --------------------------------------------------------
		 */
		TimerType	type		= TimerType.fromString( timerType );
		String		timerResult	= bxTimer.timeIt( () -> processBody( context, body, bodyOutputBuffer ), unit );
		switch ( type ) {
			case DEBUG :
				if ( label == null ) {
					label = "Timer " + UUID.randomUUID().toString();
				}
				Object debugInfo = ExpressionInterpreter.getVariable( context, "request.debugInfo", true );
				if ( debugInfo == null ) {
					ExpressionInterpreter.setVariable( context, "request.debugInfo", new Struct() );
				}
				StructCaster.cast( ExpressionInterpreter.getVariable( context, "request.debugInfo", true ) ).put( Key.of( label ),
				    timerResult );
				context.writeToBuffer( bodyOutputBuffer.toString() );
				break;
			case DUMP :
				if ( label == null ) {
					label = "Timer " + UUID.randomUUID().toString();
				}
				IStruct result = Struct.of( Key.of( label ), timerResult );
				runtime.getFunctionService().getGlobalFunction( Key.dump ).invoke( context, new Object[] { result }, false, Key.dump );
				break;
			case COMMENT :
				context.writeToBuffer( toComment( label, timerResult, bodyOutputBuffer ) );
				break;
			case INLINE :
				context.writeToBuffer( toInline( label, timerResult, bodyOutputBuffer ) );
				break;
			case OUTLINE :
				context.writeToBuffer( toOutline( label, timerResult, bodyOutputBuffer ) );
				break;
		}

		return DEFAULT_RETURN;
	}

	/**
	 * Process a comment for the timer
	 *
	 * @param label        The label to use for the comment
	 * @param result       The result of the timer
	 * @param outputBuffer The output buffer to use for the comment
	 *
	 * @return The comment string
	 */
	private String toComment( String label, String result, StringBuffer outputBuffer ) {
		String labelText = ( label == null ) ? "" : label;
		return String.format( "<!-- %s : %s -->%s", labelText, result, outputBuffer.toString() );
	}

	/**
	 * Process an inline comment for the timer
	 *
	 * @param label        The label to use for the comment
	 * @param result       The result of the timer
	 * @param outputBuffer The output buffer to use for the comment
	 *
	 * @return The inline comment string
	 */
	private String toInline( String label, String result, StringBuffer outputBuffer ) {
		String labelText = ( label == null ) ? "" : label;
		return String.format( "%s \n %s : %s", outputBuffer.toString(), labelText, result );
	}

	/**
	 * Process an outline comment for the timer
	 *
	 * @param label        The label to use for the comment
	 * @param result       The result of the timer
	 * @param outputBuffer The output buffer to use for the comment
	 *
	 * @return The outline comment string
	 */
	private String toOutline( String label, String result, StringBuffer outputBuffer ) {
		String			labelText	= ( label == null ) ? "" : label;
		StringBuilder	htmlBuilder	= new StringBuilder( "<fieldset class=\"timer\">" );
		return htmlBuilder.append( outputBuffer.toString() )
		    .append( "<legend align=\"top\">" )
		    .append( labelText )
		    .append( ":" )
		    .append( result )
		    .append( "</legend>" )
		    .append( "</fieldset>" )
		    .toString();
	}

}
