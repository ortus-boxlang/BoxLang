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
package ortus.boxlang.debugger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DebugMain {

	static CLIOptions cliArgs;

	public static void main( String... args ) {
		cliArgs = parseCommandLineOptions( args );

		DebugAdapter.startDAPServer( cliArgs.DAPPort );
	}

	private static CLIOptions parseCommandLineOptions( String[] args ) {
		List<String>	argsList	= new ArrayList<>( Arrays.asList( args ) );
		// Initialize options with defaults
		int				DAPPort		= 0;
		String			current		= null;

		// Consume args in order
		// Example: --debug
		while ( !argsList.isEmpty() ) {
			current = argsList.remove( 0 );
			// Debug mode Flag, we find and continue to the next argument
			if ( current.equalsIgnoreCase( "--DAPPort" ) ) {
				DAPPort = Integer.parseInt( argsList.remove( 0 ) );
				continue;
			}
		}

		return new CLIOptions( DAPPort );
	}

	public record CLIOptions(
	    int DAPPort ) {
	}
}
