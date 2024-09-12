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
package ortus.boxlang.runtime.config;

import java.util.List;

import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.immutable.ImmutableStruct;

/**
 * Command-line options for the BoxLang runtime.
 *
 * @param templatePath  The path to the template to execute. Can be a class or template. Mutally exclusive with code
 * @param debug         Whether or not to run in debug mode. It can be `null` if not specified
 * @param code          The source code to execute, if any
 * @param configFile    The path to the config file to use
 * @param printAST      Whether or not to print the AST of the source code
 * @param transpile     Whether or not to transpile the source code to Java
 * @param runtimeHome   The path to the runtime home
 * @param showVersion   Whether or not to show the version of the runtime
 * @param cliArgs       The arguments to pass to the template or class
 * @param cliArgsRaw    The raw arguments passed to the CLI
 * @param targetModule  The module to execute
 * @param actionCommand The action command to execute
 */
public record CLIOptions(
    String templatePath,
    Boolean debug,
    String code,
    String configFile,
    Boolean printAST,
    Boolean transpile,
    String runtimeHome,
    Boolean showVersion,
    List<String> cliArgs,
    String[] cliArgsRaw,
    String targetModule,
    String actionCommand ) {
	// The record automatically generates the constructor, getters, equals, hashCode, and toString methods.

	/**
	 * Wether or not this is an action command: Ex: <code>boxlang compile</code>
	 *
	 * @return True if it is an action command, false otherwise
	 */
	public Boolean isActionCommand() {
		return actionCommand != null;
	}

	/**
	 * Verifies if debug mode was enabled
	 *
	 * @return True if debug mode is enabled, false otherwise
	 */
	public boolean isDebugMode() {
		return Boolean.TRUE.equals( debug() );
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
	 * @return A struct of the options and positional arguments
	 */
	public IStruct parseArguments() {
		// The options are all the flags and name value pairs
		// The positionals are the arguments that are not flags or name value pairs
		IStruct	options			= new Struct();
		Array	positionalArgs	= new Array();

		for ( String arg : cliArgs() ) {
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
	private String cleanQuotedValue( String value ) {
		if ( ( value.startsWith( "\"" ) && value.endsWith( "\"" ) ) || ( value.startsWith( "'" ) && value.endsWith( "'" ) ) ) {
			return value.substring( 1, value.length() - 1 ); // Remove surrounding quotes
		}
		return value; // Return the original if no quotes are found
	}

	/**
	 * Get the state representation of this record
	 *
	 * @return The state representation of this record as a struct
	 */
	public IStruct toStruct() {
		return ImmutableStruct.of(
		    "actionCommand", actionCommand(),
		    "cliArgs", cliArgs(),
		    "cliArgsRaw", cliArgsRaw(),
		    "code", code(),
		    "configFile", configFile(),
		    "debug", debug(),
		    "printAST", printAST(),
		    "runtimeHome", runtimeHome(),
		    "showVersion", showVersion(),
		    "targetModule", targetModule(),
		    "templatePath", templatePath(),
		    "transpile", transpile()
		);
	}

}
