
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF

public class SystemExecute extends BIF {

	/**
	 * Constructor
	 */
	public SystemExecute() {
		super();
		// Uncomment and define declare argument to this BIF
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.of( "name" ) ),
		    new Argument( false, "any", Key.arguments ),
		    new Argument( false, "long", Key.timeout ),
		    new Argument( false, "boolean", Key.terminateOnTimeout, false ),
		    new Argument( false, "string", Key.directory ),
		    new Argument( false, "string", Key.output ),
		    new Argument( false, "string", Key.error ),
		};
	}

	/**
	 * Executes a system process/command on the underlying OS. Returns a struct with the following keys:
	 *
	 * {
	 * output : [ the command output ]
	 * error : [ any errors emitted by the command ]
	 * timeout : [ boolean value as to whether a timeout was reached ]
	 * terminated : [ boolean value as to whether the process was terminated ]
	 * pid : the PID of the process
	 * }
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.name The process name or binary path ( e.g. bash or /bin/sh )
	 *
	 * @argument.arguments The process arguments ( e.g. for `java --version` this would be `--version` )
	 *
	 * @argument.timeout The timeout to wait for the command, in seconds ( default unlimited )
	 *
	 * @argument.terminateOnTimeout Whether to terminate the process/command if a timeout is reached
	 *
	 * @argument.directory A working directory to execute the command from
	 *
	 * @argument.ouptut An optional file path to write the command output to
	 *
	 * @argument.error An optional file path to write errors to
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String				bin					= arguments.getAsString( Key.of( "name" ) );
		Object				args				= arguments.get( Key.arguments );
		String				directory			= arguments.getAsString( Key.directory );
		Long				timeout				= arguments.getAsLong( Key.timeout );
		Boolean				terminateOnTimeout	= arguments.getAsBoolean( Key.terminateOnTimeout );
		String				outputTarget		= arguments.getAsString( Key.output );
		String				errorTarget			= arguments.getAsString( Key.error );

		ArrayList<String>	cmd					= new ArrayList<String>( 1 );
		Struct				response			= new Struct( new HashMap<Key, Object>() {

													{
														put( Key.output, null );
														put( Key.error, null );
														put( Key.timeout, false );
														put( Key.terminated, false );
														put( Key.pid, null );
													}
												} );

		cmd.add( bin );
		if ( args instanceof String ) {
			// ensure we preserve any spaces in
			Matcher matches = Pattern.compile( "[^\\s\"']+|\"[^\"]*\"|'[^']*'" ).matcher( StringCaster.cast( args ) );
			matches.reset();
			while ( matches.find() )
				cmd.add( matches.group() );
		} else if ( args instanceof Array ) {
			ArrayCaster.cast( args ).stream().forEach( arg -> cmd.add( StringCaster.cast( arg ) ) );
		} else {
			throw new BoxRuntimeException(
			    String.format(
			        "The provided process arguments provided [%s] could not be parsed in to command arguments",
			        args.toString()
			    )
			);
		}

		ProcessBuilder processBuilder = new ProcessBuilder( cmd );

		if ( directory != null ) {
			processBuilder.directory( Path.of( directory ).toFile() );
		}

		if ( outputTarget != null ) {
			processBuilder.redirectOutput( Path.of( outputTarget ).toFile() );
		}
		if ( errorTarget != null ) {
			processBuilder.redirectError( Path.of( errorTarget ).toFile() );
		}

		Process process = null;

		try {
			process = processBuilder.start();
			response.put( Key.pid, process.pid() );
			if ( timeout != null && timeout != 0l ) {
				process.waitFor( timeout, TimeUnit.SECONDS );
				response.put( Key.timeout, true );
				if ( terminateOnTimeout && process.isAlive() ) {
					process.destroy();
					response.put( Key.terminated, true );
				}
			} else {
				process.waitFor();
			}

			if ( !response.getAsBoolean( Key.terminated ) ) {
				if ( process != null && outputTarget == null && process.getInputStream() != null ) {
					response.put( Key.output, process.inputReader().lines().collect( Collectors.joining( "\n" ) ) );
				}
				if ( process != null && errorTarget == null && process.getErrorStream() != null ) {
					response.put( Key.error, process.errorReader().lines().collect( Collectors.joining( "\n" ) ) );
				}
			}

			return response;

		} catch ( IOException e ) {
			throw new BoxRuntimeException(
			    String.format( "An exception occurred while attempting to execute the statement [%s]", StringUtils.join( cmd, " " ) ),
			    e
			);
		} catch ( InterruptedException ie ) {
			throw new BoxRuntimeException(
			    "The process was interrupted while waiting for the command to complete",
			    "InterruptedException",
			    ie
			);
		}

	}

}
