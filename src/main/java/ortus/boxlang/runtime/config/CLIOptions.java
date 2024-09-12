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

import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.immutable.ImmutableStruct;
import ortus.boxlang.runtime.util.CLIUtil;

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
		return CLIUtil.parseArguments( cliArgs() );
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
