
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

@BoxComponent( name = "Timer", allowsBody = true, requiresBody = true )
@BoxComponent( name = "Stopwatch", allowsBody = true, requiresBody = true )
public class Timer extends Component {

	private static ortus.boxlang.runtime.util.Timer	timer		= new ortus.boxlang.runtime.util.Timer();

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
		    new Attribute( Key.type, "string", Set.of( Validator.valueOneOf( "debug", "comment", "inline", "outline" ) ) ),
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
	 * @attribute.type The type of output to generate. One of `debug`, `comment`, `inline`, or `outline`.
	 *
	 * @attributes.label The label to use for the output.
	 *
	 * @attributes.unit The unit of time to use for the output. One of `nano`, `micro`, `milli`, or `second`.
	 *
	 * @attributes.variable The name of the variable to store the result in.
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		String										variable			= attributes.getAsString( Key.variable );
		String										label				= attributes.getAsString( Key.label );
		String										precision			= attributes.getAsString( Key.unit );
		Key											precisionKey		= Key.of( precision );
		ortus.boxlang.runtime.util.Timer.TimeUnit	unit				= ( ortus.boxlang.runtime.util.Timer.TimeUnit ) TIME_UNITS.get( precisionKey );
		StringBuffer								bodyOutputBuffer	= new StringBuffer();

		if ( variable != null ) {
			long timerResult = timer.timeItRaw( () -> processBody( context, body, bodyOutputBuffer ), unit );
			ExpressionInterpreter.setVariable(
			    context,
			    variable,
			    timerResult
			);
		} else if ( attributes.getAsString( Key.label ) != null && attributes.getAsString( Key.type ) == null ) {
			String	timerResult	= timer.timeIt( () -> processBody( context, body, bodyOutputBuffer ) );
			IStruct	result		= Struct.of( Key.of( attributes.getAsString( Key.label ) ), timerResult );
			runtime.getFunctionService().getGlobalFunction( Key.dump ).invoke( context, new Object[] { result }, false, Key.dump );
		} else {
			TimerType	type		= TimerType.fromString( attributes.getAsString( Key.type ) );
			String		timerResult	= timer.timeIt( () -> processBody( context, body, bodyOutputBuffer ),
			    unit );
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
		}

		return DEFAULT_RETURN;
	}

	private String toComment( String label, String result, StringBuffer outputBuffer ) {
		if ( label == null ) {
			label = "";
		}
		return "<!-- " + label + " : " + result + " -->" + outputBuffer.toString();
	}

	private String toInline( String label, String result, StringBuffer outputBuffer ) {
		if ( label == null ) {
			label = "";
		}
		String labelOutput = label + " : " + result;

		return outputBuffer.toString() + "\n" + labelOutput;
	}

	private String toOutline( String label, String result, StringBuffer outputBuffer ) {
		if ( label == null ) {
			label = "";
		}
		String labelOutput = label + " : " + result;

		return "<fieldset class=\"timer\">" +
		    outputBuffer.toString() +
		    "<legend align=\"top\">" + labelOutput + "</legend>" +
		    "</fieldset>";
	}

}
