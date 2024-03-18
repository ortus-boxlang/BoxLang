package ortus.boxlang.debugger;

import java.util.Arrays;
import java.util.List;

public class DebugMain {

	static CLIOptions cliArgs;

	public static void main( String... args ) {
		cliArgs = parseCommandLineOptions( args );

		DebugAdapter.startDAPServer( cliArgs.DAPPort );
	}

	private static CLIOptions parseCommandLineOptions( String[] args ) {
		List<String>	argsList	= Arrays.asList( args );
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
