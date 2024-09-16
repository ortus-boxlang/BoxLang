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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * A collection of utility methods for the CLI.
 */
public class CLIUtil {

	/**
	 * Parse the cli arguments into a struct of options and positional arguments
	 *
	 * @param args The cli arguments as an array
	 *
	 * @return A struct of the options and positional arguments
	 */
	public static IStruct parseArguments( String[] args ) {
		return parseArguments( new ArrayList<>( Arrays.asList( args ) ) );
	}

	/**
	 * Quick parser for options on cli arguments for BoxLang
	 * <p>
	 * This method will parse the cli arguments and return a struct of the following:
	 * <ul>
	 * <li><code>options</code> - A struct of the options</li>
	 * <li><code>positionals</code> - An array of the positional arguments</li>
	 * </ul>
	 *
	 * <p>
	 * The options can be provided in the following formats:
	 * <ul>
	 * <li><code>--option</code> - A boolean option</li>
	 * <li><code>--option=value</code> - An option with a value</li>
	 * <li><code>--option="value"</code> - An option with a quoted value</li>
	 * <li><code>--option='value'</code> - An option with a quoted value</li>
	 * <li><code>-o=value</code> - A shorthand option with a value</li>
	 * <li><code>-o</code> - A shorthand boolean option</li>
	 * <li><code>--!option</code> - A negation option</li>
	 * </ul>
	 *
	 * <p>
	 * For example, the following cli arguments:
	 *
	 * <pre>
	 * --debug --!verbose --bundles=Spec -o='/path/to/file' -v my/path/template
	 * </pre>
	 *
	 * <p>
	 * Will be parsed into the following struct:
	 *
	 * <pre>
	 * {
	 * "options" :  {
	 *    	"debug": true,
	 *   	"verbose": false,
	 *  	"bundles": "Spec",
	 * 	   	"o": "/path/to/file",
	 * 	   	"v": true
	 * },
	 * "positionals": [ "my/path/template" ]
	 * </pre>
	 *
	 * <h2>Some Ground Rules</h2>
	 * <ul>
	 * <li>Options are prefixed with --</li>
	 * <li>Shorthand options are prefixed with -</li>
	 * <li>Options can be negated with --! or --no-</li>
	 * <li>Options can have values separated by =</li>
	 * <li>Values can be quoted with single or double quotes</li>
	 * <li>Repeated options will override the previous value</li>
	 * </ul>
	 *
	 * @args The cli arguments as a list
	 *
	 * @return A struct of the options and positional arguments
	 */
	public static IStruct parseArguments( List<String> args ) {
		// The options are all the flags and name value pairs
		// The positionals are the arguments that are not flags or name value pairs
		IStruct	options			= new Struct();
		Array	positionalArgs	= new Array();

		for ( String arg : args ) {
			// Check if the argument starts with --
			if ( arg.startsWith( "--" ) ) {
				String	key;
				Object	value;

				// Handle --!key and --no-key for negation
				if ( arg.startsWith( "--!" ) || arg.startsWith( "--no-" ) ) {
					key		= arg.startsWith( "--!" ) ? arg.substring( 3 ).trim() : arg.substring( 5 ).trim();
					value	= false; // Explicitly set to false
				} else {
					String[] parts = arg.split( "=", 2 ); // Split at most into 2 parts
					key		= parts[ 0 ].substring( 2 ).trim(); // Remove the '--' prefix and trim
					// If only key is provided, set true as the value
					value	= parts.length == 1 ? true : cleanQuotedValue( parts[ 1 ].trim() );
				}
				// Insert key-value pair into options
				options.put( key, value );
			}
			// Shorthand options
			else if ( arg.startsWith( "-" ) && arg.length() > 1 ) {
				// Handle multiple shorthand options combined (e.g., -abc)
				for ( int i = 1; i < arg.length(); i++ ) {
					String	key		= String.valueOf( arg.charAt( i ) );
					Object	value	= true; // Default shorthand options to true unless specified

					// Check if there's a value after the shorthand (e.g., -o=output.txt)
					if ( i == 1 && arg.length() > 2 && arg.charAt( 2 ) == '=' ) {
						value = cleanQuotedValue( arg.substring( 3 ).trim() ); // Extract the value after = and clean quotes
						options.put( key, value );
						break;
					}
					// Insert the shorthand key-value pair into options
					options.put( key, value );
				}
			} else {
				// Handle positional arguments
				positionalArgs.add( arg );
			}
		}

		return Struct.of( "options", options, "positionals", positionalArgs );
	}

	/**
	 * Helper method to clean up quoted values
	 *
	 * @param value The value to clean
	 *
	 * @return The cleaned value
	 */
	private static String cleanQuotedValue( String value ) {
		if ( ( value.startsWith( "\"" ) && value.endsWith( "\"" ) ) || ( value.startsWith( "'" ) && value.endsWith( "'" ) ) ) {
			return value.substring( 1, value.length() - 1 ); // Remove surrounding quotes
		}
		return value; // Return the original if no quotes are found
	}

}
