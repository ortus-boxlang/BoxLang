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
package ortus.boxlang.runtime.bifs.global.cli;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

@BoxBIF( description = "Get command line arguments" )
public class CLIGetArgs extends BIF {

	/**
	 * Constructor
	 */
	public CLIGetArgs() {
		super();
	}

	/**
	 * Parses the command line arguments and returns a struct with the following keys:
	 * <ul>
	 * <li><code>options</code> - A struct of the options</li>
	 * <li><code>positionals</code> - An array of the positional arguments</li>
	 * </ul>
	 * When running a script with the <code>boxlang</code> binary, the script path is also the first positional argument.
	 * If the runtime has no CLI options (for example when embedded), a struct with an empty options struct and positionals array is returned.
	 * <p>
	 * The options can be provided in the following formats:
	 * <ul>
	 * <li><code>--option</code> - A boolean option</li>
	 * <li><code>--option=value</code> - An option with a value</li>
	 * <li><code>--option="value"</code> - An option with a quoted value</li>
	 * <li><code>--option='value'</code> - An option with a quoted value</li>
	 * <li><code>-o=value</code> - A shorthand option with a value</li>
	 * <li><code>-o</code> - A shorthand boolean option</li>
	 * <li><code>--!option</code> or <code>--no-option</code> - A negated option</li>
	 * </ul>
	 * <p>
	 * For example, the following CLI arguments:
	 *
	 * <pre>
	 * --debug --!verbose --bundles=Spec -o='/path/to/file' -v my/path/template
	 * </pre>
	 *
	 * Are parsed into the following struct:
	 *
	 * <pre>
	 * {
	 *     "options": {
	 *         "debug": true,
	 *         "verbose": false,
	 *         "bundles": "Spec",
	 *         "o": "/path/to/file",
	 *         "v": true
	 *     },
	 *     "positionals": [ "my/path/template" ]
	 * }
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
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return A struct with the options and positionals keys.
	 */
	public IStruct _invoke( IBoxContext context, ArgumentsScope arguments ) {
		if ( runtime.getCliOptions() == null ) {
			return Struct.of( "options", new Struct(), "positionals", new Array() );
		}

		return runtime.getCliOptions().parseArguments();
	}

}
